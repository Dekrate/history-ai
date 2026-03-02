package com.historyai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Client for interacting with the Ollama API.
 * Provides text generation capabilities using local LLM models.
 */
@Component
public class OllamaClient {

    private static final Logger LOG = LoggerFactory.getLogger(OllamaClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String defaultModel;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;

    /**
     * Constructs a new OllamaClient with the specified base URL and REST template builder.
     */
    public OllamaClient(
            @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${spring.ai.ollama.chat.options.model:SpeakLeash/bielik-11b-v3.0-instruct:bf16}") String defaultModel,
            @Value("${app.ai.ollama.connect-timeout-seconds:30}") int connectTimeoutSeconds,
            @Value("${app.ai.ollama.read-timeout-seconds:240}") int readTimeoutSeconds,
            RestTemplateBuilder restTemplateBuilder) {
        this.baseUrl = baseUrl;
        this.defaultModel = defaultModel;
        this.objectMapper = new ObjectMapper();
        this.connectTimeoutMillis = Math.max(1, connectTimeoutSeconds) * 1000;
        this.readTimeoutMillis = Math.max(1, readTimeoutSeconds) * 1000;
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofMillis(connectTimeoutMillis))
                .readTimeout(Duration.ofMillis(readTimeoutMillis))
                .build();
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    /**
     * Generates a response from the Ollama model using the default model.
     */
    public String generate(String prompt) {
        return generate(defaultModel, prompt);
    }

    /**
     * Generates a response from the Ollama model using the provided prompt.
     */
    public String generate(String model, String prompt) {
        LOG.debug("Generating response with model: {}", model);

        String url = baseUrl + "/api/generate";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String requestBody = String.format(
                "{\"model\": \"%s\", \"prompt\": \"%s\", \"stream\": false}",
                model,
                escapeJson(prompt)
        );
        
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        
        try {
            String response = restTemplate.postForObject(url, request, String.class);
            
            LOG.debug("Raw Ollama response: {}", response);
            
            if (response != null) {
                return parseNdjsonResponse(response);
            }
            return "";
        } catch (Exception e) {
            LOG.error("Error calling Ollama: {}", e.getMessage());
            if (isTimeoutError(e)) {
                throw new OllamaTimeoutException("Ollama request timed out", e);
            }
            throw new OllamaApiException("Failed to generate response from Ollama", e);
        }
    }

    /**
     * Generates a streaming response from the Ollama model.
     */
    public void generateStream(String model, String prompt, Consumer<String> consumer) {
        LOG.debug("Generating streaming response with model: {}", model);
        HttpURLConnection conn = null;

        try {
            URL url = new URL(baseUrl + "/api/generate");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(connectTimeoutMillis);
            conn.setReadTimeout(readTimeoutMillis);

            String requestBody = String.format(
                    "{\"model\": \"%s\", \"prompt\": \"%s\", \"stream\": true}",
                    model,
                    escapeJson(prompt)
            );

            try (OutputStream outputStream = conn.getOutputStream()) {
                outputStream.write(requestBody.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }

            int status = conn.getResponseCode();
            boolean success = status >= 200 && status < 300;
            InputStream responseStream = success ? conn.getInputStream() : conn.getErrorStream();
            if (responseStream == null) {
                throw new OllamaApiException("Ollama returned HTTP " + status + " with empty body", null);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream, StandardCharsets.UTF_8))) {
                if (!success) {
                    throw new OllamaApiException(
                            "Ollama returned HTTP " + status + ": " + readAll(reader),
                            null
                    );
                }

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    try {
                        JsonNode node = objectMapper.readTree(line);
                        JsonNode responseNode = node.get("response");
                        if (responseNode != null) {
                            consumer.accept(responseNode.asText());
                        }
                        if (node.has("done") && node.get("done").asBoolean()) {
                            break;
                        }
                    } catch (Exception e) {
                        LOG.debug("Error parsing line: {}", line);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Error calling Ollama streaming: {}", e.getMessage());
            if (isTimeoutError(e)) {
                throw new OllamaTimeoutException("Ollama streaming request timed out", e);
            }
            throw new OllamaApiException("Failed to generate streaming response from Ollama", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String readAll(BufferedReader reader) throws IOException {
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (body.length() > 0) {
                body.append('\n');
            }
            body.append(line);
        }
        return body.toString();
    }

    private String parseNdjsonResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode responseNode = root.get("response");
            if (responseNode != null) {
                return responseNode.asText();
            }
        } catch (Exception e) {
            LOG.warn("Error parsing response: {}", e.getMessage());
        }
        return "";
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private boolean isTimeoutError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof HttpTimeoutException
                    || current instanceof SocketTimeoutException) {
                return true;
            }
            if (current instanceof ResourceAccessException
                    && current.getMessage() != null
                    && current.getMessage().toLowerCase().contains("timed out")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    public static class OllamaApiException extends RuntimeException {
        public OllamaApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class OllamaTimeoutException extends OllamaApiException {
        public OllamaTimeoutException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

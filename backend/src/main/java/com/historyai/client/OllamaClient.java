package com.historyai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.function.Consumer;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
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

    /**
     * Constructs a new OllamaClient with the specified base URL and REST template builder.
     */
    public OllamaClient(
            @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${spring.ai.ollama.chat.options.model:SpeakLeash/bielik-11b-v3.0-instruct:bf16}") String defaultModel,
            RestTemplateBuilder restTemplateBuilder) {
        this.baseUrl = baseUrl;
        this.defaultModel = defaultModel;
        this.objectMapper = new ObjectMapper();
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofSeconds(120))
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
            throw new OllamaApiException("Failed to generate response from Ollama", e);
        }
    }

    /**
     * Generates a streaming response from the Ollama model.
     */
    public void generateStream(String model, String prompt, Consumer<String> consumer) {
        LOG.debug("Generating streaming response with model: {}", model);

        try {
            URL url = new URL(baseUrl + "/api/generate");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(120000);

            String requestBody = String.format(
                    "{\"model\": \"%s\", \"prompt\": \"%s\", \"stream\": true}",
                    model,
                    escapeJson(prompt)
            );

            conn.getOutputStream().write(requestBody.getBytes(StandardCharsets.UTF_8));
            conn.getOutputStream().flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            String line;
            StringBuilder fullResponse = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                try {
                    JsonNode node = objectMapper.readTree(line);
                    JsonNode responseNode = node.get("response");
                    if (responseNode != null) {
                        String text = responseNode.asText();
                        consumer.accept(text);
                        fullResponse.append(text);
                    }
                    if (node.has("done") && node.get("done").asBoolean()) {
                        break;
                    }
                } catch (Exception e) {
                    LOG.debug("Error parsing line: {}", line);
                }
            }
            reader.close();
            conn.disconnect();

        } catch (Exception e) {
            LOG.error("Error calling Ollama streaming: {}", e.getMessage());
            throw new OllamaApiException("Failed to generate streaming response from Ollama", e);
        }
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

    public static class OllamaApiException extends RuntimeException {
        public OllamaApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

package com.historyai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
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
     *
     * @param baseUrl the base URL of the Ollama API
     * @param defaultModel the default model name from configuration
     * @param restTemplateBuilder the REST template builder for configuring timeouts
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

    /**
     * Generates a response from the Ollama model using the default model from configuration.
     *
     * @param prompt the prompt to send to the model
     * @return the generated response text
     * @throws OllamaApiException if the API call fails
     */
    public String generate(String prompt) {
        return generate(defaultModel, prompt);
    }

    /**
     * Generates a response from the Ollama model using the provided prompt.
     *
     * @param model the model name to use
     * @param prompt the prompt to send to the model
     * @return the generated response text
     * @throws OllamaApiException if the API call fails
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
     * Parses JSON response to extract the response text.
     *
     * @param json the JSON response string
     * @return the extracted response text
     */
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

    /**
     * Escapes special characters for JSON string.
     */
    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Unescapes special characters in JSON string.
     */
    private String unescapeJson(String s) {
        return s.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
    
    /**
     * Exception thrown when Ollama API call fails.
     */
    public static class OllamaApiException extends RuntimeException {
        public OllamaApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

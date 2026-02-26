package com.historyai.client;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
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
            @Value("${spring.ai.ollama.chat.options.model:bielik}") String defaultModel,
            RestTemplateBuilder restTemplateBuilder) {
        this.baseUrl = baseUrl;
        this.defaultModel = defaultModel;
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
     * @param model the model name to use (e.g., "llama3.2:3b")
     * @param prompt the prompt to send to the model
     * @return the generated response text
     * @throws OllamaApiException if the API call fails
     */
    public String generate(String model, String prompt) {
        LOG.debug("Generating response with model: {}", model);

        String url = baseUrl + "/api/generate";
        
        var request = new GenerateRequest(model, prompt);
        
        try {
            GenerateResponse response = restTemplate.postForObject(
                    url, request, GenerateResponse.class);
            
            if (response != null && response.response() != null) {
                return response.response();
            }
            return "";
        } catch (Exception e) {
            LOG.error("Error calling Ollama: {}", e.getMessage());
            throw new OllamaApiException("Failed to generate response from Ollama", e);
        }
    }

    /**
     * Request payload for Ollama generate API.
     */
    public record GenerateRequest(String model, String prompt) {}
    
    /**
     * Response payload from Ollama generate API.
     */
    public record GenerateResponse(String model, String response, boolean done) {}
    
    /**
     * Exception thrown when Ollama API call fails.
     */
    public static class OllamaApiException extends RuntimeException {
        public OllamaApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

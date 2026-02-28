package com.historyai.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OllamaClientTest {

    @Test
    void generate_WithValidResponse_ShouldReturnResponse() {
        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        
        when(builder.connectTimeout(any(Duration.class))).thenReturn(builder);
        when(builder.readTimeout(any(Duration.class))).thenReturn(builder);
        when(builder.rootUri(anyString())).thenReturn(builder);
        when(builder.defaultHeader(anyString(), anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(restTemplate);
        
        String jsonResponse = "{\"model\":\"test-model\",\"response\":\"Test response\",\"done\":true}";
        when(restTemplate.postForObject(anyString(), any(Object.class), eq(String.class)))
                .thenReturn(jsonResponse);

        OllamaClient client = new OllamaClient("http://localhost:11434", "test-model", builder);
        String result = client.generate("Test prompt");

        assertEquals("Test response", result);
    }

    @Test
    void generate_WithEmptyResponse_ShouldReturnEmptyString() {
        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        
        when(builder.connectTimeout(any(Duration.class))).thenReturn(builder);
        when(builder.readTimeout(any(Duration.class))).thenReturn(builder);
        when(builder.rootUri(anyString())).thenReturn(builder);
        when(builder.defaultHeader(anyString(), anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(restTemplate);
        
        String jsonResponse = "{\"model\":\"test-model\",\"response\":\"\",\"done\":true}";
        when(restTemplate.postForObject(anyString(), any(Object.class), eq(String.class)))
                .thenReturn(jsonResponse);

        OllamaClient client = new OllamaClient("http://localhost:11434", "test-model", builder);
        String result = client.generate("Test prompt");

        assertEquals("", result);
    }

    @Test
    void generate_WithNullResponse_ShouldReturnEmptyString() {
        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        
        when(builder.connectTimeout(any(Duration.class))).thenReturn(builder);
        when(builder.readTimeout(any(Duration.class))).thenReturn(builder);
        when(builder.rootUri(anyString())).thenReturn(builder);
        when(builder.defaultHeader(anyString(), anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(restTemplate);
        
        when(restTemplate.postForObject(anyString(), any(Object.class), eq(String.class)))
                .thenReturn(null);

        OllamaClient client = new OllamaClient("http://localhost:11434", "test-model", builder);
        String result = client.generate("Test prompt");

        assertEquals("", result);
    }

    @Test
    void generate_WithApiError_ShouldThrowException() {
        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        
        when(builder.connectTimeout(any(Duration.class))).thenReturn(builder);
        when(builder.readTimeout(any(Duration.class))).thenReturn(builder);
        when(builder.rootUri(anyString())).thenReturn(builder);
        when(builder.defaultHeader(anyString(), anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(restTemplate);
        
        when(restTemplate.postForObject(anyString(), any(Object.class), eq(String.class)))
                .thenThrow(new RuntimeException("API error"));

        OllamaClient client = new OllamaClient("http://localhost:11434", "test-model", builder);

        assertThrows(OllamaClient.OllamaApiException.class, 
                () -> client.generate("Test prompt"));
    }
}

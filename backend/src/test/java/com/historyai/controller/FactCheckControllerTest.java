package com.historyai.controller;

import com.historyai.dto.FactCheckRequest;
import com.historyai.dto.FactCheckResult;
import com.historyai.service.FactCheckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FactCheckControllerTest {

    @Mock
    private FactCheckService factCheckService;

    @InjectMocks
    private FactCheckController controller;

    private FactCheckRequest request;
    private List<FactCheckResult> results;

    @BeforeEach
    void setUp() {
        request = new FactCheckRequest("Test message", "Test character");
        
        FactCheckResult result = new FactCheckResult(
                "Test claim",
                FactCheckResult.VerificationResult.VERIFIED,
                "Wikipedia",
                "This is correct",
                0.95f
        );
        results = Arrays.asList(result);
    }

    @Test
    void factCheck_WithValidRequest_ShouldReturn200() {
        when(factCheckService.factCheck(anyString(), anyString()))
                .thenReturn(results);

        ResponseEntity<List<FactCheckResult>> response = controller.factCheck(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(FactCheckResult.VerificationResult.VERIFIED, 
                response.getBody().get(0).getVerification());
    }

    @Test
    void factCheck_WithNullCharacterContext_ShouldPassNull() {
        request.setCharacterContext(null);
        when(factCheckService.factCheck(anyString(), eq(null)))
                .thenReturn(results);

        ResponseEntity<List<FactCheckResult>> response = controller.factCheck(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(factCheckService).factCheck("Test message", null);
    }

    @Test
    void factCheck_WithMultipleResults_ShouldReturnAll() {
        FactCheckResult result2 = new FactCheckResult(
                "Second claim",
                FactCheckResult.VerificationResult.FALSE,
                "Wikipedia",
                "Incorrect",
                0.9f
        );
        when(factCheckService.factCheck(anyString(), anyString()))
                .thenReturn(Arrays.asList(results.get(0), result2));

        ResponseEntity<List<FactCheckResult>> response = controller.factCheck(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void factCheck_WithEmptyResults_ShouldReturnEmptyList() {
        when(factCheckService.factCheck(anyString(), anyString()))
                .thenReturn(Arrays.asList());

        ResponseEntity<List<FactCheckResult>> response = controller.factCheck(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }
}

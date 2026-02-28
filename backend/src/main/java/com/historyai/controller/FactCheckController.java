package com.historyai.controller;

import com.historyai.dto.FactCheckRequest;
import com.historyai.dto.FactCheckResult;
import com.historyai.service.FactCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for fact-checking functionality.
 * Provides endpoints for verifying historical claims using Wikipedia and AI.
 */
@RestController
@RequestMapping("/api/factcheck")
@Tag(name = "Fact-Check", description = "Fact-checking API for historical claims")
public class FactCheckController {

    private static final Logger LOG = LoggerFactory.getLogger(FactCheckController.class);

    private final FactCheckService factCheckService;

    /**
     * Constructs a new FactCheckController.
     *
     * @param factCheckService the fact-check service
     */
    public FactCheckController(FactCheckService factCheckService) {
        this.factCheckService = factCheckService;
    }

    /**
     * Fact-checks a message containing historical claims.
     * Extracts claims and verifies each one using Wikipedia and Ollama.
     *
     * @param request the fact-check request containing the message to verify
     * @return list of verification results for each claim
     */
    @PostMapping
    @Operation(summary = "Fact-check message", description = "Verifies factual claims in a message using Wikipedia and AI")
    public ResponseEntity<List<FactCheckResult>> factCheck(
            @Valid @RequestBody FactCheckRequest request) {
        
        String message = request.getMessage();
        LOG.info("Fact-check request received for message: {}", 
                message.substring(0, Math.min(50, message.length())));
        
        List<FactCheckResult> results = factCheckService.factCheck(
                message, 
                request.getCharacterContext());
        
        return ResponseEntity.ok(results);
    }
}

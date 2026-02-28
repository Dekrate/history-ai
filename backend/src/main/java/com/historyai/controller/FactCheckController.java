package com.historyai.controller;

import com.historyai.client.OllamaClient;
import com.historyai.dto.FactCheckRequest;
import com.historyai.dto.FactCheckResult;
import com.historyai.dto.WikipediaResponse;
import com.historyai.exception.CharacterNotFoundInWikipediaException;
import com.historyai.service.FactCheckService;
import com.historyai.service.WikipediaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST controller for fact-checking functionality.
 * Provides endpoints for verifying historical claims using Wikipedia and AI.
 */
@RestController
@RequestMapping("/api/factcheck")
@Tag(name = "Fact-Check", description = "Fact-checking API for historical claims")
public class FactCheckController {

    private static final Logger LOG = LoggerFactory.getLogger(FactCheckController.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final FactCheckService factCheckService;
    private final OllamaClient ollamaClient;
    private final WikipediaService wikipediaService;

    public FactCheckController(FactCheckService factCheckService, OllamaClient ollamaClient, WikipediaService wikipediaService) {
        this.factCheckService = factCheckService;
        this.ollamaClient = ollamaClient;
        this.wikipediaService = wikipediaService;
    }

    @PostMapping
    @Operation(summary = "Fact-check message", description = "Verifies factual claims in a message using Wikipedia and AI")
    public ResponseEntity<List<FactCheckResult>> factCheck(
            @Valid @RequestBody FactCheckRequest request) {
        
        String message = request.getMessage();
        LOG.info("Fact-check request received for message: {}", 
                message.substring(0, Math.min(50, message.length())));
        
        List<FactCheckResult> results = factCheckService.factCheck(
                message, 
                request.getCharacterName(),
                request.getCharacterContext());
        
        return ResponseEntity.ok(results);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    @Operation(summary = "Fact-check message with streaming", description = "Verifies factual claims with streaming response")
    public SseEmitter factCheckStream(
            @RequestParam String message,
            @RequestParam String characterName,
            @RequestParam(required = false) String characterContext) {
        SseEmitter emitter = new SseEmitter(180000L);
        StringBuilder fullResponse = new StringBuilder();
        StringBuilder buffer = new StringBuilder();
        
        LOG.info("Streaming fact-check request for message: {}", 
                message.substring(0, Math.min(50, message.length())));
        
        try {
            emitter.send(SseEmitter.event().name("start").data("Starting verification..."));
            
            WikipediaResponse wikiContext = null;
            try {
                if (characterName != null && !characterName.isBlank()) {
                    wikiContext = wikipediaService.getCharacterInfo(characterName);
                    emitter.send(SseEmitter.event().name("wiki").data("Found Wikipedia info: " + wikiContext.title()));
                }
            } catch (CharacterNotFoundInWikipediaException e) {
                emitter.send(SseEmitter.event().name("wiki").data("No Wikipedia info found"));
            } catch (Exception e) {
                LOG.warn("Error fetching Wikipedia: {}", e.getMessage());
            }
            
            String prompt = buildStreamingPrompt(message, characterContext, wikiContext);
            emitter.send(SseEmitter.event().name("prompt").data("Analyzing with AI..."));
            
            ollamaClient.generateStream(ollamaClient.getDefaultModel(), prompt, chunk -> {
                try {
                    fullResponse.append(chunk);
                    buffer.append(chunk);
                    if (shouldFlush(buffer)) {
                        emitter.send(SseEmitter.event().name("chunk").data(buffer.toString()));
                        buffer.setLength(0);
                    }
                } catch (Exception e) {
                    LOG.error("Error sending chunk: {}", e.getMessage());
                }
            });
            
            if (buffer.length() > 0) {
                try {
                    emitter.send(SseEmitter.event().name("chunk").data(buffer.toString()));
                } catch (Exception e) {
                    LOG.error("Error flushing buffer: {}", e.getMessage());
                }
            }

            FactCheckResult parsed = factCheckService.parseOllamaResponseForStreaming(
                    message, fullResponse.toString(), wikiContext);
            String finalJson = OBJECT_MAPPER.writeValueAsString(parsed);
            emitter.send(SseEmitter.event().name("final").data(finalJson));
            emitter.send(SseEmitter.event().name("complete").data("Verification complete"));
            emitter.complete();
            
        } catch (Exception e) {
            LOG.error("Streaming error: {}", e.getMessage());
            try {
                emitter.send(SseEmitter.event().name("error").data("Error: " + e.getMessage()));
            } catch (Exception ex) {
                // ignore
            }
            emitter.completeWithError(e);
        }
        
        return emitter;
    }

    private boolean shouldFlush(StringBuilder buffer) {
        int length = buffer.length();
        if (length == 0) {
            return false;
        }
        char last = buffer.charAt(length - 1);
        if (Character.isWhitespace(last) || isPunctuation(last)) {
            return true;
        }
        return length >= 24;
    }

    private boolean isPunctuation(char c) {
        return c == '.' || c == ',' || c == '!' || c == '?' || c == ':' || c == ';' || c == ')';
    }

    private String buildStreamingPrompt(String claim, String characterContext, WikipediaResponse wikiContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a fact-checker for historical information. ");
        prompt.append("IMPORTANT: Write the EXPLANATION in the SAME language as the claim (e.g., if claim is in Polish, explain in Polish). Keep VERIFICATION, CONFIDENCE, SOURCE keywords in English.\n\n");
        
        if (characterContext != null && !characterContext.isEmpty()) {
            prompt.append("Character context: ").append(characterContext).append("\n\n");
        }
        
        if (wikiContext != null) {
            prompt.append("Reference information from Wikipedia:\n");
            prompt.append("- Title: ").append(wikiContext.title()).append("\n");
            if (wikiContext.extract() != null) {
                prompt.append("- Summary: ").append(wikiContext.extract()).append("\n");
            }
            prompt.append("\n");
        }
        
        prompt.append("Claim to verify: ").append(claim).append("\n\n");
        prompt.append("Provide your answer in the following format:\n");
        prompt.append("VERIFICATION: [TRUE/FALSE/PARTIAL/UNVERIFIABLE]\n");
        prompt.append("CONFIDENCE: [0.0-1.0]\n");
        prompt.append("EXPLANATION: [Brief explanation in the same language as the claim above]\n");
        prompt.append("SOURCE: [Source name if available]");
        
        return prompt.toString();
    }
}

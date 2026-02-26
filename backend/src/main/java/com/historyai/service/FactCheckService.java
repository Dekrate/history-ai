package com.historyai.service;

import com.historyai.client.OllamaClient;
import com.historyai.dto.FactCheckResult;
import com.historyai.dto.WikipediaResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for fact-checking historical claims.
 * Uses Wikipedia for context and Ollama (LLM) for verification.
 */
@Service
public class FactCheckService {

    private static final Logger LOG = LoggerFactory.getLogger(FactCheckService.class);

    private final OllamaClient ollamaClient;
    private final WikipediaService wikipediaService;

    /**
     * Constructs a new FactCheckService.
     *
     * @param ollamaClient the Ollama client for LLM interactions
     * @param wikipediaService the Wikipedia service for context retrieval
     */
    public FactCheckService(OllamaClient ollamaClient, WikipediaService wikipediaService) {
        this.ollamaClient = ollamaClient;
        this.wikipediaService = wikipediaService;
    }

    /**
     * Performs fact-checking on a message.
     * Extracts claims from the message and verifies each one.
     *
     * @param message the message containing claims to verify
     * @param characterContext optional context about the historical character
     * @return list of fact-check results for each claim
     */
    public List<FactCheckResult> factCheck(String message, String characterContext) {
        LOG.info("Starting fact-check for message");
        
        List<String> claims = extractClaims(message);
        LOG.debug("Extracted {} claims from message", claims.size());
        
        List<FactCheckResult> results = new ArrayList<>();
        
        for (String claim : claims) {
            try {
                FactCheckResult result = verifyClaim(claim, characterContext);
                results.add(result);
            } catch (Exception e) {
                LOG.error("Error verifying claim: {}", claim, e);
                results.add(new FactCheckResult(
                        claim,
                        FactCheckResult.VerificationResult.UNVERIFIABLE,
                        null,
                        "Error during verification: " + e.getMessage(),
                        0.0f
                ));
            }
        }
        
        if (results.isEmpty()) {
            results.add(new FactCheckResult(
                    message,
                    FactCheckResult.VerificationResult.UNVERIFIABLE,
                    null,
                    "No factual claims detected in message",
                    0.0f
            ));
        }
        
        return results;
    }

    /**
     * Extracts factual claims from a message.
     * Identifies sentences containing potentially verifiable information.
     *
     * @param message the message to extract claims from
     * @return list of extracted claims
     */
    public List<String> extractClaims(String message) {
        List<String> claims = new ArrayList<>();
        
        String[] sentences = message.split("[.!?]+");
        
        Pattern factualPattern = Pattern.compile(
                "(\\d{3,4}|urodzony|zmarl|prezydent|krol|wojna|bitwa|odkrycie|wynalazek|nagroda)",
                Pattern.CASE_INSENSITIVE
        );
        
        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.length() > 20 && factualPattern.matcher(sentence).find()) {
                claims.add(sentence.trim());
            }
        }
        
        return claims;
    }

    /**
     * Verifies a single claim using Wikipedia context and Ollama.
     *
     * @param claim the claim to verify
     * @param characterContext optional context about the historical character
     * @return the verification result
     */
    public FactCheckResult verifyClaim(String claim, String characterContext) {
        LOG.debug("Verifying claim: {}", claim);
        
        WikipediaResponse wikiContext = null;
        
        // First try characterContext if available
        if (characterContext != null && !characterContext.isBlank()) {
            try {
                wikiContext = wikipediaService.getCharacterInfo(characterContext);
                LOG.debug("Found Wikipedia context for character: {}", characterContext);
            } catch (Exception e) {
                LOG.warn("Could not fetch Wikipedia context for character: {}", characterContext);
            }
        }
        
        // Fallback to extracted keywords from claim
        if (wikiContext == null) {
            String keywords = extractFirstNameLastName(claim);
            try {
                wikiContext = wikipediaService.getCharacterInfo(keywords);
                LOG.debug("Found Wikipedia context for keywords: {}", keywords);
            } catch (Exception e) {
                LOG.warn("Could not fetch Wikipedia context for keywords: {}", keywords);
            }
        }
        
        String prompt = buildVerificationPrompt(claim, characterContext, wikiContext);
        
        String ollamaResponse = ollamaClient.generate(prompt);
        
        return parseOllamaResponse(claim, ollamaResponse, wikiContext);
    }

    /**
     * Extracts first two words as potential name for Wikipedia lookup.
     *
     * @param claim the claim to extract name from
     * @return first two words
     */
    private String extractFirstNameLastName(String claim) {
        String[] words = claim.split("\\s+");
        if (words.length >= 2) {
            return words[0] + " " + words[1];
        }
        return words[0];
    }

    /**
     * Builds a verification prompt for Ollama.
     *
     * @param claim the claim to verify
     * @param characterContext optional character context
     * @param wikiContext optional Wikipedia context
     * the prompt string
     */
    private String buildVerificationPrompt(String claim, String characterContext, WikipediaResponse wikiContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a fact-checker for historical information. ");
        prompt.append("Analyze the following claim and determine if it is factually correct.\n\n");
        
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
        prompt.append("EXPLANATION: [Brief explanation of your reasoning]\n");
        prompt.append("SOURCE: [Source name if available]");
        
        return prompt.toString();
    }

    /**
     * Parses the Ollama response to extract verification result.
     *
     * @param claim the original claim
     * @param ollamaResponse the response from Ollama
     * @param wikiContext the Wikipedia context used
     * @return the parsed FactCheckResult
     */
    private FactCheckResult parseOllamaResponse(String claim, String ollamaResponse, WikipediaResponse wikiContext) {
        LOG.debug("Parsing Ollama response: {}", ollamaResponse);
        
        FactCheckResult.VerificationResult verification = FactCheckResult.VerificationResult.UNVERIFIABLE;
        float confidence = 0.5f;
        String explanation = "";
        
        Pattern verificationPattern = Pattern.compile("^VERIFICATION:\\s*(\\w+)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher verificationMatcher = verificationPattern.matcher(ollamaResponse);
        if (verificationMatcher.find()) {
            String result = verificationMatcher.group(1).toUpperCase();
            if ("TRUE".equals(result) || "PRAWDA".equals(result)) {
                verification = FactCheckResult.VerificationResult.VERIFIED;
            } else if ("FALSE".equals(result) || "FAŁSZ".equals(result)) {
                verification = FactCheckResult.VerificationResult.FALSE;
            } else if ("PARTIAL".equals(result) || "CZEŚCIOWO".equals(result)) {
                verification = FactCheckResult.VerificationResult.PARTIAL;
            }
        }
        
        LOG.debug("Parsed verification: {}", verification);
        
        Pattern confidencePattern = Pattern.compile("CONFIDENCE:\\s*([0-9.]+)");
        Matcher matcher = confidencePattern.matcher(ollamaResponse);
        if (matcher.find()) {
            try {
                confidence = Float.parseFloat(matcher.group(1));
                LOG.debug("Parsed confidence: {}", confidence);
            } catch (NumberFormatException e) {
                LOG.warn("Could not parse confidence value");
            }
        }
        
        Pattern explanationPattern = Pattern.compile("EXPLANATION:\\s*(.+?)(?:\\nSOURCE:|$)", Pattern.DOTALL);
        Matcher explanationMatcher = explanationPattern.matcher(ollamaResponse);
        if (explanationMatcher.find()) {
            explanation = explanationMatcher.group(1).trim();
        }
        
        String source = wikiContext != null ? "Wikipedia - " + wikiContext.title() : "Ollama LLM";
        
        return new FactCheckResult(claim, verification, source, explanation, confidence);
    }
}

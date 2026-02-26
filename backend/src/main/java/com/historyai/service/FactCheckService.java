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
        
        String keywords = extractKeywords(claim);
        LOG.debug("Extracted keywords: {}", keywords);
        
        WikipediaResponse wikiContext = null;
        try {
            wikiContext = wikipediaService.getCharacterInfo(keywords);
        } catch (Exception e) {
            LOG.warn("Could not fetch Wikipedia context for keywords: {}", keywords);
        }
        
        String prompt = buildVerificationPrompt(claim, characterContext, wikiContext);
        
        String model = "llama3.2:3b";
        String ollamaResponse = ollamaClient.generate(model, prompt);
        
        return parseOllamaResponse(claim, ollamaResponse, wikiContext);
    }

    /**
     * Extracts keywords from a claim for Wikipedia lookup.
     *
     * @param claim the claim to extract keywords from
     * @return space-separated keywords
     */
    private String extractKeywords(String claim) {
        String[] words = claim.split("\\s+");
        StringBuilder keywords = new StringBuilder();
        
        for (String word : words) {
            if (word.length() > 4 && !isCommonWord(word)) {
                if (keywords.length() > 0) {
                    keywords.append(" ");
                }
                keywords.append(word);
            }
        }
        
        return keywords.length() > 0 ? keywords.toString() : claim;
    }

    /**
     * Checks if a word is a common word that should be excluded from keywords.
     *
     * @param word the word to check
     * @return true if the word is common, false otherwise
     */
    private boolean isCommonWord(String word) {
        String[] common = {"był", "jest", "była", "było", "byli", "oraz", "dla", "tego", "który", "która"};
        String lower = word.toLowerCase();
        for (String w : common) {
            if (lower.equals(w)) return true;
        }
        return false;
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
        FactCheckResult.VerificationResult verification = FactCheckResult.VerificationResult.UNVERIFIABLE;
        float confidence = 0.5f;
        String explanation = ollamaResponse;
        
        String upperResponse = ollamaResponse.toUpperCase();
        
        if (upperResponse.contains("TRUE")) {
            verification = FactCheckResult.VerificationResult.VERIFIED;
        } else if (upperResponse.contains("FALSE")) {
            verification = FactCheckResult.VerificationResult.FALSE;
        } else if (upperResponse.contains("PARTIAL")) {
            verification = FactCheckResult.VerificationResult.PARTIAL;
        }
        
        Pattern confidencePattern = Pattern.compile("CONFIDENCE:\\s*([0-9.]+)");
        Matcher matcher = confidencePattern.matcher(ollamaResponse);
        if (matcher.find()) {
            try {
                confidence = Float.parseFloat(matcher.group(1));
            } catch (NumberFormatException e) {
                LOG.warn("Could not parse confidence value");
            }
        }
        
        String source = wikiContext != null ? "Wikipedia - " + wikiContext.title() : "Ollama LLM";
        
        return new FactCheckResult(claim, verification, source, explanation, confidence);
    }
}

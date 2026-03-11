package com.historyai.service;

import com.historyai.dto.WikipediaResponse;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Builds prompts for fact-checking using shared formatting rules.
 */
@Component
public class FactCheckPromptBuilder {

    public String build(
            String claim,
            String characterContext,
            WikipediaResponse wikiContext,
            List<String> quotes) {
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

        if (quotes != null && !quotes.isEmpty()) {
            prompt.append("Relevant quotes from Wikiquote:\n");
            for (String quote : quotes) {
                prompt.append("- ").append(quote).append("\n");
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

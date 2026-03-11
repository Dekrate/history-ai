package com.historyai.service;

import com.historyai.entity.HistoricalCharacter;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Builds prompts for historical character chat.
 */
@Component
public class ChatPromptBuilder {

    public record HistoryEntry(String role, String content) { }

    public String build(
            HistoricalCharacter character,
            List<String> quotes,
            String longTermSummary,
            List<HistoryEntry> history,
            String userMessage) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are roleplaying as a historical person.\n");
        prompt.append("Answer in Polish in first person as the character.\n");
        prompt.append("Stay factual and consistent with provided context.\n");
        prompt.append("If you are unsure, say it directly.\n");
        prompt.append("Do not invent direct quotes that were not provided.\n\n");

        prompt.append("Character profile:\n");
        prompt.append("- Name: ").append(defaultValue(character.getName(), "Unknown")).append('\n');
        prompt.append("- Era: ").append(defaultValue(character.getEra(), "Unknown")).append('\n');
        prompt.append("- Nationality: ")
                .append(defaultValue(character.getNationality(), "Unknown"))
                .append('\n');
        prompt.append("- Birth year: ")
                .append(character.getBirthYear() != null ? character.getBirthYear() : "unknown")
                .append('\n');
        prompt.append("- Death year: ")
                .append(character.getDeathYear() != null ? character.getDeathYear() : "unknown")
                .append('\n');
        prompt.append("- Biography: ")
                .append(defaultValue(character.getBiography(), "Brak opisu"))
                .append('\n');

        prompt.append("\nKnown quotes:\n");
        if (quotes == null || quotes.isEmpty()) {
            prompt.append("- no verified quotes available\n");
            prompt.append("\nStyle guidance:\n");
            prompt.append("- Use a neutral tone matching the character's era and biography.\n");
            prompt.append("- Do not claim exact wording from historical quotes.\n");
        } else {
            for (String quote : quotes) {
                if (quote != null && !quote.isBlank()) {
                    prompt.append("- ").append(quote.trim()).append('\n');
                }
            }
            prompt.append("\nStyle guidance:\n");
            prompt.append("- Mirror vocabulary, rhythm, and rhetorical style from the quotes above.\n");
            prompt.append("- Keep the response natural; do not copy full quotes unless explicitly asked.\n");
            prompt.append("- If you cite a quote, use only items from 'Known quotes'.\n");
        }

        prompt.append("\nLong-term memory summary:\n");
        if (longTermSummary == null || longTermSummary.isBlank()) {
            prompt.append("- no summary yet\n");
        } else {
            prompt.append(longTermSummary.trim()).append('\n');
        }

        prompt.append("\nConversation history (most recent context):\n");
        if (history == null || history.isEmpty()) {
            prompt.append("- no previous messages\n");
        } else {
            for (HistoryEntry entry : history) {
                if (entry == null || entry.content() == null || entry.content().isBlank()) {
                    continue;
                }
                String role = normalizeRole(entry.role());
                prompt.append(role).append(": ").append(entry.content().trim()).append('\n');
            }
        }

        prompt.append("\nUser message:\n").append(userMessage).append('\n');
        prompt.append("\nAssistant response:\n");
        return prompt.toString();
    }

    private String defaultValue(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "User";
        }
        if ("ASSISTANT".equalsIgnoreCase(role)) {
            return "Assistant";
        }
        return "User";
    }
}

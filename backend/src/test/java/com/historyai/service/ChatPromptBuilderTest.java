package com.historyai.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.historyai.entity.HistoricalCharacter;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatPromptBuilderTest {

    private final ChatPromptBuilder builder = new ChatPromptBuilder();

    @Test
    void build_WhenQuotesExist_ShouldIncludeQuoteStyleGuidance() {
        HistoricalCharacter character = new HistoricalCharacter();
        character.setName("Mikołaj Kopernik");
        character.setEra("Renesans");
        character.setNationality("Polska");
        character.setBiography("Astronom i matematyk.");

        String prompt = builder.build(
                character,
                List.of("Wstrzymał Słońce, ruszył Ziemię."),
                "Key prior conversation points:\n- User: pytał o teorię heliocentryczną.",
                List.of(new ChatPromptBuilder.HistoryEntry("USER", "Jak działa kosmos?")),
                "Powiedz coś o nauce.");

        assertTrue(prompt.contains("Known quotes:"));
        assertTrue(prompt.contains("Mirror vocabulary, rhythm, and rhetorical style from the quotes above."));
        assertTrue(prompt.contains("If you cite a quote, use only items from 'Known quotes'."));
        assertTrue(prompt.contains("Long-term memory summary:"));
        assertTrue(prompt.contains("Key prior conversation points:"));
        assertTrue(prompt.contains("Conversation history (most recent context):"));
        assertTrue(prompt.contains("User: Jak działa kosmos?"));
    }

    @Test
    void build_WhenQuotesMissing_ShouldUseNeutralStyleGuidance() {
        HistoricalCharacter character = new HistoricalCharacter();
        character.setName("Nieznana postać");
        character.setEra("Unknown");
        character.setNationality("Unknown");
        character.setBiography("Brak danych.");

        String prompt = builder.build(
                character,
                List.of(),
                "",
                List.of(new ChatPromptBuilder.HistoryEntry("ASSISTANT", "Witaj.")),
                "Kim jesteś?");

        assertTrue(prompt.contains("no verified quotes available"));
        assertTrue(prompt.contains("Use a neutral tone matching the character's era and biography."));
        assertTrue(prompt.contains("Do not claim exact wording from historical quotes."));
        assertTrue(prompt.contains("Long-term memory summary:"));
        assertTrue(prompt.contains("no summary yet"));
        assertTrue(prompt.contains("Assistant: Witaj."));
    }
}

package com.historyai.service;

import com.historyai.client.WikiquoteApiClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Service for retrieving quotes from Wikiquote.
 * Tries Polish first, then English.
 */
@Service
@CacheConfig(cacheNames = "wikiquote")
public class WikiquoteService {

    private static final Logger LOG = LoggerFactory.getLogger(WikiquoteService.class);
    private static final String WIKIQUOTE_PL_BASE_URL = "https://pl.wikiquote.org/w/api.php";
    private static final String WIKIQUOTE_EN_BASE_URL = "https://en.wikiquote.org/w/api.php";
    private static final int MAX_QUOTES = 5;

    private final WikiquoteApiClient wikiquoteApiClient;

    public WikiquoteService(WikiquoteApiClient wikiquoteApiClient) {
        this.wikiquoteApiClient = wikiquoteApiClient;
    }

    public List<String> getQuotes(String characterName) {
        return getQuotes(characterName, "pl");
    }

    @Cacheable(value = "wikiquote", key = "#characterName + '|' + #lang", unless = "#result == null")
    public List<String> getQuotes(String characterName, String lang) {
        if (characterName == null || characterName.isBlank()) {
            return Collections.emptyList();
        }
        boolean preferEnglish = lang != null && lang.equalsIgnoreCase("en");
        String primary = preferEnglish ? WIKIQUOTE_EN_BASE_URL : WIKIQUOTE_PL_BASE_URL;
        String fallback = preferEnglish ? WIKIQUOTE_PL_BASE_URL : WIKIQUOTE_EN_BASE_URL;

        String wikitext = wikiquoteApiClient.getPageWikitext(primary, characterName);
        if (wikitext == null) {
            wikitext = wikiquoteApiClient.getPageWikitext(fallback, characterName);
        }
        if (wikitext != null) {
            List<String> quotes = extractQuotes(wikitext);
            if (!quotes.isEmpty()) {
                return quotes;
            }
        }

        String extract = wikiquoteApiClient.getPageExtract(primary, characterName);
        if (extract == null) {
            extract = wikiquoteApiClient.getPageExtract(fallback, characterName);
        }
        if (extract == null) {
            return Collections.emptyList();
        }
        List<String> quotes = extractQuotes(extract);
        if (quotes.isEmpty()) {
            LOG.debug("No quotes extracted for {}", characterName);
        }
        return quotes;
    }

    List<String> extractQuotes(String extract) {
        if (extract == null || extract.isBlank()) {
            return Collections.emptyList();
        }
        List<String> quotes = new ArrayList<>();
        String[] lines = extract.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.startsWith("*") || trimmed.startsWith("•") || trimmed.startsWith("-")) {
                String cleaned = cleanedBullet(trimmed);
                if (!cleaned.isBlank() && isQuoteLine(cleaned)) {
                    quotes.add(cleaned);
                }
            }
            if (quotes.size() >= MAX_QUOTES) {
                break;
            }
        }
        return quotes;
    }

    private String cleanedBullet(String trimmed) {
        String cleaned = trimmed.replaceFirst("^[*•-]+\\s*", "");
        cleaned = cleanWikiMarkup(cleaned);
        cleaned = cleaned.replaceAll("\\s+", " ");
        return cleaned;
    }

    private boolean isQuoteLine(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return !(lower.startsWith("opis:") || lower.startsWith("źródło:")
                || lower.startsWith("zrodlo:") || lower.startsWith("autor:")
                || lower.startsWith("zobacz też") || lower.startsWith("zobacz tez"));
    }

    private String cleanWikiMarkup(String text) {
        String cleaned = text;
        cleaned = cleaned.replaceAll("<ref[^>]*>.*?</ref>", "");
        cleaned = cleaned.replaceAll("\\[\\[([^\\]|]+)\\|([^\\]]+)\\]\\]", "$2");
        cleaned = cleaned.replaceAll("\\[\\[([^\\]]+)\\]\\]", "$1");
        cleaned = cleaned.replaceAll("'''''(.*?)'''''", "$1");
        cleaned = cleaned.replaceAll("'''(.*?)'''", "$1");
        cleaned = cleaned.replaceAll("''(.*?)''", "$1");
        cleaned = cleaned.replaceAll("\\{\\{[^}]+\\}\\}", "");
        cleaned = cleaned.replaceAll("\\*\\*$", "");
        return cleaned.trim();
    }
}

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

    @Cacheable(value = "wikiquote", key = "#characterName", unless = "#result == null")
    public List<String> getQuotes(String characterName) {
        if (characterName == null || characterName.isBlank()) {
            return Collections.emptyList();
        }
        String extract = wikiquoteApiClient.getPageExtract(WIKIQUOTE_PL_BASE_URL, characterName);
        if (extract == null) {
            extract = wikiquoteApiClient.getPageExtract(WIKIQUOTE_EN_BASE_URL, characterName);
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
                if (!cleaned.isBlank()) {
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
        cleaned = cleaned.replaceAll("\\s+", " ");
        return cleaned;
    }
}

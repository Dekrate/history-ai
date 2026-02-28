package com.historyai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.historyai.exception.WikipediaApiException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Client for interacting with the Wikiquote API.
 * Retrieves page extracts for quotes.
 */
@Component
public class WikiquoteApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(WikiquoteApiClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String defaultBaseUrl;

    public WikiquoteApiClient(
            @Value("${wikiquote.api.base-url:https://pl.wikiquote.org/w/api.php}") String baseUrl,
            @Value("${wikiquote.api.connect-timeout:10s}") Duration connectTimeout,
            @Value("${wikiquote.api.read-timeout:15s}") Duration readTimeout,
            RestTemplateBuilder restTemplateBuilder) {
        this.defaultBaseUrl = baseUrl;
        this.objectMapper = new ObjectMapper();
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(connectTimeout)
                .setReadTimeout(readTimeout)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent", "HistoryAI/1.0 (contact: info@historyai.app)")
                .build();
    }

    public String getPageExtract(String title) {
        return getPageExtract(defaultBaseUrl, title);
    }

    public String getPageExtract(String baseUrl, String title) {
        String requestBase = baseUrl == null || baseUrl.isBlank() ? defaultBaseUrl : baseUrl;
        String url = requestBase
                + "?action=query&prop=extracts&explaintext=1&format=json&formatversion=2&redirects=1&titles={title}";
        try {
            String response = restTemplate.getForObject(url, String.class, title.replace(" ", "_"));
            if (response == null) {
                return null;
            }
            JsonNode root = objectMapper.readTree(response);
            JsonNode pages = root.path("query").path("pages");
            if (!pages.isArray() || pages.isEmpty()) {
                return null;
            }
            JsonNode page = pages.get(0);
            if (page.has("missing")) {
                LOG.debug("Wikiquote page missing for {} at {}", title, requestBase);
                return null;
            }
            String extract = page.path("extract").asText(null);
            return extract == null || extract.isBlank() ? null : extract;
        } catch (Exception e) {
            LOG.error("Error fetching Wikiquote extract for {}: {}", title, e.getMessage());
            throw new WikipediaApiException("Failed to fetch Wikiquote data", e);
        }
    }

    public String getPageWikitext(String baseUrl, String title) {
        String requestBase = baseUrl == null || baseUrl.isBlank() ? defaultBaseUrl : baseUrl;
        String url = requestBase
                + "?action=parse&prop=wikitext&format=json&formatversion=2&redirects=1&page={title}";
        try {
            String response = restTemplate.getForObject(url, String.class, title.replace(" ", "_"));
            if (response == null) {
                return null;
            }
            JsonNode root = objectMapper.readTree(response);
            JsonNode parse = root.path("parse");
            if (parse.isMissingNode()) {
                return null;
            }
            String wikitext = parse.path("wikitext").asText(null);
            return wikitext == null || wikitext.isBlank() ? null : wikitext;
        } catch (Exception e) {
            LOG.error("Error fetching Wikiquote wikitext for {}: {}", title, e.getMessage());
            throw new WikipediaApiException("Failed to fetch Wikiquote data", e);
        }
    }
}

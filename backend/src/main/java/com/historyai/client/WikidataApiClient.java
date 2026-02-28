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
 * Client for interacting with Wikidata to validate entity types.
 */
@Component
public class WikidataApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(WikidataApiClient.class);
    private static final String HUMAN_QID = "Q5";
    private static final String P31 = "P31";
    private static final String P27 = "P27";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public WikidataApiClient(
            @Value("${wikidata.api.base-url:https://www.wikidata.org/wiki/Special:EntityData}") String baseUrl,
            @Value("${wikidata.api.connect-timeout:10s}") Duration connectTimeout,
            @Value("${wikidata.api.read-timeout:15s}") Duration readTimeout,
            RestTemplateBuilder restTemplateBuilder) {
        this.baseUrl = baseUrl;
        this.objectMapper = new ObjectMapper();
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(connectTimeout)
                .setReadTimeout(readTimeout)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent", "HistoryAI/1.0 (contact: info@historyai.app)")
                .build();
    }

    /**
     * Returns true if the Wikidata entity is a human (P31 = Q5).
     *
     * @param wikibaseItem the Wikidata entity ID (e.g., Q12345)
     */
    public boolean isHuman(String wikibaseItem) {
        if (wikibaseItem == null || wikibaseItem.isBlank()) {
            return false;
        }
        String url = baseUrl + "/" + wikibaseItem + ".json";
        try {
            String response = restTemplate.getForObject(url, String.class);
            if (response == null) {
                return false;
            }
            JsonNode root = objectMapper.readTree(response);
            JsonNode claims = root.path("entities")
                    .path(wikibaseItem)
                    .path("claims")
                    .path(P31);
            if (!claims.isArray()) {
                return false;
            }
            for (JsonNode claim : claims) {
                JsonNode idNode = claim.path("mainsnak")
                        .path("datavalue")
                        .path("value")
                        .path("id");
                if (HUMAN_QID.equals(idNode.asText())) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            LOG.error("Error fetching Wikidata entity {}: {}", wikibaseItem, e.getMessage());
            throw new WikipediaApiException("Failed to fetch Wikidata data", e);
        }
    }

    /**
     * Returns the first citizenship label (P27) for the entity, in preferred language.
     */
    public java.util.Optional<String> getFirstCitizenshipLabel(
            String wikibaseItem, String preferredLang, String fallbackLang) {
        if (wikibaseItem == null || wikibaseItem.isBlank()) {
            return java.util.Optional.empty();
        }
        String url = baseUrl + "/" + wikibaseItem + ".json";
        try {
            String response = restTemplate.getForObject(url, String.class);
            if (response == null) {
                return java.util.Optional.empty();
            }
            JsonNode root = objectMapper.readTree(response);
            JsonNode claims = root.path("entities")
                    .path(wikibaseItem)
                    .path("claims")
                    .path(P27);
            if (!claims.isArray() || claims.isEmpty()) {
                return java.util.Optional.empty();
            }
            String countryId = selectBestClaimEntityId(claims);
            if (countryId == null || countryId.isBlank()) {
                return java.util.Optional.empty();
            }
            return getEntityLabel(countryId, preferredLang, fallbackLang);
        } catch (Exception e) {
            LOG.error("Error fetching Wikidata citizenship for {}: {}", wikibaseItem, e.getMessage());
            throw new WikipediaApiException("Failed to fetch Wikidata data", e);
        }
    }

    String selectBestClaimEntityId(JsonNode claims) {
        String normal = null;
        String fallback = null;
        for (JsonNode claim : claims) {
            String entityId = claim.path("mainsnak")
                    .path("datavalue")
                    .path("value")
                    .path("id")
                    .asText(null);
            if (entityId == null || entityId.isBlank()) {
                continue;
            }
            String rank = claim.path("rank").asText("normal");
            if ("preferred".equalsIgnoreCase(rank)) {
                return entityId;
            }
            if ("normal".equalsIgnoreCase(rank) && normal == null) {
                normal = entityId;
            }
            if (fallback == null) {
                fallback = entityId;
            }
        }
        return normal != null ? normal : fallback;
    }

    private java.util.Optional<String> getEntityLabel(String entityId, String preferredLang, String fallbackLang) {
        String url = baseUrl + "/" + entityId + ".json";
        try {
            String response = restTemplate.getForObject(url, String.class);
            if (response == null) {
                return java.util.Optional.empty();
            }
            JsonNode root = objectMapper.readTree(response);
            JsonNode labels = root.path("entities").path(entityId).path("labels");
            String label = labels.path(preferredLang).path("value").asText(null);
            if (label == null || label.isBlank()) {
                label = labels.path(fallbackLang).path("value").asText(null);
            }
            return java.util.Optional.ofNullable(label);
        } catch (Exception e) {
            LOG.error("Error fetching Wikidata label for {}: {}", entityId, e.getMessage());
            throw new WikipediaApiException("Failed to fetch Wikidata data", e);
        }
    }
}

package com.historyai.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

class WikidataApiClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void selectBestClaimEntityId_PrefersPreferredRank() throws Exception {
        String json = """
            [
              { "rank": "normal", "mainsnak": { "datavalue": { "value": { "id": "Q1" } } } },
              { "rank": "preferred", "mainsnak": { "datavalue": { "value": { "id": "Q2" } } } }
            ]
            """;
        JsonNode claims = objectMapper.readTree(json);
        WikidataApiClient client = new WikidataApiClient(
                "https://www.wikidata.org/wiki/Special:EntityData",
                java.time.Duration.ofSeconds(1),
                java.time.Duration.ofSeconds(1),
                new RestTemplateBuilder());

        String result = client.selectBestClaimEntityId(claims);

        assertEquals("Q2", result);
    }

    @Test
    void selectBestClaimEntityId_FallsBackToNormal() throws Exception {
        String json = """
            [
              { "rank": "normal", "mainsnak": { "datavalue": { "value": { "id": "Q1" } } } },
              { "rank": "deprecated", "mainsnak": { "datavalue": { "value": { "id": "Q2" } } } }
            ]
            """;
        JsonNode claims = objectMapper.readTree(json);
        WikidataApiClient client = new WikidataApiClient(
                "https://www.wikidata.org/wiki/Special:EntityData",
                java.time.Duration.ofSeconds(1),
                java.time.Duration.ofSeconds(1),
                new RestTemplateBuilder());

        String result = client.selectBestClaimEntityId(claims);

        assertEquals("Q1", result);
    }

    @Test
    void selectBestClaimEntityId_ReturnsNullWhenEmpty() throws Exception {
        JsonNode claims = objectMapper.readTree("[]");
        WikidataApiClient client = new WikidataApiClient(
                "https://www.wikidata.org/wiki/Special:EntityData",
                java.time.Duration.ofSeconds(1),
                java.time.Duration.ofSeconds(1),
                new RestTemplateBuilder());

        String result = client.selectBestClaimEntityId(claims);

        assertNull(result);
    }
}

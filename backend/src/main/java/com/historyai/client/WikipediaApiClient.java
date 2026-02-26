package com.historyai.client;

import com.historyai.dto.WikipediaResponse;
import com.historyai.exception.CharacterNotFoundInWikipediaException;
import com.historyai.exception.WikipediaApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Client for interacting with the Wikipedia REST API.
 * Retrieves summary information about historical characters and topics.
 */
@Component
public class WikipediaApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(WikipediaApiClient.class);
    private static final String WIKIPEDIA_API_BASE = "https://en.wikipedia.org/api/rest_v1";

    private final RestTemplate restTemplate;

    /**
     * Constructs a new WikipediaApiClient.
     *
     * @param baseUrl the base URL of the Wikipedia API
     * @param restTemplateBuilder the REST template builder for configuring timeouts
     */
    public WikipediaApiClient(
            @Value("${wikipedia.api.base-url:${WIKIPEDIA_API_BASE}}") String baseUrl,
            RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(java.time.Duration.ofSeconds(10))
                .setReadTimeout(java.time.Duration.ofSeconds(15))
                .rootUri(baseUrl)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent", "HistoryAI/1.0 (contact: info@historyai.app)")
                .build();
    }

    /**
     * Retrieves a summary for a character from Wikipedia.
     *
     * @param characterName the name of the character to look up
     * @return WikipediaResponse containing the summary
     * @throws CharacterNotFoundInWikipediaException if the character is not found
     * @throws WikipediaApiException if the API call fails
     */
    public WikipediaResponse getCharacterSummary(String characterName) {
        LOG.debug("Fetching Wikipedia summary for: {}", characterName);

        try {
            WikipediaResponse result = restTemplate.getForObject(
                    "/page/summary/{title}",
                    WikipediaResponse.class,
                    characterName.replace(" ", "_")
            );

            if (result == null) {
                throw new CharacterNotFoundInWikipediaException(characterName);
            }

            LOG.debug("Successfully fetched Wikipedia summary for: {}", characterName);
            return result;

        } catch (CharacterNotFoundInWikipediaException e) {
            throw e;
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            LOG.warn("Character not found in Wikipedia: {}", characterName);
            throw new CharacterNotFoundInWikipediaException(characterName);
        } catch (Exception e) {
            LOG.error("Error fetching Wikipedia data for {}: {}", characterName, e.getMessage());
            throw new WikipediaApiException("Failed to fetch Wikipedia data", e);
        }
    }
}

package com.historyai.client;

import com.historyai.dto.WikipediaResponse;
import com.historyai.exception.CharacterNotFoundInWikipediaException;
import com.historyai.exception.WikipediaApiException;
import java.time.Duration;
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

    private final RestTemplate restTemplate;
    private final String defaultBaseUrl;

    /**
     * Constructs a new WikipediaApiClient.
     *
     * @param baseUrl the base URL of the Wikipedia API
     * @param connectTimeout connection timeout
     * @param readTimeout read timeout
     * @param restTemplateBuilder the REST template builder for configuring timeouts
     */
    public WikipediaApiClient(
            @Value("${wikipedia.api.base-url:https://en.wikipedia.org/api/rest_v1}") String baseUrl,
            @Value("${wikipedia.api.connect-timeout:10s}") Duration connectTimeout,
            @Value("${wikipedia.api.read-timeout:15s}") Duration readTimeout,
            RestTemplateBuilder restTemplateBuilder) {
        this.defaultBaseUrl = baseUrl;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(connectTimeout)
                .setReadTimeout(readTimeout)
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

    /**
     * Retrieves a summary for a character from a specific Wikipedia base URL.
     *
     * @param baseUrl the Wikipedia REST API base URL (e.g., https://pl.wikipedia.org/api/rest_v1)
     * @param characterName the name of the character to look up
     * @return WikipediaResponse containing the summary
     */
    public WikipediaResponse getCharacterSummary(String baseUrl, String characterName) {
        String requestBase = baseUrl == null || baseUrl.isBlank() ? defaultBaseUrl : baseUrl;
        String url = requestBase + "/page/summary/{title}";
        LOG.debug("Fetching Wikipedia summary from {} for: {}", requestBase, characterName);

        try {
            WikipediaResponse result = restTemplate.getForObject(
                    url,
                    WikipediaResponse.class,
                    characterName.replace(" ", "_")
            );

            if (result == null) {
                throw new CharacterNotFoundInWikipediaException(characterName);
            }

            LOG.debug("Successfully fetched Wikipedia summary from {} for: {}", requestBase, characterName);
            return result;

        } catch (CharacterNotFoundInWikipediaException e) {
            throw e;
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            LOG.warn("Character not found in Wikipedia ({}) : {}", requestBase, characterName);
            throw new CharacterNotFoundInWikipediaException(characterName);
        } catch (Exception e) {
            LOG.error("Error fetching Wikipedia data from {} for {}: {}", requestBase, characterName, e.getMessage());
            throw new WikipediaApiException("Failed to fetch Wikipedia data", e);
        }
    }
}

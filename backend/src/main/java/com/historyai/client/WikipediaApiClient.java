package com.historyai.client;

import com.historyai.dto.WikipediaResponse;
import com.historyai.exception.CharacterNotFoundInWikipediaException;
import com.historyai.exception.WikipediaApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class WikipediaApiClient {

    private static final Logger logger = LoggerFactory.getLogger(WikipediaApiClient.class);
    private static final String WIKIPEDIA_API_BASE = "https://en.wikipedia.org/api/rest_v1";

    private final RestTemplate restTemplate;

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

    public WikipediaResponse getCharacterSummary(String characterName) {
        logger.debug("Fetching Wikipedia summary for: {}", characterName);

        try {
            WikipediaResponse result = restTemplate.getForObject(
                    "/page/summary/{title}",
                    WikipediaResponse.class,
                    characterName.replace(" ", "_")
            );

            if (result == null) {
                throw new CharacterNotFoundInWikipediaException(characterName);
            }

            logger.debug("Successfully fetched Wikipedia summary for: {}", characterName);
            return result;

        } catch (CharacterNotFoundInWikipediaException e) {
            throw e;
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            logger.warn("Character not found in Wikipedia: {}", characterName);
            throw new CharacterNotFoundInWikipediaException(characterName);
        } catch (Exception e) {
            logger.error("Error fetching Wikipedia data for {}: {}", characterName, e.getMessage());
            throw new WikipediaApiException("Failed to fetch Wikipedia data", e);
        }
    }
}

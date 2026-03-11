package com.historyai.service;

import com.historyai.client.WikipediaApiClient;
import com.historyai.client.WikidataApiClient;
import com.historyai.dto.WikipediaResponse;
import com.historyai.exception.CharacterNotFoundInWikipediaException;
import com.historyai.exception.WikipediaApiException;
import com.historyai.exception.WikipediaRateLimitException;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Service for retrieving Wikipedia information about historical characters.
 * Provides caching and rate limiting for Wikipedia API calls.
 */
@Service
@CacheConfig(cacheNames = "wikipedia")
public class WikipediaService {

    private static final Logger LOG = LoggerFactory.getLogger(WikipediaService.class);
    private static final String WIKI_PL_BASE_URL = "https://pl.wikipedia.org/api/rest_v1";
    private static final String WIKI_EN_BASE_URL = "https://en.wikipedia.org/api/rest_v1";

    private final WikipediaApiClient wikipediaApiClient;
    private final WikidataApiClient wikidataApiClient;

    /**
     * Constructs a new WikipediaService.
     *
     * @param wikipediaApiClient the Wikipedia API client
     */
    public WikipediaService(WikipediaApiClient wikipediaApiClient,
            WikidataApiClient wikidataApiClient) {
        this.wikipediaApiClient = wikipediaApiClient;
        this.wikidataApiClient = wikidataApiClient;
    }

    /**
     * Gets character information from Wikipedia.
     * Results are cached for 24 hours to reduce API calls.
     * Rate limited to 10 requests per minute.
     *
     * @param characterName the name of the historical character
     * @return WikipediaResponse containing character information
     */
    @Cacheable(value = "wikipedia", key = "#characterName", unless = "#result == null")
    @RateLimiter(name = "wikipedia", fallbackMethod = "getCharacterInfoFallback")
    public WikipediaResponse getCharacterInfo(String characterName) {
        LOG.info("Fetching Wikipedia info for: {}", characterName);
        try {
            WikipediaResponse response = wikipediaApiClient.getCharacterSummary(WIKI_PL_BASE_URL, characterName);
            ensureHuman(response);
            return response;
        } catch (CharacterNotFoundInWikipediaException | WikipediaApiException e) {
            LOG.debug("PL Wikipedia lookup failed for {}: {}", characterName, e.getMessage());
            WikipediaResponse response = wikipediaApiClient.getCharacterSummary(WIKI_EN_BASE_URL, characterName);
            ensureHuman(response);
            return response;
        }
    }

    public String getNationalityFromWikidata(String wikibaseItem) {
        return wikidataApiClient.getFirstCitizenshipLabel(wikibaseItem, "pl", "en")
                .orElse("Unknown");
    }

    private void ensureHuman(WikipediaResponse response) {
        String wikibaseItem = response != null ? response.wikibaseItem() : null;
        if (wikibaseItem == null || wikibaseItem.isBlank()) {
            throw new CharacterNotFoundInWikipediaException("Unknown");
        }
        if (!wikidataApiClient.isHuman(wikibaseItem)) {
            throw new CharacterNotFoundInWikipediaException("Not a person");
        }
    }

    /**
     * Fallback method when rate limit is exceeded or error occurs.
     *
     * @param characterName the character name
     * @param t the throwable that caused the failure
     * @return never returns, always throws an exception
     */
    private WikipediaResponse getCharacterInfoFallback(String characterName, Throwable t) {
        if (t instanceof CharacterNotFoundInWikipediaException) {
            throw (CharacterNotFoundInWikipediaException) t;
        }
        if (t instanceof WikipediaApiException) {
            throw (WikipediaApiException) t;
        }
        LOG.error("Rate limit exceeded or error for {}: {}", characterName, t.getMessage());
        throw new WikipediaRateLimitException("Wikipedia API rate limit exceeded. Please try again later.", t);
    }
}

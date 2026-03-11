package com.historyai.service;

import com.historyai.client.WikipediaApiClient;
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

    private final WikipediaApiClient wikipediaApiClient;

    /**
     * Constructs a new WikipediaService.
     *
     * @param wikipediaApiClient the Wikipedia API client
     */
    public WikipediaService(WikipediaApiClient wikipediaApiClient) {
        this.wikipediaApiClient = wikipediaApiClient;
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
        return wikipediaApiClient.getCharacterSummary(characterName);
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

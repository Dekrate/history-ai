package com.historyai.service;

import com.historyai.client.WikipediaApiClient;
import com.historyai.dto.WikipediaResponse;
import com.historyai.exception.CharacterNotFoundInWikipediaException;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@CacheConfig(cacheNames = "wikipedia")
public class WikipediaService {

    private static final Logger logger = LoggerFactory.getLogger(WikipediaService.class);

    private final WikipediaApiClient wikipediaApiClient;

    public WikipediaService(WikipediaApiClient wikipediaApiClient) {
        this.wikipediaApiClient = wikipediaApiClient;
    }

    @Cacheable(value = "wikipedia", key = "#characterName", unless = "#result == null")
    @RateLimiter(name = "wikipedia", fallbackMethod = "getCharacterInfoFallback")
    public WikipediaResponse getCharacterInfo(String characterName) {
        logger.info("Fetching Wikipedia info for: {}", characterName);
        return wikipediaApiClient.getCharacterSummary(characterName);
    }

    private WikipediaResponse getCharacterInfoFallback(String characterName, Throwable t) {
        if (t instanceof CharacterNotFoundInWikipediaException) {
            throw (CharacterNotFoundInWikipediaException) t;
        }
        logger.error("Rate limit exceeded or error for {}: {}", characterName, t.getMessage());
        throw new RuntimeException("Wikipedia API rate limit exceeded. Please try again later.");
    }
}

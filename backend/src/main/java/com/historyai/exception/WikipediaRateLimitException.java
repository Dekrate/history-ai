package com.historyai.exception;

/**
 * Exception thrown when Wikipedia API rate limit is exceeded.
 */
public class WikipediaRateLimitException extends RuntimeException {

    public WikipediaRateLimitException(String message) {
        super(message);
    }

    public WikipediaRateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}

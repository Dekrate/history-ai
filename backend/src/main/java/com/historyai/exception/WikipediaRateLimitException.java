package com.historyai.exception;

/**
 * Exception thrown when Wikipedia API rate limit is exceeded.
 *
 * <p>Indicates that the application has made too many requests to the Wikipedia API
 * and has been temporarily blocked. The application implements rate limiting
 * to respect Wikipedia's usage policies.</p>
 *
 * <p>When this exception occurs, the application should wait before making
 * additional requests to the Wikipedia API.</p>
 *
 * @author HistoryAI Team
 * @version 1.0
 * @see WikipediaApiException
 */
public class WikipediaRateLimitException extends RuntimeException {

    public WikipediaRateLimitException(String message) {
        super(message);
    }

    public WikipediaRateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}

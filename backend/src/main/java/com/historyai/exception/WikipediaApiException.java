package com.historyai.exception;

/**
 * Exception thrown when Wikipedia API call fails.
 *
 * <p>Represents a general failure when communicating with the Wikipedia API,
 * such as network errors, timeouts, or unexpected response formats.</p>
 *
 * @author HistoryAI Team
 * @version 1.0
 * @see CharacterNotFoundInWikipediaException
 * @see WikipediaRateLimitException
 */
public class WikipediaApiException extends RuntimeException {

    /**
     * Constructs a new WikipediaApiException.
     *
     * @param message the error message
     * @param cause the underlying cause of the exception
     */
    public WikipediaApiException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.historyai.exception;

/**
 * Exception thrown when Wikipedia API call fails.
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

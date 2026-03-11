package com.historyai.exception;

/**
 * Exception thrown when a character is not found in Wikipedia.
 *
 * <p>This exception indicates that a search for a historical character
 * in the Wikipedia API did not return any results. It typically results
 * in a 404 NOT_FOUND HTTP response when importing characters from Wikipedia.</p>
 *
 * @author HistoryAI Team
 * @version 1.0
 */
public class CharacterNotFoundInWikipediaException extends RuntimeException {

    /**
     * Constructs a new CharacterNotFoundInWikipediaException.
     *
     * @param characterName the name of the character that was not found
     */
    public CharacterNotFoundInWikipediaException(String characterName) {
        super("Character not found in Wikipedia: " + characterName);
    }
}

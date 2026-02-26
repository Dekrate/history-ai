package com.historyai.exception;

/**
 * Exception thrown when a character is not found in Wikipedia.
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

package com.historyai.exception;

/**
 * Exception thrown when attempting to create a character that already exists.
 * This exception typically results in a 409 CONFLICT HTTP response.
 */
public class CharacterAlreadyExistsException extends RuntimeException {

    private final String characterName;

    /**
     * Constructs a new CharacterAlreadyExistsException with the character name.
     *
     * @param characterName the name of the character that already exists
     */
    public CharacterAlreadyExistsException(String characterName) {
        super("Character already exists with name: " + characterName);
        this.characterName = characterName;
    }

    /**
     * Returns the name of the character that already exists.
     *
     * @return the character name
     */
    public String getCharacterName() {
        return characterName;
    }
}

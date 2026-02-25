package com.historyai.exception;

import java.util.UUID;

/**
 * Exception thrown when a requested historical character is not found in the system.
 * This exception typically results in a 404 NOT_FOUND HTTP response.
 */
public class CharacterNotFoundException extends RuntimeException {

    private final UUID characterId;

    /**
     * Constructs a new CharacterNotFoundException with the specified character ID.
     *
     * @param characterId the UUID of the character that was not found
     */
    public CharacterNotFoundException(UUID characterId) {
        super("Character not found with ID: " + characterId);
        this.characterId = characterId;
    }

    /**
     * Constructs a new CharacterNotFoundException with a custom message.
     *
     * @param message the detail message
     */
    public CharacterNotFoundException(String message) {
        super(message);
        this.characterId = null;
    }

    /**
     * Returns the UUID of the character that was not found.
     *
     * @return the character ID, or null if not applicable
     */
    public UUID getCharacterId() {
        return characterId;
    }
}

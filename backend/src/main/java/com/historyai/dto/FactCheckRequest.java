package com.historyai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for fact-checking endpoint.
 * Contains the message to be verified and optional character context.
 */
public class FactCheckRequest {

    /** The message containing claims to be verified */
    @NotBlank
    private String message;
    
    /** Name of the historical character for Wikipedia lookup */
    private String characterName;
    
    /** Optional context about the historical character */
    private String characterContext;

    public FactCheckRequest() {
    }

    public FactCheckRequest(String message, String characterName, String characterContext) {
        this.message = message;
        this.characterName = characterName;
        this.characterContext = characterContext;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCharacterName() {
        return characterName;
    }

    public void setCharacterName(String characterName) {
        this.characterName = characterName;
    }

    public String getCharacterContext() {
        return characterContext;
    }

    public void setCharacterContext(String characterContext) {
        this.characterContext = characterContext;
    }
}

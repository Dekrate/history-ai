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
    
    /** Optional context about the historical character */
    private String characterContext;

    public FactCheckRequest() {
    }

    public FactCheckRequest(String message, String characterContext) {
        this.message = message;
        this.characterContext = characterContext;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCharacterContext() {
        return characterContext;
    }

    public void setCharacterContext(String characterContext) {
        this.characterContext = characterContext;
    }
}

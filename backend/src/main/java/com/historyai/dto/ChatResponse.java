package com.historyai.dto;

import java.util.UUID;

/**
 * Chat response payload.
 */
public class ChatResponse {

    private String message;
    private String conversationId;
    private UUID characterId;

    public ChatResponse() {
    }

    public ChatResponse(String message, String conversationId, UUID characterId) {
        this.message = message;
        this.conversationId = conversationId;
        this.characterId = characterId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public UUID getCharacterId() {
        return characterId;
    }

    public void setCharacterId(UUID characterId) {
        this.characterId = characterId;
    }
}

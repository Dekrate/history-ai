package com.historyai.dto;

import java.util.List;

/**
 * Chat history response payload.
 */
public class ChatHistoryResponse {

    private List<ChatMessageDto> messages;

    public ChatHistoryResponse() {
    }

    public ChatHistoryResponse(List<ChatMessageDto> messages) {
        this.messages = messages;
    }

    public List<ChatMessageDto> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessageDto> messages) {
        this.messages = messages;
    }
}

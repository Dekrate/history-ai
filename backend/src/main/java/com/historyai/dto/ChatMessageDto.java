package com.historyai.dto;

import com.historyai.entity.Message;
import java.time.LocalDateTime;

/**
 * Chat message DTO returned in conversation history endpoint.
 */
public class ChatMessageDto {

    private String role;
    private String content;
    private LocalDateTime timestamp;

    public ChatMessageDto() {
    }

    public ChatMessageDto(String role, String content, LocalDateTime timestamp) {
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
    }

    public static ChatMessageDto fromEntity(Message message) {
        return new ChatMessageDto(
                message.getRole() != null ? message.getRole().name() : "ASSISTANT",
                message.getContent(),
                message.getTimestamp()
        );
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

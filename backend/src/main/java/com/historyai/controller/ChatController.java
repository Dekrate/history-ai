package com.historyai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.historyai.dto.ChatHistoryResponse;
import com.historyai.dto.ChatRequest;
import com.historyai.dto.ChatResponse;
import com.historyai.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST controller for AI chat with historical characters.
 */
@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat", description = "Chat API for historical character roleplay")
public class ChatController {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;
    private final Executor streamingExecutor;

    public ChatController(
            ChatService chatService,
            ObjectMapper objectMapper,
            @Qualifier("streamingExecutor") Executor streamingExecutor) {
        this.chatService = chatService;
        this.objectMapper = objectMapper;
        this.streamingExecutor = streamingExecutor;
    }

    @PostMapping
    @Operation(summary = "Send a chat message", description = "Returns AI response in character style")
    public ResponseEntity<ChatResponse> sendMessage(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.chat(request));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    @Operation(summary = "Stream chat response", description = "Streams AI response chunks in real time")
    public SseEmitter streamMessage(
            @RequestParam String message,
            @RequestParam String characterId,
            @RequestParam(required = false) String conversationId) {
        SseEmitter emitter = new SseEmitter(180000L);
        CompletableFuture.runAsync(
                () -> streamChat(emitter, message, characterId, conversationId),
                streamingExecutor
        );
        return emitter;
    }

    @GetMapping("/{conversationId}/history")
    @Operation(summary = "Get conversation history", description = "Returns stored messages for conversation")
    public ResponseEntity<ChatHistoryResponse> getHistory(@PathVariable String conversationId) {
        return ResponseEntity.ok(chatService.getHistory(conversationId));
    }

    private void streamChat(
            SseEmitter emitter,
            String message,
            String characterId,
            String conversationId) {
        StringBuilder buffer = new StringBuilder();
        try {
            ChatRequest request = new ChatRequest();
            request.setMessage(message);
            request.setCharacterId(UUID.fromString(characterId));
            request.setConversationId(conversationId);

            emitter.send(SseEmitter.event().name("start").data("Generating response..."));

            ChatResponse finalResponse = chatService.chatStream(request, chunk -> {
                try {
                    buffer.append(chunk);
                    if (shouldFlush(buffer)) {
                        emitter.send(SseEmitter.event().name("chunk").data(buffer.toString()));
                        buffer.setLength(0);
                    }
                } catch (Exception ignored) {
                    // ignore send failure inside chunk callback
                }
            });

            if (buffer.length() > 0) {
                emitter.send(SseEmitter.event().name("chunk").data(buffer.toString()));
            }
            emitter.send(SseEmitter.event().name("final").data(objectMapper.writeValueAsString(finalResponse)));
            emitter.send(SseEmitter.event().name("complete").data("ok"));
            emitter.complete();
        } catch (Exception e) {
            try {
                emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
            } catch (Exception ignored) {
                // ignore
            }
            emitter.completeWithError(e);
        }
    }

    private boolean shouldFlush(StringBuilder buffer) {
        int length = buffer.length();
        if (length == 0) {
            return false;
        }
        char last = buffer.charAt(length - 1);
        if (Character.isWhitespace(last) || isPunctuation(last)) {
            return true;
        }
        return length >= 24;
    }

    private boolean isPunctuation(char c) {
        return c == '.' || c == ',' || c == '!' || c == '?' || c == ':' || c == ';' || c == ')';
    }
}

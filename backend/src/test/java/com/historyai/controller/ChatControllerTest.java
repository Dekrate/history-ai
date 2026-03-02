package com.historyai.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.historyai.dto.ChatHistoryResponse;
import com.historyai.dto.ChatMessageDto;
import com.historyai.dto.ChatRequest;
import com.historyai.dto.ChatResponse;
import com.historyai.service.ChatService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Executor streamingExecutor;

    @Test
    void sendMessage_ShouldReturnOkWithResponseBody() {
        ChatController chatController = new ChatController(chatService, objectMapper, streamingExecutor);
        UUID characterId = UUID.randomUUID();

        ChatRequest request = new ChatRequest();
        request.setCharacterId(characterId);
        request.setMessage("Kim jesteś?");
        request.setConversationId("conv-1");

        ChatResponse responseBody = new ChatResponse("Jestem historyczną postacią.", "conv-1", characterId);
        when(chatService.chat(request)).thenReturn(responseBody);

        ResponseEntity<ChatResponse> response = chatController.sendMessage(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseBody, response.getBody());
        verify(chatService).chat(request);
    }

    @Test
    void getHistory_ShouldReturnOkWithMessages() {
        ChatController chatController = new ChatController(chatService, objectMapper, streamingExecutor);
        ChatHistoryResponse responseBody = new ChatHistoryResponse(List.of(
                new ChatMessageDto("USER", "Pytanie", LocalDateTime.now()),
                new ChatMessageDto("ASSISTANT", "Odpowiedź", LocalDateTime.now())
        ));
        when(chatService.getHistory("conv-1")).thenReturn(responseBody);

        ResponseEntity<ChatHistoryResponse> response = chatController.getHistory("conv-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().getMessages().size());
        verify(chatService).getHistory("conv-1");
    }

    @Test
    void streamMessage_ShouldReturnEmitter() {
        ChatController chatController = new ChatController(chatService, objectMapper, streamingExecutor);
        SseEmitter emitter = chatController.streamMessage("Hej", UUID.randomUUID().toString(), null);
        assertEquals(SseEmitter.class, emitter.getClass());
    }
}

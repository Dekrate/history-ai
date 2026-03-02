package com.historyai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import com.historyai.client.OllamaClient;
import com.historyai.dto.ChatHistoryResponse;
import com.historyai.dto.ChatRequest;
import com.historyai.dto.ChatResponse;
import com.historyai.entity.Conversation;
import com.historyai.entity.HistoricalCharacter;
import com.historyai.entity.Message;
import com.historyai.exception.CharacterNotFoundException;
import com.historyai.repository.ConversationRepository;
import com.historyai.repository.HistoricalCharacterRepository;
import com.historyai.repository.MessageRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private HistoricalCharacterRepository characterRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private WikiquoteService wikiquoteService;

    @Mock
    private ChatPromptBuilder chatPromptBuilder;

    @Mock
    private OllamaClient ollamaClient;

    private ChatService chatService;

    private UUID characterId;
    private HistoricalCharacter character;

    @BeforeEach
    void setUp() {
        characterId = UUID.randomUUID();
        character = new HistoricalCharacter();
        character.setId(characterId);
        character.setName("Mikołaj Kopernik");
        character.setBiography("Astronom i matematyk.");
        character.setEra("Renesans");
        character.setNationality("Polska");
        chatService = new ChatService(
                characterRepository,
                conversationRepository,
                messageRepository,
                wikiquoteService,
                chatPromptBuilder,
                ollamaClient,
                12,
                600,
                20,
                1200
        );
    }

    @Test
    void chat_WhenCharacterExists_ShouldReturnModelResponseWithConversationId() {
        ChatRequest request = new ChatRequest();
        request.setMessage("Co odkryłeś?");
        request.setCharacterId(characterId);

        Conversation conversation = new Conversation();
        UUID conversationId = UUID.randomUUID();
        conversation.setId(conversationId);
        conversation.setCharacter(character);

        Message previousMessage = new Message();
        previousMessage.setRole(Message.Role.USER);
        previousMessage.setContent("Kim jesteś?");
        previousMessage.setTimestamp(LocalDateTime.now().minusMinutes(1));

        when(characterRepository.findById(characterId)).thenReturn(Optional.of(character));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(messageRepository.findByConversationIdOrderByTimestampAsc(conversationId))
                .thenReturn(List.of(previousMessage));
        when(wikiquoteService.getQuotes("Mikołaj Kopernik")).thenReturn(List.of("Wstrzymał Słońce."));
        when(chatPromptBuilder.build(eq(character), any(), any(), any(), eq("Co odkryłeś?")))
                .thenReturn("prompt");
        when(ollamaClient.getDefaultModel()).thenReturn("model");
        when(ollamaClient.generate("model", "prompt")).thenReturn("Opracowałem teorię heliocentryczną.");

        ChatResponse response = chatService.chat(request);

        assertEquals("Opracowałem teorię heliocentryczną.", response.getMessage());
        assertEquals(characterId, response.getCharacterId());
        assertEquals(conversationId.toString(), response.getConversationId());
        verify(messageRepository, times(2)).save(any(Message.class));
        verify(wikiquoteService).getQuotes("Mikołaj Kopernik");
    }

    @Test
    void chat_WhenConversationIdProvided_ShouldReuseIt() {
        UUID conversationId = UUID.randomUUID();

        ChatRequest request = new ChatRequest();
        request.setMessage("Kim jesteś?");
        request.setCharacterId(characterId);
        request.setConversationId(conversationId.toString());

        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setCharacter(character);

        when(characterRepository.findById(characterId)).thenReturn(Optional.of(character));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderByTimestampAsc(conversationId))
                .thenReturn(List.of());
        when(wikiquoteService.getQuotes("Mikołaj Kopernik")).thenReturn(List.of());
        when(chatPromptBuilder.build(eq(character), any(), any(), any(), eq("Kim jesteś?"))).thenReturn("prompt");
        when(ollamaClient.getDefaultModel()).thenReturn("model");
        when(ollamaClient.generate("model", "prompt")).thenReturn("Jestem Kopernik.");

        ChatResponse response = chatService.chat(request);

        assertEquals(conversationId.toString(), response.getConversationId());
    }

    @Test
    void chat_WhenCharacterMissing_ShouldThrowNotFound() {
        ChatRequest request = new ChatRequest();
        request.setMessage("Pytanie");
        request.setCharacterId(characterId);

        when(characterRepository.findById(characterId)).thenReturn(Optional.empty());

        assertThrows(CharacterNotFoundException.class, () -> chatService.chat(request));
    }

    @Test
    void chatStream_ShouldEmitChunksAndReturnFinalResponse() {
        ChatRequest request = new ChatRequest();
        request.setMessage("Powiedz coś");
        request.setCharacterId(characterId);

        Conversation conversation = new Conversation();
        UUID conversationId = UUID.randomUUID();
        conversation.setId(conversationId);
        conversation.setCharacter(character);

        when(characterRepository.findById(characterId)).thenReturn(Optional.of(character));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(messageRepository.findByConversationIdOrderByTimestampAsc(conversationId)).thenReturn(List.of());
        when(wikiquoteService.getQuotes("Mikołaj Kopernik")).thenReturn(List.of());
        when(chatPromptBuilder.build(eq(character), any(), any(), any(), eq("Powiedz coś"))).thenReturn("prompt");
        when(ollamaClient.getDefaultModel()).thenReturn("model");

        AtomicReference<String> streamed = new AtomicReference<>("");
        org.mockito.Mockito.doAnswer(invocation -> {
            java.util.function.Consumer<String> consumer = invocation.getArgument(2);
            consumer.accept("To ");
            consumer.accept("jest ");
            consumer.accept("stream.");
            return null;
        }).when(ollamaClient).generateStream(eq("model"), eq("prompt"), any());

        ChatResponse response = chatService.chatStream(request, chunk -> streamed.set(streamed.get() + chunk));

        assertEquals("To jest stream.", response.getMessage());
        assertEquals(conversationId.toString(), response.getConversationId());
        assertEquals("To jest stream.", streamed.get());
        verify(messageRepository, times(2)).save(any(Message.class));
    }

    @Test
    void getHistory_WhenConversationExists_ShouldReturnMessages() {
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);

        Message user = new Message();
        user.setRole(Message.Role.USER);
        user.setContent("Pytanie");
        user.setTimestamp(LocalDateTime.now().minusMinutes(1));

        Message assistant = new Message();
        assistant.setRole(Message.Role.ASSISTANT);
        assistant.setContent("Odpowiedź");
        assistant.setTimestamp(LocalDateTime.now());

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderByTimestampAsc(conversationId))
                .thenReturn(List.of(user, assistant));

        ChatHistoryResponse response = chatService.getHistory(conversationId.toString());

        assertNotNull(response);
        assertEquals(2, response.getMessages().size());
        assertEquals("USER", response.getMessages().get(0).getRole());
        assertEquals("ASSISTANT", response.getMessages().get(1).getRole());
    }

    @Test
    void chat_WhenHistoryIsLong_ShouldPersistConversationSummary() {
        ChatService service = new ChatService(
                characterRepository,
                conversationRepository,
                messageRepository,
                wikiquoteService,
                chatPromptBuilder,
                ollamaClient,
                2,
                600,
                3,
                1200
        );

        ChatRequest request = new ChatRequest();
        request.setMessage("Nowe pytanie");
        request.setCharacterId(characterId);

        Conversation conversation = new Conversation();
        UUID conversationId = UUID.randomUUID();
        conversation.setId(conversationId);
        conversation.setCharacter(character);

        Message m1 = new Message();
        m1.setRole(Message.Role.USER);
        m1.setContent("Pytanie 1");
        m1.setTimestamp(LocalDateTime.now().minusMinutes(3));
        Message m2 = new Message();
        m2.setRole(Message.Role.ASSISTANT);
        m2.setContent("Odpowiedź 1");
        m2.setTimestamp(LocalDateTime.now().minusMinutes(2));
        Message m3 = new Message();
        m3.setRole(Message.Role.USER);
        m3.setContent("Pytanie 2");
        m3.setTimestamp(LocalDateTime.now().minusMinutes(1));

        when(characterRepository.findById(characterId)).thenReturn(Optional.of(character));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(messageRepository.findByConversationIdOrderByTimestampAsc(conversationId))
                .thenReturn(List.of(m1, m2, m3));
        when(wikiquoteService.getQuotes("Mikołaj Kopernik")).thenReturn(List.of());
        when(chatPromptBuilder.build(eq(character), any(), any(), any(), eq("Nowe pytanie"))).thenReturn("prompt");
        when(ollamaClient.getDefaultModel()).thenReturn("model");
        when(ollamaClient.generate("model", "prompt")).thenReturn("Odpowiedź nowa");

        service.chat(request);

        verify(conversationRepository, times(2)).save(any(Conversation.class));
    }
}

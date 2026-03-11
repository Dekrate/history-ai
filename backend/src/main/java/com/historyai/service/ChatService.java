package com.historyai.service;

import com.historyai.client.OllamaClient;
import com.historyai.dto.ChatHistoryResponse;
import com.historyai.dto.ChatMessageDto;
import com.historyai.dto.ChatRequest;
import com.historyai.dto.ChatResponse;
import com.historyai.entity.Conversation;
import com.historyai.entity.HistoricalCharacter;
import com.historyai.entity.Message;
import com.historyai.exception.CharacterNotFoundException;
import com.historyai.repository.ConversationRepository;
import com.historyai.repository.HistoricalCharacterRepository;
import com.historyai.repository.MessageRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for roleplay chat with historical characters.
 */
@Service
public class ChatService {

    private static final String DEFAULT_USER_ID = "anonymous";

    private final HistoricalCharacterRepository characterRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final WikiquoteService wikiquoteService;
    private final ChatPromptBuilder chatPromptBuilder;
    private final OllamaClient ollamaClient;
    private final int contextWindowMessages;
    private final int contextMessageMaxChars;
    private final int summaryTriggerMessages;
    private final int summaryMaxChars;

    public ChatService(HistoricalCharacterRepository characterRepository,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            WikiquoteService wikiquoteService,
            ChatPromptBuilder chatPromptBuilder,
            OllamaClient ollamaClient,
            @Value("${app.chat.context-window-messages:12}") int contextWindowMessages,
            @Value("${app.chat.context-message-max-chars:600}") int contextMessageMaxChars,
            @Value("${app.chat.summary-trigger-messages:20}") int summaryTriggerMessages,
            @Value("${app.chat.summary-max-chars:1200}") int summaryMaxChars) {
        this.characterRepository = characterRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.wikiquoteService = wikiquoteService;
        this.chatPromptBuilder = chatPromptBuilder;
        this.ollamaClient = ollamaClient;
        this.contextWindowMessages = Math.max(1, contextWindowMessages);
        this.contextMessageMaxChars = Math.max(100, contextMessageMaxChars);
        this.summaryTriggerMessages = Math.max(contextWindowMessages + 1, summaryTriggerMessages);
        this.summaryMaxChars = Math.max(300, summaryMaxChars);
    }

    @Transactional
    public ChatResponse chat(ChatRequest request) {
        HistoricalCharacter character = characterRepository.findById(request.getCharacterId())
                .orElseThrow(() -> new CharacterNotFoundException(request.getCharacterId()));

        Conversation conversation = resolveConversation(character, request.getConversationId());
        MemoryContext memoryContext = buildMemoryContext(conversation);
        saveMessage(conversation, Message.Role.USER, request.getMessage());
        String userMessageForPrompt = clipForPrompt(request.getMessage());

        List<String> quotes = wikiquoteService.getQuotes(character.getName());
        String prompt = chatPromptBuilder.build(
                character,
                quotes,
                memoryContext.longTermSummary(),
                memoryContext.history(),
                userMessageForPrompt);
        String modelResponse = ollamaClient.generate(ollamaClient.getDefaultModel(), prompt);

        String content = modelResponse == null ? "" : modelResponse.trim();
        if (content.isBlank()) {
            content = "Nie mam teraz pewnej odpowiedzi. Sformułuj proszę pytanie inaczej.";
        }
        saveMessage(conversation, Message.Role.ASSISTANT, content);

        return new ChatResponse(content, conversation.getId().toString(), character.getId());
    }

    @Transactional
    public ChatResponse chatStream(ChatRequest request, Consumer<String> chunkConsumer) {
        HistoricalCharacter character = characterRepository.findById(request.getCharacterId())
                .orElseThrow(() -> new CharacterNotFoundException(request.getCharacterId()));

        Conversation conversation = resolveConversation(character, request.getConversationId());
        MemoryContext memoryContext = buildMemoryContext(conversation);
        saveMessage(conversation, Message.Role.USER, request.getMessage());
        String userMessageForPrompt = clipForPrompt(request.getMessage());

        List<String> quotes = wikiquoteService.getQuotes(character.getName());
        String prompt = chatPromptBuilder.build(
                character,
                quotes,
                memoryContext.longTermSummary(),
                memoryContext.history(),
                userMessageForPrompt);

        StringBuilder fullResponse = new StringBuilder();
        ollamaClient.generateStream(ollamaClient.getDefaultModel(), prompt, chunk -> {
            fullResponse.append(chunk);
            if (chunkConsumer != null) {
                chunkConsumer.accept(chunk);
            }
        });

        String content = fullResponse.toString().trim();
        if (content.isBlank()) {
            content = "Nie mam teraz pewnej odpowiedzi. Sformułuj proszę pytanie inaczej.";
        }
        saveMessage(conversation, Message.Role.ASSISTANT, content);
        return new ChatResponse(content, conversation.getId().toString(), character.getId());
    }

    @Transactional(readOnly = true)
    public ChatHistoryResponse getHistory(String conversationId) {
        UUID parsedConversationId = parseConversationId(conversationId);
        conversationRepository.findById(parsedConversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));
        List<ChatMessageDto> messages = messageRepository.findByConversationIdOrderByTimestampAsc(parsedConversationId)
                .stream()
                .map(ChatMessageDto::fromEntity)
                .toList();
        return new ChatHistoryResponse(messages);
    }

    private MemoryContext buildMemoryContext(Conversation conversation) {
        List<Message> allMessages = messageRepository.findByConversationIdOrderByTimestampAsc(conversation.getId());
        String summary = conversation.getContextSummary();

        if (allMessages.size() >= summaryTriggerMessages) {
            int boundary = Math.max(0, allMessages.size() - contextWindowMessages);
            List<Message> olderMessages = allMessages.subList(0, boundary);
            String generated = generateSummary(olderMessages);
            if (!generated.isBlank() && !generated.equals(summary)) {
                conversation.setContextSummary(generated);
                conversationRepository.save(conversation);
                summary = generated;
            }
        }

        int fromIndex = Math.max(0, allMessages.size() - contextWindowMessages);
        List<Message> recentMessages = allMessages.subList(fromIndex, allMessages.size());
        List<ChatPromptBuilder.HistoryEntry> history = toHistoryEntries(recentMessages);
        return new MemoryContext(summary, history);
    }

    private Conversation resolveConversation(HistoricalCharacter character, String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            Conversation conversation = new Conversation();
            conversation.setCharacter(character);
            conversation.setUserId(DEFAULT_USER_ID);
            conversation.setTitle(buildTitle(character.getName()));
            return conversationRepository.save(conversation);
        }

        UUID parsedConversationId = parseConversationId(conversationId);
        Conversation conversation = conversationRepository.findById(parsedConversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));

        if (!conversation.getCharacter().getId().equals(character.getId())) {
            throw new IllegalArgumentException("Conversation does not belong to requested character");
        }
        return conversation;
    }

    private UUID parseConversationId(String conversationId) {
        try {
            return UUID.fromString(conversationId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid conversationId format: " + conversationId);
        }
    }

    private List<ChatPromptBuilder.HistoryEntry> toHistoryEntries(List<Message> recentMessages) {
        List<ChatPromptBuilder.HistoryEntry> history = new ArrayList<>();
        for (Message message : recentMessages) {
            history.add(new ChatPromptBuilder.HistoryEntry(
                    message.getRole() != null ? message.getRole().name() : "USER",
                    clipForPrompt(message.getContent())
            ));
        }
        return history;
    }

    private String generateSummary(List<Message> messages) {
        if (messages.isEmpty()) {
            return "";
        }
        StringBuilder summary = new StringBuilder("Key prior conversation points:\n");
        int count = 0;
        for (Message message : messages) {
            if (message.getContent() == null || message.getContent().isBlank()) {
                continue;
            }
            String role = message.getRole() == Message.Role.ASSISTANT ? "Assistant" : "User";
            String line = role + ": " + clipForSummary(message.getContent());
            if (summary.length() + line.length() + 2 > summaryMaxChars) {
                break;
            }
            summary.append("- ").append(line).append('\n');
            count++;
        }
        if (count == 0) {
            return "";
        }
        return summary.toString().trim();
    }

    private void saveMessage(Conversation conversation, Message.Role role, String content) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setRole(role);
        message.setContent(content != null ? content.trim() : "");
        messageRepository.save(message);
    }

    private String buildTitle(String characterName) {
        String base = characterName == null || characterName.isBlank() ? "Rozmowa" : "Rozmowa z " + characterName;
        if (base.length() <= 255) {
            return base;
        }
        return base.substring(0, 255);
    }

    private String clipForPrompt(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.length() <= contextMessageMaxChars) {
            return normalized;
        }
        return normalized.substring(0, contextMessageMaxChars) + "...";
    }

    private String clipForSummary(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        int max = Math.min(220, contextMessageMaxChars);
        if (normalized.length() <= max) {
            return normalized;
        }
        return normalized.substring(0, max) + "...";
    }

    private record MemoryContext(String longTermSummary, List<ChatPromptBuilder.HistoryEntry> history) { }
}

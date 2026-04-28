package com.smartdesk.backend.service;


import com.smartdesk.backend.entity.Conversation;
import com.smartdesk.backend.entity.Message;
import com.smartdesk.backend.repository.ConversationRepository;
import com.smartdesk.backend.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private AIClientService aiClientService;

    @InjectMocks
    private ChatService chatService;

    private Conversation testConversation;

    @BeforeEach
    void setUp() {
        testConversation = new Conversation();
        testConversation.setId(1L);
        testConversation.setUserId(1L);
        testConversation.setTitle("New Conversation");
        testConversation.setStatus("ACTIVE");
    }

    @Test
    void createConversation_returnsConversation() {
        when(conversationRepository.save(any(Conversation.class)))
                .thenReturn(testConversation);

        Conversation result = chatService.createConversation(1L, "Test");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(conversationRepository, times(1)).save(any());
    }

    @Test
    void createConversation_withNullTitle_usesDefault() {
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Conversation result = chatService.createConversation(1L, null);

        assertEquals("New Conversation", result.getTitle());
    }

    @Test
    void sendMessage_callsAIServiceAndSavesMessages() {
        when(conversationRepository.findById(1L))
                .thenReturn(Optional.of(testConversation));
        when(messageRepository.save(any(Message.class)))
                .thenAnswer(inv -> {
                    Message m = inv.getArgument(0);
                    m.setId(System.currentTimeMillis());
                    return m;
                });
        when(messageRepository
                .findTop10ByConversationIdOrderByCreatedAtDesc(1L))
                .thenReturn(new ArrayList<>());

        Map<String, Object> aiResult = new HashMap<>();
        aiResult.put("answer", "This is the AI answer");
        aiResult.put("sources", new ArrayList<>());
        aiResult.put("model", "gemini-1.5-flash");

        when(aiClientService.queryRAG(anyString(), anyList(), anyInt()))
                .thenReturn(aiResult);
        when(conversationRepository.save(any()))
                .thenReturn(testConversation);

        Map<String, Object> result = chatService.sendMessage(
                1L, "What is RAG?", 1L, 5);

        assertNotNull(result);
        assertEquals("This is the AI answer", result.get("answer"));
        verify(messageRepository, times(2)).save(any(Message.class));
        verify(aiClientService, times(1))
                .queryRAG(anyString(), anyList(), anyInt());
    }

    @Test
    void sendMessage_withInvalidConversation_throwsException() {
        when(conversationRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> chatService.sendMessage(99L, "Hello", 1L, 5));
    }

    @Test
    void getUserConversations_returnsUserConversations() {
        List<Conversation> convList = List.of(testConversation);
        when(conversationRepository
                .findByUserIdOrderByUpdatedAtDesc(1L))
                .thenReturn(convList);

        List<Conversation> result = chatService.getUserConversations(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }
}

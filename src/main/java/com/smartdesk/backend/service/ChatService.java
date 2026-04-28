package com.smartdesk.backend.service;


import com.smartdesk.backend.entity.Conversation;
import com.smartdesk.backend.entity.Message;
import com.smartdesk.backend.repository.ConversationRepository;
import com.smartdesk.backend.repository.MessageRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final AIClientService aiClientService;

    public ChatService(ConversationRepository conversationRepository,
                       MessageRepository messageRepository,
                       AIClientService aiClientService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.aiClientService = aiClientService;
    }

    public Conversation createConversation(Long userId, String title) {
        Conversation conv = new Conversation();
        conv.setUserId(userId);
        conv.setTitle(title != null ? title : "New Conversation");
        conv.setStatus("ACTIVE");
        return conversationRepository.save(conv);
    }

    public List<Conversation> getUserConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    public Map<String, Object> sendMessage(Long conversationId,
                                           String userMessage,
                                           Long userId,
                                           int topK) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        Message userMsg = new Message();
        userMsg.setConversationId(conversationId);
        userMsg.setRole("USER");
        userMsg.setContent(userMessage);
        messageRepository.save(userMsg);

        List<Message> history = messageRepository
                .findTop10ByConversationIdOrderByCreatedAtDesc(conversationId);
        Collections.reverse(history);

        List<Map<String, Object>> historyMaps = history.stream()
                .map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("role", m.getRole().toLowerCase());
                    map.put("content", m.getContent());
                    return map;
                })
                .collect(Collectors.toList());

        long startTime = System.currentTimeMillis();
        Map<String, Object> aiResponse =
                aiClientService.queryRAG(userMessage, historyMaps, topK);
        long latency = System.currentTimeMillis() - startTime;

        String answer = (String) aiResponse.getOrDefault("answer", "No answer");

        Message aiMsg = new Message();
        aiMsg.setConversationId(conversationId);
        aiMsg.setRole("ASSISTANT");
        aiMsg.setContent(answer);
        aiMsg.setLatencyMs(latency);
        aiMsg.setModelUsed((String) aiResponse.getOrDefault("model", "gemini"));
        messageRepository.save(aiMsg);

        conv.setUpdatedAt(LocalDateTime.now());
        if (conv.getTitle().equals("New Conversation")) {
            conv.setTitle(userMessage.length() > 50
                    ? userMessage.substring(0, 50) + "..."
                    : userMessage);
        }
        conversationRepository.save(conv);

        Map<String, Object> result = new HashMap<>();
        result.put("messageId", aiMsg.getId());
        result.put("answer", answer);
        result.put("sources", aiResponse.getOrDefault("sources", List.of()));
        result.put("latencyMs", latency);
        result.put("model", aiMsg.getModelUsed());
        return result;
    }

    public List<Message> getMessages(Long conversationId) {
        return messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    public void deleteConversation(Long conversationId, Long userId) {
        conversationRepository.findById(conversationId).ifPresent(conv -> {
            if (conv.getUserId().equals(userId)) {
                conv.setStatus("DELETED");
                conversationRepository.save(conv);
            }
        });
    }
}

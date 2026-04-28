package com.smartdesk.backend.controller;



import com.smartdesk.backend.dto.ApiResponse;
import com.smartdesk.backend.dto.ChatRequest;
import com.smartdesk.backend.entity.Conversation;
import com.smartdesk.backend.entity.Message;
import com.smartdesk.backend.repository.UserRepository;
import com.smartdesk.backend.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    public ChatController(ChatService chatService,
                          UserRepository userRepository) {
        this.chatService = chatService;
        this.userRepository = userRepository;
    }

    @PostMapping("/conversations")
    public ResponseEntity<ApiResponse<Conversation>> createConversation(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        Conversation conv = chatService.createConversation(
                userId, body.get("title"));
        return ResponseEntity.ok(ApiResponse.ok(conv));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<Conversation>>> getConversations(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(
                ApiResponse.ok(chatService.getUserConversations(userId)));
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiResponse<List<Message>>> getMessages(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok(chatService.getMessages(id)));
    }

    @PostMapping("/message")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendMessage(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        try {
            Map<String, Object> result = chatService.sendMessage(
                    request.getConversationId(),
                    request.getMessage(),
                    userId,
                    request.getTopK());
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        chatService.deleteConversation(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Deleted", null));
    }

    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow().getId();
    }
}
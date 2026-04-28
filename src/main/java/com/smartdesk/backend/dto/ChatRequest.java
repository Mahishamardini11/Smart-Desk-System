package com.smartdesk.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ChatRequest {

    @NotNull
    private Long conversationId;

    @NotBlank
    private String message;

    private Integer topK = 5;

    public ChatRequest() {}

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }
}

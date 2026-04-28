package com.smartdesk.backend.service;


import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AIClientService {

    private final WebClient aiWebClient;

    public AIClientService(WebClient aiWebClient) {
        this.aiWebClient = aiWebClient;
    }

    public Map<String, Object> queryRAG(String question,
                                        List<Map<String, Object>> history,
                                        int topK) {
        Map<String, Object> request = new HashMap<>();
        request.put("question", question);
        request.put("conversation_history", history);
        request.put("top_k", topK);

        try {
            Map response = aiWebClient.post()
                    .uri("/api/rag/query")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return response != null ? response : Map.of("answer", "AI service unavailable");
        } catch (Exception e) {
            return Map.of("answer",
                    "AI service error: " + e.getMessage(),
                    "sources", List.of());
        }
    }

    public Flux<String> queryRAGStream(String question,
                                       List<Map<String, Object>> history,
                                       int topK) {
        Map<String, Object> request = new HashMap<>();
        request.put("question", question);
        request.put("conversation_history", history);
        request.put("top_k", topK);
        request.put("stream", true);

        return aiWebClient.post()
                .uri("/api/rag/stream")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class);
    }
}

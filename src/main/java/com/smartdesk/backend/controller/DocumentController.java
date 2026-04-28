package com.smartdesk.backend.controller;



import com.smartdesk.backend.dto.ApiResponse;
import com.smartdesk.backend.entity.Document;
import com.smartdesk.backend.service.DocumentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.smartdesk.backend.repository.UserRepository;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final UserRepository userRepository;

    public DocumentController(DocumentService documentService,
                              UserRepository userRepository) {
        this.documentService = documentService;
        this.userRepository = userRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Document>> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = getUserId(userDetails);
            Document doc = documentService.uploadDocument(file, userId);
            return ResponseEntity.ok(ApiResponse.ok("Document uploaded", doc));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Document>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        Page<Document> docs = documentService.getDocuments(
                userId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(docs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Document>> getById(
            @PathVariable Long id) {
        return documentService.getDocument(id)
                .map(doc -> ResponseEntity.ok(ApiResponse.ok(doc)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ResponseEntity.ok(ApiResponse.ok("Document deleted", null));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stats(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(
                ApiResponse.ok(documentService.getStats(userId)));
    }

    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow().getId();
    }
}
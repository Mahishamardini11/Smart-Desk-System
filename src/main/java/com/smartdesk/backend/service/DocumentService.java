package com.smartdesk.backend.service;



import com.smartdesk.backend.entity.Document;
import com.smartdesk.backend.entity.DocumentChunk;
import com.smartdesk.backend.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final WebClient aiWebClient;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public DocumentService(DocumentRepository documentRepository,
                           WebClient aiWebClient) {
        this.documentRepository = documentRepository;
        this.aiWebClient = aiWebClient;
    }

    public Document uploadDocument(MultipartFile file, Long userId) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String uniqueFilename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        Document doc = new Document();
        doc.setFilename(uniqueFilename);
        doc.setOriginalName(file.getOriginalFilename());
        doc.setFileType(getFileExtension(file.getOriginalFilename()));
        doc.setFileSize(file.getSize());
        doc.setUserId(userId);
        doc.setStatus("UPLOADED");
        documentRepository.save(doc);

        processDocumentAsync(doc.getId(), filePath.toString());
        return doc;
    }

    @Async
    public void processDocumentAsync(Long documentId, String filePath) {
        Document doc = documentRepository.findById(documentId).orElse(null);
        if (doc == null) return;

        doc.setStatus("PROCESSING");
        documentRepository.save(doc);

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("document_id", documentId);
            request.put("file_path", filePath);
            request.put("chunking_strategy", doc.getChunkingStrategy());

            Map response = aiWebClient.post()
                    .uri("/api/documents/process")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                doc.setStatus("INDEXED");
                doc.setChunkCount((Integer) response.getOrDefault("chunk_count", 0));
            } else {
                doc.setStatus("FAILED");
                doc.setErrorMessage("AI service processing failed");
            }
        } catch (Exception e) {
            doc.setStatus("FAILED");
            doc.setErrorMessage(e.getMessage());
        }
        documentRepository.save(doc);
    }

    public Page<Document> getDocuments(Long userId, Pageable pageable) {
        return documentRepository.findByUserId(userId, pageable);
    }

    public Optional<Document> getDocument(Long id) {
        return documentRepository.findById(id);
    }

    public void deleteDocument(Long id) {
        documentRepository.findById(id).ifPresent(doc -> {
            doc.setStatus("DELETED");
            documentRepository.save(doc);
            try {
                aiWebClient.delete()
                        .uri("/api/documents/" + id)
                        .retrieve()
                        .bodyToMono(Void.class)
                        .block();
            } catch (Exception ignored) {}
        });
    }

    public Map<String, Object> getStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDocuments", documentRepository.countByUserId(userId));
        stats.put("totalSize", documentRepository.sumFileSizeByUserId(userId));
        return stats;
    }

    private String getFileExtension(String filename) {
        if (filename == null) return "unknown";
        int idx = filename.lastIndexOf('.');
        return idx > 0 ? filename.substring(idx + 1).toLowerCase() : "unknown";
    }
}

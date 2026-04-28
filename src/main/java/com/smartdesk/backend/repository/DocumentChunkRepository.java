package com.smartdesk.backend.repository;


import com.smartdesk.backend.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository
        extends JpaRepository<DocumentChunk, Long> {
    List<DocumentChunk> findByDocumentIdOrderByChunkIndex(Long documentId);
    void deleteByDocumentId(Long documentId);
    Long countByDocumentId(Long documentId);
}
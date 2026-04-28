package com.smartdesk.backend.repository;



import com.smartdesk.backend.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    Page<Document> findByUserId(Long userId, Pageable pageable);
    List<Document> findByStatus(String status);

    @Query("SELECT COUNT(d) FROM Document d WHERE d.userId = :userId")
    Long countByUserId(Long userId);

    @Query("SELECT SUM(d.fileSize) FROM Document d WHERE d.userId = :userId")
    Long sumFileSizeByUserId(Long userId);
}
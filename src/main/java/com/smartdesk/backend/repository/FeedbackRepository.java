package com.smartdesk.backend.repository;

import com.smartdesk.backend.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByMessageId(Long messageId);
    List<Feedback> findByUserId(Long userId);
}
package com.smartdesk.backend.repository;



import com.smartdesk.backend.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByUserIdOrderByUpdatedAtDesc(Long userId);
    List<Conversation> findByUserIdAndStatus(Long userId, String status);
}

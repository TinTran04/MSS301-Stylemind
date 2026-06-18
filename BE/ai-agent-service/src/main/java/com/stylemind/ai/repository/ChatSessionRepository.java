package com.stylemind.ai.repository;

import com.stylemind.ai.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    List<ChatSession> findByUserId(String userId);
    Optional<ChatSession> findByIdAndUserId(UUID id, String userId);
}
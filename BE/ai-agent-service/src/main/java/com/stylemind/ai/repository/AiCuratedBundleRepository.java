package com.stylemind.ai.repository;

import com.stylemind.ai.entity.AiCuratedBundle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiCuratedBundleRepository extends JpaRepository<AiCuratedBundle, String> {
    Optional<AiCuratedBundle> findByMessageId(String messageId);
}
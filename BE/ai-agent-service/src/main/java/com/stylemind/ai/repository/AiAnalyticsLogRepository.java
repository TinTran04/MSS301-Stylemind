package com.stylemind.ai.repository;

import com.stylemind.ai.entity.AiAnalyticsLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiAnalyticsLogRepository extends JpaRepository<AiAnalyticsLog, String> {
    List<AiAnalyticsLog> findByUserId(String userId);
    List<AiAnalyticsLog> findByBundleId(String bundleId);
    List<AiAnalyticsLog> findByInteractionType(String interactionType);
    Page<AiAnalyticsLog> findByUserId(String userId, Pageable pageable);
    Page<AiAnalyticsLog> findByBundleId(String bundleId, Pageable pageable);
    Page<AiAnalyticsLog> findByInteractionType(String interactionType, Pageable pageable);
}

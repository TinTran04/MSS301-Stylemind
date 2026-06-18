package com.stylemind.ai.repository;

import com.stylemind.ai.entity.AiIndexJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiIndexJobRepository extends JpaRepository<AiIndexJob, String> {
    List<AiIndexJob> findByStatus(String status);
    List<AiIndexJob> findByTargetTypeAndTargetId(String targetType, String targetId);
    List<AiIndexJob> findByStatusAndRetryCountLessThan(String status, int maxRetry);
}
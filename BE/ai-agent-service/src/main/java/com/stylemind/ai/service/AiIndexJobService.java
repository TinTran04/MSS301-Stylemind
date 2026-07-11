package com.stylemind.ai.service;

import com.stylemind.ai.dto.IndexJobRequest;
import com.stylemind.ai.dto.IndexJobResponse;
import com.stylemind.ai.entity.AiIndexJob;
import com.stylemind.ai.repository.AiIndexJobRepository;
import com.stylemind.common.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;

// MVP: jobs are recorded as PENDING; no background worker processes them yet
// (see docs/services/ai-agent-service.md - vector/graph indexing is phase sau).
@Service
@RequiredArgsConstructor
@Transactional
public class AiIndexJobService {

    private final AiIndexJobRepository indexJobRepository;

    public Page<IndexJobResponse> getIndexJobs(String status, String targetType, Pageable pageable) {
        return indexJobRepository.search(status, targetType, pageable).map(this::toResponse);
    }

    public IndexJobResponse createIndexJob(IndexJobRequest request) {
        AiIndexJob job = AiIndexJob.builder()
                .id(StringUtil.generateUniqueId())
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .operationType(request.getOperationType())
                .status("PENDING")
                .retryCount(0)
                .build();
        return toResponse(indexJobRepository.save(job));
    }

    private IndexJobResponse toResponse(AiIndexJob job) {
        return IndexJobResponse.builder()
                .id(job.getId())
                .targetType(job.getTargetType())
                .targetId(job.getTargetId())
                .operationType(job.getOperationType())
                .status(job.getStatus())
                .retryCount(job.getRetryCount())
                .lastErrorMessage(job.getLastErrorMessage())
                .createdAt(job.getCreatedAt() == null ? null : job.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant())
                .updatedAt(job.getUpdatedAt() == null ? null : job.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant())
                .build();
    }
}

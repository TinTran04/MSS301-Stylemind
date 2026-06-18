package com.stylemind.ai.controller;

import com.stylemind.ai.dto.*;
import com.stylemind.ai.entity.AiAnalyticsLog;
import com.stylemind.ai.entity.AiIndexJob;
import com.stylemind.ai.repository.AiAnalyticsLogRepository;
import com.stylemind.ai.repository.AiIndexJobRepository;
import com.stylemind.ai.service.AiIndexJobService;
import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAiController {

    private final AiIndexJobService indexJobService;
    private final AiIndexJobRepository indexJobRepository;
    private final AiAnalyticsLogRepository analyticsLogRepository;

    @GetMapping("/pipeline/events")
    public ResponseEntity<ApiResponse<List<PipelineEventResponse>>> getPipelineEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        Page<AiIndexJob> jobs = indexJobRepository.findAll(
                org.springframework.data.domain.PageRequest.of(page, size)
        );
        
        List<PipelineEventResponse> events = jobs.getContent().stream()
                .map(job -> PipelineEventResponse.builder()
                        .eventId(job.getId())
                        .eventType(String.format("%s_%s", job.getTargetType(), job.getOperationType()))
                        .targetId(job.getTargetId())
                        .status(job.getStatus())
                        .durationMs(0L) // TODO: Calculate actual duration
                        .timestamp(job.getCreatedAt().toInstant())
                        .build())
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success("Lấy logs sự kiện AI Pipeline thành công", events));
    }

    @GetMapping("/graph/status")
    public ResponseEntity<ApiResponse<GraphStatusResponse>> getGraphStatus() {
        GraphStatusResponse response = GraphStatusResponse.builder()
                .totalNodes(0L) // TODO: Query Neo4j
                .totalRelationships(0L)
                .neo4jStatus("UP")
                .build();
        return ResponseEntity.ok(ApiResponse.success("Trạng thái đồ thị tri thức", response));
    }

    @GetMapping("/analytics-logs")
    public ResponseEntity<ApiResponse<PageResponse<AnalyticsLogResponse>>> getAnalyticsLogs(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String bundleId,
            @RequestParam(required = false) String interactionType,
            @PageableDefault(size = 50) Pageable pageable) {
        
        Page<AiAnalyticsLog> logs;
        if (userId != null) {
            logs = analyticsLogRepository.findByUserId(userId, pageable);
        } else if (bundleId != null) {
            logs = analyticsLogRepository.findByBundleId(bundleId, pageable);
        } else if (interactionType != null) {
            logs = analyticsLogRepository.findByInteractionType(interactionType, pageable);
        } else {
            logs = analyticsLogRepository.findAll(pageable);
        }
        
        PageResponse<AnalyticsLogResponse> response = new PageResponse<>();
        response.setContent(logs.getContent().stream()
                .map(log -> AnalyticsLogResponse.builder()
                        .id(log.getId())
                        .userId(log.getUserId())
                        .bundleId(log.getBundleId())
                        .interactionType(log.getInteractionType())
                        .createdAt(log.getCreatedAt().toInstant())
                        .build())
                .collect(Collectors.toList()));
        response.setPage(logs.getNumber());
        response.setSize(logs.getSize());
        response.setTotalElements(logs.getTotalElements());
        response.setTotalPages(logs.getTotalPages());
        
        return ResponseEntity.ok(ApiResponse.success("Lấy logs tương tác AI thành công", response));
    }

    @GetMapping("/index-jobs")
    public ResponseEntity<ApiResponse<PageResponse<IndexJobResponse>>> getIndexJobs(
            @PageableDefault(size = 50) Pageable pageable) {
        
        Page<AiIndexJob> jobs = indexJobRepository.findAll(pageable);
        
        PageResponse<IndexJobResponse> response = new PageResponse<>();
        response.setContent(jobs.getContent().stream()
                .map(job -> IndexJobResponse.builder()
                        .id(job.getId())
                        .targetType(job.getTargetType())
                        .targetId(job.getTargetId())
                        .operationType(job.getOperationType())
                        .status(job.getStatus())
                        .retryCount(job.getRetryCount())
                        .lastErrorMessage(job.getLastErrorMessage())
                        .createdAt(job.getCreatedAt().toInstant())
                        .updatedAt(job.getUpdatedAt().toInstant())
                        .build())
                .collect(Collectors.toList()));
        response.setPage(jobs.getNumber());
        response.setSize(jobs.getSize());
        response.setTotalElements(jobs.getTotalElements());
        response.setTotalPages(jobs.getTotalPages());
        
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách index jobs thành công", response));
    }

    @PostMapping("/index-jobs/retry")
    public ResponseEntity<ApiResponse<Void>> retryFailedJobs() {
        indexJobService.retryFailedJobs();
        return ResponseEntity.ok(ApiResponse.success("Đã retry các job thất bại", null));
    }

    @PostMapping("/index/products/reindex")
    public ResponseEntity<ApiResponse<Void>> reindexAllProducts() {
        indexJobService.triggerFullReindex();
        return ResponseEntity.ok(ApiResponse.success("Đã kích hoạt reindex toàn bộ sản phẩm", null));
    }
}

// Admin response DTOs
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class PipelineEventResponse {
    private String eventId;
    private String eventType;
    private String targetId;
    private String status;
    private Long durationMs;
    private Instant timestamp;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class GraphStatusResponse {
    private Long totalNodes;
    private Long totalRelationships;
    private String neo4jStatus;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class AnalyticsLogResponse {
    private String id;
    private String userId;
    private String bundleId;
    private String interactionType;
    private Instant createdAt;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class IndexJobResponse {
    private String id;
    private String targetType;
    private String targetId;
    private String operationType;
    private String status;
    private Integer retryCount;
    private String lastErrorMessage;
    private Instant createdAt;
    private Instant updatedAt;
}
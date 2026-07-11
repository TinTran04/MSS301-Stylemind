package com.stylemind.ai.controller;

import com.stylemind.ai.dto.IndexJobRequest;
import com.stylemind.ai.dto.IndexJobResponse;
import com.stylemind.ai.service.AiIndexJobService;
import com.stylemind.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/ai/index-jobs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAiIndexJobController {

    private final AiIndexJobService indexJobService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<IndexJobResponse>>> getIndexJobs(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String targetType,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<IndexJobResponse> jobs = indexJobService.getIndexJobs(status, targetType, pageable);
        return ResponseEntity.ok(ApiResponse.success("Index jobs fetched", jobs));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<IndexJobResponse>> createIndexJob(@Valid @RequestBody IndexJobRequest request) {
        IndexJobResponse job = indexJobService.createIndexJob(request);
        return ResponseEntity.ok(ApiResponse.success("Index job created", job));
    }
}

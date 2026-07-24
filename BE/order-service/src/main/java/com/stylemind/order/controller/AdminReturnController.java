package com.stylemind.order.controller;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.order.dto.*;
import com.stylemind.order.service.ReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/returns")
@RequiredArgsConstructor
public class AdminReturnController {

    private final ReturnService returnService;

    @GetMapping
    public ApiResponse<Page<ReturnResponse>> adminGetReturns(
            @RequestParam(name = "status", required = false) String status,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<ReturnResponse> response = returnService.adminGetReturns(status, pageable);
        return ApiResponse.success(response);
    }

    @GetMapping("/{returnId}")
    public ApiResponse<ReturnResponse> adminGetReturnById(
            @PathVariable("returnId") String returnId) {
        ReturnResponse response = returnService.adminGetReturnById(returnId);
        return ApiResponse.success(response);
    }

    @PostMapping("/{returnId}/review")
    public ApiResponse<ReturnResponse> adminReviewReturn(
            @RequestHeader(name = "X-User-Id", defaultValue = "admin") String adminUserId,
            @PathVariable("returnId") String returnId,
            @Valid @RequestBody AdminReviewReturnRequest request) {
        ReturnResponse response = returnService.adminReviewReturn(adminUserId, returnId, request);
        return ApiResponse.success(response);
    }

    @PostMapping("/{returnId}/receive-qc")
    public ApiResponse<ReturnResponse> adminReceiveAndQc(
            @RequestHeader(name = "X-User-Id", defaultValue = "admin") String adminUserId,
            @PathVariable("returnId") String returnId,
            @Valid @RequestBody AdminQcRequest request) {
        ReturnResponse response = returnService.adminReceiveAndQc(adminUserId, returnId, request);
        return ApiResponse.success(response);
    }
}

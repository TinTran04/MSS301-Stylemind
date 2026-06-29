package com.stylemind.notification.controller;

import com.stylemind.notification.dto.*;
import com.stylemind.notification.service.NotificationService;
import com.stylemind.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationResponse>> createNotification(
            @Valid @RequestBody NotificationRequest request) {
        NotificationResponse response = notificationService.createNotification(request);
        return ResponseEntity.ok(ApiResponse.success("Tạo thông báo thành công", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<NotificationResponse> notifications = notificationService.getNotifications(userId, status, type, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thông báo thành công", notifications));
    }

    @PutMapping("/{id}/sent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> markAsSent(@PathVariable Long id) {
        notificationService.markAsSent(id);
        return ResponseEntity.ok(ApiResponse.success("Đánh dấu đã gửi thành công", null));
    }

    @PutMapping("/{id}/failed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> markAsFailed(@PathVariable Long id) {
        notificationService.markAsFailed(id);
        return ResponseEntity.ok(ApiResponse.success("Đánh dấu thất bại thành công", null));
    }
}

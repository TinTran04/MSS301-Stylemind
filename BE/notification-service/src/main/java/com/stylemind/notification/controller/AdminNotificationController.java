package com.stylemind.notification.controller;

import com.stylemind.notification.dto.AdminNotificationSummaryResponse;
import com.stylemind.notification.dto.NotificationResponse;
import com.stylemind.notification.service.NotificationService;
import com.stylemind.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            Pageable pageable) {
        Page<NotificationResponse> notifications = notificationService.getNotifications(userId, status, type, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thông báo thành công", notifications));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AdminNotificationSummaryResponse>> getSummary() {
        AdminNotificationSummaryResponse summary = notificationService.getAdminSummary();
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê thông báo thành công", summary));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<ApiResponse<NotificationResponse>> retry(@PathVariable Long id) {
        NotificationResponse response = notificationService.retryNotification(id);
        return ResponseEntity.ok(ApiResponse.success("Đã gửi lại thông báo", response));
    }
}

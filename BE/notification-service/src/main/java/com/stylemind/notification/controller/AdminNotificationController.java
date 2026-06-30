package com.stylemind.notification.controller;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.dto.PageResponse;
import com.stylemind.notification.dto.NotificationRequest;
import com.stylemind.notification.dto.NotificationResponse;
import com.stylemind.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationResponse>> createNotification(
            @Valid @RequestBody NotificationRequest request) {
        NotificationResponse response = notificationService.createNotification(request);
        return ResponseEntity.ok(ApiResponse.success("Tao thong bao thanh cong", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getNotifications(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<NotificationResponse> notifications =
                notificationService.getAdminNotifications(userId, status, type, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lay danh sach thong bao thanh cong", notifications));
    }

    @PatchMapping("/{id}/sent")
    public ResponseEntity<ApiResponse<Void>> markAsSent(@PathVariable Long id) {
        notificationService.markAsSent(id);
        return ResponseEntity.ok(ApiResponse.success("Danh dau da gui thanh cong", null));
    }

    @PatchMapping("/{id}/failed")
    public ResponseEntity<ApiResponse<Void>> markAsFailed(@PathVariable Long id) {
        notificationService.markAsFailed(id);
        return ResponseEntity.ok(ApiResponse.success("Danh dau that bai thanh cong", null));
    }
}

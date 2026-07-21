package com.stylemind.notification.controller;

import com.stylemind.common.dto.PageResponse;
import com.stylemind.common.web.PaginationSupport;
import com.stylemind.notification.dto.*;
import com.stylemind.notification.service.NotificationService;
import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "id");

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Boolean read) {
        Pageable pageable = PaginationSupport.customerPageable(page, size, sort, ALLOWED_SORT_FIELDS);
        PageResponse<NotificationResponse> notifications =
                notificationService.getCustomerNotifications(principal.getUserId(), read, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thông báo thành công", notifications));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<NotificationUnreadCountResponse>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        NotificationUnreadCountResponse response = notificationService.getUnreadCount(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Lấy số thông báo chưa đọc thành công", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationResponse>> getMyNotification(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        NotificationResponse notification = notificationService.getNotificationForUser(principal.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông báo thành công", notification));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markNotificationRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        NotificationResponse notification = notificationService.markNotificationRead(principal.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu thông báo là đã đọc", notification));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<NotificationReadAllResponse>> markAllNotificationsRead(
            @AuthenticationPrincipal UserPrincipal principal) {
        NotificationReadAllResponse response = notificationService.markAllNotificationsRead(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu tất cả thông báo là đã đọc", response));
    }
}

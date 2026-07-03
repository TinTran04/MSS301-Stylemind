package com.stylemind.notification.controller;

import com.stylemind.notification.dto.*;
import com.stylemind.notification.service.NotificationService;
import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<NotificationResponse> notifications = notificationService.getNotifications(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thông báo thành công", notifications));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationResponse>> getMyNotification(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        NotificationResponse notification = notificationService.getNotificationForUser(principal.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông báo thành công", notification));
    }
}

package com.stylemind.notification.controller;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.dto.PageResponse;
import com.stylemind.common.security.UserPrincipal;
import com.stylemind.notification.dto.NotificationResponse;
import com.stylemind.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<NotificationResponse> notifications =
                notificationService.getUserNotifications(principal.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Lay danh sach thong bao thanh cong", notifications));
    }
}

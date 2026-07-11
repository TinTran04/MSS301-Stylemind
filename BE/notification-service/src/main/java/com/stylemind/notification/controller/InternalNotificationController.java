package com.stylemind.notification.controller;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.notification.dto.InternalEmailRequest;
import com.stylemind.notification.dto.NotificationRequest;
import com.stylemind.notification.dto.NotificationResponse;
import com.stylemind.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/notifications")
@RequiredArgsConstructor
public class InternalNotificationController {

    private final NotificationService notificationService;

    // Generic log-creation entry point for other services (e.g. order-service on
    // checkout). Delivery may be stubbed per app.mail.enabled - the log row is
    // always written regardless of whether the send actually goes out.
    @PostMapping
    public ResponseEntity<ApiResponse<NotificationResponse>> createNotification(@Valid @RequestBody NotificationRequest request) {
        NotificationResponse response = notificationService.createNotification(request);
        return ResponseEntity.ok(ApiResponse.success("Tạo thông báo thành công", response));
    }

    @PostMapping("/email")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendEmail(@Valid @RequestBody InternalEmailRequest request) {
        NotificationResponse response = notificationService.sendInternalEmail(request);
        return ResponseEntity.ok(ApiResponse.success("Gửi email nội bộ thành công", response));
    }
}

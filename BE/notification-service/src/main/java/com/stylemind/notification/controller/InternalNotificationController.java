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
@RequestMapping("/internal/notifications")
@RequiredArgsConstructor
public class InternalNotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationResponse>> createNotificationLog(
            @Valid @RequestBody NotificationRequest request) {
        NotificationResponse response = notificationService.createNotificationLog(request);
        return ResponseEntity.ok(ApiResponse.success("Tao thong bao noi bo thanh cong", response));
    }

    @PostMapping("/email")
    public ResponseEntity<ApiResponse<Void>> sendEmail(@Valid @RequestBody InternalEmailRequest request) {
        notificationService.sendInternalEmail(request);
        return ResponseEntity.ok(ApiResponse.success("Gui email noi bo thanh cong", null));
    }
}

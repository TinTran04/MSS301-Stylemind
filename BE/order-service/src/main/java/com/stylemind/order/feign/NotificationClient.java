package com.stylemind.order.feign;

import com.stylemind.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", url = "${notification.service.url:http://notification-service:8089}")
public interface NotificationClient {

    @PostMapping("/internal/v1/notifications/email")
    ApiResponse<Void> sendEmail(@RequestBody EmailRequest request);

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    class EmailRequest {
        private String userId;
        private String recipientEmail;
        private String type;
        private String title;
        private String content;
        private String htmlContent;
    }
}

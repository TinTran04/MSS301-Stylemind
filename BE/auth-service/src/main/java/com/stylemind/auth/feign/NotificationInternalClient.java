package com.stylemind.auth.feign;

import com.stylemind.auth.dto.InternalEmailNotificationRequest;
import com.stylemind.common.dto.ApiResponse;
import com.stylemind.common.feign.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "notification-internal-client",
        url = "${notification.service.url}",
        configuration = FeignClientConfig.class
)
public interface NotificationInternalClient {

    @PostMapping("/internal/notifications/email")
    ApiResponse<Void> sendEmail(@RequestBody InternalEmailNotificationRequest request);
}

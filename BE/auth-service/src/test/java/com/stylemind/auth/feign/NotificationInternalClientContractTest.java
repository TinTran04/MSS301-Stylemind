package com.stylemind.auth.feign;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationInternalClientContractTest {

    @Test
    void clientUsesConfiguredBaseUrlAndProtectedEmailEndpoint() throws NoSuchMethodException {
        FeignClient client = NotificationInternalClient.class.getAnnotation(FeignClient.class);
        PostMapping mapping = NotificationInternalClient.class
                .getMethod("sendEmail", com.stylemind.auth.dto.InternalEmailNotificationRequest.class)
                .getAnnotation(PostMapping.class);

        assertThat(client.url()).isEqualTo("${notification.service.url}");
        assertThat(mapping.value()).containsExactly("/internal/v1/notifications/email");
    }
}

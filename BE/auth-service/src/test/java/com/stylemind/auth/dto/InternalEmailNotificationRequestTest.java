package com.stylemind.auth.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InternalEmailNotificationRequestTest {

    @Test
    void toString_doesNotExposeEmailBodiesContainingSecrets() {
        InternalEmailNotificationRequest request = InternalEmailNotificationRequest.builder()
                .recipientEmail("user@example.com")
                .content("reset-token-secret")
                .htmlContent("<strong>otp-secret</strong>")
                .build();

        assertThat(request.toString())
                .doesNotContain("reset-token-secret")
                .doesNotContain("otp-secret");
    }
}

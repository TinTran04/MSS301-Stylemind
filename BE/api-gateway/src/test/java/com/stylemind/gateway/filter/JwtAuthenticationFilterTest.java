package com.stylemind.gateway.filter;

import com.stylemind.gateway.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JwtAuthenticationFilterTest {

    @Test
    void invitePasswordSetupEndpointIsPublic() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(mock(JwtUtil.class));

        boolean publicPath = (boolean) ReflectionTestUtils.invokeMethod(
                filter,
                "isPublicPath",
                "/api/v1/auth/password/setup"
        );

        assertThat(publicPath).isTrue();
    }

    @Test
    void resetPasswordEndpointRemainsPublic() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(mock(JwtUtil.class));

        boolean publicPath = (boolean) ReflectionTestUtils.invokeMethod(
                filter,
                "isPublicPath",
                "/api/v1/auth/reset-password"
        );

        assertThat(publicPath).isTrue();
    }

    @Test
    void sepayWebhookExactEndpointIsPublic() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(mock(JwtUtil.class));

        boolean publicPath = (boolean) ReflectionTestUtils.invokeMethod(
                filter,
                "isPublicPath",
                "/api/v1/payments/webhook/sepay"
        );

        assertThat(publicPath).isTrue();
    }

    @Test
    void similarlyPrefixedPaymentWebhookPathIsNotPublic() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(mock(JwtUtil.class));

        boolean publicPath = (boolean) ReflectionTestUtils.invokeMethod(
                filter,
                "isPublicPath",
                "/api/v1/payments/webhook/sepay/other"
        );

        assertThat(publicPath).isFalse();
    }
}

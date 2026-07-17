package com.stylemind.gateway.filter;

import com.stylemind.gateway.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(mock(JwtUtil.class));
    }

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

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/register/verify-otp",
            "/api/v1/auth/register/resend-otp"
    })
    void preAuthenticationEndpointsPassWithoutBearerToken(String path) {
        MockServerWebExchange exchange = exchange(path);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(any());
        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedAuthEndpointWithoutBearerTokenIsRejected() {
        MockServerWebExchange exchange = exchange("/api/v1/auth/me");
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(chain);
    }

    @Test
    void invalidBearerTokenOnProtectedEndpointIsRejected() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        when(jwtUtil.extractUserId("invalid-token")).thenThrow(new IllegalArgumentException("invalid token"));
        JwtAuthenticationFilter protectedFilter = new JwtAuthenticationFilter(jwtUtil);
        org.springframework.web.server.ServerWebExchange exchange = exchange("/api/v1/auth/me")
                .mutate()
                .request(MockServerHttpRequest.get("/api/v1/auth/me")
                        .header("Authorization", "Bearer invalid-token")
                        .build())
                .build();
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        protectedFilter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(chain);
    }

    private MockServerWebExchange exchange(String path) {
        return MockServerWebExchange.from(MockServerHttpRequest.post(path).build());
    }
}

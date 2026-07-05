package com.stylemind.gateway.filter;

import com.stylemind.gateway.security.JwtUtil;
import com.stylemind.gateway.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            // Pre-login password recovery: the user has no JWT yet, so these must be
            // public at the gateway (auth-service SecurityConfig already permits them).
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/verify-reset-otp",
            "/api/v1/auth/reset-password",
            "/actuator/health",
            "/actuator/info",
            "/api/v1/products",
            "/api/v1/categories",
            "/api/v1/cart",
            "/v3/api-docs",
            "/swagger-ui",
            // SePay's own servers call this directly - no user JWT exists for that
            // request. Authenticity is instead verified inside payment-service via
            // the webhook's Authorization/API-key header (see PaymentService).
            "/api/v1/payments/webhook/sepay"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Correlation id: every request gets one, regardless of whether it's a
        // public or authenticated path, so downstream logs can always be traced.
        String requestId = request.getHeaders().getFirst("X-Request-Id");
        if (!StringUtils.hasText(requestId)) {
            requestId = UUID.randomUUID().toString();
        }
        String finalRequestId = requestId;

        ServerHttpRequest mutatedRequest = request.mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-User-Roles");
                    headers.remove("X-User-Email");
                    headers.set("X-Request-Id", finalRequestId);
                })
                .build();

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            if (isPublicPath(path)) {
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            }
            return unauthorizedResponse(exchange.getResponse(), "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        try {
            // Validate token and extract claims
            String userId = jwtUtil.extractUserId(token);
            String role = jwtUtil.extractRole(token);
            String email = jwtUtil.extractUsername(token);

            if (!StringUtils.hasText(userId) || !StringUtils.hasText(role)) {
                return unauthorizedResponse(exchange.getResponse(), "Invalid token claims");
            }

            // Add user info to headers for downstream services
            mutatedRequest = mutatedRequest.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Roles", "ROLE_" + role)
                    .header("X-User-Email", email)
                    .build();

        } catch (Exception ex) {
            log.warn("JWT validation failed: {}", ex.getMessage());
            return unauthorizedResponse(exchange.getResponse(), "Invalid token: " + ex.getMessage());
        }

        // WebFlux handles one request per thread only within a given operator
        // chain, not for its whole lifetime, so MDC is set for this synchronous
        // block (covers this filter's own log lines) and passed to downstream
        // services via the header for their own MDC population.
        org.slf4j.MDC.put("X-Request-Id", finalRequestId);
        try {
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } finally {
            org.slf4j.MDC.remove("X-Request-Id");
        }
    }

    private boolean isPublicPath(String path) {
        if (path.startsWith("/api/v1/products") || path.startsWith("/api/v1/categories") || path.startsWith("/api/v1/cart")) {
            return true;
        }
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorizedResponse(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        String body = String.format("{\"success\":false,\"errorCode\":\"AUTH_TOKEN_INVALID\",\"message\":\"%s\"}", message);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        return -100; // Run early
    }
}

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
            "/api/auth/login",
            "/api/auth/register",
            "/actuator/health",
            "/actuator/info",
            "/api/products",
            "/api/categories",
            "/api/cart",
            "/v3/api-docs",
            "/swagger-ui"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        ServerHttpRequest mutatedRequest = request.mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-User-Roles");
                    headers.remove("X-User-Email");
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

            // Add request ID for tracing
            String requestId = request.getHeaders().getFirst("X-Request-Id");
            if (!StringUtils.hasText(requestId)) {
                requestId = UUID.randomUUID().toString();
            }
            mutatedRequest = mutatedRequest.mutate()
                    .header("X-Request-Id", requestId)
                    .build();

        } catch (Exception ex) {
            log.warn("JWT validation failed: {}", ex.getMessage());
            return unauthorizedResponse(exchange.getResponse(), "Invalid token: " + ex.getMessage());
        }

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublicPath(String path) {
        if (path.startsWith("/api/products") || path.startsWith("/api/categories") || path.startsWith("/api/cart")) {
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

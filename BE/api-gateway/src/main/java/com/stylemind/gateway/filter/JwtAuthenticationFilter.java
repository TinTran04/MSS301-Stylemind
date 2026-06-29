package com.stylemind.gateway.filter;

import com.stylemind.gateway.config.GatewaySecurityProperties;
import com.stylemind.gateway.error.GatewayErrorResponseWriter;
import com.stylemind.gateway.security.GatewayTokenClaims;
import com.stylemind.gateway.security.JwtAccessTokenValidator;
import com.stylemind.gateway.security.JwtValidationCode;
import com.stylemind.gateway.security.JwtValidationException;
import com.stylemind.gateway.support.GatewayExchangeAttributes;
import com.stylemind.gateway.support.GatewayHeaders;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class    JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN");
    private static final Set<String> STAFF_ROLES = Set.of("STAFF", "ADMIN");

    private final JwtAccessTokenValidator tokenValidator;
    private final GatewaySecurityProperties securityProperties;
    private final GatewayErrorResponseWriter errorResponseWriter;

    private static final List<PublicRoute> PUBLIC_ROUTES = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/verify-email"
    ).stream().map(path -> new PublicRoute(HttpMethod.POST, path)).toList();

    private static final List<String> ALWAYS_PUBLIC_PREFIXES = List.of(
            "/actuator/health",
            "/actuator/info",
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-ui.html"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String requestId = request.getHeaders().getFirst(GatewayHeaders.REQUEST_ID);

        ServerHttpRequest.Builder requestBuilder = request.mutate();
        sanitizeClientHeaders(requestBuilder);
        if (StringUtils.hasText(requestId)) {
            requestBuilder.header(GatewayHeaders.REQUEST_ID, requestId);
        }
        addInternalHeaders(requestBuilder);

        if (isPublicRoute(request.getMethod(), path)) {
            ServerWebExchange sanitizedExchange = exchange.mutate().request(requestBuilder.build()).build();
            return chain.filter(sanitizedExchange);
        }

        String bearerToken = resolveBearerToken(request);
        if (!StringUtils.hasText(bearerToken)) {
            return errorResponseWriter.write(exchange, HttpStatus.UNAUTHORIZED,
                    JwtValidationCode.INVALID_ACCESS_TOKEN.name(), "Access token is required");
        }

        try {
            GatewayTokenClaims claims = tokenValidator.validateAccessToken(bearerToken);
            String normalizedRole = normalizeRole(claims.role());

            if (isAdminRoute(path) && !ADMIN_ROLES.contains(normalizedRole)) {
                return errorResponseWriter.write(exchange, HttpStatus.FORBIDDEN,
                        "FORBIDDEN", "You do not have permission to access this resource");
            }
            if (isStaffRoute(path) && !STAFF_ROLES.contains(normalizedRole)) {
                return errorResponseWriter.write(exchange, HttpStatus.FORBIDDEN,
                        "FORBIDDEN", "You do not have permission to access this resource");
            }

            exchange.getAttributes().put(GatewayExchangeAttributes.USER_ID, claims.userId());
            exchange.getAttributes().put(GatewayExchangeAttributes.USER_ROLE, normalizedRole);

            requestBuilder.header(GatewayHeaders.USER_ID, claims.userId());
            requestBuilder.header(GatewayHeaders.USER_ROLE, normalizedRole);
            requestBuilder.header(GatewayHeaders.TOKEN_ID, claims.jwtId());
            requestBuilder.header(GatewayHeaders.TOKEN_TYPE, claims.type());
            requestBuilder.header(GatewayHeaders.LEGACY_USER_ROLES, "ROLE_" + normalizedRole);
            if (shouldRemoveAuthorization(path)) {
                requestBuilder.headers(headers -> headers.remove(HttpHeaders.AUTHORIZATION));
            }

            ServerWebExchange authenticatedExchange = exchange.mutate().request(requestBuilder.build()).build();
            return chain.filter(authenticatedExchange);
        } catch (JwtValidationException ex) {
            HttpStatus status = ex.getCode() == JwtValidationCode.ACCESS_TOKEN_EXPIRED
                    ? HttpStatus.UNAUTHORIZED
                    : HttpStatus.UNAUTHORIZED;
            log.warn("Gateway JWT validation failed: code={}, requestId={}", ex.getCode(), requestId);
            return errorResponseWriter.write(exchange, status, ex.getCode().name(), safeMessage(ex.getCode()));
        }
    }

    private void sanitizeClientHeaders(ServerHttpRequest.Builder requestBuilder) {
        requestBuilder.headers(headers -> {
            headers.remove(GatewayHeaders.USER_ID);
            headers.remove(GatewayHeaders.USER_ROLE);
            headers.remove(GatewayHeaders.TOKEN_ID);
            headers.remove(GatewayHeaders.TOKEN_TYPE);
            headers.remove(GatewayHeaders.LEGACY_USER_ROLES);
            headers.remove(GatewayHeaders.INTERNAL_REQUEST);
            headers.remove(GatewayHeaders.INTERNAL_TOKEN);
            headers.remove(GatewayHeaders.INTERNAL_SERVICE_SECRET);
        });
    }

    private void addInternalHeaders(ServerHttpRequest.Builder requestBuilder) {
        requestBuilder.header(GatewayHeaders.INTERNAL_REQUEST, "true");
        String secret = securityProperties.getInternalServiceSecret();
        if (StringUtils.hasText(secret)) {
            requestBuilder.header(GatewayHeaders.INTERNAL_TOKEN, secret);
            requestBuilder.header(GatewayHeaders.INTERNAL_SERVICE_SECRET, secret);
        }
    }

    private String resolveBearerToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7).trim();
    }

    private boolean isPublicRoute(HttpMethod method, String path) {
        return ALWAYS_PUBLIC_PREFIXES.stream().anyMatch(path::startsWith)
                || PUBLIC_ROUTES.stream().anyMatch(route -> route.matches(method, path));
    }

    private boolean isAdminRoute(String path) {
        return path.startsWith("/api/admin/");
    }

    private boolean isStaffRoute(String path) {
        return path.startsWith("/api/staff/");
    }

    private boolean shouldRemoveAuthorization(String path) {
        return path.startsWith("/api/users/") || path.startsWith("/api/admin/users/");
    }

    private String normalizeRole(String role) {
        String normalized = role == null ? "" : role.trim().toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized.substring("ROLE_".length()) : normalized;
    }

    private String safeMessage(JwtValidationCode code) {
        return code == JwtValidationCode.ACCESS_TOKEN_EXPIRED
                ? "Access token expired"
                : "Access token is invalid";
    }

    @Override
    public int getOrder() {
        return -100; // Run early
    }

    private record PublicRoute(HttpMethod method, String path) {
        boolean matches(HttpMethod actualMethod, String actualPath) {
            return method.equals(actualMethod) && path.equals(actualPath);
        }
    }
}

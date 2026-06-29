package com.stylemind.gateway.filter;

import com.stylemind.gateway.ApiGatewayApplication;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest(
        classes = ApiGatewayApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "AUTH_SERVICE_URL=forward:/__downstream",
                "USER_PROFILE_SERVICE_URL=forward:/__downstream",
                "USER_SERVICE_URL=forward:/__downstream",
                "JWT_SECRET=gateway-test-secret-key-with-32-characters",
                "INTERNAL_SERVICE_SECRET=test-internal-service-secret",
                "spring.data.redis.host=localhost",
                "spring.data.redis.port=6379"
        })
@Import(JwtAuthenticationFilterTest.DownstreamController.class)
class JwtAuthenticationFilterTest {

    private static final String USER_ID = "8c14bace-c8ba-4f6d-b088-f9055c2f25d8";
    private static final String JWT_SECRET = "gateway-test-secret-key-with-32-characters";
    private static final String BAD_JWT_SECRET = "gateway-test-bad-secret-key-with-32-chars";

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void publicRouteDoesNotRequireToken() {
        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"customer@example.com\",\"password\":\"StrongPassword123!\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.headers.X-Request-Id").isNotEmpty()
                .jsonPath("$.headers.X-Internal-Request").isEqualTo("true");
    }

    @Test
    void protectedRouteWithoutTokenReturnsInvalidAccessToken() {
        webTestClient.post()
                .uri("/api/auth/logout")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("INVALID_ACCESS_TOKEN");
    }

    @Test
    void tokenWithInvalidSignatureReturnsInvalidAccessToken() {
        webTestClient.get()
                .uri("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(USER_ID, "CUSTOMER", BAD_JWT_SECRET, Duration.ofMinutes(15))))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("INVALID_ACCESS_TOKEN");
    }

    @Test
    void expiredTokenReturnsAccessTokenExpired() {
        webTestClient.get()
                .uri("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(USER_ID, "CUSTOMER", JWT_SECRET, Duration.ofMinutes(-1))))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("ACCESS_TOKEN_EXPIRED");
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() {
        webTestClient.get()
                .uri("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(token(USER_ID, "CUSTOMER", "refresh", JWT_SECRET, Duration.ofDays(7))))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("INVALID_ACCESS_TOKEN");
    }

    @Test
    void customerCannotAccessAdminRoute() {
        webTestClient.get()
                .uri("/api/admin/users")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(USER_ID, "CUSTOMER", JWT_SECRET, Duration.ofMinutes(15))))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("FORBIDDEN");
    }

    @Test
    void adminCanAccessAdminRoute() {
        webTestClient.get()
                .uri("/api/admin/users")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(USER_ID, "ADMIN", JWT_SECRET, Duration.ofMinutes(15))))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.headers.X-User-Id").isEqualTo(USER_ID)
                .jsonPath("$.headers.X-User-Role").isEqualTo("ADMIN")
                .jsonPath("$.headers.X-Token-Id").isNotEmpty()
                .jsonPath("$.headers.X-Token-Type").isEqualTo("access");
    }

    @Test
    void spoofedUserRoleHeaderIsRemovedAndReplacedByJwtRole() {
        webTestClient.get()
                .uri("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(USER_ID, "CUSTOMER", JWT_SECRET, Duration.ofMinutes(15))))
                .header("X-User-Role", "ADMIN")
                .header("X-User-Id", UUID.randomUUID().toString())
                .header("X-Internal-Request", "false")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.headers.X-User-Id").isEqualTo(USER_ID)
                .jsonPath("$.headers.X-User-Role").isEqualTo("CUSTOMER")
                .jsonPath("$.headers.X-Internal-Request").isEqualTo("true");
    }

    @Test
    void downstreamReceivesTrustedUserIdAndAuthorizationIsRemovedForUserProfileService() {
        webTestClient.patch()
                .uri("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(USER_ID, "CUSTOMER", JWT_SECRET, Duration.ofMinutes(15))))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"fullName\":\"Nguyen Van A\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.headers.X-User-Id").isEqualTo(USER_ID)
                .jsonPath("$.headers.Authorization").isEqualTo("");
    }

    private String accessToken(String userId, String role, String secret, Duration ttl) {
        return token(userId, role, "access", secret, ttl);
    }

    private String token(String userId, String role, String type, String secret, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .claim("type", type)
                .claim("jti", UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(secretKey(secret), SignatureAlgorithm.HS256)
                .compact();
    }

    private SecretKey secretKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @RestController
    static class DownstreamController {

        @RequestMapping(value = {"/__downstream", "/__downstream/**"})
        Mono<Map<String, Object>> echo(ServerHttpRequest request) {
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("X-User-Id", header(request, "X-User-Id"));
            headers.put("X-User-Role", header(request, "X-User-Role"));
            headers.put("X-Token-Id", header(request, "X-Token-Id"));
            headers.put("X-Token-Type", header(request, "X-Token-Type"));
            headers.put("X-Request-Id", header(request, "X-Request-Id"));
            headers.put("X-Internal-Request", header(request, "X-Internal-Request"));
            headers.put("X-Internal-Token", header(request, "X-Internal-Token"));
            headers.put("Authorization", header(request, HttpHeaders.AUTHORIZATION));

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("method", request.getMethod() == null ? "" : request.getMethod().name());
            response.put("path", request.getURI().getPath());
            response.put("headers", headers);
            return Mono.just(response);
        }

        private String header(ServerHttpRequest request, String name) {
            String value = request.getHeaders().getFirst(name);
            return value == null ? "" : value;
        }
    }
}

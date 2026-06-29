package com.stylemind.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@SpringBootTest(
        classes = ApiGatewayApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "AUTH_SERVICE_URL=forward:/__downstream",
                "USER_PROFILE_SERVICE_URL=forward:/__downstream",
                "USER_SERVICE_URL=forward:/__downstream",
                "JWT_SECRET=gateway-test-secret-key-with-32-characters",
                "INTERNAL_SERVICE_SECRET=test-internal-service-secret",
                "CORS_ALLOWED_ORIGINS=http://localhost:5173",
                "CORS_ALLOW_CREDENTIALS=true"
        })
@Import(GatewayCrossCuttingFilterTest.DownstreamController.class)
class GatewayCrossCuttingFilterTest {

    private static final String JWT_SECRET = "gateway-test-secret-key-with-32-characters";
    private static final String USER_ID = "8c14bace-c8ba-4f6d-b088-f9055c2f25d8";

    @Autowired
    private WebTestClient webTestClient;

    @MockBean(name = "reactiveStringRedisTemplate")
    private ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    @MockBean
    private ReactiveValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUpRedis() {
        Mockito.reset(reactiveStringRedisTemplate, valueOperations);
        when(reactiveStringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(Mono.just(1L));
        when(reactiveStringRedisTemplate.expire(anyString(), any(Duration.class))).thenReturn(Mono.just(true));
    }

    @Test
    void requestIdIsGeneratedForwardedAndReturned() {
        EntityExchangeResultBody result = webTestClient.post()
                .uri("/api/auth/login")
                .header("X-Request-Id", "client-request-id")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"customer@example.com\",\"password\":\"StrongPassword123!\"}")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueMatches("X-Request-Id", "^[0-9a-fA-F-]{36}$")
                .expectBody(EntityExchangeResultBody.class)
                .returnResult()
                .getResponseBody();

        assertThat(result).isNotNull();
        assertThat(result.headers().get("X-Request-Id")).matches("^[0-9a-fA-F-]{36}$");
        assertThat(result.headers().get("X-Request-Id")).isNotEqualTo("client-request-id");
    }

    @Test
    void corsUsesConfiguredOriginWithoutWildcardWhenCredentialsAreEnabled() {
        webTestClient.options()
                .uri("/api/auth/login")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173")
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                .expectHeader().value(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, value -> assertThat(value).isNotEqualTo("*"));
    }

    @Test
    void loginIsRateLimited() {
        when(valueOperations.increment(anyString())).thenReturn(Mono.just(999L));

        webTestClient.post()
                .uri("/api/auth/login")
                .header("X-Forwarded-For", "203.0.113.10")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"customer@example.com\",\"password\":\"StrongPassword123!\"}")
                .exchange()
                .expectStatus().isEqualTo(429)
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("RATE_LIMIT_EXCEEDED")
                .jsonPath("$.requestId").isNotEmpty();
    }

    @Test
    void resetPasswordIsRateLimited() {
        when(valueOperations.increment(anyString())).thenReturn(Mono.just(999L));

        webTestClient.post()
                .uri("/api/auth/reset-password")
                .header("X-Forwarded-For", "203.0.113.20")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"token\":\"reset-token-value\",\"newPassword\":\"NewStrongPassword123!\"}")
                .exchange()
                .expectStatus().isEqualTo(429)
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("RATE_LIMIT_EXCEEDED")
                .jsonPath("$.requestId").isNotEmpty();
    }

    @Test
    void redisUnavailableFailsOpenByPolicy() {
        when(valueOperations.increment(anyString())).thenReturn(Mono.error(new IllegalStateException("redis unavailable")));

        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"customer@example.com\",\"password\":\"StrongPassword123!\"}")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void accessLogDoesNotContainTokenPasswordOrCookie() {
        Logger logger = (Logger) LoggerFactory.getLogger(AccessLogFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            webTestClient.mutate().responseTimeout(Duration.ofSeconds(30)).build().post()
                    .uri("/api/auth/login")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer secret-access-token")
                    .header(HttpHeaders.COOKIE, "refresh_token=secret-refresh-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"email\":\"customer@example.com\",\"password\":\"StrongPassword123!\"}")
                    .exchange()
                    .expectStatus().isOk();
        } finally {
            logger.detachAppender(appender);
        }

        String logOutput = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (left, right) -> left + "\n" + right);

        assertThat(logOutput).doesNotContain("StrongPassword123!");
        assertThat(logOutput).doesNotContain("secret-access-token");
        assertThat(logOutput).doesNotContain("secret-refresh-token");
    }

    private String accessToken() {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(USER_ID)
                .claim("role", "CUSTOMER")
                .claim("type", "access")
                .claim("jti", UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(15))))
                .signWith(secretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    record EntityExchangeResultBody(String method, String path, Map<String, String> headers) {
    }

    @RestController
    static class DownstreamController {

        @RequestMapping(value = {"/__downstream", "/__downstream/**"})
        Mono<Map<String, Object>> echo(ServerHttpRequest request) {
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("X-Request-Id", header(request, "X-Request-Id"));
            headers.put("X-User-Id", header(request, "X-User-Id"));
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

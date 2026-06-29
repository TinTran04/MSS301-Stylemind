package com.stylemind.gateway.filter;

import com.stylemind.gateway.ApiGatewayApplication;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

@SpringBootTest(
        classes = ApiGatewayApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "AUTH_SERVICE_URL=forward:/__unused",
                "JWT_SECRET=gateway-test-secret-key-with-32-characters",
                "INTERNAL_SERVICE_SECRET=test-internal-service-secret",
                "GATEWAY_RESPONSE_TIMEOUT=100ms",
                "GATEWAY_RETRY_SAFE_RETRIES=1",
                "GATEWAY_RATE_LIMIT_ENABLED=false"
        })
class GatewayTimeoutTest {

    private static final String JWT_SECRET = "gateway-test-secret-key-with-32-characters";
    private static final String USER_ID = "8c14bace-c8ba-4f6d-b088-f9055c2f25d8";

    private static final DisposableServer DOWNSTREAM = HttpServer.create()
            .port(0)
            .handle((request, response) -> Mono.delay(Duration.ofMillis(500))
                    .then(response.sendString(Mono.just("{\"ok\":true}")).then()))
            .bindNow();

    @Autowired
    private WebTestClient webTestClient;

    @DynamicPropertySource
    static void downstreamProperties(DynamicPropertyRegistry registry) {
        String downstreamUrl = "http://localhost:" + DOWNSTREAM.port();
        registry.add("USER_PROFILE_SERVICE_URL", () -> downstreamUrl);
        registry.add("USER_SERVICE_URL", () -> downstreamUrl);
    }

    @AfterAll
    static void stopDownstream() {
        DOWNSTREAM.disposeNow();
    }

    @Test
    void downstreamTimeoutReturnsStandardError() {
        webTestClient.get()
                .uri("/api/users/me")
                .header("Authorization", "Bearer " + accessToken())
                .exchange()
                .expectStatus().isEqualTo(504)
                .expectHeader().exists("X-Request-Id")
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.code").isEqualTo("DOWNSTREAM_TIMEOUT")
                .jsonPath("$.requestId").isNotEmpty();
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
}

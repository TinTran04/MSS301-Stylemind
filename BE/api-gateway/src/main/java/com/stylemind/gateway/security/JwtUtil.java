package com.stylemind.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class JwtUtil {

    // Must match common-lib JwtUtil so a token minted by auth-service validates at the gateway.
    private static final String DEFAULT_SECRET = "super-secure-stylemind-secret-key-signature-2026-xyz";
    private static final int MIN_HS256_SECRET_BYTES = 32;

    private final SecretKey secretKey;

    public JwtUtil(@Value("${jwt.secret:${JWT_SECRET:" + DEFAULT_SECRET + "}}") String secret) {
        String resolvedSecret = resolveSecret(secret);
        this.secretKey = Keys.hmacShaKeyFor(resolvedSecret.getBytes(StandardCharsets.UTF_8));
    }

    private String resolveSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            log.warn("JWT secret is blank; using local development default. Set JWT_SECRET for shared environments.");
            return DEFAULT_SECRET;
        }

        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_HS256_SECRET_BYTES) {
            log.warn("JWT secret is shorter than {} bytes; using local development default. Set a longer JWT_SECRET.",
                    MIN_HS256_SECRET_BYTES);
            return DEFAULT_SECRET;
        }

        return secret;
    }

    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
}

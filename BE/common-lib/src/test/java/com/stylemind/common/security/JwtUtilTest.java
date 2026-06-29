package com.stylemind.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import io.jsonwebtoken.ExpiredJwtException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String VALID_SECRET = "super-secure-stylemind-secret-key-signature-2026-xyz";

    private JwtUtil jwtUtil(String secret) {
        return new JwtUtil(secret, 3_600_000L, 604_800_000L);
    }

    private UserDetails userDetails(String email) {
        return new User(email, "pw", Collections.emptyList());
    }

    @Test
    void generateAndValidateToken() {
        JwtUtil util = jwtUtil(VALID_SECRET);
        UserDetails ud = userDetails("alice@example.com");

        String token = util.generateAccessToken(ud, "user-1", "CUSTOMER");

        assertThat(util.extractUsername(token)).isEqualTo("alice@example.com");
        assertThat(util.extractUserId(token)).isEqualTo("user-1");
        assertThat(util.extractRole(token)).isEqualTo("CUSTOMER");
        assertThat(util.validateToken(token, ud)).isTrue();
    }

    @Test
    void usesDefaultSecretWhenBlank() {
        // Should not throw — falls back to hardcoded default
        JwtUtil util = jwtUtil("");
        UserDetails ud = userDetails("b@b.com");
        String token = util.generateAccessToken(ud, "u2", "ADMIN");
        assertThat(util.validateToken(token, ud)).isTrue();
    }

    @Test
    void usesDefaultSecretWhenTooShort() {
        JwtUtil util = jwtUtil("short");
        UserDetails ud = userDetails("c@c.com");
        String token = util.generateAccessToken(ud, "u3", "CUSTOMER");
        assertThat(util.validateToken(token, ud)).isTrue();
    }

    @Test
    void tokenIsExpiredAfterNegativeExpiry() {
        JwtUtil util = new JwtUtil(VALID_SECRET, -1L, -1L);
        UserDetails ud = userDetails("d@d.com");
        String token = util.generateAccessToken(ud, "u4", "CUSTOMER");
        // JJWT throws ExpiredJwtException when parsing an already-expired token;
        // isTokenExpired internally calls extractExpiration which parses the token.
        assertThatThrownBy(() -> util.isTokenExpired(token))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
        // validateToken wraps the parse; it must also propagate the exception
        // (callers in JwtAuthenticationFilter already catch Exception broadly)
        assertThatThrownBy(() -> util.validateToken(token, ud))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }
}

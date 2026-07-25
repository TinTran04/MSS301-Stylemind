package com.stylemind.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.*;
import java.util.Collections;

import io.jsonwebtoken.ExpiredJwtException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private KeyPair generateRsaKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    private JwtUtil jwtUtilIssuer() throws NoSuchAlgorithmException {
        KeyPair keyPair = generateRsaKeyPair();
        return new JwtUtil(keyPair.getPrivate(), keyPair.getPublic(), 3_600_000L, 604_800_000L);
    }

    private JwtUtil jwtUtilConsumer(PublicKey publicKey) {
        return new JwtUtil(publicKey, 3_600_000L, 604_800_000L);
    }

    private UserDetails userDetails(String email) {
        return new User(email, "pw", Collections.emptyList());
    }

    @Test
    void generateAndValidateToken() throws Exception {
        JwtUtil issuer = jwtUtilIssuer();
        KeyPair keyPair = generateRsaKeyPair();
        JwtUtil consumer = jwtUtilConsumer(keyPair.getPublic());
        
        UserDetails ud = userDetails("alice@example.com");
        String token = issuer.generateAccessToken(ud, "user-1", "CUSTOMER");

        assertThat(consumer.extractUsername(token)).isEqualTo("alice@example.com");
        assertThat(consumer.extractUserId(token)).isEqualTo("user-1");
        assertThat(consumer.extractRole(token)).isEqualTo("CUSTOMER");
        assertThat(consumer.validateToken(token, ud)).isTrue();
    }

    @Test
    void tokenIsExpiredAfterNegativeExpiry() throws Exception {
        JwtUtil issuer = new JwtUtil(generateRsaKeyPair().getPrivate(), generateRsaKeyPair().getPublic(), -1L, -1L);
        UserDetails ud = userDetails("d@d.com");
        String token = issuer.generateAccessToken(ud, "u4", "CUSTOMER");
        
        assertThatThrownBy(() -> issuer.isTokenExpired(token))
                .isInstanceOf(ExpiredJwtException.class);
        assertThatThrownBy(() -> issuer.validateToken(token, ud))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void consumerModeCannotSign() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        JwtUtil consumer = jwtUtilConsumer(keyPair.getPublic());
        UserDetails ud = userDetails("e@e.com");
        
        assertThatThrownBy(() -> consumer.generateAccessToken(ud, "u5", "CUSTOMER"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not configured for signing");
    }
}

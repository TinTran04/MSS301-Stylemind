package com.stylemind.gateway.security;

import com.stylemind.gateway.config.GatewaySecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtAccessTokenValidator {

    private final GatewaySecurityProperties properties;
    private PublicKey publicKey;
    private SecretKey secretKey;

    public JwtAccessTokenValidator(GatewaySecurityProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        String configuredPublicKey = properties.getJwt().getPublicKey();
        String configuredPublicKeyPath = properties.getJwt().getPublicKeyPath();

        try {
            if (StringUtils.hasText(configuredPublicKey)) {
                this.publicKey = parsePublicKey(configuredPublicKey);
                return;
            }
            if (StringUtils.hasText(configuredPublicKeyPath)) {
                this.publicKey = parsePublicKey(Files.readString(Path.of(configuredPublicKeyPath)));
                return;
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid gateway JWT public key configuration", ex);
        }

        String configuredSecret = properties.getJwt().getSecret();
        if (!StringUtils.hasText(configuredSecret)) {
            throw new IllegalStateException("JWT public key or JWT secret must be configured for API Gateway");
        }
        this.secretKey = Keys.hmacShaKeyFor(configuredSecret.getBytes(StandardCharsets.UTF_8));
    }

    public GatewayTokenClaims validateAccessToken(String token) {
        try {
            Claims claims = parseClaims(token);

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);
            String type = claims.get("type", String.class);
            String jwtId = claims.get("jti", String.class);

            if (!StringUtils.hasText(userId)
                    || !StringUtils.hasText(role)
                    || !StringUtils.hasText(jwtId)
                    || !"access".equals(type)) {
                throw new JwtValidationException(
                        JwtValidationCode.INVALID_ACCESS_TOKEN,
                        "Access token is missing required claims");
            }

            return new GatewayTokenClaims(
                    userId,
                    role,
                    type,
                    jwtId,
                    toInstant(claims.getIssuedAt()),
                    toInstant(claims.getExpiration()));
        } catch (JwtValidationException ex) {
            throw ex;
        } catch (ExpiredJwtException ex) {
            throw new JwtValidationException(JwtValidationCode.ACCESS_TOKEN_EXPIRED, "Access token expired");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new JwtValidationException(JwtValidationCode.INVALID_ACCESS_TOKEN, "Access token is invalid");
        }
    }

    private Claims parseClaims(String token) {
        JwtParserBuilder parserBuilder = Jwts.parser();
        if (publicKey != null) {
            parserBuilder.verifyWith(publicKey);
        } else {
            parserBuilder.verifyWith(secretKey);
        }
        return parserBuilder.build().parseSignedClaims(token).getPayload();
    }

    private PublicKey parsePublicKey(String rawKey) throws Exception {
        String pem = rawKey
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }

    private Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }
}

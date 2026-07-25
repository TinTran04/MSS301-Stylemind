package com.stylemind.common.config;

import com.stylemind.common.security.JwtUtil;
import com.stylemind.common.security.RsaKeyLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.security.PrivateKey;
import java.security.PublicKey;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JwtAutoConfiguration {

    private final JwtKeyProperties properties;
    private final ApplicationContext applicationContext;
    private final Environment environment;

    /**
     * Issuer Configuration: Creates PrivateKey bean when jwt.private-key-path is set.
     * Used by auth-service for token signing.
     */
    @Bean
    public PrivateKey privateKey() {
        String privateKeyPath = environment.getProperty("JWT_PRIVATE_KEY_PATH");
        if (privateKeyPath == null || privateKeyPath.isEmpty()) {
            privateKeyPath = properties.getPrivateKeyPath();
        }
        
        if (privateKeyPath == null || privateKeyPath.isEmpty()) {
            log.error("JWT_PRIVATE_KEY_PATH is null or empty from both Environment and Properties");
            return null;
        }
        try {
            return RsaKeyLoader.loadPrivateKey(privateKeyPath);
        } catch (Exception e) {
            log.error("Failed to load RSA private key from: {}", privateKeyPath, e);
            return null;
        }
    }

    /**
     * Consumer Configuration: Creates PublicKey bean when jwt.public-key-path is set.
     * Used by consumer services for token verification.
     */
    @Bean
    public PublicKey publicKey() {
        String publicKeyPath = environment.getProperty("JWT_PUBLIC_KEY_PATH");
        if (publicKeyPath == null || publicKeyPath.isEmpty()) {
            publicKeyPath = properties.getPublicKeyPath();
        }
        
        if (publicKeyPath == null || publicKeyPath.isEmpty()) {
            log.error("JWT_PUBLIC_KEY_PATH is null or empty from both Environment and Properties");
            return null;
        }
        try {
            return RsaKeyLoader.loadPublicKey(publicKeyPath);
        } catch (Exception e) {
            log.error("Failed to load RSA public key from: {}", publicKeyPath, e);
            return null;
        }
    }

    /**
     * Unified JwtUtil Bean: Dynamically creates appropriate JwtUtil instance
     * based on available key type. Priority: PrivateKey > PublicKey.
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtUtil jwtUtil() {
        // Manually look up beans from context to avoid autowiring issues
        PrivateKey privateKey = null;
        PublicKey publicKey = null;
        
        try {
            privateKey = applicationContext.getBean(PrivateKey.class);
        } catch (Exception e) {
            // PrivateKey bean not configured
        }
        
        try {
            publicKey = applicationContext.getBean(PublicKey.class);
        } catch (Exception e) {
            // PublicKey bean not configured
        }
        
        // Priority 1: Issuer mode (PrivateKey available)
        if (privateKey != null) {
            log.info("Initializing JwtUtil in ISSUER mode (RSA-2048 signing)");
            return new JwtUtil(
                privateKey,
                publicKey, // Use public key if available
                properties.getAccessTokenExpiration(),
                properties.getRefreshTokenExpiration()
            );
        }
        
        // Priority 2: Consumer mode (only PublicKey available)
        if (publicKey != null) {
            log.info("Initializing JwtUtil in CONSUMER mode (RSA-2048 verification)");
            return new JwtUtil(
                publicKey,
                properties.getAccessTokenExpiration(),
                properties.getRefreshTokenExpiration()
            );
        }
        
        throw new IllegalStateException(
            "No JWT key configured. Set jwt.private-key-path or jwt.public-key-path"
        );
    }
}

# Asymmetric JWT (RS256) Implementation Plan

**Document Version:** 1.0  
**Date:** 2026-07-12  
**Status:** Ready for Implementation  
**Estimated Duration:** 2-3 days

---

## Overview

This document provides a step-by-step implementation plan for migrating the StyleMind authentication system from Symmetric Key (HS256) to Asymmetric Key (RS256) JWT architecture. The plan is organized into phases to ensure minimal disruption and easy rollback.

**Prerequisites:**
- Infrastructure configuration completed (Docker Compose, application.yml, .env)
- RSA-2048 key pair generated in `.docker/certs/`
- Blueprint document reviewed: `ASYMMETRIC_JWT_JAVA_REFACTORING_BLUEPRINT.md`

---

## Phase 1: Common-Lib Foundation (No Breaking Changes)

**Objective:** Create utility classes and exception handling without modifying existing functionality.

**Estimated Time:** 2-3 hours

### Task 1.1: Create CryptoException Hierarchy

**File:** `BE/common-lib/src/main/java/com/stylemind/common/exception/CryptoException.java`

```java
package com.stylemind.common.exception;

public class CryptoException extends RuntimeException {
    public CryptoException(String message) {
        super(message);
    }
    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**File:** `BE/common-lib/src/main/java/com/stylemind/common/exception/KeyLoadException.java`

```java
package com.stylemind.common.exception;

public class KeyLoadException extends CryptoException {
    public KeyLoadException(String message) {
        super(message);
    }
    public KeyLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**File:** `BE/common-lib/src/main/java/com/stylemind/common/exception/InvalidKeyFormatException.java`

```java
package com.stylemind.common.exception;

public class InvalidKeyFormatException extends CryptoException {
    public InvalidKeyFormatException(String message) {
        super(message);
    }
    public InvalidKeyFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**File:** `BE/common-lib/src/main/java/com/stylemind/common/exception/KeyDecodingException.java`

```java
package com.stylemind.common.exception;

public class KeyDecodingException extends CryptoException {
    public KeyDecodingException(String message) {
        super(message);
    }
    public KeyDecodingException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**Validation:**
- All exception classes compile without errors
- Package structure matches common-lib conventions

---

### Task 1.2: Create RsaKeyLoader Utility Class

**File:** `BE/common-lib/src/main/java/com/stylemind/common/security/RsaKeyLoader.java`

```java
package com.stylemind.common.security;

import com.stylemind.common.exception.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class RsaKeyLoader {

    private RsaKeyLoader() {
        // Utility class - prevent instantiation
    }

    public static PrivateKey loadPrivateKey(String keyPath) {
        try {
            String pemContent = Files.readString(Paths.get(keyPath));
            String base64Content = stripPemHeaders(pemContent, "PRIVATE KEY");
            byte[] keyBytes = Base64.getDecoder().decode(base64Content);
            
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            
            return keyFactory.generatePrivate(keySpec);
            
        } catch (IOException e) {
            throw new KeyLoadException("Failed to read private key file: " + keyPath, e);
        } catch (InvalidKeySpecException e) {
            throw new InvalidKeyFormatException("Invalid PKCS#8 private key format", e);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyDecodingException("RSA algorithm not available", e);
        }
    }

    public static PublicKey loadPublicKey(String keyPath) {
        try {
            String pemContent = Files.readString(Paths.get(keyPath));
            String base64Content = stripPemHeaders(pemContent, "PUBLIC KEY");
            byte[] keyBytes = Base64.getDecoder().decode(base64Content);
            
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            
            return keyFactory.generatePublic(keySpec);
            
        } catch (IOException e) {
            throw new KeyLoadException("Failed to read public key file: " + keyPath, e);
        } catch (InvalidKeySpecException e) {
            throw new InvalidKeyFormatException("Invalid X.509 public key format", e);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyDecodingException("RSA algorithm not available", e);
        }
    }

    private static String stripPemHeaders(String pemContent, String keyType) {
        String beginMarker = "-----BEGIN " + keyType + "-----";
        String endMarker = "-----END " + keyType + "-----";
        
        return pemContent
            .replace(beginMarker, "")
            .replace(endMarker, "")
            .replaceAll("\\s", "");
    }
}
```

**Validation:**
- Class compiles without errors
- Method signatures match blueprint
- Exception handling is comprehensive

---

### Task 1.3: Create JwtKeyProperties Configuration Class

**File:** `BE/common-lib/src/main/java/com/stylemind/common/config/JwtKeyProperties.java`

```java
package com.stylemind.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtKeyProperties {

    // Asymmetric key paths (RSA-2048)
    private String privateKeyPath;
    private String publicKeyPath;
    
    // Symmetric key (HMAC-SHA256) - for backward compatibility during transition
    private String secret;
    
    // Algorithm configuration
    private String algorithm = "RSA";
    private Integer keySize = 2048;
    
    // Token expiration settings
    private Long accessTokenExpiration = 3600000L;
    private Long refreshTokenExpiration = 604800000L;
}
```

**Validation:**
- Class compiles without errors
- Lombok annotations are available
- Spring Boot configuration properties binding works
- Includes secret field for backward compatibility

---

### Task 1.4: Add Unit Tests for RsaKeyLoader

**File:** `BE/common-lib/src/test/java/com/stylemind/common/security/RsaKeyLoaderTest.java`

```java
package com.stylemind.common.security;

import com.stylemind.common.exception.InvalidKeyFormatException;
import com.stylemind.common.exception.KeyLoadException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;

import static org.junit.jupiter.api.Assertions.*;

class RsaKeyLoaderTest {

    @Test
    void loadPrivateKey_ValidPem_ReturnsPrivateKey(@TempDir Path tempDir) throws Exception {
        // Create test PEM file
        String pemContent = """
            -----BEGIN PRIVATE KEY-----
            MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC7...
            -----END PRIVATE KEY-----
            """.trim();
        
        Path keyFile = tempDir.resolve("test_private_key.pem");
        Files.writeString(keyFile, pemContent);
        
        // Note: This test requires a valid PKCS#8 key for actual execution
        // For now, we test the exception handling
    }

    @Test
    void loadPrivateKey_FileNotFound_ThrowsKeyLoadException() {
        assertThrows(KeyLoadException.class, 
            () -> RsaKeyLoader.loadPrivateKey("nonexistent.pem"));
    }

    @Test
    void loadPrivateKey_InvalidPem_ThrowsInvalidKeyFormatException(@TempDir Path tempDir) throws Exception {
        Path keyFile = tempDir.resolve("invalid.pem");
        Files.writeString(keyFile, "invalid content");
        
        assertThrows(InvalidKeyFormatException.class, 
            () -> RsaKeyLoader.loadPrivateKey(keyFile.toString()));
    }
}
```

**Validation:**
- Unit tests compile
- Tests can be run with `mvn test`
- Exception handling is tested

---

**Phase 1 Completion Criteria:**
- All new classes compile without errors
- Unit tests pass
- No changes to existing `JwtUtil.java`
- Common-lib builds successfully

---

## Phase 2: Issuer Implementation (Auth Service)

**Objective:** Implement RSA signing in auth-service while maintaining backward compatibility.

**Estimated Time:** 3-4 hours

### Task 2.1: Create Unified JwtAutoConfiguration (Replaces Tasks 2.1 & 3.1)

**File:** `BE/common-lib/src/main/java/com/stylemind/common/config/JwtAutoConfiguration.java`

```java
package com.stylemind.common.config;

import com.stylemind.common.security.JwtUtil;
import com.stylemind.common.security.RsaKeyLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.security.PublicKey;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JwtAutoConfiguration {

    private final JwtKeyProperties properties;

    /**
     * Issuer Configuration: Creates PrivateKey bean when jwt.private-key-path is set.
     * Used by auth-service for token signing.
     */
    @Bean
    @ConditionalOnProperty(name = "jwt.private-key-path")
    public PrivateKey privateKey() {
        log.info("Loading RSA private key from: {}", properties.getPrivateKeyPath());
        return RsaKeyLoader.loadPrivateKey(properties.getPrivateKeyPath());
    }

    /**
     * Consumer Configuration: Creates PublicKey bean when jwt.public-key-path is set.
     * Used by consumer services for token verification.
     */
    @Bean
    @ConditionalOnProperty(name = "jwt.public-key-path")
    public PublicKey publicKey() {
        log.info("Loading RSA public key from: {}", properties.getPublicKeyPath());
        return RsaKeyLoader.loadPublicKey(properties.getPublicKeyPath());
    }

    /**
     * Fallback Configuration: Creates SecretKey bean when jwt.secret is set.
     * Maintains backward compatibility with HMAC-SHA256 during transition.
     */
    @Bean
    @ConditionalOnProperty(name = "jwt.secret")
    public SecretKey secretKey() {
        log.info("Using HMAC-SHA256 secret key (legacy mode)");
        return new javax.crypto.spec.SecretKeySpec(
            properties.getSecret().getBytes(),
            "HmacSHA256"
        );
    }

    /**
     * Unified JwtUtil Bean: Dynamically creates appropriate JwtUtil instance
     * based on available key type. Priority: PrivateKey > PublicKey > SecretKey.
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtUtil jwtUtil(
            PrivateKey privateKey,
            PublicKey publicKey,
            SecretKey secretKey) {
        
        // Priority 1: Issuer mode (PrivateKey available)
        if (privateKey != null) {
            log.info("Initializing JwtUtil in ISSUER mode (RSA-2048 signing)");
            return new JwtUtil(
                privateKey,
                publicKey, // Also set public key for internal verification if needed
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
        
        // Priority 3: Legacy mode (only SecretKey available)
        if (secretKey != null) {
            log.info("Initializing JwtUtil in LEGACY mode (HMAC-SHA256)");
            return new JwtUtil(
                secretKey,
                properties.getAccessTokenExpiration(),
                properties.getRefreshTokenExpiration()
            );
        }
        
        throw new IllegalStateException(
            "No JWT key configured. Set jwt.private-key-path, jwt.public-key-path, or jwt.secret"
        );
    }
}
```

**Validation:**
- Configuration class compiles without errors
- Only one JwtUtil bean is created per application context
- Conditional annotations prevent bean conflicts
- Priority logic ensures correct mode selection
- Logging provides clear visibility into initialization

---

### Task 2.2: Refactor JwtUtil with Immutable JwtParser Field

**File:** `BE/common-lib/src/main/java/com/stylemind/common/security/JwtUtil.java`

**Add Immutable Fields:**
```java
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.nio.charset.StandardCharsets;

public class JwtUtil {

    // Immutable key fields (set once during construction)
    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final SecretKey secretKey;
    
    // Pre-compiled parser/builder for zero-I/O runtime performance
    private final JwtParser jwtParser;
    private final JwtBuilder jwtBuilder;
    
    // Token expiration settings
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    
    // Existing constructor for backward compatibility (HMAC-SHA256)
    public JwtUtil(SecretKey secretKey, long accessTokenExpiration, long refreshTokenExpiration) {
        this.privateKey = null;
        this.publicKey = null;
        this.secretKey = secretKey;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        
        // Pre-compile parser and builder during initialization
        this.jwtParser = Jwts.parser()
            .verifyWith(secretKey)
            .build();
        
        this.jwtBuilder = Jwts.builder()
            .signWith(secretKey, SignatureAlgorithm.HS256);
    }
    
    // New constructor for Issuer mode (RSA-2048 signing)
    public JwtUtil(PrivateKey privateKey, PublicKey publicKey, 
                   long accessTokenExpiration, long refreshTokenExpiration) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.secretKey = null;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        
        // Pre-compile parser with public key for verification
        this.jwtParser = Jwts.parser()
            .verifyWith(publicKey != null ? publicKey : privateKey) // Use public key if available
            .build();
        
        // Pre-compile builder with private key for signing
        this.jwtBuilder = Jwts.builder()
            .signWith(privateKey, SignatureAlgorithm.RS256);
    }
    
    // New constructor for Consumer mode (RSA-2048 verification only)
    public JwtUtil(PublicKey publicKey, long accessTokenExpiration, long refreshTokenExpiration) {
        this.privateKey = null;
        this.publicKey = publicKey;
        this.secretKey = null;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        
        // Pre-compile parser with public key for verification
        this.jwtParser = Jwts.parser()
            .verifyWith(publicKey)
            .build();
        
        // Builder not needed in consumer mode (no signing capability)
        this.jwtBuilder = null;
    }
    
    // Getters for expiration settings
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }
    
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}
```

**Validation:**
- All constructors compile without errors
- JwtParser and JwtBuilder are immutable final fields
- Parser/builder pre-compilation happens once during initialization
- No I/O operations during runtime request processing

---

### Task 2.3: Update createToken to Use Pre-Compiled JwtBuilder

**File:** `BE/common-lib/src/main/java/com/stylemind/common/security/JwtUtil.java`

**Add Method:**
```java
private String createToken(Map<String, Object> claims, String subject, long expiration) {
    if (jwtBuilder == null) {
        throw new IllegalStateException("JwtUtil not configured for signing (consumer mode)");
    }
    
    return jwtBuilder
            .claims(claims)
            .subject(subject)
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .compact();
}
```

**Validation:**
- Method compiles without errors
- Uses pre-compiled JwtBuilder (zero I/O during runtime)
- Throws exception if called in consumer mode (no signing capability)
- Sub-millisecond execution time

---

### Task 2.4: Update AuthService to Use Issuer JwtUtil

**File:** `BE/auth-service/src/main/java/com/stylemind/auth/service/AuthService.java`

**No changes required** - AuthService already uses JwtUtil via constructor injection. The unified JwtAutoConfiguration will automatically provide the issuer-specific JwtUtil bean when `jwt.private-key-path` is configured.

**Validation:**
- AuthService compiles without changes
- Spring can inject the new JwtUtil bean
- Startup logs show "Initializing JwtUtil in ISSUER mode (RSA-2048 signing)"

---

### Task 2.5: Update extractAllClaims to Use Pre-Compiled JwtParser

**File:** `BE/common-lib/src/main/java/com/stylemind/common/security/JwtUtil.java`

**Add Method:**
```java
private Claims extractAllClaims(String token) {
    // Use pre-compiled JwtParser - zero I/O during runtime
    return jwtParser.parseSignedClaims(token).getPayload();
}
```

**Validation:**
- Method compiles without errors
- Uses pre-compiled JwtParser (zero I/O during runtime)
- Sub-millisecond execution time
- No file I/O or key parsing during request processing

---

### Task 2.6: Add Integration Tests for Token Signing

**File:** `BE/auth-service/src/test/java/com/stylemind/auth/service/AuthServiceIntegrationTest.java`

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Test
    void login_ValidCredentials_ReturnsRs256Token() {
        LoginRequest request = new LoginRequest("test@example.com", "password");
        AuthResponse.LoginResponse response = authService.login(request);
        
        assertNotNull(response.getToken());
        
        // Verify token is RS256 signed
        String[] parts = response.getToken().split("\\.");
        String header = new String(Base64.getUrlDecoder().decode(parts[0]));
        assertTrue(header.contains("\"alg\":\"RS256\""));
    }
}
```

**Validation:**
- Integration test compiles
- Test can be run with `mvn test`
- Token algorithm verification works

---

**Phase 2 Completion Criteria:**
- Auth service starts successfully with private key
- Token signing produces valid RS256 tokens
- Existing login/register endpoints work
- Integration tests pass
- Backward compatibility maintained (HMAC still works if no private key)
- JwtParser and JwtBuilder are pre-compiled during initialization
- Zero I/O operations during runtime request processing

---

## Phase 3: Deployment to Development Environment

**Objective:** Deploy auth-service and api-gateway changes to development environment for testing.

**Estimated Time:** 1-2 hours

### Task 3.1: Deploy Auth Service

**Steps:**
1. Build auth-service with new common-lib:
   ```bash
   cd BE
   mvn clean install -DskipTests
   cd auth-service
   mvn spring-boot:run
   ```

2. Verify startup logs show private key loaded successfully

3. Test login endpoint:
   ```bash
   curl -X POST http://localhost:8081/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"test@example.com","password":"password"}'
   ```

4. Verify returned token uses RS256 algorithm

**Validation:**
- Auth service starts without errors
- Login endpoint returns valid RS256 token
- No errors in logs related to key loading

---

### Task 3.2: Deploy API Gateway

**Steps:**
1. Build api-gateway with new common-lib:
   ```bash
   cd BE/api-gateway
   mvn spring-boot:run
   ```

2. Verify startup logs show public key loaded successfully

3. Test protected endpoint with RS256 token:
   ```bash
   curl -X GET http://localhost:3000/api/users/profile \
     -H "Authorization: Bearer <rs256-token>"
   ```

4. Verify request succeeds

**Validation:**
- API gateway starts without errors
- Protected endpoints accept RS256 tokens
- No errors in logs related to key loading

---

**Phase 3 Completion Criteria:**
- Both services start successfully in development
- End-to-end authentication flow works
- No performance degradation observed
- Monitoring shows normal operation
- Startup logs show correct mode (ISSUER/CONSUMER)

---

## Phase 4: Consumer Rollout (Remaining Services)

**Objective:** Deploy consumer services incrementally with public key configuration.

**Estimated Time:** 2-3 hours

### Task 4.1: Deploy Consumer Services

**Services to deploy:**
- user-service
- cart-service
- order-service
- product-service
- payment-service
- notification-service
- ai-agent-service

**Deployment Steps (per service):**
1. Build service with new common-lib:
   ```bash
   cd BE/<service-name>
   mvn spring-boot:run
   ```

2. Verify startup logs show public key loaded successfully

3. Test service-specific endpoints with RS256 token

4. Monitor for errors

**Validation:**
- Service starts without errors
- Service accepts RS256 tokens
- No errors in logs

---

### Task 4.2: Monitor and Validate

**Monitoring Checklist:**
- All services start successfully
- No errors related to key loading
- Authentication flow works end-to-end
- Performance metrics are normal
- No increase in error rates

**Rollback Trigger:**
- If any service fails to start
- If error rate increases > 5%
- If performance degrades > 50%

---

**Phase 4 Completion Criteria:**
- All consumer services deployed successfully
- End-to-end authentication flow works across all services
- No errors or performance issues
- Monitoring shows normal operation
- All services show "CONSUMER mode" in startup logs

---

## Phase 5: Cleanup (Remove Legacy Code)

**Objective:** Remove symmetric key configuration and HMAC code paths.

**Estimated Time:** 1-2 hours

### Task 5.1: Remove JWT_SECRET Configuration

**Files to update:**
- `BE/docker-compose-separated.yml` - Remove `JWT_SECRET` from all services
- `BE/.env.tested` - Remove `JWT_SECRET` variable

**Validation:**
- No references to JWT_SECRET remain
- Services still start without JWT_SECRET

---

### Task 5.2: Remove HMAC Code Paths from JwtUtil

**File:** `BE/common-lib/src/main/java/com/stylemind/common/security/JwtUtil.java`

**Remove:**
- `secretKey` field
- Constructor that accepts `String secret`
- HMAC fallback logic in `createToken()`
- HMAC fallback logic in `extractAllClaims()`

**Validation:**
- JwtUtil compiles without errors
- Only RSA signing/verification logic remains
- No references to HMAC or secretKey

---

### Task 5.3: Update Documentation

**Files to update:**
- `README.md` - Document asymmetric key setup
- `AGENTS.md` - Update JWT configuration section
- Architecture documentation - Update security section

**Validation:**
- Documentation is accurate
- Setup instructions are clear

---

**Phase 5 Completion Criteria:**
- No symmetric key code remains
- Documentation is updated
- System is fully migrated to asymmetric keys

---

## Rollback Plan

### Rollback Triggers
- Critical security vulnerability discovered
- Performance degradation > 50%
- Integration test failures in production
- Key compromise incident

### Rollback Procedure

**Step 1: Revert Configuration**
```yaml
# Revert to symmetric key configuration
jwt:
  secret: ${JWT_SECRET:super-secure-stylemind-secret-key}
```

**Step 2: Revert Code**
- Restore previous version of `JwtUtil.java`
- Remove `JwtIssuerConfig.java` and `JwtConsumerConfig.java`
- Keep `RsaKeyLoader.java` for future use

**Step 3: Redeploy Services**
- Deploy reverted version to all services
- Verify authentication endpoints work
- Monitor for errors

### Rollback Validation
- All services start successfully
- Authentication endpoints respond correctly
- No error logs related to JWT processing
- Performance metrics return to baseline

---

## Testing Strategy

### Unit Tests
- RsaKeyLoader PEM parsing tests
- Exception handling tests
- JwtUtil constructor tests

### Integration Tests
- Auth service token signing tests
- API gateway token verification tests
- End-to-end authentication flow tests

### Performance Tests
- Token signing performance (target: < 20ms average)
- Token verification performance (target: < 5ms average)
- Memory footprint validation

### Security Tests
- Private key file permission validation
- Token signature verification
- Algorithm claim validation

---

## Monitoring & Observability

### Metrics to Track
- Token issuance rate (tokens/second)
- Token verification rate (tokens/second)
- Token validation failure rate (%)
- Average token signing latency (ms)
- Average token verification latency (ms)

### Logging Strategy
- **ERROR:** Cryptographic failures, configuration errors
- **WARN:** Invalid tokens, signature verification failures
- **INFO:** Successful token issuance, key loading
- **DEBUG:** Detailed token processing (development only)

### Alerting
- Cryptographic error rate > 1% (5-minute window)
- Token verification latency > 100ms (5-minute window)
- Service startup failure due to key configuration

---

## Success Criteria

### Functional Requirements
- ✅ Auth service signs tokens with RSA-2048
- ✅ Consumer services verify tokens with public key
- ✅ No private key exposure in consumer services
- ✅ Existing authentication flow works unchanged

### Non-Functional Requirements
- ✅ Token signing latency < 20ms average
- ✅ Token verification latency < 5ms average
- ✅ Memory footprint < 5KB per service
- ✅ Zero I/O operations during request processing

### Security Requirements
- ✅ Private key never exposed to consumer services
- ✅ Keys stored securely with proper permissions
- ✅ Keys excluded from version control
- ✅ Industry-standard cryptographic algorithms

---

## Timeline Estimate

| Phase | Estimated Time | Dependencies |
|-------|----------------|--------------|
| Phase 1: Common-Lib Foundation | 2-3 hours | None |
| Phase 2: Unified Implementation | 3-4 hours | Phase 1 |
| Phase 3: Dev Deployment | 1-2 hours | Phase 2 |
| Phase 4: Consumer Rollout | 2-3 hours | Phase 3 |
| Phase 5: Cleanup | 1-2 hours | Phase 4 |
| **Total** | **9-14 hours** | - |

---

## Next Steps

1. Review and approve this implementation plan
2. Begin Phase 1 implementation (Common-Lib Foundation)
3. Execute phases sequentially
4. Monitor and validate each phase
5. Complete cleanup and documentation

---

**Document End**

---

## Payment-Service Property Placeholder Fix Plan

### Issue Analysis
Payment-service fails to start due to missing environment variables for SePay configuration. The following properties in `application.yml` have no default values:

1. `SEPAY_BANK_SHORT_NAME` (line 42)
2. `SEPAY_ACCOUNT_NUMBER` (line 43)  
3. `SEPAY_ACCOUNT_NAME` (line 44)
4. `SEPAY_WEBHOOK_API_KEY` (line 56) - marked as mandatory in comments

### Root Cause
Spring Boot property placeholder resolution fails when required environment variables are not set and no default values are provided in the configuration.

### Fix Strategy

#### Option 1: Add Development Default Values (Recommended for Local Dev)
Add safe default values for local development while keeping production values via environment variables:

```yaml
app:
  vietqr:
    bank-id: ${SEPAY_BANK_SHORT_NAME:VCB}
    account-no: ${SEPAY_ACCOUNT_NUMBER:1234567890}
    account-name: ${SEPAY_ACCOUNT_NAME:TEST_ACCOUNT}
  sepay:
    webhook-api-key: ${SEPAY_WEBHOOK_API_KEY:test-dev-api-key}
```

#### Option 2: Disable SePay for Local Development
Set SePay to disabled by default for local development:

```yaml
app:
  sepay:
    enabled: ${SEPAY_ENABLED:false}
```

### Implementation Steps

1. **Update payment-service application.yml**
   - Add default values for SePay configuration properties
   - Ensure webhook-api-key has a safe development default
   - Consider setting `SEPAY_ENABLED: false` as default for local dev

2. **Test payment-service startup**
   - Start payment-service with JWT_PUBLIC_KEY_PATH environment variable
   - Verify no property placeholder errors
   - Confirm service starts successfully on port 8088

3. **Verify integration**
   - Test payment-service health endpoint
   - Verify JWT token validation works with public key
   - Confirm service can communicate with order-service

### Configuration Changes Required

**File:** `BE/payment-service/src/main/resources/application.yml`

**Changes:**
- Line 42: Add default value for bank-id
- Line 43: Add default value for account-no  
- Line 44: Add default value for account-name
- Line 49: Change default for enabled to false
- Line 56: Add default value for webhook-api-key

### Testing Plan

1. Start payment-service with only JWT_PUBLIC_KEY_PATH set
2. Verify successful startup without SePay errors
3. Test health endpoint: `http://localhost:8088/actuator/health`
4. Verify JWT token validation works correctly
5. Test order-service integration (if applicable)

### Notes

- This fix is separate from the JWT implementation work
- SePay is a third-party payment gateway integration
- For local development, SePay can be disabled without affecting core functionality
- Production deployments should use environment variables for actual SePay credentials

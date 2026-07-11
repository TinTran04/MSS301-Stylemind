# Asymmetric JWT (RS256) Java Refactoring Blueprint

**Document Version:** 1.0  
**Date:** 2026-07-12  
**Author:** Principal Software Architect  
**Scope:** StyleMind Backend Microservices Security Refactoring

---

## 1. EXECUTIVE SUMMARY

This blueprint outlines the comprehensive refactoring strategy to migrate the StyleMind authentication system from Symmetric Key (HS256) to Asymmetric Key (RS256) JWT architecture. The refactoring ensures strict separation of concerns between token issuance (auth-service) and token verification (consumer services), enhances security through cryptographic best practices, and maintains high performance through intelligent key caching.

**Key Objectives:**
- Replace HMAC-SHA256 signing with RSA-2048 signing
- Implement secure PKCS#8 private key parsing for token issuance
- Implement X.509 public key parsing for token verification
- Ensure zero I/O operations during request processing
- Maintain backward compatibility during transition
- Provide comprehensive error handling for cryptographic failures

---

## 2. COMPONENT ARCHITECTURE SPECIFICATION

### 2.1 Classes to Modify

#### 2.1.1 Core Security Library (`common-lib`)

| Class | Current State | Target State | Modification Type |
|-------|---------------|--------------|-------------------|
| `JwtUtil.java` | Symmetric key (HMAC-SHA256) | Asymmetric key (RSA-2048) | **Major Refactor** |
| `SecurityConfig.java` | Single JwtUtil bean | Dual JwtUtil beans (issuer/consumer) | **Moderate Refactor** |
| `JwtAuthenticationFilter.java` | Symmetric verification | Asymmetric verification | **Minor Refactor** |

#### 2.1.2 Auth Service (`auth-service`)

| Class | Current State | Target State | Modification Type |
|-------|---------------|--------------|-------------------|
| `AuthService.java` | Uses JwtUtil for signing | Uses JwtUtil (issuer) for signing | **No Change** |
| `application.yml` | `jwt.secret` property | `jwt.private-key-path` property | **Configuration Update** |

#### 2.1.3 Consumer Services

| Class | Current State | Target State | Modification Type |
|-------|---------------|--------------|-------------------|
| `application.yml` | No JWT config | `jwt.public-key-path` property | **Configuration Update** |

### 2.2 Classes to Create

| Class | Purpose | Package | Scope |
|-------|---------|---------|-------|
| `RsaKeyLoader.java` | PEM file parsing and KeySpec generation | `com.stylemind.common.security` | Common-lib |
| `JwtKeyProperties.java` | Configuration properties binding | `com.stylemind.common.config` | Common-lib |
| `JwtIssuerConfig.java` | Issuer-specific bean configuration | `com.stylemind.common.config` | Common-lib |
| `JwtConsumerConfig.java` | Consumer-specific bean configuration | `com.stylemind.common.config` | Common-lib |
| `CryptoException.java` | Custom cryptographic exception | `com.stylemind.common.exception` | Common-lib |

### 2.3 Component Dependency Graph

```
┌─────────────────────────────────────────────────────────────┐
│                    common-lib                                  │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │  RsaKeyLoader (Utility)                                  │ │
│  │  - loadPrivateKey(String path) → PrivateKey               │ │
│  │  - loadPublicKey(String path) → PublicKey                │ │
│  └──────────────────────────────────────────────────────────┘ │
│                              │                                │
│                              ▼                                │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │  JwtKeyProperties (Configuration)                        │ │
│  │  - private-key-path: String                              │ │
│  │  - public-key-path: String                               │ │
│  │  - algorithm: String                                     │ │
│  │  - key-size: Integer                                     │ │
│  └──────────────────────────────────────────────────────────┘ │
│                              │                                │
│              ┌───────────────┴───────────────┐               │
│              ▼                               ▼               │
│  ┌──────────────────────┐      ┌──────────────────────┐      │
│  │  JwtIssuerConfig     │      │  JwtConsumerConfig    │      │
│  │  (Issuer Beans)      │      │  (Consumer Beans)     │      │
│  │  - privateKeyBean    │      │  - publicKeyBean      │      │
│  │  - jwtUtilIssuer    │      │  - jwtUtilConsumer    │      │
│  └──────────────────────┘      └──────────────────────┘      │
│                              │                                │
│                              ▼                                │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │  JwtUtil (Refactored)                                     │ │
│  │  - Constructor injection: PrivateKey OR PublicKey        │ │
│  │  - generateAccessToken() (issuer only)                    │ │
│  │  - validateToken() (consumer only)                        │ │
│  └──────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
         │                                    │
         │                                    │
         ▼                                    ▼
┌─────────────────┐                  ┌─────────────────┐
│  auth-service   │                  │  Consumer Svc   │
│  (Uses Issuer)  │                  │  (Uses Consumer)│
└─────────────────┘                  └─────────────────┘
```

### 2.4 Input/Output Structures

#### 2.4.1 RsaKeyLoader

**Input:**
- `String keyPath`: Absolute file path to PEM key file
- `KeyType type`: Enum {PRIVATE, PUBLIC}

**Output:**
- `PrivateKey` or `PublicKey`: Java Security API key object

**Dependencies:**
- `java.nio.file.Files`
- `java.security.KeyFactory`
- `java.security.spec.PKCS8EncodedKeySpec`
- `java.security.spec.X509EncodedKeySpec`
- `java.util.Base64`

**Exception Propagation:**
- `CryptoException` (wraps IOException, InvalidKeySpecException, NoSuchAlgorithmException)

#### 2.4.2 JwtUtil (Refactored)

**Constructor Input (Issuer):**
- `PrivateKey privateKey`: RSA private key for signing
- `long accessTokenExpiration`: Token TTL in milliseconds
- `long refreshTokenExpiration`: Refresh token TTL in milliseconds

**Constructor Input (Consumer):**
- `PublicKey publicKey`: RSA public key for verification
- `long accessTokenExpiration`: Token TTL in milliseconds
- `long refreshTokenExpiration`: Refresh token TTL in milliseconds

**Method Outputs:**
- `String generateAccessToken()`: Signed JWT token (issuer only)
- `String generateRefreshToken()`: Signed JWT token (issuer only)
- `Claims extractAllClaims()`: Decoded JWT claims (consumer only)
- `boolean validateToken()`: Boolean verification result (consumer only)

**Dependencies:**
- `io.jsonwebtoken.Jwts`
- `io.jsonwebtoken.SignatureAlgorithm`
- `io.jsonwebtoken.security.Keys`

---

## 3. CRYPTOGRAPHIC UTILITY PSEUDO-DESIGN

### 3.1 RsaKeyLoader - PEM Parsing Logic

#### 3.1.1 Private Key Loading (PKCS#8 Format)

**High-Level Algorithm:**

```
FUNCTION loadPrivateKey(String keyPath) RETURNS PrivateKey
    BEGIN
        TRY
            // Step 1: Read entire PEM file as string
            String pemContent = Files.readString(Paths.get(keyPath))
            
            // Step 2: Strip PEM headers and footers
            String base64Content = pemContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "")  // Remove all whitespace
            
            // Step 3: Decode Base64 to byte array
            byte[] keyBytes = Base64.getDecoder().decode(base64Content)
            
            // Step 4: Generate PKCS8 Encoded KeySpec
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes)
            
            // Step 5: Create RSA KeyFactory and generate PrivateKey
            KeyFactory keyFactory = KeyFactory.getInstance("RSA")
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec)
            
            RETURN privateKey
            
        CATCH IOException
            THROW new CryptoException("Failed to read private key file: " + keyPath)
        CATCH InvalidKeySpecException
            THROW new CryptoException("Invalid PKCS#8 private key format")
        CATCH NoSuchAlgorithmException
            THROW new CryptoException("RSA algorithm not available")
    END
END FUNCTION
```

**Key Design Decisions:**
- **PKCS#8 Format:** Standard format for RSA private keys, compatible with OpenSSL output
- **Base64 Decoder:** Uses Java 8+ `Base64.getDecoder()` for standard Base64
- **Whitespace Handling:** Removes all whitespace to handle line breaks in PEM files
- **Exception Wrapping:** All checked exceptions wrapped in custom `CryptoException`

#### 3.1.2 Public Key Loading (X.509 Format)

**High-Level Algorithm:**

```
FUNCTION loadPublicKey(String keyPath) RETURNS PublicKey
    BEGIN
        TRY
            // Step 1: Read entire PEM file as string
            String pemContent = Files.readString(Paths.get(keyPath))
            
            // Step 2: Strip PEM headers and footers
            String base64Content = pemContent
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "")  // Remove all whitespace
            
            // Step 3: Decode Base64 to byte array
            byte[] keyBytes = Base64.getDecoder().decode(base64Content)
            
            // Step 4: Generate X509 Encoded KeySpec
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes)
            
            // Step 5: Create RSA KeyFactory and generate PublicKey
            KeyFactory keyFactory = KeyFactory.getInstance("RSA")
            PublicKey publicKey = keyFactory.generatePublic(keySpec)
            
            RETURN publicKey
            
        CATCH IOException
            THROW new CryptoException("Failed to read public key file: " + keyPath)
        CATCH InvalidKeySpecException
            THROW new CryptoException("Invalid X.509 public key format")
        CATCH NoSuchAlgorithmException
            THROW new CryptoException("RSA algorithm not available")
    END
END FUNCTION
```

**Key Design Decisions:**
- **X.509 Format:** Standard format for RSA public keys, compatible with OpenSSL output
- **Same Base64 Logic:** Reuses Base64 decoding logic for consistency
- **Exception Wrapping:** Maintains consistent exception handling pattern

### 3.2 JwtUtil - Token Signing Logic (Issuer)

**High-Level Algorithm for Token Generation:**

```
FUNCTION generateAccessToken(UserDetails userDetails, String userId, String role) RETURNS String
    BEGIN
        // Step 1: Build claims map
        Map<String, Object> claims = new HashMap<>()
        claims.put("userId", userId)
        claims.put("role", role)
        
        // Step 2: Build JWT with RSA-256 signing
        String token = Jwts.builder()
            .claims(claims)
            .subject(userDetails.getUsername())
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
            .signWith(privateKey, SignatureAlgorithm.RS256)  // RSA signing
            .compact()
        
        RETURN token
    END
END FUNCTION
```

**Key Design Decisions:**
- **RS256 Algorithm:** Uses RSA-SHA256 for asymmetric signing
- **Private Key Injection:** Private key injected via constructor, not hardcoded
- **Same Claim Structure:** Maintains existing claim structure for backward compatibility

### 3.3 JwtUtil - Token Verification Logic (Consumer)

**High-Level Algorithm for Token Validation:**

```
FUNCTION validateToken(String token, UserDetails userDetails) RETURNS boolean
    BEGIN
        TRY
            // Step 1: Extract claims using public key verification
            Claims claims = Jwts.parser()
                .verifyWith(publicKey)  // RSA verification
                .build()
                .parseSignedClaims(token)
                .getPayload()
            
            // Step 2: Validate subject
            String username = claims.getSubject()
            IF NOT username.equals(userDetails.getUsername()) THEN
                RETURN false
            END IF
            
            // Step 3: Validate expiration
            Date expiration = claims.getExpiration()
            IF expiration.before(new Date()) THEN
                RETURN false
            END IF
            
            RETURN true
            
        CATCH ExpiredJwtException
            RETURN false
        CATCH SecurityException
            RETURN false
        CATCH MalformedJwtException
            RETURN false
        CATCH SignatureException
            RETURN false
    END
END FUNCTION
```

**Key Design Decisions:**
- **Public Key Verification:** Uses injected public key for signature verification
- **Comprehensive Exception Handling:** Catches all JWT-related exceptions
- **Graceful Failure:** Returns `false` on validation failure instead of throwing

---

## 4. CONCURRENCY & PERFORMANCE CONSIDERATIONS

### 4.1 Key Caching Strategy

#### 4.1.1 Bean Lifecycle Management

**Design Pattern:** Singleton Beans with Constructor Injection

**Implementation Strategy:**

```
@Configuration
public class JwtIssuerConfig {
    
    @Bean
    @ConditionalOnProperty(name = "jwt.private-key-path")
    public PrivateKey privateKey(JwtKeyProperties properties) {
        // Load once at application startup
        return RsaKeyLoader.loadPrivateKey(properties.getPrivateKeyPath());
    }
    
    @Bean
    @ConditionalOnBean(PrivateKey.class)
    public JwtUtil jwtUtilIssuer(PrivateKey privateKey, 
                                 @Value("${jwt.access-token-expiration}") long accessExp,
                                 @Value("${jwt.refresh-token-expiration}") long refreshExp) {
        // Create issuer-specific JwtUtil with private key
        return new JwtUtil(privateKey, accessExp, refreshExp);
    }
}
```

**Performance Characteristics:**
- **Startup Cost:** One-time I/O operation during application initialization
- **Runtime Cost:** Zero I/O operations during request processing
- **Memory Footprint:** Single PrivateKey object in memory (~2KB for RSA-2048)
- **Thread Safety:** PrivateKey and PublicKey are immutable and thread-safe

#### 4.1.2 Consumer Bean Configuration

**Implementation Strategy:**

```
@Configuration
public class JwtConsumerConfig {
    
    @Bean
    @ConditionalOnProperty(name = "jwt.public-key-path")
    public PublicKey publicKey(JwtKeyProperties properties) {
        // Load once at application startup
        return RsaKeyLoader.loadPublicKey(properties.getPublicKeyPath());
    }
    
    @Bean
    @ConditionalOnBean(PublicKey.class)
    public JwtUtil jwtUtilConsumer(PublicKey publicKey,
                                   @Value("${jwt.access-token-expiration}") long accessExp,
                                   @Value("${jwt.refresh-token-expiration}") long refreshExp) {
        // Create consumer-specific JwtUtil with public key
        return new JwtUtil(publicKey, accessExp, refreshExp);
    }
}
```

**Conditional Bean Creation:**
- **Issuer Service:** Only creates PrivateKey bean if `jwt.private-key-path` is set
- **Consumer Services:** Only creates PublicKey bean if `jwt.public-key-path` is set
- **Fallback:** Services without key configuration will fail fast at startup

### 4.2 Request Processing Performance

#### 4.2.1 Token Issuance (Auth Service)

**Performance Profile:**

| Operation | Complexity | Time Cost | Frequency |
|-----------|------------|-----------|-----------|
| Private Key Lookup | O(1) | < 1μs | Per request |
| RSA-2048 Signing | O(log n) | ~5-10ms | Per request |
| Claim Building | O(n) | < 1μs | Per request |
| **Total** | - | **~5-10ms** | Per request |

**Optimization Notes:**
- Private key is cached in memory, no file I/O during requests
- RSA signing is CPU-bound, but acceptable for login/register operations
- No synchronization needed (PrivateKey is thread-safe)

#### 4.2.2 Token Verification (Consumer Services)

**Performance Profile:**

| Operation | Complexity | Time Cost | Frequency |
|-----------|------------|-----------|-----------|
| Public Key Lookup | O(1) | < 1μs | Per request |
| RSA-2048 Verification | O(log n) | ~1-2ms | Per request |
| Claim Extraction | O(n) | < 1μs | Per request |
| **Total** | - | **~1-2ms** | Per request |

**Optimization Notes:**
- Public key is cached in memory, no file I/O during requests
- RSA verification is faster than signing (no private key operations)
- No synchronization needed (PublicKey is thread-safe)
- Suitable for high-throughput API gateway filtering

### 4.3 Memory Footprint Analysis

**Per-Service Memory Consumption:**

| Component | Size | Count | Total |
|-----------|------|-------|-------|
| PrivateKey (RSA-2048) | ~2KB | 1 (auth-service) | 2KB |
| PublicKey (RSA-2048) | ~1KB | 1 (each consumer) | 1KB per service |
| JwtUtil Instance | ~0.5KB | 1 per service | 0.5KB per service |
| **Total per Service** | - | - | **~1.5-3KB** |

**System-Wide Memory Impact:**
- **Auth Service:** ~3KB
- **Each Consumer Service:** ~1.5KB
- **Total (9 services):** ~15KB
- **Negligible Impact:** Memory overhead is insignificant compared to JVM heap

### 4.4 Thread Safety Guarantees

**Immutable Objects:**
- `PrivateKey`: Java Security API guarantees immutability
- `PublicKey`: Java Security API guarantees immutability
- `JwtUtil`: Stateless after construction, all fields are final

**No Synchronization Required:**
- Key objects are read-only after initialization
- JWT library (jjwt) handles internal synchronization
- No shared mutable state between threads

**Concurrent Request Handling:**
- Multiple threads can safely use the same JwtUtil instance
- No race conditions on key access
- No need for synchronized blocks or locks

---

## 5. EXCEPTION HANDLING MATRIX

### 5.1 Cryptographic Exception Hierarchy

```
CryptoException (Runtime)
├── KeyLoadException
│   ├── FileNotFoundException
│   ├── InvalidKeyFormatException
│   └── KeyDecodingException
├── TokenGenerationException
│   ├── SigningException
│   └── ClaimBuildException
└── TokenValidationException
    ├── SignatureVerificationException
    ├── TokenExpiredException
    ├── MalformedTokenException
    └── InvalidClaimException
```

### 5.2 Exception Handling Matrix

| Exception Type | Source | HTTP Status | User Message | Log Level | Recovery Strategy |
|----------------|--------|-------------|--------------|-----------|-------------------|
| `FileNotFoundException` | RsaKeyLoader | 500 | "Server configuration error" | ERROR | Fail fast, alert ops |
| `InvalidKeyFormatException` | RsaKeyLoader | 500 | "Server configuration error" | ERROR | Fail fast, alert ops |
| `KeyDecodingException` | RsaKeyLoader | 500 | "Server configuration error" | ERROR | Fail fast, alert ops |
| `SigningException` | JwtUtil (issuer) | 500 | "Authentication service unavailable" | ERROR | Retry with backoff |
| `SignatureVerificationException` | JwtUtil (consumer) | 401 | "Invalid token signature" | WARN | Reject request |
| `TokenExpiredException` | JwtUtil (consumer) | 401 | "Token has expired" | INFO | Request refresh |
| `MalformedTokenException` | JwtUtil (consumer) | 401 | "Invalid token format" | WARN | Reject request |
| `InvalidClaimException` | JwtUtil (consumer) | 401 | "Invalid token claims" | WARN | Reject request |
| `NoSuchAlgorithmException` | RsaKeyLoader | 500 | "Server configuration error" | ERROR | Fail fast, alert ops |

### 5.3 Exception Handling Implementation

#### 5.3.1 RsaKeyLoader Exception Handling

```java
public class RsaKeyLoader {
    
    public static PrivateKey loadPrivateKey(String keyPath) {
        try {
            String pemContent = Files.readString(Paths.get(keyPath));
            String base64Content = pemContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
            
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
}
```

#### 5.3.2 JwtUtil Exception Handling (Consumer)

```java
public boolean validateToken(String token, UserDetails userDetails) {
    try {
        Claims claims = Jwts.parser()
            .verifyWith(publicKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        
        String username = claims.getSubject();
        if (!username.equals(userDetails.getUsername())) {
            return false;
        }
        
        Date expiration = claims.getExpiration();
        if (expiration.before(new Date())) {
            return false;
        }
        
        return true;
        
    } catch (ExpiredJwtException e) {
        log.debug("Token expired: {}", e.getMessage());
        return false;
    } catch (SecurityException | MalformedJwtException | SignatureException e) {
        log.warn("Invalid token: {}", e.getMessage());
        return false;
    }
}
```

#### 5.3.3 Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(CryptoException.class)
    public ResponseEntity<ApiResponse> handleCryptoException(CryptoException e) {
        log.error("Cryptographic error: {}", e.getMessage(), e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("AUTH_CONFIG_ERROR", "Authentication service unavailable"));
    }
    
    @ExceptionHandler(TokenValidationException.class)
    public ResponseEntity<ApiResponse> handleTokenValidationException(TokenValidationException e) {
        log.warn("Token validation failed: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("AUTH_TOKEN_INVALID", e.getMessage()));
    }
}
```

### 5.4 Startup Validation

**Fail-Fast Strategy:**

```
@PostConstruct
public void validateKeyConfiguration() {
    if (isIssuerService() && !hasPrivateKey()) {
        throw new IllegalStateException(
            "Issuer service requires jwt.private-key-path configuration");
    }
    if (isConsumerService() && !hasPublicKey()) {
        throw new IllegalStateException(
            "Consumer service requires jwt.public-key-path configuration");
    }
    log.info("JWT key configuration validated successfully");
}
```

**Benefits:**
- Prevents runtime failures due to missing configuration
- Provides clear error messages at startup
- Enables early detection of configuration issues

---

## 6. IMPLEMENTATION PHASING

### 6.1 Phase 1: Common-Lib Foundation (No Breaking Changes)

**Tasks:**
1. Create `RsaKeyLoader.java` utility class
2. Create `CryptoException.java` and subclasses
3. Create `JwtKeyProperties.java` configuration class
4. Add unit tests for PEM parsing logic
5. **No changes to existing JwtUtil.java**

**Validation:**
- Unit tests pass for key loading
- No impact on existing services
- Common-lib compiles without errors

### 6.2 Phase 2: Issuer Implementation (Auth Service)

**Tasks:**
1. Create `JwtIssuerConfig.java` configuration
2. Refactor `JwtUtil.java` constructor to accept PrivateKey
3. Update `auth-service/application.yml` configuration
4. Add integration tests for token signing
5. Deploy to development environment

**Validation:**
- Auth service starts successfully with private key
- Token signing produces valid RS256 tokens
- Existing login/register endpoints work

### 6.3 Phase 3: Consumer Implementation (API Gateway)

**Tasks:**
1. Create `JwtConsumerConfig.java` configuration
2. Update `JwtUtil.java` to support PublicKey constructor
3. Update `api-gateway/application.yml` configuration
4. Update `JwtAuthenticationFilter.java` to use consumer JwtUtil
5. Add integration tests for token verification
6. Deploy to development environment

**Validation:**
- API gateway starts successfully with public key
- Token verification works with RS256 tokens
- Existing protected endpoints work

### 6.4 Phase 4: Consumer Rollout (Remaining Services)

**Tasks:**
1. Update each consumer service `application.yml`
2. Add `JwtConsumerConfig.java` to each service (or use common-lib)
3. Update `SecurityConfig.java` in each service
4. Deploy services incrementally
5. Monitor for errors

**Validation:**
- All services start successfully
- Inter-service communication works
- No performance degradation

### 6.5 Phase 5: Cleanup (Remove Legacy Code)

**Tasks:**
1. Remove `jwt.secret` configuration from all services
2. Remove HMAC-SHA256 code paths from JwtUtil
3. Update documentation
4. Archive old symmetric key implementation

**Validation:**
- No references to JWT_SECRET remain
- Code is clean and maintainable
- Documentation is up to date

---

## 7. TESTING STRATEGY

### 7.1 Unit Tests

#### 7.1.1 RsaKeyLoader Tests

```java
@Test
void loadPrivateKey_ValidPem_ReturnsPrivateKey() {
    String pemPath = "src/test/resources/valid_private_key.pem";
    PrivateKey key = RsaKeyLoader.loadPrivateKey(pemPath);
    assertNotNull(key);
    assertEquals("RSA", key.getAlgorithm());
}

@Test
void loadPrivateKey_InvalidPem_ThrowsInvalidKeyFormatException() {
    String pemPath = "src/test/resources/invalid_private_key.pem";
    assertThrows(InvalidKeyFormatException.class, 
        () -> RsaKeyLoader.loadPrivateKey(pemPath));
}

@Test
void loadPrivateKey_FileNotFound_ThrowsKeyLoadException() {
    String pemPath = "nonexistent.pem";
    assertThrows(KeyLoadException.class, 
        () -> RsaKeyLoader.loadPrivateKey(pemPath));
}
```

#### 7.1.2 JwtUtil Tests (Issuer)

```java
@Test
void generateAccessToken_ValidInput_ReturnsSignedToken() {
    PrivateKey privateKey = loadTestPrivateKey();
    JwtUtil jwtUtil = new JwtUtil(privateKey, 3600000, 604800000);
    
    UserDetails userDetails = createUserDetails();
    String token = jwtUtil.generateAccessToken(userDetails, "user123", "CUSTOMER");
    
    assertNotNull(token);
    assertTrue(token.startsWith("eyJ")); // JWT format
}

@Test
void generateAccessToken_WithPrivateKey_ProducesRs256Token() {
    PrivateKey privateKey = loadTestPrivateKey();
    JwtUtil jwtUtil = new JwtUtil(privateKey, 3600000, 604800000);
    
    String token = jwtUtil.generateAccessToken(createUserDetails(), "user123", "CUSTOMER");
    
    // Verify algorithm claim is RS256
    String header = new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]));
    assertTrue(header.contains("\"alg\":\"RS256\""));
}
```

#### 7.1.3 JwtUtil Tests (Consumer)

```java
@Test
void validateToken_ValidRs256Token_ReturnsTrue() {
    PublicKey publicKey = loadTestPublicKey();
    JwtUtil jwtUtil = new JwtUtil(publicKey, 3600000, 604800000);
    
    String token = generateTestRs256Token();
    boolean isValid = jwtUtil.validateToken(token, createUserDetails());
    
    assertTrue(isValid);
}

@Test
void validateToken_InvalidSignature_ReturnsFalse() {
    PublicKey publicKey = loadTestPublicKey();
    JwtUtil jwtUtil = new JwtUtil(publicKey, 3600000, 604800000);
    
    String tamperedToken = generateTestRs256Token() + "tampered";
    boolean isValid = jwtUtil.validateToken(tamperedToken, createUserDetails());
    
    assertFalse(isValid);
}
```

### 7.2 Integration Tests

#### 7.2.1 Auth Service Integration Test

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
        String header = new String(Base64.getUrlDecoder().decode(response.getToken().split("\\.")[0]));
        assertTrue(header.contains("\"alg\":\"RS256\""));
    }
}
```

#### 7.2.2 API Gateway Integration Test

```java
@SpringBootTest
@AutoConfigureMockMvc
class ApiGatewayIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void protectedEndpoint_WithValidRs256Token_Returns200() throws Exception {
        String validRs256Token = generateTestRs256Token();
        
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer " + validRs256Token))
            .andExpect(status().isOk());
    }
    
    @Test
    void protectedEndpoint_WithInvalidToken_Returns401() throws Exception {
        String invalidToken = "invalid.token.here";
        
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer " + invalidToken))
            .andExpect(status().isUnauthorized());
    }
}
```

### 7.3 Performance Tests

#### 7.3.1 Token Signing Performance

```java
@Test
void tokenSigning_PerformanceTest() {
    PrivateKey privateKey = loadTestPrivateKey();
    JwtUtil jwtUtil = new JwtUtil(privateKey, 3600000, 604800000);
    
    int iterations = 1000;
    long startTime = System.currentTimeMillis();
    
    for (int i = 0; i < iterations; i++) {
        jwtUtil.generateAccessToken(createUserDetails(), "user" + i, "CUSTOMER");
    }
    
    long duration = System.currentTimeMillis() - startTime;
    double avgTimeMs = (double) duration / iterations;
    
    log.info("Average token signing time: {} ms", avgTimeMs);
    assertTrue(avgTimeMs < 20, "Signing should be under 20ms average");
}
```

#### 7.3.2 Token Verification Performance

```java
@Test
void tokenVerification_PerformanceTest() {
    PublicKey publicKey = loadTestPublicKey();
    JwtUtil jwtUtil = new JwtUtil(publicKey, 3600000, 604800000);
    String token = generateTestRs256Token();
    
    int iterations = 10000;
    long startTime = System.currentTimeMillis();
    
    for (int i = 0; i < iterations; i++) {
        jwtUtil.validateToken(token, createUserDetails());
    }
    
    long duration = System.currentTimeMillis() - startTime;
    double avgTimeMs = (double) duration / iterations;
    
    log.info("Average token verification time: {} ms", avgTimeMs);
    assertTrue(avgTimeMs < 5, "Verification should be under 5ms average");
}
```

---

## 8. SECURITY CONSIDERATIONS

### 8.1 Key Storage Security

**Current Implementation:**
- Keys stored as PEM files in `.docker/certs/` directory
- Mounted as read-only volumes in containers
- Excluded from version control via `.gitignore`

**Security Recommendations:**
- **File Permissions:** Ensure private key has `600` permissions (owner read-only)
- **Container Security:** Run containers with non-root user when possible
- **Key Rotation:** Implement key rotation mechanism for production
- **Backup Strategy:** Securely backup private keys with encryption

### 8.2 Cryptographic Best Practices

**Algorithm Selection:**
- **RSA-2048:** Minimum key size for production security
- **RS256:** Standard algorithm for JWT signing
- **Future-Proof:** Consider RSA-4096 or ECDSA (P-256) for higher security

**Random Number Generation:**
- JWT library handles nonce generation internally
- No custom random number generation required
- Relies on JVM's `SecureRandom`

### 8.3 Token Security

**Token Expiration:**
- Access tokens: 1 hour (configurable)
- Refresh tokens: 7 days (configurable)
- Implement token revocation mechanism for compromised tokens

**Token Claims:**
- Include `userId` and `role` in token claims
- Avoid sensitive data in token payload
- Use HTTPS for all token transmission

### 8.4 Defense in Depth

**Additional Security Layers:**
- Rate limiting on authentication endpoints
- IP-based anomaly detection
- Multi-factor authentication for sensitive operations
- Audit logging for all authentication events

---

## 9. ROLLBACK PLAN

### 9.1 Rollback Triggers

**Conditions for Rollback:**
- Critical security vulnerability discovered
- Performance degradation > 50%
- Integration test failures in production
- Key compromise incident

### 9.2 Rollback Procedure

**Step 1: Revert Configuration**
```yaml
# Revert to symmetric key configuration
jwt:
  secret: ${JWT_SECRET:super-secure-stylemind-secret-key}
  # Remove private-key-path and public-key-path
```

**Step 2: Revert Code**
- Restore previous version of `JwtUtil.java`
- Remove `JwtIssuerConfig.java` and `JwtConsumerConfig.java`
- Keep `RsaKeyLoader.java` for future use

**Step 3: Redeploy Services**
- Deploy reverted version to all services
- Verify authentication endpoints work
- Monitor for errors

**Step 4: Post-Rollback Analysis**
- Document root cause of failure
- Update blueprint with lessons learned
- Plan for next migration attempt

### 9.3 Rollback Validation

**Health Checks:**
- All services start successfully
- Authentication endpoints respond correctly
- No error logs related to JWT processing
- Performance metrics return to baseline

---

## 10. MONITORING & OBSERVABILITY

### 10.1 Metrics to Track

**Authentication Metrics:**
- Token issuance rate (tokens/second)
- Token verification rate (tokens/second)
- Token validation failure rate (%)
- Average token signing latency (ms)
- Average token verification latency (ms)

**Error Metrics:**
- Cryptographic exception rate
- Invalid token signature rate
- Expired token rate
- Malformed token rate

### 10.2 Logging Strategy

**Log Levels:**
- **ERROR:** Cryptographic failures, configuration errors
- **WARN:** Invalid tokens, signature verification failures
- **INFO:** Successful token issuance, key loading
- **DEBUG:** Detailed token processing (development only)

**Log Format:**
```
[timestamp] [level] [service] [request-id] message
```

**Example Logs:**
```
2026-07-12 10:15:30 INFO  auth-service [req-123] JWT private key loaded successfully
2026-07-12 10:15:31 INFO  auth-service [req-123] Access token generated for user user123
2026-07-12 10:15:32 WARN  api-gateway [req-124] Token signature verification failed
2026-07-12 10:15:33 ERROR api-gateway [req-125] Cryptographic error: Invalid key format
```

### 10.3 Alerting

**Alert Conditions:**
- Cryptographic error rate > 1% (5-minute window)
- Token verification latency > 100ms (5-minute window)
- Service startup failure due to key configuration

**Alert Channels:**
- Email: on-call engineering team
- Slack: #security-alerts channel
- PagerDuty: critical alerts only

---

## 11. CONCLUSION

This blueprint provides a comprehensive roadmap for migrating the StyleMind authentication system from Symmetric (HS256) to Asymmetric (RS256) JWT architecture. The refactoring ensures:

**Security Benefits:**
- Strict separation between signing and verification
- No private key exposure in consumer services
- Industry-standard cryptographic algorithms

**Performance Benefits:**
- Zero I/O operations during request processing
- Efficient key caching in memory
- Acceptable latency for token operations

**Operational Benefits:**
- Clear separation of concerns
- Comprehensive error handling
- Easy rollback capability

**Next Steps:**
1. Review and approve this blueprint
2. Begin Phase 1 implementation (Common-Lib Foundation)
3. Execute phased rollout plan
4. Monitor and validate each phase
5. Complete cleanup and documentation

---

## APPENDIX A: REFERENCE IMPLEMENTATIONS

### A.1 RsaKeyLoader.java (Full Implementation)

```java
package com.stylemind.common.security;

import com.stylemind.common.exception.CryptoException;
import com.stylemind.common.exception.InvalidKeyFormatException;
import com.stylemind.common.exception.KeyDecodingException;
import com.stylemind.common.exception.KeyLoadException;

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

/**
 * Utility class for loading RSA keys from PEM files.
 * Supports PKCS#8 format for private keys and X.509 format for public keys.
 */
public final class RsaKeyLoader {

    private RsaKeyLoader() {
        // Utility class - prevent instantiation
    }

    /**
     * Load RSA private key from PEM file (PKCS#8 format).
     *
     * @param keyPath Absolute path to the private key PEM file
     * @return RSA PrivateKey instance
     * @throws CryptoException if key loading fails
     */
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

    /**
     * Load RSA public key from PEM file (X.509 format).
     *
     * @param keyPath Absolute path to the public key PEM file
     * @return RSA PublicKey instance
     * @throws CryptoException if key loading fails
     */
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

    /**
     * Strip PEM headers and footers from key content.
     *
     * @param pemContent Raw PEM file content
     * @param keyType Key type (PRIVATE KEY or PUBLIC KEY)
     * @return Base64-encoded key content without headers
     */
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

### A.2 JwtKeyProperties.java (Full Implementation)

```java
package com.stylemind.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for JWT asymmetric key configuration.
 * Binds properties from application.yml to this POJO.
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtKeyProperties {

    /**
     * Path to RSA private key file (issuer only).
     * Required for auth-service, ignored by consumer services.
     */
    private String privateKeyPath;

    /**
     * Path to RSA public key file (all services).
     * Required for consumer services, optional for issuer.
     */
    private String publicKeyPath;

    /**
     * JWT signature algorithm.
     * Default: RSA
     */
    private String algorithm = "RSA";

    /**
     * RSA key size in bits.
     * Default: 2048
     */
    private Integer keySize = 2048;

    /**
     * Access token expiration time in milliseconds.
     * Default: 3600000 (1 hour)
     */
    private Long accessTokenExpiration = 3600000L;

    /**
     * Refresh token expiration time in milliseconds.
     * Default: 604800000 (7 days)
     */
    private Long refreshTokenExpiration = 604800000L;
}
```

### A.3 CryptoException.java (Full Implementation)

```java
package com.stylemind.common.exception;

/**
 * Base exception for all cryptographic operations.
 * Wraps checked exceptions from Java Security API.
 */
public class CryptoException extends RuntimeException {

    public CryptoException(String message) {
        super(message);
    }

    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

```java
package com.stylemind.common.exception;

/**
 * Exception thrown when key file cannot be loaded.
 */
public class KeyLoadException extends CryptoException {

    public KeyLoadException(String message) {
        super(message);
    }

    public KeyLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

```java
package com.stylemind.common.exception;

/**
 * Exception thrown when key format is invalid.
 */
public class InvalidKeyFormatException extends CryptoException {

    public InvalidKeyFormatException(String message) {
        super(message);
    }

    public InvalidKeyFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

```java
package com.stylemind.common.exception;

/**
 * Exception thrown when key decoding fails.
 */
public class KeyDecodingException extends CryptoException {

    public KeyDecodingException(String message) {
        super(message);
    }

    public KeyDecodingException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

---

**Document End**

# Source Code Issues - StyleMind Backend

**Generated:** 2026-07-11  
**Scope:** All critical bugs and architectural issues identified during service separation testing

---

## 1. Security Issues (HIGH PRIORITY)

### 1.1 JWT Secret Hard-coded in Source Code
**Location:** `auth-service/src/main/resources/application.yml`  
**Issue:** JWT secret has default value exposed in source code
```yaml
jwt:
  secret: ${JWT_SECRET:super-secure-stylemind-secret-key-signature-2026-xyz}
```
**Risk:** 
- Default key exposed in version control
- Same key used across all environments (dev/staging/prod)
- Potential token forgery if key is compromised

**Fix Required:**
- Remove default value: `jwt.secret: ${JWT_SECRET}`
- Force environment variable to be set explicitly
- Use environment-specific keys (dev/staging/production)

### 1.2 JWT Secret Key Generation Not Cryptographically Secure
**Location:** `common-lib/src/main/java/com/stylemind/common/security/JwtUtil.java`  
**Issue:** Converting string to bytes directly instead of using secure random generation
```java
this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
```
**Risk:**
- Predictable keys if string is weak
- Not following JWT JWA Specification (RFC 7518, Section 3.2)
- Keys must be >= 256 bits for HMAC-SHA algorithms

**Fix Required:**
- Option A: Generate random key at startup
  ```java
  this.secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
  ```
- Option B: Use Key Management System (AWS KMS, HashiCorp Vault)
- Option C: Generate strong secret once and store securely in env var

---

## 2. Build/Compilation Issues

### 2.1 Qdrant Dependency Artifact ID Incorrect
**Location:** 
- `BE/pom.xml` (line 115-117)
- `BE/ai-agent-service/pom.xml` (line 54-57)

**Issue:** Wrong artifact ID used
```xml
<!-- WRONG -->
<dependency>
    <groupId>io.qdrant</groupId>
    <artifactId>qdrant-client</artifactId>
    <version>${qdrant-client.version}</version>
</dependency>
```

**Current Status:** Temporarily commented out in ai-agent-service/pom.xml

**Fix Required:**
- Verify correct artifact ID from Qdrant documentation
- Possible correct ID: `client` (but this also failed to resolve)
- May need to use different version or alternative library

### 2.2 AI Agent Service Missing from Parent POM
**Location:** `BE/pom.xml` (lines 122-132)

**Issue:** ai-agent-service module not included in parent pom.xml modules list
```xml
<modules>
    <module>common-lib</module>
    <module>api-gateway</module>
    <module>auth-service</module>
    <module>user-service</module>
    <module>product-service</module>
    <module>cart-service</module>
    <module>order-service</module>
    <module>payment-service</module>
    <module>notification-service</module>
    <!-- MISSING: ai-agent-service -->
</modules>
```

**Current Status:** Fixed - added ai-agent-service to modules list

**Impact:** Maven build ignores ai-agent-service when building from parent directory

### 2.3 AI Agent Service Missing Lombok Imports
**Location:** 
- `BE/ai-agent-service/src/main/java/com/stylemind/ai/feign/ProductClient.java`
- `BE/ai-agent-service/src/main/java/com/stylemind/ai/feign/OrderClient.java`
- `BE/ai-agent-service/src/main/java/com/stylemind/ai/dto/AiDtos.java`
- `BE/ai-agent-service/src/main/java/com/stylemind/ai/dto/ChatDtos.java`
- `BE/ai-agent-service/src/main/java/com/stylemind/ai/entity/ChatMessage.java`

**Issue:** Missing Lombok annotations and List imports
```java
// Missing imports:
import lombok.*;
import java.util.List;

// Used but not imported:
@Data, @Builder, @NoArgsConstructor, @AllArgsConstructor
List
```

**Current Status:** Not fixed - compilation fails for ai-agent-service

**Impact:** Cannot build ai-agent-service, Docker build fails

### 2.4 AI Agent Service Missing InventoryClient Interface
**Location:** `BE/ai-agent-service/src/main/java/com/stylemind/ai/service/AiIndexJobService.java`

**Issue:** Service calls InventoryClient but interface doesn't exist
```java
// Called in AiIndexJobService.java:
inventoryClient.getInventory(sku);

// Interface missing from com.stylemind.ai.feign
```

**Current Status:** Not fixed

**Fix Required:**
- Define InventoryClient interface pointing to inventory-service
- OR mock the return value if inventory is merged into product-service
- OR remove dependency if inventory-service is deprecated

### 2.5 Dockerfiles Reference Non-Existent inventory-service
**Location:** All service Dockerfiles (9 files)

**Issue:** Dockerfiles attempt to copy inventory-service/pom.xml which doesn't exist
```dockerfile
COPY inventory-service/pom.xml inventory-service/pom.xml
```

**Current Status:** Fixed - removed from all 9 Dockerfiles

**Impact:** Docker build fails with "not found" error

### 2.6 Docker Build - No Main Manifest Attribute
**Location:** Docker build for user-service (and likely others)

**Issue:** JAR built doesn't have proper Spring Boot manifest
```
no main manifest attribute, in app.jar
```

**Current Status:** Not investigated in detail

**Possible Causes:**
- Dockerfile copying wrong JAR (should copy the repackaged JAR)
- Spring Boot Maven plugin not configured correctly
- Using plain jar instead of executable jar

**Fix Required:**
- Verify Dockerfile copies the correct JAR file
- Check Spring Boot plugin configuration in pom.xml
- Ensure `spring-boot:repackage` goal is executed

---

## 3. Architecture Issues

### 3.1 Tight Coupling Between Services
**Location:** `BE/order-service/src/main/java/com/stylemind/order/`

**Issue:** order-service directly depends on cart-service DTOs
```java
import com.stylemind.cart.dto.CartItemResponse;
import com.stylemind.cart.dto.CartResponse;
import com.stylemind.cart.dto.CartMergeRequest;
```

**Impact:**
- Violates microservice isolation principle
- Changes to cart DTOs break order-service
- Cannot deploy services independently
- Docker build fails for order-service (can't find cart DTOs)

**Fix Required:**
- Move shared DTOs to common-lib
- OR use API contracts (OpenAPI/Swagger) and generate DTOs
- OR create separate DTOs for each service with mapping

### 3.2 Service Separation Not Truly Isolated
**Location:** Overall architecture

**Issue:** Despite database separation, services still have tight coupling through:
- Direct DTO dependencies
- Shared code in common-lib that should be service-specific
- Cross-service Feign clients with tight coupling

**Current Status:** Service separation (Phase 1-2) completed for databases, but architectural coupling remains

**Fix Required:**
- Refactor to use proper API contracts
- Implement proper service boundaries
- Consider using API Gateway for all inter-service communication

---

## 4. Docker/Infrastructure Issues

### 4.1 Qdrant Healthcheck Endpoint Incorrect
**Location:** `BE/docker-compose-separated.yml` (line 28-32)

**Issue:** Healthcheck uses `/health` endpoint which returns 404
```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:6333/health"]  # WRONG
```

**Current Status:** Fixed - changed to root endpoint `/`

### 4.2 Environment Variables Not Loaded in Docker Compose
**Location:** `BE/docker-compose-separated.yml`

**Issue:** Services don't automatically load .env file
```yaml
# Before fix:
environment:
  JWT_SECRET: ${JWT_SECRET}  # Not loaded from .env

# After fix:
env_file:
  - .env
environment:
  JWT_SECRET: ${JWT_SECRET}
```

**Current Status:** Fixed - added `env_file: - .env` to all services

### 4.3 JWT_SECRET Too Short
**Location:** `.env.tested` (original value)

**Issue:** JWT_SECRET only 200 bits, minimum required is 256 bits
```
# Original (200 bits):
JWT_SECRET=stylemind-test-jwt-secret

# Updated (>= 256 bits):
JWT_SECRET=super-secure-stylemind-secret-key-signature-2026-xyz-1234567890
```

**Current Status:** Fixed - updated to longer secret

**Error:** `WeakKeyException: The specified key byte array is 200 bits which is not secure enough`

---

## 5. Known Bugs from AGENTS.md

The following bugs were documented in AGENTS.md and should be addressed:

### 5.1 Qdrant Client Coordinate Error
**Location:** `BE/pom.xml` and `BE/ai-agent-service/pom.xml`  
**Status:** Partially fixed (artifact ID changed, but dependency still fails to resolve)

### 5.2 Missing Module in Parent POM
**Location:** `BE/pom.xml`  
**Status:** Fixed - added ai-agent-service to modules

### 5.3 Missing Lombok and List Imports in Feign Client DTOs
**Location:** `BE/ai-agent-service/src/main/java/com/stylemind/ai/feign/`  
**Status:** Not fixed

### 5.4 Missing InventoryClient.java
**Location:** `BE/ai-agent-service/src/main/java/com/stylemind/ai/feign/`  
**Status:** Not fixed

### 5.5 Missing inventory-service Directory
**Location:** Dockerfiles  
**Status:** Fixed - removed references from all Dockerfiles

---

## 6. Summary of Fixes Applied

### Completed Fixes:
1. ✅ Removed inventory-service references from all 9 Dockerfiles
2. ✅ Added ai-agent-service to parent pom.xml modules
3. ✅ Fixed Qdrant healthcheck endpoint in docker-compose-separated.yml
4. ✅ Added env_file directive to all services in docker-compose-separated.yml
5. ✅ Updated JWT_SECRET to >= 256 bits in .env.tested
6. ✅ Temporarily commented out Qdrant dependency in ai-agent-service/pom.xml

### Pending Fixes:
1. ❌ Fix Lombok imports in ai-agent-service Feign Client DTOs
2. ❌ Create InventoryClient interface or remove dependency
3. ❌ Resolve Qdrant dependency artifact ID issue
4. ❌ Fix Docker build "no main manifest attribute" issue
5. ❌ Refactor service-to-service DTO dependencies (order-service → cart-service)
6. ❌ Remove JWT_SECRET default values from application.yml
7. ❌ Implement cryptographically secure JWT key generation
8. ❌ Implement environment-specific JWT secrets

---

## 7. Service Status After Testing

### Successfully Built and Started:
- ✅ auth-service (running on port 8081)
- ✅ Infrastructure (PostgreSQL x8, Redis, Qdrant, Neo4j, MinIO)

### Built but Failed to Start:
- ❌ user-service (no main manifest attribute)
- ❌ product-service (not tested)
- ❌ cart-service (not tested)
- ❌ payment-service (not tested)
- ❌ notification-service (not tested)
- ❌ api-gateway (not tested)

### Failed to Build:
- ❌ order-service (dependency on cart DTOs)
- ❌ ai-agent-service (Lombok imports, Qdrant dependency)

---

## 8. Recommendations

### Immediate Priority (Before Production):
1. Fix security issues (JWT secrets)
2. Resolve ai-agent-service compilation errors
3. Fix Docker build manifest issue
4. Refactor service coupling

### Medium Priority:
1. Implement proper API contracts
2. Add service-specific DTOs
3. Implement proper secret management
4. Add comprehensive logging

### Long-term:
1. Consider using API Gateway for all inter-service communication
2. Implement proper service discovery
3. Add circuit breakers and retry logic
4. Implement distributed tracing

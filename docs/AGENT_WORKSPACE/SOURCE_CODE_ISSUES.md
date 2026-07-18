# Source Code Issues - StyleMind Backend

## 2026-07-17 Gateway Registration OTP Public Path

### Registration OTP 401

**Root cause:** `JwtAuthenticationFilter` runs as a custom Gateway `GlobalFilter` before routing. Its exact public-path list did not include `/api/v1/auth/register/verify-otp`, so a registration request without a Bearer token was rejected with HTTP 401 before Auth Service could validate the OTP. `SecurityConfig.anyExchange().permitAll()` does not bypass this custom filter.

**Fix:** Added the exact pre-authentication paths `/api/v1/auth/register/verify-otp` and `/api/v1/auth/register/resend-otp` to `PUBLIC_EXACT_PATHS`. No wildcard Auth allowlist was introduced.

**Verification:** Filter regression tests cover verify-OTP, resend-OTP, register, and login without JWT, plus protected Auth requests without a token and with an invalid token. The protected cases remain rejected.

### Cart public-path concern (separate follow-up)

`JwtAuthenticationFilter.isPublicPath` still treats `/api/v1/cart` as public using a prefix match. This was intentionally not changed while fixing registration OTP. Confirm whether Cart Service independently authenticates user operations and whether Gateway should protect cart routes in a separate security task.

## 2026-07-17 Verified Fixes

### Cart duplicate insert

**Root cause:** `CartService` used `shoppingCartRepository.findById(userId)` for authenticated users. The database stores a cart ID separately from the user ID (`cart_customer` versus `usr_customer`), so the lookup missed the existing row and attempted a second insert against the intentional unique constraint on `shopping_carts.user_id`.

**Fix:** Authenticated flows now use `findByUserId`, retain the actual cart ID for item operations, and create a first cart with `ON CONFLICT (user_id) DO NOTHING` followed by a reload. The unique constraint remains unchanged.

### Registration OTP notification connection

**Root cause:** The Auth Feign client uses `notification.service.url`, whose local default is `http://localhost:8089`. The Docker `auth-service` container did not receive `NOTIFICATION_SERVICE_URL`, so it used localhost inside the Auth container instead of Docker DNS.

**Fix:** Compose passes `NOTIFICATION_SERVICE_URL` to Auth. Compose also passes one shared internal token value to Auth's `INTERNAL_TOKEN` binding and Notification's existing `X_INTERNAL_TOKEN` binding. Internal endpoint security remains enabled.

**Verification:** Cart and Auth focused test suites pass; Compose configuration resolves successfully. Auth, Notification, and Cart containers were rebuilt and restarted without volume deletion. Auth resolves `notification-service` over Docker DNS, and an authenticated empty internal request reaches Notification validation with HTTP 400.

**Generated:** 2026-07-12 (Updated after origin/dev merge)  
**Scope:** All critical bugs and architectural issues identified during service separation testing and origin/dev review

---

## 1. Security Issues (HIGH PRIORITY)

### 1.1 JWT Secret Hard-coded in Source Code
**Location:** All service `application.yml` files  
**Issue:** JWT secret has default value exposed in source code
```yaml
jwt:
  secret: ${JWT_SECRET:super-secure-stylemind-secret-key-signature-2026-xyz}
```
**Risk:** 
- Default key exposed in version control
- Same key used across all environments (dev/staging/prod)
- Potential token forgery if key is compromised

**Status:** **STILL EXISTS** - Reverted from asymmetric JWT to symmetric in origin/dev

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

**Status:** **STILL EXISTS** - Asymmetric JWT implementation was reverted in origin/dev

**Fix Required:**
- Option A: Generate random key at startup
  ```java
  this.secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
  ```
- Option B: Use Key Management System (AWS KMS, HashiCorp Vault)
- Option C: Generate strong secret once and store securely in env var

### 1.3 SePay Webhook API Key Not Set
**Location:** `BE/payment-service/src/main/resources/application.yml`  
**Issue:** SePay webhook API key has no default value (correct) but may cause runtime errors if not set
```yaml
webhook-api-key: ${SEPAY_WEBHOOK_API_KEY}
```
**Status:** **CORRECT** - No default value is intentional for security

**Note:** This is the correct approach - the key MUST be set via environment variable

---

## 2. Build/Compilation Issues

### 2.1 Qdrant/Neo4j Dependencies Removed (MVP Approach)
**Location:** `BE/ai-agent-service/pom.xml`

**Issue:** Qdrant and Neo4j dependencies were removed in origin/dev for MVP approach
**Previous Issue:** Wrong artifact ID used (qdrant-client vs client)

**Status:** **RESOLVED** - Dependencies removed as ai-agent-service is now MVP (rule-based recommendations only)

**Impact:** 
- AI agent service no longer has vector search capability
- No knowledge graph integration
- Simplified to rule-based recommendations only

**Future:** May need to re-add these dependencies when implementing full AI features

### 2.2 AI Agent Service Missing from Parent POM
**Location:** `BE/pom.xml` (lines 122-132)

**Issue:** ai-agent-service module not included in parent pom.xml modules list

**Status:** **FIXED** - Added ai-agent-service to modules list in VoKhai branch

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

**Status:** **STILL EXISTS** - Compilation fails for ai-agent-service

**Impact:** Cannot build ai-agent-service, Docker build fails

**Fix Required:** Add missing imports to all affected files

### 2.4 AI Agent Service Missing InventoryClient Interface
**Location:** `BE/ai-agent-service/src/main/java/com/stylemind/ai/service/AiIndexJobService.java`

**Issue:** Service calls InventoryClient but interface doesn't exist
```java
// Called in AiIndexJobService.java:
inventoryClient.getInventory(sku);

// Interface missing from com.stylemind.ai.feign
```

**Status:** **STILL EXISTS**

**Fix Required:**
- Define InventoryClient interface pointing to inventory-service
- OR mock the return value if inventory is merged into product-service
- OR remove dependency if inventory-service is deprecated

**Note:** Since Qdrant/Neo4j were removed for MVP, this indexing job may not be needed currently

### 2.5 Dockerfiles Reference Non-Existent inventory-service
**Location:** All service Dockerfiles (9 files)

**Issue:** Dockerfiles attempt to copy inventory-service/pom.xml which doesn't exist
```dockerfile
COPY inventory-service/pom.xml inventory-service/pom.xml
```

**Status:** **FIXED** - Removed from all 9 Dockerfiles in VoKhai branch

**Impact:** Docker build fails with "not found" error

### 2.6 Docker Build - No Main Manifest Attribute
**Location:** Docker build for user-service (and likely others)

**Issue:** JAR built doesn't have proper Spring Boot manifest
```
no main manifest attribute, in app.jar
```

**Status:** **NOT INVESTIGATED** - May still exist

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

**Status:** **STILL EXISTS**

**Impact:**
- Violates microservice isolation principle
- Changes to cart DTOs break order-service
- Cannot deploy services independently
- Docker build fails for order-service (can't find cart DTOs)

**Fix Required:**
- Move shared DTOs to common-lib
- OR use API contracts (OpenAPI/Swagger) and generate DTOs
- OR create separate DTOs for each service with mapping

### 3.2 Service Separation Not Implemented
**Location:** Overall architecture

**Issue:** Service separation (multiple PostgreSQL instances) was attempted in VoKhai branch but reverted in origin/dev

**Status:** **REVERTED** - origin/dev uses single PostgreSQL instance approach

**Current State:**
- Single PostgreSQL instance (port 5432) hosting 9 databases
- All services connect to localhost:5432
- No physical isolation between service databases

**Decision:** origin/dev team chose to keep single instance for simplicity

**Future Consideration:** May need to re-implement for production isolation requirements

### 3.3 Database Migration Inconsistency
**Location:** Service configurations

**Issue:** Only product-service uses Flyway for migrations, other services use init scripts

**Status:** **PARTIALLY ADDRESSED** - product-service has Flyway, others don't

**Impact:**
- Inconsistent migration approach across services
- Harder to track schema changes
- Potential for drift between environments

**Fix Required:**
- Add Flyway to all services for consistency
- OR standardize on init scripts for all services

---

## 4. Docker/Infrastructure Issues

### 4.1 Qdrant Healthcheck Endpoint Incorrect
**Location:** `BE/docker-compose-separated.yml` (line 28-32)

**Issue:** Healthcheck uses `/health` endpoint which returns 404
```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:6333/health"]  # WRONG
```

**Status:** **FIXED** - Changed to root endpoint `/` in VoKhai branch

**Note:** docker-compose-separated.yml was removed in origin/dev, so this is no longer relevant

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

**Status:** **FIXED** - Added `env_file: - .env` to all services in VoKhai branch

**Note:** docker-compose-separated.yml was removed in origin/dev, so this is no longer relevant

### 4.3 JWT_SECRET Too Short
**Location:** `.env.tested` (original value)

**Issue:** JWT_SECRET only 200 bits, minimum required is 256 bits
```
# Original (200 bits; redacted):
JWT_SECRET=<redacted-example>

# Updated (>= 256 bits):
JWT_SECRET=<redacted-example>
```

**Status:** **FIXED** - Updated to longer secret in VoKhai branch

**Error:** `WeakKeyException: The specified key byte array is 200 bits which is not secure enough`

### 4.4 docker-compose-separated.yml Removed
**Location:** `BE/docker-compose-separated.yml`

**Issue:** Service separation configuration file was removed in origin/dev

**Status:** **INTENTIONAL** - origin/dev team reverted to single PostgreSQL approach

**Impact:** Service separation (multiple PostgreSQL instances) is no longer available

---

## 5. Known Bugs from AGENTS.md

The following bugs were documented in AGENTS.md and should be addressed:

### 5.1 Qdrant Client Coordinate Error
**Location:** `BE/pom.xml` and `BE/ai-agent-service/pom.xml`  
**Status:** **RESOLVED** - Dependencies removed in origin/dev (MVP approach)

### 5.2 Missing Module in Parent POM
**Location:** `BE/pom.xml`  
**Status:** **FIXED** - Added ai-agent-service to modules in VoKhai branch

### 5.3 Missing Lombok and List Imports in Feign Client DTOs
**Location:** `BE/ai-agent-service/src/main/java/com/stylemind/ai/feign/`  
**Status:** **STILL EXISTS** - Not fixed

### 5.4 Missing InventoryClient.java
**Location:** `BE/ai-agent-service/src/main/java/com/stylemind/ai/feign/`  
**Status:** **STILL EXISTS** - Not fixed

### 5.5 Missing inventory-service Directory
**Location:** Dockerfiles  
**Status:** **FIXED** - Removed references from all Dockerfiles in VoKhai branch

---

## 6. Summary of Fixes Applied

### Completed Fixes (VoKhai branch):
1. ✅ Removed inventory-service references from all 9 Dockerfiles
2. ✅ Added ai-agent-service to parent pom.xml modules
3. ✅ Fixed Qdrant healthcheck endpoint in docker-compose-separated.yml
4. ✅ Added env_file directive to all services in docker-compose-separated.yml
5. ✅ Updated JWT_SECRET to >= 256 bits
6. ✅ Temporarily commented out Qdrant dependency in ai-agent-service/pom.xml

### Changes from origin/dev:
1. ✅ Removed Qdrant and Neo4j dependencies from ai-agent-service (MVP approach)
2. ✅ Reverted database URLs to single PostgreSQL instance (localhost:5432)
3. ✅ Reverted JWT configuration from asymmetric to symmetric
4. ✅ Added Feign client timeouts to cart-service, order-service, ai-agent-service
5. ✅ Added SePay/VietQR integration config to payment-service
6. ✅ Added Flyway migration support to product-service
7. ✅ Removed JWT asymmetric key configuration from api-gateway, auth-service, user-service
8. ✅ Removed docker-compose-separated.yml (service separation reverted)

### Pending Fixes:
1. ❌ Fix Lombok imports in ai-agent-service Feign Client DTOs
2. ❌ Create InventoryClient interface or remove dependency
3. ❌ Fix Docker build "no main manifest attribute" issue
4. ❌ Refactor service coupling (order-service → cart-service DTOs)
5. ❌ Remove JWT_SECRET default values from application.yml
6. ❌ Implement cryptographically secure JWT key generation
7. ❌ Implement environment-specific JWT secrets

---

## 7. Service Status After Testing

### Successfully Built Locally (excluding ai-agent-service):
- ✅ auth-service
- ✅ user-service
- ✅ product-service
- ✅ cart-service
- ✅ order-service
- ✅ payment-service
- ✅ notification-service
- ✅ api-gateway
- ✅ common-lib

### Failed to Build:
- ❌ ai-agent-service (Lombok imports missing, InventoryClient missing)

### Docker Build Status:
- ✅ product-service (built successfully)
- ✅ cart-service (built successfully)
- ✅ payment-service (built successfully)
- ✅ notification-service (built successfully)
- ✅ api-gateway (built successfully)
- ❌ user-service (no main manifest attribute - not tested after merge)
- ❌ order-service (dependency on cart DTOs - not tested after merge)
- ❌ ai-agent-service (compilation errors)

### Current Infrastructure (origin/dev):
- ✅ Single PostgreSQL instance (port 5432) with 9 databases
- ✅ Redis (port 6379)
- ✅ Qdrant (port 6333)
- ✅ Neo4j (port 7474/7687)
- ✅ MinIO (port 9000/9001)

---

## 8. Recommendations

### Immediate Priority (Before Production):
1. Fix security issues (JWT secrets - remove defaults, use environment-specific keys)
2. Resolve ai-agent-service compilation errors (Lombok imports, InventoryClient)
3. Fix Docker build manifest issue (verify JAR copying in Dockerfiles)
4. Refactor service coupling (order-service → cart-service DTOs)

### Medium Priority:
1. Evaluate if service separation (multiple PostgreSQL) should be re-implemented for production
2. Add Flyway migration to other services (currently only product-service)
3. Implement proper API contracts to reduce service coupling
4. Add comprehensive logging and monitoring

### Long-term:
1. Consider implementing asymmetric JWT for better security
2. Implement service discovery or remove Eureka references
3. Add circuit breakers and retry logic for inter-service communication
4. Implement distributed tracing
5. Containerize frontend
6. Add missing security features (refresh token endpoint, token blacklist)

### Note on Service Separation:
The service separation approach (multiple PostgreSQL instances) was implemented in VoKhai branch but reverted in origin/dev. The team chose to keep a single PostgreSQL instance for simplicity. This decision should be revisited before production deployment to ensure proper isolation between services.

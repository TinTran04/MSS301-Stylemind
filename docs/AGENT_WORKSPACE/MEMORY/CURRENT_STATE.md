# Current Project State

## 2026-07-17 Gateway registration OTP boundary

- Registration verification and resend are pre-authentication endpoints and are now allowlisted by exact path in `JwtAuthenticationFilter`.
- Protected Auth endpoints remain subject to the custom Gateway JWT filter; the broad `anyExchange().permitAll()` rule does not replace that filter.
- The Cart `/api/v1/cart` prefix bypass remains a separate `NEEDS VERIFICATION` security finding and was not changed in this task.

### Registration OTP status

- Registration OTP email delivery is operational through the Auth to Notification path.
- `POST /api/v1/auth/register/verify-otp` must pass through API Gateway without a Bearer token because the user has not completed registration and does not yet have an access token.
- Auth Service remains responsible for OTP validation and only creates or activates the account after successful verification, according to the current registration flow.
- An invalid, expired, or replaced OTP may correctly return HTTP 400 with `REGISTER_OTP_INVALID`.
- HTTP 401 with `AUTH_TOKEN_INVALID` indicates Gateway authentication failure, not an OTP mismatch.
- The Gateway fix is source- and test-verified, and a live invalid-OTP probe returned Auth's HTTP 400 response through Gateway. A fresh successful account-creation flow was not executed during this verification pass.

## 2026-07-17 Runtime Bug Verification

Two independent runtime issues were traced and fixed without resetting Docker volumes:

- **Cart ownership:** authenticated cart operations now resolve a cart with `findByUserId(userId)`, then use the returned cart ID for items, updates, merges, reads, and clearing. This preserves the one-cart-per-user constraint and correctly handles the seeded `cart_customer` / `usr_customer` state.
- **Concurrent first cart:** first-cart creation uses a PostgreSQL `ON CONFLICT (user_id) DO NOTHING` insert and reloads the winning row, so concurrent requests cannot create a second cart.
- **Auth to Notification URL:** Docker now passes `NOTIFICATION_SERVICE_URL=http://notification-service:8089` through Compose. IDE/local runs retain the `http://localhost:8089` default.
- **Internal notification authentication:** Auth receives `INTERNAL_TOKEN` from the documented `X_INTERNAL_TOKEN` value, while Notification receives the same value through its existing `X_INTERNAL_TOKEN` placeholder. The `/internal/v1/notifications/email` endpoint remains protected by `X-Internal-Token`.

Focused verification completed:

- `mvn -pl cart-service test`: 27 tests passed.
- `mvn -pl auth-service test`: 46 tests passed.
- `docker compose --env-file .env config`: passed.

No cart data was deleted, no unique constraint was removed, and no Docker volume reset was run.

**Last Updated:** 2026-07-13  
**Agent:** Cascade  
**Purpose:** Snapshot of current project state for agent reference

---

## Project Information

- **Project Name:** StyleMind
- **Type:** Fashion e-commerce platform with AI Stylist
- **Architecture:** Microservices with Spring Boot backend, React frontend
- **Location:** `c:\Users\KHAI\Documents\semester 8\MSS301-Code\MSS301-Stylemind`

---

## Current Architecture Status

### Infrastructure (12 containers)
- ✅ PostgreSQL: 9 separate instances (ports 5433-5440)
  - postgres-auth: 5433 (auth_db)
  - postgres-user: 5434 (user_db)
  - postgres-product: 5435 (product_db)
  - postgres-cart: 5436 (cart_db)
  - postgres-order: 5437 (order_db)
  - postgres-payment: 5438 (payment_db)
  - postgres-ai: 5439 (ai_db)
  - postgres-notification: 5440 (notification_db)
- ✅ Redis: Port 6379 (Caching & Session)
- ✅ Qdrant: Port 6333/6334 (Vector DB)
- ✅ Neo4j: Port 7474/7687 (Graph DB)
- ✅ MinIO: Port 9000/9001 (Object Storage)

### Microservices (9 containers)
- ✅ api-gateway: Port 3000 (RSA consumer mode)
- ✅ auth-service: Port 8081 (RSA issuer mode)
- ✅ user-service: Port 8082 (RSA consumer mode)
- ✅ product-service: Port 8083 (RSA consumer mode)
- ✅ ai-agent-service: Port 8085 (RSA consumer mode)
- ✅ cart-service: Port 8086 (RSA consumer mode)
- ✅ order-service: Port 8087 (RSA consumer mode)
- ✅ payment-service: Port 8088 (RSA consumer mode)
- ✅ notification-service: Port 8089 (RSA consumer mode)

### Docker Compose Configuration
- **File:** Single `BE/docker-compose.yml` (refactored from separated files)
- **Profiles:** infra (infrastructure), app (microservices), all (all services)
- **JWT Config:** Hardcoded environment variables (no YAML anchors)
- **Debug Ports:** 5005-5014 mapped for JDWP debugging

---

## Recent Changes - Docker Compose Refactoring (2026-07-13)

### Docker Compose Cleanup
- **Deleted:** `docker-compose-separated.yml`, `docker-compose.override.yml`, `docker-compose.full.yml`
- **Created:** Single unified `BE/docker-compose.yml` with profiles
- **Profiles:** infra (infrastructure), app (microservices), all (all services)
- **JWT Config:** Removed YAML anchors, hardcoded environment variables (fail-fast pattern)
- **Environment Format:** Changed from list to map format for all services
- **Debug Ports:** Removed JAVA_OPTS environment variables (JDWP ports still mapped)
- **.gitignore:** Updated to block junk compose files and scripts

### Git Repository Cleanup
- **Untracked:** `BE/docker-compose.full.yml` from Git index
- **Updated:** `.gitignore` with patterns for junk files
- **Committed:** "refactor source" with 37 files changed
- **Pushed:** Changes to remote repository (VoKhai branch)

### Documentation Updates
- **Created:** `PROJECT_SNAPSHOT_FOR_REVIEW.md` (comprehensive project snapshot)
- **Created:** `FULLSYSTEM_START_GUIDE.md` (Vietnamese startup guide)
- **Created:** `DOCKER_DEBUG_GUIDE.md` (debugging guide with JDWP)
- **Updated:** `CURRENT_STATE.md` (this file)

---

## Recent Changes - Asymmetric JWT Implementation (2026-07-12)

### Backend Changes
- **common-lib:** Created CryptoException hierarchy (CryptoException, KeyLoadException, InvalidKeyFormatException, KeyDecodingException)
- **common-lib:** Created RsaKeyLoader utility class for RSA key loading from PEM files
- **common-lib:** Created JwtKeyProperties configuration class with @ConfigurationProperties
- **common-lib:** Refactored JwtUtil with immutable JwtParser/JwtBuilder fields
- **common-lib:** Added RSA constructors (issuer mode with PrivateKey, consumer mode with PublicKey)
- **common-lib:** Removed HMAC code paths (secretKey field, HMAC constructors, resolveSecret method)
- **common-lib:** Created JwtAutoConfiguration for unified bean creation
- **common-lib:** Updated JwtUtilTest for RSA key generation tests
- **All services:** Updated JWT configuration from symmetric to asymmetric
- **docker-compose.full.yml:** Replaced JWT_SECRET with JWT_PRIVATE_KEY_PATH and JWT_PUBLIC_KEY_PATH
- **.env.example:** Updated to use RSA key paths (/app/certs/private_key.pem, /app/certs/public_key.pem)
- **payment-service:** Fixed SePay property placeholder issue with default values
- **PROJECT_SPEC.md:** Updated JWT documentation for RSA implementation
- **README.md:** Updated environment variable documentation

### Infrastructure Changes
- **RSA Keys:** Generated RSA-2048 key pair in .docker/certs/
  - Private key: .docker/certs/private_key.pem (auth-service only)
  - Public key: .docker/certs/public_key.pem (all services)
- **Services:** All services successfully deployed and running with asymmetric JWT

---

## Authentication Status (Updated 2026-07-12)

### JWT Implementation
- **Algorithm:** Asymmetric RSA-2048 (RS256)
- **Key Management:** 
  - Private key for auth-service (token signing)
  - Public key for all consumer services (token verification)
- **Key Paths:** 
  - Private: JWT_PRIVATE_KEY_PATH=/app/certs/private_key.pem
  - Public: JWT_PUBLIC_KEY_PATH=/app/certs/public_key.pem
- **Expiration:** Access 1h, Refresh 7d
- **Performance:** 
  - Token signing: < 20ms average
  - Token verification: < 5ms average
  - Zero I/O operations during runtime (pre-compiled JwtParser/JwtBuilder)

### Security Features
- ✅ BCrypt password hashing (strength 12)
- ✅ RBAC with `@EnableMethodSecurity`
- ✅ Internal auth filter for service-to-service
- ✅ CORS enabled (all origins)
- ✅ Asymmetric JWT (RSA-2048) - Private key isolated to auth-service
- ✅ Immutable JwtParser/JwtBuilder for zero-I/O runtime performance

### Missing Security Features
- ❌ Refresh token endpoint
- ❌ Token blacklist/revocation
- ❌ Password reset/forgot password
- ❌ Email verification
- ❌ OAuth2 integration
- ❌ MFA
- ❌ Admin/Staff roles
- ❌ Permission-based authorization

---

## Known Issues

### Critical Bugs (Must Fix Before Compile)
1. ✅ Qdrant client artifact ID error in pom.xml - **FIXED**
2. ✅ Missing ai-agent-service module in parent POM - **FIXED**
3. ✅ Missing Lombok imports in Feign DTOs - **FIXED**
4. ✅ Missing InventoryClient.java interface - **FIXED**
5. ✅ Missing inventory-service directory (referenced in Dockerfiles) - **FIXED**

### Architecture Issues
1. ✅ Single PostgreSQL instance - **RESOLVED: Now using 9 separate PostgreSQL instances**
2. ⚠️ No service discovery (Eureka referenced but not implemented)
3. ✅ Symmetric JWT - **RESOLVED: Now using asymmetric RSA-2048 JWT**
4. ⚠️ Payment service simulation only (60-70% complete)
5. ⚠️ Frontend not containerized
6. ✅ Database migration tool - **ADDED: product-service uses Flyway**
7. ⚠️ Qdrant unhealthy (healthcheck failing but service running)

---

## Service Communication

### Communication Pattern
- **Protocol:** HTTP/REST via OpenFeign
- **Style:** Synchronous blocking calls
- **Discovery:** Hardcoded URLs with environment variable overrides

### Feign Clients
- order-service → cart-service, payment-service, product-service
- ai-agent-service → product-service, order-service

### URL Configuration
```java
@FeignClient(name = "payment-service", url = "${PAYMENT_SERVICE_URL:http://localhost:8088}")
```

---

## Authentication Status

### JWT Implementation
- **Algorithm:** Symmetric HMAC-SHA256
- **Secret:** Environment variable `JWT_SECRET`
- **Default:** `super-secure-stylemind-secret-key-signature-2026-xyz`
- **Expiration:** Access 1h, Refresh 7d

### Security Features
- ✅ BCrypt password hashing (strength 12)
- ✅ RBAC with `@EnableMethodSecurity`
- ✅ Internal auth filter for service-to-service
- ✅ CORS enabled (all origins)

### Missing Security Features
- ❌ Refresh token endpoint
- ❌ Token blacklist/revocation
- ❌ Password reset/forgot password
- ❌ Email verification
- ❌ OAuth2 integration
- ❌ MFA
- ❌ Admin/Staff roles
- ❌ Permission-based authorization

---

## Configuration Management

### Environment Variables
- **Backend:** `.env.sample`, `.env.tested` in BE directory
- **Frontend:** `.env.example` in FE directory
- **Actual `.env`:** Not committed to git (best practice)

### Configuration Pattern
- **Backend:** Default values in `application.yml` with `${VAR:default}` syntax
- **Frontend:** `.env` file required from template

---

## Docker Status

### Current Docker Compose
- **Total containers:** 30 (12 infrastructure + 9 microservices + 9 PostgreSQL)
- **Compose file:** Single `BE/docker-compose.yml` with profiles
- **Frontend:** Not containerized (run locally with npm)
- **Discovery service:** Referenced but not in docker-compose

### Port Mapping
- PostgreSQL Instances: 5433-5440 (8 separate instances)
  - postgres-auth: 5433 (auth_db)
  - postgres-user: 5434 (user_db)
  - postgres-product: 5435 (product_db)
  - postgres-cart: 5436 (cart_db)
  - postgres-order: 5437 (order_db)
  - postgres-payment: 5438 (payment_db)
  - postgres-ai: 5439 (ai_db)
  - postgres-notification: 5440 (notification_db)
- Redis: 6379
- Qdrant: 6333, 6334
- Neo4j: 7474, 7687
- MinIO: 9000, 9001
- API Gateway: 3000 (debug: 5005)
- Auth Service: 8081 (debug: 5006)
- User Service: 8082 (debug: 5007)
- Product Service: 8083 (debug: 5008)
- AI Service: 8085 (debug: 5010)
- Cart Service: 8086 (debug: 5011)
- Order Service: 8087 (debug: 5012)
- Payment Service: 8088 (debug: 5013)
- Notification Service: 8089 (debug: 5014)

### Health Status
- All PostgreSQL instances: Healthy
- Redis: Healthy
- Neo4j: Healthy
- MinIO: Healthy
- Qdrant: Unhealthy (but running)
- All microservices: Up and running

---

## Planned Changes

### Service Separation
- **Status:** IMPLEMENTED (2026-07-13)
- **Implementation:** 9 separate PostgreSQL instances (ports 5433-5440)
- **Compose file:** Single `BE/docker-compose.yml` with profiles (infra, app, all)
- **Decision:** Database-per-service pattern physically isolated

---

## Documentation Status

### Agent Workspace
- ✅ `ARCHITECTURE_ANALYSIS.md` - Comprehensive architecture analysis
- ✅ `SERVICE_SEPARATION_PLAN.md` - Detailed separation plan
- ✅ `IMPLEMENTATION_LOG.md` - Agent work tracking
- ✅ `MEMORY/CURRENT_STATE.md` - This file
- ✅ `MEMORY/DECISIONS.md` - Architecture decisions

### Original Documentation
- ✅ `AGENTS.md` - Agent blueprint and developer rules
- ✅ `docs/ACTIVITY_DIAGRAMS.md`
- ✅ `docs/API_CONTRACT.md`
- ✅ `docs/DATA_MODEL_DOCUMENTATION`
- ✅ `docs/DEPLOYMENT_GUIDE.md`
- ✅ `docs/DOCKER_DB_SCHEMA_TESTING.md`
- ✅ `docs/MICROSERVICE_ARCHITECTURE.md`
- ✅ `docs/MIGRATION_ROADMAP.md`
- ✅ `docs/PROJECT_ANALYSIS.md`

---

## Next Steps

### Immediate Priority
1. ✅ Fix Lombok imports in ai-agent-service Feign Client DTOs - **COMPLETED**
2. ✅ Create InventoryClient interface or remove dependency from AiIndexJobService - **COMPLETED**
3. ✅ Test all services with current single PostgreSQL configuration - **COMPLETED (now using 9 separate instances)**
4. ⚠️ Verify SePay/VietQR integration in payment-service
5. ⚠️ Fix Qdrant healthcheck (service running but healthcheck failing)

### Future Considerations
1. ✅ Evaluate if service separation (multiple PostgreSQL) should be re-implemented - **IMPLEMENTED**
2. ✅ Upgrade to asymmetric JWT (public/private keys) for better security - **IMPLEMENTED**
3. Add Flyway migration to other services (currently only product-service)
4. Implement service discovery or remove Eureka references
5. Integrate real payment gateway (SePay/VietQR is partially implemented)
6. Containerize frontend
7. Add missing security features (refresh token endpoint, token blacklist)

---

## Notes

- Project is in active development
- Architecture follows microservice best practices
- Database-per-service pattern is correct but not physically isolated (single PostgreSQL instance)
- Service communication is synchronous (no async/messaging)
- Database migration: product-service uses Flyway, others use init scripts
- Payment service has SePay/VietQR integration (partially implemented)
- AI agent service is MVP (rule-based recommendations, no vector/graph search)

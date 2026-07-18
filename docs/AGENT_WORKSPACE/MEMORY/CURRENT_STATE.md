# Current Project State

## 2026-07-19 Fix: Order/Auth → Notification internal-token 403 (ORDER_PAID email)

**Status: RESOLVED** for `order-service → notification-service`, runtime-verified with a direct
probe (403 → HTTP 200 after the fix, using the exact production header/token/endpoint).
`auth-service → notification-service` shares the identical root cause and fix but was not
independently re-probed — **IMPLEMENTED, RUNTIME VERIFICATION PENDING** for that specific pair.

- Root cause: `com.stylemind.common.security.InternalAuthFilter.doFilterInternal:33` rejected
  `order-service`'s internal call because `order-service`'s effective `internal.token` (`.env
  INTERNAL_TOKEN`, via a change made earlier the same day to fix order↔auth) no longer matched
  `notification-service`'s effective `internal.token` (`.env X_INTERNAL_TOKEN`, unchanged) — same
  bug class as the order-to-auth fix below, a different pair.
  Confirmed via: log correlation (request id `30f94034-5062-4eba-83bf-ec17d4536f81`, 3 retries all
  `[403]`, matched to 3 `BusinessException` traces in notification-service's log at the same
  timestamps), a SET/SET-but-MISMATCH token comparison (no values printed), and a direct probe
  (403 before, 200 after).
- Fix: `BE/docker-compose.yml` — `notification-service.environment.X_INTERNAL_TOKEN` changed from
  `${X_INTERNAL_TOKEN}` to `${INTERNAL_TOKEN}` (Compose-only value alias; no Java/production code,
  no security-filter, no endpoint-visibility change). `notification-service` was recreated
  (`--force-recreate`, no rebuild needed); `order-service` was not recreated (unchanged by this
  fix).
- Tests added: `common-lib`'s `InternalAuthFilterTest` (new, 4/4 pass — first coverage of that
  class); `order-service`'s `ServiceUrlConfigurationTest` gained a Compose-content test that failed
  before the fix and passed after, plus a Feign-configuration test ruling out "client doesn't apply
  the shared interceptor"; `order-service`'s `OrderServiceTest` gained a regression test proving a
  notification-client exception never reverts a PAID order.
- Full test suites run: `common-lib` 17 run / 15 pass (2 pre-existing, unrelated `JwtUtilTest`
  RSA-signature failures, not caused by or related to this fix); `order-service` 39 run / 35 pass
  (4 pre-existing, unrelated `ServiceUrlConfigurationTest` failures for `cartClient`/
  `paymentClient`/`productClient`/`applicationConfig`, confirmed unchanged at `HEAD` before this
  session touched anything); `notification-service` 8/8 pass clean.
- **Not done:** a live end-to-end SePay bank-transfer replay (a human scanning the VietQR code with
  a real bank app) was not performed — that requires real money movement outside what this agent
  can or should automate. The fix is verified via the exact production code path
  (`OrderService.notifyOrderBestEffort` → `NotificationClient` → the same
  `/internal/v1/notifications/email` endpoint) using the real configured token, just not via an
  actual SePay-triggered request.
- **Explicitly not touched (per this fix's scope):** the SePay/payment-to-order flow itself, and
  the still-open `payment-service → order-service`, `ai-agent-service → order-service`, and
  `order-service → product-service`/`cart-service`/`payment-service` internal-token mismatches
  documented below — those remain NEEDS VERIFICATION.

## 2026-07-19 Documentation sync — verified SePay flow, new internal-token risk

**Branch/repo state:** `VoKhai`, identical to `origin/VoKhai` at commit `9ec9b22f`. The working
tree also has additional **uncommitted** changes (`BE/docker-compose.yml`'s auth-service block,
`BE/order-service/src/main/resources/application.yml`, and a new
`ServiceUrlConfigurationTest.java` case) that complete the "Order-to-Auth internal authentication"
fix below. These uncommitted changes are already built into the currently running
`order-service`/`auth-service` containers (both recreated 2026-07-18 21:34, confirmed via
`docker compose ps` and container startup log timestamps), so the live system reflects them even
though `git status` still shows them as modified, not committed.

**VERIFIED (live evidence, this session):**
- API Gateway is published on host port 3000 (`BE/docker-compose.yml` `api-gateway.ports`).
- A public ngrok tunnel forwards SePay webhook calls to the Gateway: a live Gateway DEBUG log line
  shows route `sepay-webhook` (`Path=/api/v1/payments/webhook/sepay`) matching a request whose
  `Host` header was an `*.ngrok-free.dev` domain, forwarded to `http://payment-service:8088`.
- The webhook path is exactly `POST /api/v1/payments/webhook/sepay` (`SepayWebhookController`,
  Gateway `PUBLIC_EXACT_PATHS`, and Gateway route config all agree).
- On 2026-07-18 21:12:42, a real SePay webhook reached Payment Service and was authenticated
  (`payment-service` log: "Authenticated SePay webhook accepted for gatewayTxnId=68890960"). A
  direct, read-only query against `order_db` confirms order `3176d387b91b442f93767833b099c281`
  transitioned to `order_status = PAID` at `21:12:42.637`, ~0.5s after the webhook — proving the
  full SePay → Payment → Order PAID path worked end-to-end under the configuration running at that
  time (**before** the order-service/auth-service rebuild described below).
- Order status transitions only via `OrderStatusService.changeStatus`, gated by
  `OrderStatus.allowedTransitions()`. `PAYMENT_PENDING → PAID` happens only in
  `OrderService.updateOrderStatusFromPayment`, called from `InternalOrderController`. Nothing in
  that path also transitions to `PROCESSING` — `PAID → PROCESSING` requires a separate manual/admin
  status change. A notification failure never rolls back the order: `notifyOrderBestEffort` retries
  3 times, then only logs a warning (see the "Compensation guardrail" comment,
  `OrderService.java:493`).
- The Shop/Checkout frontend polls payment status on an interval (`payment.store.js`, `pollTimer` /
  `setInterval`) as a read-only fallback observer; it does not process the webhook itself.

**UPDATE (same day, see the entry at the top of this file):** the `notification-service` part of
this was confirmed broken (live 403s, request id `30f94034-5062-4eba-83bf-ec17d4536f81`) and then
fixed and runtime-verified. The remaining paragraph below is preserved for the parts that are
**still open**.

**INVESTIGATING (high priority, source-verified, NOT yet runtime-tested against the current
build):** The same-day fix that aligned `order-service` and `auth-service` on the canonical
`INTERNAL_TOKEN` value was not propagated to `product-service`, `cart-service`,
`payment-service`, or `ai-agent-service` (all still resolve `internal.token` from
`X_INTERNAL_TOKEN`, which Compose does not inject into their containers, so they silently fall
back to the hardcoded default). `.env`'s `INTERNAL_TOKEN` and `X_INTERNAL_TOKEN` values were
confirmed different by a read-only comparison that did not print either value. Full per-service
breakdown and the resulting call-pair risk (including `payment-service → order-service`, the exact
call that marks an order PAID — still unresolved) are in
[ENVIRONMENT_MATRIX.md](../ENVIRONMENT_MATRIX.md) and [KNOWN_ISSUES.md](../KNOWN_ISSUES.md). No
SePay webhook replay has been observed in logs since the 2026-07-18 21:34 order/auth rebuild, so
`payment-service → order-service` is not yet proven broken in the current build — it is a
source-verified regression risk, not a confirmed one, and was deliberately not touched by this
session's notification fix.

**CONFIRMED security findings (live evidence, out of scope to fix in a documentation task):**
- API Gateway's `org.springframework.cloud.gateway: DEBUG` logging level is currently printing full
  `Authorization` header values (the SePay static API key and live customer JWTs) plus
  `X-User-Id`/`X-User-Roles`/`X-User-Email` from inbound requests, observed directly in current
  container logs.
- `BE/PAYMENT_REDIRECT_ISSUE.md` (outside `docs/AGENT_WORKSPACE`, not modified by this task) is
  committed and pushed to `origin/VoKhai` (commit `4bbf1a7c`) and contains the plaintext SePay
  webhook API key in two places. This needs rotation and redaction by the user; it was not edited
  here because it is outside this task's allowed scope
  (`docs/AGENT_WORKSPACE/**` only).

## 2026-07-19 Order-to-Auth internal authentication

- The `ORDER_PAID` notification path was failing before notification delivery because Order Service received a different internal-token value from Auth Service. Compose injected `${INTERNAL_TOKEN}` into Order Service but `${X_INTERNAL_TOKEN}` into Auth Service, while the repository's shared filter/interceptor uses the `X-Internal-Token` header and the `internal.token` property.
- Compose and Order Service configuration now use the canonical `INTERNAL_TOKEN` source for both services. The internal endpoint remains protected; no `/internal/**` route was made public.
- After rebuilding only `order-service` and `auth-service`, both containers reported the token as set and matching without printing its value. A read-only `GET /internal/v1/users/{userId}/email` probe from Order Service to Auth Service returned HTTP 200, and resolved Compose validation passed.
- Payment and order `PAID` behavior was not changed or manually mutated. A real SePay webhook/payment replay was not run in this verification pass, so notification delivery under an actual `ORDER_PAID` event remains runtime-pending.

## 2026-07-19 Payment callback URL propagation

- Payment Service now consumes the explicit `ORDER_SERVICE_URL` environment variable for its Order Service Feign client.
- Docker Compose injects `ORDER_SERVICE_URL` under the `payment-service` environment block; the value resolves to `http://order-service:8087`.
- The payment container was recreated without dependencies, database changes, or volume changes. Runtime verification reported the expected environment value, Docker DNS resolved `order-service`, and `GET http://localhost:8088/actuator/health` returned HTTP 200.
- This verifies configuration, service startup, and Docker connectivity. A controlled SePay webhook replay was not run, so the end-to-end Payment `PAID` and Order `PAID` transition remains runtime-pending.

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
- **Internal notification authentication:** Auth and Order use the canonical `INTERNAL_TOKEN` environment variable, while Notification retains its existing compatible binding. Internal endpoints remain protected by `X-Internal-Token`; values must be checked only for presence or equality, never printed.

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

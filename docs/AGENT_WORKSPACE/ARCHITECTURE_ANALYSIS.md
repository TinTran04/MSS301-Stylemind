# StyleMind Architecture Analysis

**Last Updated:** 2026-07-11  
**Agent:** Cascade  
**Purpose:** Comprehensive analysis of current architecture for agent reference

## 2026-07-19 Verified SePay payment flow and internal-auth boundary

Verified components only (see MEMORY/CURRENT_STATE.md for the evidence behind each step):

```text
SePay (internet)
  -> public HTTPS tunnel (ngrok in local dev)
  -> API Gateway, published host port 3000, route id `sepay-webhook`
     (Path=/api/v1/payments/webhook/sepay, public at the Gateway JWT filter -
      no user JWT exists for this call)
  -> Payment Service :8088 (SepayWebhookController)
     - authenticity verified via the webhook's own Authorization/API-key header,
       independent of the internal-service-token mechanism below
  -> Order Service :8087, POST /internal/v1/orders/{orderId}/payment-status
     (guarded by X-Internal-Token; unreachable through the Gateway - see the
      api-gateway `internal-block` route for /internal/v1/**)
     - OrderService.updateOrderStatusFromPayment: PAYMENT_PENDING -> PAID (or FAILED)
     - best-effort cart clear
     - Order Service -> Auth Service (X-Internal-Token) to look up the user's email
     - Order Service -> Notification Service (X-Internal-Token) for the ORDER_PAID email
```

A real webhook was observed completing this whole path on 2026-07-18 (order moved
PAYMENT_PENDING -> PAID within ~0.5s of the authenticated webhook), but that
observation predates a same-day internal-token configuration change described below and in
KNOWN_ISSUES.md, so it does not confirm the path still works end-to-end under the current build.

**Update (same day):** the `Order Service -> Notification Service` hop was confirmed broken (live
403s) and then fixed and runtime-verified via a direct probe (403 -> HTTP 200). The
`Payment Service -> Order Service` hop (the call that marks the order PAID) was deliberately not
touched and remains a source-verified, not-yet-runtime-confirmed risk - see KNOWN_ISSUES.md.

**Payment/order status update and notification are separate responsibilities.** Marking the order
PAID happens first and is durable regardless of what happens next; `notifyOrderBestEffort` retries
the notification a few times and only logs a warning on failure - a notification failure must never
roll back an already-paid order (`OrderService.java`, "Compensation guardrail" comment). `PAID` is
an allowed source state for a manual/admin transition to `PROCESSING`
(`OrderStatus.PAID -> {CONFIRMED, PROCESSING, CANCELLED}`), but nothing in the webhook path performs
that transition automatically.

**Internal-auth boundary.** Every `/internal/v1/**` endpoint across every service is guarded by the
same shared `common-lib` mechanism: `FeignClientConfig` (sender) and `InternalAuthFilter`
(receiver) both bind to one Spring property, `internal.token`, sent/checked as the
`X-Internal-Token` header. Each service's own `application.yml` independently decides which
environment variable that property resolves from, and Compose independently decides which
environment variables are injected into each container - the two are not currently kept in sync
for every service. See ENVIRONMENT_MATRIX.md for the current, verified per-service binding and the
resulting call-pair risk, and KNOWN_ISSUES.md for the full write-up.

**Docker vs. host networking (verified, matches ENVIRONMENT_MATRIX.md).** A browser or a host-side
`curl` may use a published port such as `http://localhost:3000`. A container must reach another
container using that container's Compose service name and internal port (e.g.
`http://order-service:8087`); `localhost` inside a container refers to that container itself, not
another service. `application-local.yml` files retain `localhost` defaults only for non-Docker
(IntelliJ/local-process) runs.

## 2026-07-17 Current registration OTP request flow

The registration verification path is a staged request flow:

```text
Frontend
  -> API Gateway
  -> JwtAuthenticationFilter
  -> auth-service route
  -> Auth Service OTP controller
  -> OTP validation
  -> account creation or activation
```

The Gateway route and the Gateway authentication filter are separate stages. A request can match the `auth-service` route (`/api/v1/auth/**` to `http://auth-service:8081`) and still be rejected by a custom `GlobalFilter` before `NettyRoutingFilter` forwards it.

`SecurityConfig` currently uses `.anyExchange().permitAll()`, but that rule does not automatically bypass `JwtAuthenticationFilter`. Pre-authentication endpoints must therefore be represented in the custom Gateway exact public-path list as well as in Auth Service security rules.

The verified public registration paths are:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/register/verify-otp`
- `POST /api/v1/auth/register/resend-otp`

Gateway does not validate OTP values. Auth Service performs business-level OTP validation. A missing or invalid JWT on verify-OTP should not produce Gateway 401; an invalid or expired OTP may produce Auth Service HTTP 400 with `REGISTER_OTP_INVALID`.

---

## 1. System Overview

StyleMind is a fashion e-commerce platform with AI Stylist, utilizing:
- **Frontend:** ReactJS (Port 5173)
- **Backend:** Spring Boot Microservices (8 services)
- **API Gateway:** Port 3000 (routes all external requests)

---

## 2. Current Infrastructure

### 2.1 Infrastructure Components (5 containers)
| Component | Port | Purpose |
|-----------|------|---------|
| PostgreSQL | 5432 | 9 databases in single instance |
| Redis | 6379 | Caching & Session storage |
| Qdrant | 6333 | Vector DB for product search |
| Neo4j | 7474/7687 | Graph DB for fashion taxonomy |
| MinIO | 9000/9001 | S3-compatible object storage |

### 2.2 Microservices (8 containers)
| Service | Port | Database | Purpose |
|---------|------|----------|---------|
| api-gateway | 3000 | None | Gateway, JWT Auth, rate limiting |
| auth-service | 8081 | auth_db | Authentication, RBAC |
| user-service | 8082 | user_db | Biometric profiles, addresses |
| product-service | 8083 | product_db | Categories, products, variants |
| ai-agent-service | 8085 | ai_db | Chatbot, AI bundles, analytics |
| cart-service | 8086 | cart_db | Shopping carts |
| order-service | 8087 | order_db | Orders management |
| payment-service | 8088 | payment_db | Payment checkout, refunds |
| notification-service | 8089 | notification_db | Notifications logging |

---

## 3. Database Architecture

### 3.1 Current State
- **Single PostgreSQL instance** hosting 9 databases
- **Database-per-service pattern** (correct microservice practice)
- **Init via shell script:** `00-create-databases.sh` creates databases
- **Schema init:** Individual SQL files in `init-scripts/`

### 3.2 Databases
1. `auth_db` - users table
2. `user_db` - customer_style_profiles, delivery_addresses
3. `product_db` - categories, products, product_variants, product_images
4. `cart_db` - shopping_carts, cart_items
5. `order_db` - orders, order_items
6. `payment_db` - transactions
7. `ai_db` - chat_sessions, chat_messages, ai_curated_bundles, ai_analytics_logs, ai_index_jobs
8. `notification_db` - notification_logs
9. `inventory_db` - (service not in codebase)

### 3.3 Database Storage
- **Docker volume:** `pgdata` mounted to `/var/lib/postgresql/data`
- **Persistent:** Data survives container restarts

---

## 4. Service Communication

### 4.1 Communication Pattern
- **Protocol:** HTTP/REST via OpenFeign
- **Style:** Synchronous blocking calls
- **Discovery:** Hardcoded URLs with environment variable overrides

### 4.2 Feign Clients
| Service | Calls |
|---------|-------|
| order-service | cart-service, payment-service, product-service |
| ai-agent-service | product-service, order-service |

### 4.3 URL Configuration
```java
@FeignClient(name = "payment-service", url = "${PAYMENT_SERVICE_URL:http://localhost:8088}")
```
- **Local dev:** Uses default `http://localhost:8088`
- **Docker:** Override via env var `PAYMENT_SERVICE_URL: http://payment-service:8088`

### 4.4 Service Discovery
- **Eureka referenced but not implemented** in docker-compose
- **No actual service discovery** - relies on hardcoded URLs

---

## 5. Authentication & Security

### 5.1 JWT Implementation
- **Algorithm:** Symmetric HMAC-SHA256 (not asymmetric)
- **Secret storage:** Environment variable `JWT_SECRET`
- **Default:** `super-secure-stylemind-secret-key-signature-2026-xyz`
- **Token expiration:** Access 1h, Refresh 7d
- **Stateless:** No token blacklist/revocation

### 5.2 Security Features
- BCrypt password hashing (strength 12)
- RBAC: Role `CUSTOMER` with `@EnableMethodSecurity`
- Internal auth filter for service-to-service communication
- CORS: All origins allowed (development mode)

### 5.3 Missing Security Features
- Refresh token endpoint
- Token blacklist/revocation
- Password reset/forgot password
- Email verification
- OAuth2 integration (Google, Facebook)
- MFA
- Admin/Staff roles
- Permission-based authorization

---

## 6. Payment Service

### 6.1 Implementation Status
- **Completion:** ~60-70% (MVP/simulation only)
- **Methods:** COD, online_simulated
- **Simulation:** `Math.random()` for 99% success rate

### 6.2 Missing Features
- Real payment gateway integration (Stripe, PayPal)
- Webhook handlers
- Retry logic
- Idempotency keys
- Partial refunds
- Dispute resolution
- Fraud detection

---

## 7. Configuration Management

### 7.1 Environment Variables
- **Backend:** `.env.sample`, `.env.tested` in BE directory
- **Frontend:** `.env.example` in FE directory
- **Actual `.env` files:** Not committed to git (best practice)

### 7.2 Configuration Pattern
- **Backend:** Default values in `application.yml` with `${VAR:default}` syntax
- **Frontend:** `.env` file required from template

---

## 8. Docker Configuration

### 8.1 Current Docker Compose
- **Total containers:** 13 (5 infrastructure + 8 microservices)
- **Frontend:** Commented out (not containerized)
- **Discovery service:** Referenced in config but not in docker-compose

### 8.2 Service-to-Database Mapping
- **Current:** All services → Single PostgreSQL instance
- **Issue:** Not truly isolated database per service

---

## 9. Known Bugs (Must Fix Before Compile)

### 9.1 Qdrant Client Artifact ID
- **Location:** BE/pom.xml (line 115), BE/ai-agent-service/pom.xml (line 55)
- **Issue:** Wrong artifact ID `qdrant-client`
- **Fix:** Change to `client` (group ID: `io.qdrant`, version: `1.5.0+`)

### 9.2 Missing Module in Parent POM
- **Location:** BE/pom.xml under `<modules>`
- **Issue:** `ai-agent-service` not included
- **Fix:** Add `<module>ai-agent-service</module>`

### 9.3 Missing Lombok Imports in Feign DTOs
- **Location:** ProductClient.java, OrderClient.java in ai-agent-service
- **Issue:** Missing Lombok annotations and List imports
- **Fix:** Add `import lombok.*;` and `import java.util.List;`

### 9.4 Missing InventoryClient.java
- **Location:** AiIndexJobService.java calls InventoryClient
- **Issue:** Interface doesn't exist
- **Fix:** Create InventoryClient interface or mock return value

### 9.5 Missing inventory-service Directory
- **Location:** Dockerfiles reference inventory-service
- **Issue:** Directory doesn't exist
- **Fix:** Remove COPY lines or create skeleton directory

---

## 10. Async/Reactive Patterns

### 10.1 Reactive Usage
- **API Gateway:** Spring WebFlux with `Mono<Void>` in filters
- **Microservices:** Synchronous blocking (no async)

### 10.2 No Async Patterns
- No `@Async` annotations
- No reactive programming in business services
- No message queues (RabbitMQ, Kafka)

---

## 11. Database Migration

### 11.1 Current Approach
- **Tool:** Shell scripts + SQL files
- **No Flyway/Liquibase**
- **No version control** for schema changes
- **No rollback mechanism**

### 11.2 Limitations
- Difficult to manage schema updates
- No migration history tracking
- Not suitable for production with frequent changes

---

## 12. Summary

### 12.1 Strengths
- Clear microservice boundaries
- Database-per-service pattern
- JWT-based authentication
- Comprehensive infrastructure (PostgreSQL, Redis, Qdrant, Neo4j, MinIO)
- Docker containerization

### 12.2 Weaknesses
- Single PostgreSQL instance (not truly isolated)
- No service discovery (Eureka referenced but not implemented)
- Symmetric JWT (less secure than asymmetric)
- Payment service simulation only
- Missing many security features
- No database migration tool
- Frontend not containerized
- Hardcoded service URLs

### 12.3 Next Steps
1. Separate databases into individual PostgreSQL instances per service
2. Implement service discovery or remove Eureka references
3. Upgrade to asymmetric JWT (public/private keys)
4. Integrate real payment gateway
5. Add Flyway/Liquibase for database migrations
6. Containerize frontend
7. Add missing security features

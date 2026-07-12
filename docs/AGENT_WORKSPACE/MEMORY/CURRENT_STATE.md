# Current Project State

**Last Updated:** 2026-07-12  
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

### Infrastructure (5 containers)
- ✅ PostgreSQL: Port 5432 (Single instance, 9 databases)
- ✅ Redis: Port 6379 (Caching & Session)
- ✅ Qdrant: Port 6333 (Vector DB)
- ✅ Neo4j: Port 7474/7687 (Graph DB)
- ✅ MinIO: Port 9000/9001 (Object Storage)

### Microservices (8 containers)
- ✅ api-gateway: Port 3000
- ✅ auth-service: Port 8081 (auth_db)
- ✅ user-service: Port 8082 (user_db)
- ✅ product-service: Port 8083 (product_db)
- ✅ ai-agent-service: Port 8085 (ai_db)
- ✅ cart-service: Port 8086 (cart_db)
- ✅ order-service: Port 8087 (order_db)
- ✅ payment-service: Port 8088 (payment_db)
- ✅ notification-service: Port 8089 (notification_db)

### Database Configuration
- **Current:** Single PostgreSQL instance (port 5432) hosting 9 databases
- **Databases:** auth_db, user_db, product_db, cart_db, order_db, payment_db, ai_db, notification_db, inventory_db
- **Init Method:** SQL init scripts in BE/init-scripts/
- **Storage:** Docker volume (pgdata)
- **Migration:** product-service uses Flyway (others use init scripts)

**Note:** Service separation (multiple PostgreSQL instances) was attempted in branch VoKhai but origin/dev has reverted to single instance approach.

---

## Recent Changes from origin/dev (2026-07-12)

### Backend Changes
- **ai-agent-service:** Removed Qdrant and Neo4j dependencies (MVP approach - rule-based recommendations only)
- **All services:** Reverted database URLs to single PostgreSQL instance (localhost:5432)
- **All services:** Reverted JWT configuration from asymmetric to symmetric
- **cart-service, order-service, ai-agent-service:** Added Feign client timeouts (connect: 3s, read: 5s)
- **order-service:** Added auth-service and notification-service URLs, order payment timeout config
- **payment-service:** Added SePay/VietQR integration config (bank-id, account-no, webhook-api-key)
- **product-service:** Added Flyway migration support, default currency config
- **api-gateway, auth-service, user-service:** Removed JWT asymmetric key configuration

### Infrastructure Changes
- **Removed:** docker-compose-separated.yml (service separation approach reverted)
- **Kept:** Single PostgreSQL instance with 9 databases

### Documentation Changes
- **New docs structure:** Organized into api/, architecture/, business/, database/, delivery/, frontend/, overview/, product/, requirements/, services/, superpowers/
- **New ADRs:** ADR-001 for Cloudinary product images
- **New scripts:** full-up.bat, full-up.sh, windows scripts for deployment

### Frontend Changes
- **New features:** Admin product management, order tracking, notifications, password recovery
- **New pages:** User management, notification management, forgot/reset password pages
- **Refactoring:** API client improvements, mapper utilities, test files added

---

## Known Issues

### Critical Bugs (Must Fix Before Compile)
1. ❌ Qdrant client artifact ID error in pom.xml - **REMOVED in origin/dev (Qdrant/Neo4j dependencies removed from ai-agent-service)**
2. ✅ Missing ai-agent-service module in parent POM - **FIXED**
3. ❌ Missing Lombok imports in Feign DTOs - **STILL EXISTS**
4. ❌ Missing InventoryClient.java interface - **STILL EXISTS**
5. ✅ Missing inventory-service directory (referenced in Dockerfiles) - **FIXED**

### Architecture Issues
1. ✅ Single PostgreSQL instance (not truly isolated) - **REVERTED: origin/dev uses single instance approach**
2. ⚠️ No service discovery (Eureka referenced but not implemented)
3. ⚠️ Symmetric JWT (less secure than asymmetric) - **REVERTED: origin/dev uses symmetric JWT**
4. ⚠️ Payment service simulation only (60-70% complete)
5. ⚠️ Frontend not containerized
6. ✅ Database migration tool - **ADDED: product-service uses Flyway**

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
- **Total containers:** 13 (5 infrastructure + 8 microservices)
- **Frontend:** Commented out (not containerized)
- **Discovery service:** Referenced but not in docker-compose

### Port Mapping
- PostgreSQL (Old): 5432 (single instance)
- PostgreSQL (New): 5433-5440 (8 separate instances)
  - postgres-auth: 5433
  - postgres-user: 5434
  - postgres-product: 5435
  - postgres-cart: 5436
  - postgres-order: 5437
  - postgres-payment: 5438
  - postgres-ai: 5439
  - postgres-notification: 5440
- Redis: 6379
- Qdrant: 6333, 6334
- Neo4j: 7474, 7687
- MinIO: 9000, 9001
- API Gateway: 3000
- Auth Service: 8081
- User Service: 8082
- Product Service: 8083
- AI Service: 8085
- Cart Service: 8086
- Order Service: 8087
- Payment Service: 8088
- Notification Service: 8089

---

## Planned Changes

### Service Separation
- **Status:** NOT IMPLEMENTED (reverted in origin/dev)
- **Previous Attempt:** Branch VoKhai had docker-compose-separated.yml with 8 PostgreSQL instances
- **Current:** Single PostgreSQL instance with 9 databases
- **Decision:** origin/dev team chose to keep single instance for simplicity
- **Plan Location:** `SERVICE_SEPARATION_PLAN.md` (kept for reference)

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
1. Fix Lombok imports in ai-agent-service Feign Client DTOs
2. Create InventoryClient interface or remove dependency from AiIndexJobService
3. Test all services with current single PostgreSQL configuration
4. Verify SePay/VietQR integration in payment-service

### Future Considerations
1. Evaluate if service separation (multiple PostgreSQL) should be re-implemented
2. Upgrade to asymmetric JWT (public/private keys) for better security
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

# Current Project State

**Last Updated:** 2026-07-11  
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
- **Previous:** Single PostgreSQL instance (port 5432) hosting 9 databases
- **Current (In Progress):** 8 separate PostgreSQL instances (ports 5433-5440)
- **Databases:** auth_db, user_db, product_db, cart_db, order_db, payment_db, ai_db, notification_db, inventory_db
- **Init Method:** Individual SQL files per database (00-create-databases.sh removed)
- **Storage:** Docker volumes (pgdata-auth, pgdata-user, pgdata-product, pgdata-cart, pgdata-order, pgdata-payment, pgdata-ai, pgdata-notification)

---

## Known Issues

### Critical Bugs (Must Fix Before Compile)
1. ❌ Qdrant client artifact ID error in pom.xml
2. ❌ Missing ai-agent-service module in parent POM
3. ❌ Missing Lombok imports in Feign DTOs
4. ❌ Missing InventoryClient.java interface
5. ❌ Missing inventory-service directory (referenced in Dockerfiles)

### Architecture Issues
1. ✅ Single PostgreSQL instance (not truly isolated) - **FIXED: Created docker-compose-separated.yml with 8 separate instances**
2. ⚠️ No service discovery (Eureka referenced but not implemented)
3. ⚠️ Symmetric JWT (less secure than asymmetric)
4. ⚠️ Payment service simulation only (60-70% complete)
5. ⚠️ Frontend not containerized
6. ⚠️ No database migration tool (Flyway/Liquibase)

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

### Service Separation (In Progress)
- **Goal:** Separate each microservice into individual PostgreSQL instances
- **Status:** Phase 1 & 2 Completed, Phase 3 (Testing) Pending User Permission
- **Estimated Time:** 12 hours (5 hours completed)
- **Plan Location:** `SERVICE_SEPARATION_PLAN.md`
- **Progress:**
  - ✅ Phase 1: Preparation (backup, init scripts, application.yml updates)
  - ✅ Phase 2: Docker Compose Update (docker-compose-separated.yml created)
  - ⏳ Phase 3: Testing (awaiting user permission)
  - ⏳ Phase 4: Data Migration (if needed)
  - ⏳ Phase 5: Cleanup

### Target Architecture
- 8 PostgreSQL instances (ports 5433-5440)
- Each service with dedicated database
- Complete isolation per service
- Independent backup/restore capability
- **New File:** `BE/docker-compose-separated.yml`

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
1. **WAIT FOR USER PERMISSION** before proceeding with Phase 3 (Testing)
2. Phase 3: Test infrastructure startup with docker-compose-separated.yml
3. Phase 3: Verify database initialization
4. Phase 3: Test service connectivity
5. Fix critical bugs (Priority 1) if needed

### Future Considerations
1. Implement service discovery or remove Eureka references
2. Upgrade to asymmetric JWT (public/private keys)
3. Integrate real payment gateway
4. Add Flyway/Liquibase for database migrations
5. Containerize frontend
6. Add missing security features

---

## Notes

- Project is in active development
- Architecture follows microservice best practices
- Database-per-service pattern is correct but not physically isolated
- Service communication is synchronous (no async/messaging)
- No database migration tool currently in use
- Payment service needs real integration for production

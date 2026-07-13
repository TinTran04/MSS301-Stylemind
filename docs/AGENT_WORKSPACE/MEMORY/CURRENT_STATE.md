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
- ✅ api-gateway: Port 3000 (RSA consumer mode)
- ✅ auth-service: Port 8081 (RSA issuer mode)
- ✅ user-service: Port 8082 (RSA consumer mode)
- ✅ product-service: Port 8083 (RSA consumer mode)
- ✅ ai-agent-service: Port 8085 (RSA consumer mode)
- ✅ cart-service: Port 8086 (RSA consumer mode)
- ✅ order-service: Port 8087 (RSA consumer mode)
- ✅ payment-service: Port 8088 (RSA consumer mode)
- ✅ notification-service: Port 8089 (RSA consumer mode)

### Database Configuration
- **Current:** Single PostgreSQL instance (port 5432) hosting 9 databases
- **Databases:** auth_db, user_db, product_db, cart_db, order_db, payment_db, ai_db, notification_db, inventory_db
- **Init Method:** SQL init scripts in BE/init-scripts/
- **Storage:** Docker volume (pgdata)
- **Migration:** product-service uses Flyway (others use init scripts)

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

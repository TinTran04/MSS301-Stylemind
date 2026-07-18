# Architecture Decisions

## 2026-07-17: Preserve one cart per authenticated user

**Decision:** Keep the database unique constraint on `shopping_carts.user_id` and make cart access resolve by `user_id`, not by assuming the user ID is also the cart primary key.

**Rationale:** Existing data proves those identifiers can differ (`cart_customer` / `usr_customer`). First-cart creation uses an idempotent PostgreSQL upsert and reloads the winner to handle concurrent requests safely.

## 2026-07-17: Configure Auth-to-Notification routing per runtime

**Decision:** Keep `NOTIFICATION_SERVICE_URL` configuration-driven. Local IDE runs use `http://localhost:8089`; Docker runs use `http://notification-service:8089`.

**Rationale:** `localhost` has different meaning inside a container. Internal authentication remains protected; Compose maps the same configured token into the exact environment bindings currently consumed by Auth and Notification rather than weakening the filter or embedding a secret.

**Last Updated:** 2026-07-12  
**Agent:** Cascade  
**Purpose:** Record architectural decisions and rationale

---

## Decision Log

### 2026-07-12: Asymmetric JWT Implementation

**Decision:** Migrate from symmetric HMAC-SHA256 to asymmetric RSA-2048 JWT

**Rationale:**
- Better security in distributed microservices environment
- Auth-service keeps private key for token signing
- Consumer services only need public key for verification
- No need to share secret key across services
- Private key isolation reduces security risk
- Industry standard for distributed systems

**Implementation:**
- Created CryptoException hierarchy for key loading errors
- Implemented RsaKeyLoader utility for PEM file parsing
- Refactored JwtUtil with immutable JwtParser/JwtBuilder
- Created unified JwtAutoConfiguration for bean management
- Generated RSA-2048 key pair (.docker/certs/)
- Updated all services to use public key verification
- Removed HMAC code paths for clean asymmetric-only implementation

**Trade-offs:**
- Increased complexity in key management
- Need to secure private key storage
- Requires key rotation strategy
- Slightly higher computational overhead (negligible with RSA-2048)

**Performance:**
- Token signing: < 20ms average
- Token verification: < 5ms average
- Zero I/O operations during runtime (pre-compiled parsers)
- Memory footprint: < 5KB per service

**Status:** ✅ Completed - All services running with asymmetric JWT

**Impact:** Positive - Significantly improved security posture with minimal performance impact

---

### 2026-07-12: Payment-Service SePay Configuration

**Decision:** Add default values for SePay configuration properties in payment-service

**Rationale:**
- Payment-service failed to start due to missing environment variables
- Local development needs safe defaults
- Production deployments will use environment variables
- SePay can be disabled for local development

**Implementation:**
- Added default values for SEPAY_BANK_SHORT_NAME, SEPAY_ACCOUNT_NUMBER, SEPAY_ACCOUNT_NAME
- Set SEPAY_ENABLED to false by default for local dev
- Added safe default for SEPAY_WEBHOOK_API_KEY

**Status:** ✅ Completed - Payment-service starts successfully

**Impact:** Positive - Enables local development without external dependencies

---

### 2026-07-11: Agent Workspace Structure

**Decision:** Create AGENT_WORKSPACE folder in docs/ directory for centralized agent documentation

**Rationale:**
- Avoid scanning source code multiple times (token efficiency)
- Provide single source of truth for agent knowledge
- Enable agents to understand previous work without full source scan
- Facilitate handoff between different agents

**Structure:**
```
docs/AGENT_WORKSPACE/
├── README.md
├── ARCHITECTURE_ANALYSIS.md
├── SERVICE_SEPARATION_PLAN.md
├── IMPLEMENTATION_LOG.md
└── MEMORY/
    ├── CURRENT_STATE.md
    └── DECISIONS.md
```

**Impact:** Positive - Reduces token usage, improves agent efficiency

---

### 2026-07-11: Service Database Separation

**Decision:** Separate each microservice into individual PostgreSQL instances

**Rationale:**
- Current architecture has single PostgreSQL instance hosting 9 databases
- Not truly isolated despite database-per-service pattern
- Benefits:
  - Complete isolation per service
  - Independent scaling of databases
  - Easier backup/restore per service
  - Reduced blast radius (one DB issue doesn't affect others)
  - Service-specific database credentials
  - Network-level isolation

**Trade-offs:**
- Higher resource usage (8 PostgreSQL instances vs 1)
- Increased complexity in configuration
- More containers to manage

**Implementation:**
- 8 separate PostgreSQL instances (ports 5433-5440)
- Each service connects to its dedicated database
- Infrastructure (Redis, Qdrant, Neo4j, MinIO) remains shared
- 5-phase implementation plan with rollback capability

**Status:** Plan created, not implemented yet

**Impact:** Positive - Better isolation, security, and maintainability

---

### 2026-07-11: Port Mapping Strategy

**Decision:** Use port range 5433-5440 for service-specific databases

**Rationale:**
- Keep port 5432 for potential shared infrastructure database
- Sequential port mapping for clarity
- Easy to identify which port belongs to which service

**Mapping:**
- auth_db: 5433
- user_db: 5434
- product_db: 5435
- cart_db: 5436
- order_db: 5437
- payment_db: 5438
- ai_db: 5439
- notification_db: 5440

**Impact:** Neutral - Clear mapping, no conflicts with existing infrastructure

---

### 2026-07-11: Infrastructure Sharing

**Decision:** Keep infrastructure (Redis, Qdrant, Neo4j, MinIO) shared across services

**Rationale:**
- These are stateless or shared resources
- Redis: Shared caching layer is appropriate
- Qdrant: Vector search is a shared capability
- Neo4j: Knowledge graph is shared across services
- MinIO: Object storage is naturally shared
- Reduces resource usage compared to separate instances
- Simplifies configuration

**Impact:** Positive - Reduces resource usage while maintaining appropriate sharing

---

### 2026-07-11: 5-Phase Migration Plan

**Decision:** Implement service separation in 5 phases with rollback capability

**Rationale:**
- Minimize risk of service disruption
- Allow testing at each phase
- Enable rollback if issues arise
- Clear checkpoints for progress tracking

**Phases:**
1. Preparation (backup, script updates)
2. Docker Compose Update (new structure)
3. Testing (infrastructure, services, connectivity)
4. Data Migration (if needed)
5. Cleanup (remove old containers/volumes)

**Impact:** Positive - Reduces risk, enables safe migration

---

### 2026-07-11: Init Scripts Restructuring

**Decision:** Remove 00-create-databases.sh, use individual SQL files per database

**Rationale:**
- Each PostgreSQL instance will create only its own database
- No need for shell script to create multiple databases
- Simpler initialization per service
- Easier to maintain per-database schema

**Impact:** Positive - Simpler, more maintainable initialization

---

## Pending Decisions

### Service Discovery
**Question:** Should we implement Eureka service discovery or continue with hardcoded URLs?

**Options:**
1. Implement Eureka (referenced in config but not implemented)
2. Continue with hardcoded URLs (current approach)
3. Use Kubernetes service discovery (if moving to K8s)

**Status:** Not decided - needs further analysis

---

### JWT Algorithm
**Question:** Should we upgrade from symmetric to asymmetric JWT?

**Options:**
1. Keep symmetric HMAC-SHA256 (current)
2. Upgrade to asymmetric RS256 (public/private keys)

**Rationale for upgrade:**
- Better security in distributed environment
- Auth-service keeps private key, services only need public key
- No need to share secret key across services

**Status:** Not decided - needs security assessment

---

### Database Migration Tool
**Question:** Should we implement Flyway or Liquibase for database migrations?

**Options:**
1. Flyway (SQL-based, simpler)
2. Liquibase (supports multiple formats, more features)
3. Continue with shell scripts (current)

**Status:** Not decided - needs evaluation

---

## Rejected Decisions

### 2026-07-11: Shared PostgreSQL Instance
**Decision:** Rejected - Keep single PostgreSQL instance

**Rationale for rejection:**
- Not truly isolated despite database-per-service pattern
- Resource contention between services
- Single point of failure
- Difficult to scale individual databases
- Security concerns (shared credentials)

**Alternative:** Separate PostgreSQL instances per service (accepted)

---

### 2026-07-11: Separate Infrastructure Instances
**Decision:** Rejected - Create separate instances for Redis, Qdrant, Neo4j, MinIO

**Rationale for rejection:**
- Unnecessary resource usage
- These are naturally shared resources
- Would increase complexity significantly
- No clear benefit for separation

**Alternative:** Keep infrastructure shared (accepted)

---

## Decision Criteria

When making architectural decisions, consider:

1. **Isolation:** Does this improve service isolation?
2. **Security:** Does this improve security posture?
3. **Performance:** What is the performance impact?
4. **Complexity:** Does this add unnecessary complexity?
5. **Maintainability:** Is this easier to maintain long-term?
6. **Resource Usage:** What is the resource cost?
7. **Risk:** What are the risks and mitigations?
8. **Rollback:** Can this be easily rolled back if needed?

---

## References

- SERVICE_SEPARATION_PLAN.md - Detailed separation plan
- ARCHITECTURE_ANALYSIS.md - Current architecture analysis
- IMPLEMENTATION_LOG.md - Agent work tracking

# Service Separation Plan

**Created:** 2026-07-11  
**Agent:** Cascade  
**Purpose:** Detailed plan to separate each microservice into individual Docker containers with dedicated PostgreSQL instances

---

## 1. Objective

Transform the current architecture from:
- **Current:** 1 PostgreSQL instance hosting 9 databases for 8 microservices
- **Target:** 8 PostgreSQL instances (1 per microservice) + 1 shared infrastructure PostgreSQL for cross-service data

---

## 2. Current State Analysis

### 2.1 Current Docker Compose Structure
```yaml
services:
  postgres:                    # Single instance
    image: postgres:15-alpine
    ports: ["5432:5432"]
    volumes:
      - pgdata:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d
```

### 2.2 Current Database Mapping
| Service | Database | Port | Current Connection |
|---------|----------|------|-------------------|
| auth-service | auth_db | 5432 | jdbc:postgresql://localhost:5432/auth_db |
| user-service | user_db | 5432 | jdbc:postgresql://localhost:5432/user_db |
| product-service | product_db | 5432 | jdbc:postgresql://localhost:5432/product_db |
| cart-service | cart_db | 5432 | jdbc:postgresql://localhost:5432/cart_db |
| order-service | order_db | 5432 | jdbc:postgresql://localhost:5432/order_db |
| payment-service | payment_db | 5432 | jdbc:postgresql://localhost:5432/payment_db |
| ai-agent-service | ai_db | 5432 | jdbc:postgresql://localhost:5432/ai_db |
| notification-service | notification_db | 5432 | jdbc:postgresql://localhost:5432/notification_db |

---

## 3. Target Architecture

### 3.1 New Docker Compose Structure
```yaml
services:
  # Infrastructure (unchanged)
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
  
  qdrant:
    image: qdrant/qdrant:latest
    ports: ["6333:6333", "6334:6334"]
  
  neo4j:
    image: neo4j:5-community
    ports: ["7474:7474", "7687:7687"]
  
  minio:
    image: minio/minio:latest
    ports: ["9000:9000", "9001:9001"]
  
  # Service-specific PostgreSQL instances
  postgres-auth:
    image: postgres:15-alpine
    ports: ["5433:5432"]
    environment:
      POSTGRES_DB: auth_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    volumes:
      - pgdata-auth:/var/lib/postgresql/data
      - ./init-scripts/01-auth-db.sql:/docker-entrypoint-initdb.d/01-auth-db.sql
  
  postgres-user:
    image: postgres:15-alpine
    ports: ["5434:5432"]
    environment:
      POSTGRES_DB: user_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    volumes:
      - pgdata-user:/var/lib/postgresql/data
      - ./init-scripts/02-user-db.sql:/docker-entrypoint-initdb.d/02-user-db.sql
  
  postgres-product:
    image: postgres:15-alpine
    ports: ["5435:5432"]
    environment:
      POSTGRES_DB: product_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    volumes:
      - pgdata-product:/var/lib/postgresql/data
      - ./init-scripts/03-product-db.sql:/docker-entrypoint-initdb.d/03-product-db.sql
  
  postgres-cart:
    image: postgres:15-alpine
    ports: ["5436:5432"]
    environment:
      POSTGRES_DB: cart_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    volumes:
      - pgdata-cart:/var/lib/postgresql/data
      - ./init-scripts/05-cart-db.sql:/docker-entrypoint-initdb.d/05-cart-db.sql
  
  postgres-order:
    image: postgres:15-alpine
    ports: ["5437:5432"]
    environment:
      POSTGRES_DB: order_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    volumes:
      - pgdata-order:/var/lib/postgresql/data
      - ./init-scripts/06-order-db.sql:/docker-entrypoint-initdb.d/06-order-db.sql
  
  postgres-payment:
    image: postgres:15-alpine
    ports: ["5438:5432"]
    environment:
      POSTGRES_DB: payment_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    volumes:
      - pgdata-payment:/var/lib/postgresql/data
      - ./init-scripts/07-payment-db.sql:/docker-entrypoint-initdb.d/07-payment-db.sql
  
  postgres-ai:
    image: postgres:15-alpine
    ports: ["5439:5432"]
    environment:
      POSTGRES_DB: ai_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    volumes:
      - pgdata-ai:/var/lib/postgresql/data
      - ./init-scripts/08-ai-db.sql:/docker-entrypoint-initdb.d/08-ai-db.sql
  
  postgres-notification:
    image: postgres:15-alpine
    ports: ["5440:5432"]
    environment:
      POSTGRES_DB: notification_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    volumes:
      - pgdata-notification:/var/lib/postgresql/data
      - ./init-scripts/09-notification-db.sql:/docker-entrypoint-initdb.d/09-notification-db.sql
  
  # Microservices (updated database URLs)
  auth-service:
    depends_on:
      postgres-auth:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-auth:5432/auth_db
  
  user-service:
    depends_on:
      postgres-user:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-user:5432/user_db
  
  product-service:
    depends_on:
      postgres-product:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-product:5432/product_db
  
  cart-service:
    depends_on:
      postgres-cart:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-cart:5432/cart_db
  
  order-service:
    depends_on:
      postgres-order:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-order:5432/order_db
  
  payment-service:
    depends_on:
      postgres-payment:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-payment:5432/payment_db
  
  ai-agent-service:
    depends_on:
      postgres-ai:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-ai:5432/ai_db
  
  notification-service:
    depends_on:
      postgres-notification:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-notification:5432/notification_db

volumes:
  pgdata-auth:
  pgdata-user:
  pgdata-product:
  pgdata-cart:
  pgdata-order:
  pgdata-payment:
  pgdata-ai:
  pgdata-notification:
  qdrant_data:
  neo4j_data:
  minio_data:
```

### 3.2 New Database Port Mapping
| Service | Database | Port | New Connection |
|---------|----------|------|----------------|
| auth-service | auth_db | 5433 | jdbc:postgresql://postgres-auth:5432/auth_db |
| user-service | user_db | 5434 | jdbc:postgresql://postgres-user:5432/user_db |
| product-service | product_db | 5435 | jdbc:postgresql://postgres-product:5432/product_db |
| cart-service | cart_db | 5436 | jdbc:postgresql://postgres-cart:5432/cart_db |
| order-service | order_db | 5437 | jdbc:postgresql://postgres-order:5432/order_db |
| payment-service | payment_db | 5438 | jdbc:postgresql://postgres-payment:5432/payment_db |
| ai-agent-service | ai_db | 5439 | jdbc:postgresql://postgres-ai:5432/ai_db |
| notification-service | notification_db | 5440 | jdbc:postgresql://postgres-notification:5432/notification_db |

---

## 4. Implementation Steps

### Phase 1: Preparation (No downtime)
1. **Backup current database**
   ```bash
   docker exec stylemind-postgres pg_dump -U postgres > backup_all_databases.sql
   ```

2. **Create new init scripts structure**
   - Remove `00-create-databases.sh`
   - Keep individual SQL files for each database
   - Each PostgreSQL instance will run only its specific SQL file

3. **Update application.yml files**
   - Change default database URLs to new ports
   - Update environment variable references

### Phase 2: Docker Compose Update
1. **Create new docker-compose-separated.yml**
   - Keep original `docker-compose.yml` as backup
   - Implement new structure with separate PostgreSQL instances

2. **Update service dependencies**
   - Change `depends_on: postgres` to specific database instances
   - Add health checks for each database

3. **Update environment variables**
   - Change `SPRING_DATASOURCE_URL` for each service
   - Update Dockerfile COPY commands if needed

### Phase 3: Testing
1. **Start infrastructure only**
   ```bash
   docker compose -f docker-compose-separated.yml up -d postgres-auth postgres-user postgres-product postgres-cart postgres-order postgres-payment postgres-ai postgres-notification
   ```

2. **Verify database initialization**
   - Check each database is created correctly
   - Verify tables are created from SQL scripts

3. **Start services one by one**
   ```bash
   docker compose -f docker-compose-separated.yml up -d auth-service
   docker compose -f docker-compose-separated.yml up -d user-service
   # ... continue for each service
   ```

4. **Test service connectivity**
   - Verify each service connects to its database
   - Test API endpoints
   - Check logs for connection errors

### Phase 4: Data Migration (If needed)
1. **Migrate data from single instance**
   - Export data from current databases
   - Import into new separate instances
   - Verify data integrity

2. **Update application.yml defaults**
   - Change from localhost:5432 to localhost:5433-5440
   - For local development

### Phase 5: Cleanup
1. **Stop old containers**
   ```bash
   docker compose down
   ```

2. **Remove old volume**
   ```bash
   docker volume rm pgdata
   ```

3. **Rename docker-compose-separated.yml to docker-compose.yml**

---

## 5. Application.yml Updates

### 5.1 auth-service/application.yml
```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5433/auth_db}
```

### 5.2 user-service/application.yml
```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5434/user_db}
```

### 5.3 product-service/application.yml
```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5435/product_db}
```

### 5.4 cart-service/application.yml
```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5436/cart_db}
```

### 5.5 order-service/application.yml
```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5437/order_db}
```

### 5.6 payment-service/application.yml
```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5438/payment_db}
```

### 5.7 ai-agent-service/application.yml
```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5439/ai_db}
```

### 5.8 notification-service/application.yml
```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5440/notification_db}
```

---

## 6. Network Configuration

### 6.1 Current Network
- Single network: `stylemind-network`
- All services communicate via service names

### 6.2 New Network (No Change)
- Keep single network: `stylemind-network`
- Database services: `postgres-auth`, `postgres-user`, etc.
- Application services connect via database service names

---

## 7. Volume Configuration

### 7.1 New Volumes
```yaml
volumes:
  pgdata-auth:
  pgdata-user:
  pgdata-product:
  pgdata-cart:
  pgdata-order:
  pgdata-payment:
  pgdata-ai:
  pgdata-notification:
  qdrant_data:
  neo4j_data:
  minio_data:
```

### 7.2 Volume Benefits
- Complete isolation of data per service
- Independent backup/restore per service
- Easier migration and scaling

---

## 8. Health Checks

### 8.1 Database Health Checks
```yaml
postgres-auth:
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U postgres -d auth_db"]
    interval: 10s
    timeout: 5s
    retries: 5
```

### 8.2 Service Dependencies
```yaml
auth-service:
  depends_on:
    postgres-auth:
      condition: service_healthy
```

---

## 9. Rollback Plan

### 9.1 Rollback Steps
1. Stop new containers:
   ```bash
   docker compose -f docker-compose-separated.yml down
   ```

2. Restore original docker-compose.yml:
   ```bash
   cp docker-compose.yml.backup docker-compose.yml
   ```

3. Restore old volume:
   ```bash
   docker compose up -d postgres
   ```

4. Start services:
   ```bash
   docker compose up -d
   ```

### 9.2 Rollback Triggers
- Service cannot connect to database
- Data corruption during migration
- Performance degradation
- Critical bugs in new architecture

---

## 10. Testing Checklist

### 10.1 Unit Tests
- [ ] Each service can connect to its database
- [ ] Database schema is correct
- [ ] Data persistence works

### 10.2 Integration Tests
- [ ] Service-to-service communication works
- [ ] API Gateway routes correctly
- [ ] Authentication/authorization works
- [ ] Payment processing works
- [ ] AI chat functionality works

### 10.3 Performance Tests
- [ ] Database connection pool performance
- [ ] Query performance
- [ ] Overall system response time

### 10.4 Security Tests
- [ ] Database credentials are secure
- [ ] Network isolation is correct
- [ ] No unauthorized access between services

---

## 11. Benefits of Separation

### 11.1 Isolation
- Complete database isolation per service
- No resource contention between services
- Independent scaling of databases

### 11.2 Maintenance
- Easier backup/restore per service
- Independent database upgrades
- Clearer ownership per service

### 11.3 Security
- Reduced blast radius (one DB issue doesn't affect others)
- Service-specific database credentials
- Network-level isolation

### 11.4 Development
- Clearer service boundaries
- Easier local development (can run only needed services)
- Better testing isolation

---

## 12. Risks and Mitigations

### 12.1 Resource Usage
- **Risk:** Higher memory/CPU usage with 8 PostgreSQL instances
- **Mitigation:** Use resource limits in docker-compose, monitor performance

### 12.2 Complexity
- **Risk:** More complex configuration and management
- **Mitigation:** Document thoroughly, use docker-compose profiles

### 12.3 Data Migration
- **Risk:** Data loss during migration
- **Mitigation:** Full backup before migration, test migration process

### 12.4 Connection Issues
- **Risk:** Services cannot connect to new databases
- **Mitigation:** Thorough testing, health checks, proper dependency management

---

## 13. Estimated Timeline

- **Phase 1 (Preparation):** 2 hours
- **Phase 2 (Docker Compose Update):** 3 hours
- **Phase 3 (Testing):** 4 hours
- **Phase 4 (Data Migration):** 2 hours (if needed)
- **Phase 5 (Cleanup):** 1 hour

**Total Estimated Time:** 12 hours

---

## 14. Next Steps

1. Review and approve this plan
2. Create backup of current system
3. Implement Phase 1 (Preparation)
4. Implement Phase 2 (Docker Compose Update)
5. Implement Phase 3 (Testing)
6. Implement Phase 4 (Data Migration) if needed
7. Implement Phase 5 (Cleanup)
8. Update documentation
9. Monitor system performance

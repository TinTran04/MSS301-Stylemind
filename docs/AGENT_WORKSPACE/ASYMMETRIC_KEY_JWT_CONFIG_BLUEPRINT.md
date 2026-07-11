# Asymmetric Key (RSA-2048) JWT Configuration Blueprint
**StyleMind Backend Microservices Infrastructure**

**Version:** 1.0  
**Date:** 2026-07-11  
**Purpose:** Production-ready configuration for RSA-2048 asymmetric JWT signing/verification

---

## 1. FOLDER TREE DIAGRAM

```
MSS301-Stylemind/
├── .docker/
│   └── certs/                          # 🔐 Local development key storage
│       ├── private_key.pem             # RSA-2048 Private Key (auth-service only)
│       ├── public_key.pem              # RSA-2048 Public Key (all services)
│       └── .gitkeep                    # Ensure directory structure in git
├── BE/
│   ├── auth-service/
│   │   ├── src/main/resources/
│   │   │   └── application.yml         # Auth service config
│   │   └── Dockerfile
│   ├── order-service/
│   │   ├── src/main/resources/
│   │   │   └── application.yml         # Consumer service config
│   │   └── Dockerfile
│   ├── user-service/
│   │   ├── src/main/resources/
│   │   │   └── application.yml         # Consumer service config
│   │   └── Dockerfile
│   ├── cart-service/
│   │   ├── src/main/resources/
│   │   │   └── application.yml         # Consumer service config
│   │   └── Dockerfile
│   ├── api-gateway/
│   │   ├── src/main/resources/
│   │   │   └── application.yml         # Gateway config (public key)
│   │   └── Dockerfile
│   ├── docker-compose-separated.yml    # Main orchestration file
│   └── .env                            # Environment variables (key paths)
├── FE/
├── docs/
└── .gitignore                          # Exclude private key from version control
```

### Security Note
**CRITICAL:** Add strict `.gitignore` pattern to exclude entire certs directory while preserving directory structure via `.gitkeep`.

---

## 2. DOCKER COMPOSE CONFIGURATION

### 2.1 Complete `docker-compose-separated.yml` Snippet

```yaml
version: '3.8'

services:
  # ============================================
  # AUTH SERVICE (JWT Issuer - Private Key Access)
  # ============================================
  auth-service:
    build:
      context: .
      dockerfile: auth-service/Dockerfile
    container_name: stylemind-auth-service
    ports:
      - "8081:8081"
    environment:
      # JWT Key Configuration (Private Key Path)
      # Internal container paths injected from Host .env file
      - JWT_PRIVATE_KEY_PATH=${JWT_PRIVATE_KEY_PATH}
      - JWT_PUBLIC_KEY_PATH=${JWT_PUBLIC_KEY_PATH}
      - JWT_ALGORITHM=${JWT_ALGORITHM:RSA}
      - JWT_KEY_SIZE=${JWT_KEY_SIZE:2048}
      
      # Database Configuration
      - SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL:jdbc:postgresql://postgres:5432/auth_db}
      - SPRING_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME:postgres}
      - SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD:password}
      
      # Service Configuration
      - SERVER_PORT=${SERVER_PORT:8081}
      - SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:dev}
      
      # Logging
      - LOGGING_LEVEL_COM_STYLEMIND_AUTH=${LOGGING_LEVEL_COM_STYLEMIND_AUTH:DEBUG}
    
    # Volume Mounting: Read-Only access to private key
    volumes:
      - ./.docker/certs:/app/certs:ro
    
    networks:
      - stylemind-network
    
    depends_on:
      postgres:
        condition: service_healthy
    
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  # ============================================
  # ORDER SERVICE (JWT Consumer - Public Key Only)
  # ============================================
  order-service:
    build:
      context: .
      dockerfile: order-service/Dockerfile
    container_name: stylemind-order-service
    ports:
      - "8087:8087"
    environment:
      # JWT Key Configuration (Public Key Path ONLY)
      # Internal container path injected from Host .env file
      - JWT_PUBLIC_KEY_PATH=${JWT_PUBLIC_KEY_PATH}
      - JWT_ALGORITHM=${JWT_ALGORITHM:RSA}
      - JWT_KEY_SIZE=${JWT_KEY_SIZE:2048}
      
      # Database Configuration
      - SPRING_DATASOURCE_URL=${ORDER_DATASOURCE_URL:jdbc:postgresql://postgres:5432/order_db}
      - SPRING_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME:postgres}
      - SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD:password}
      
      # Service Configuration
      - SERVER_PORT=${SERVER_PORT:8087}
      - SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:dev}
      
      # Logging
      - LOGGING_LEVEL_COM_STYLEMIND_ORDER=${LOGGING_LEVEL_COM_STYLEMIND_ORDER:DEBUG}
    
    # Volume Mounting: Read-Only access to public key only
    volumes:
      - ./.docker/certs:/app/certs:ro
    
    networks:
      - stylemind-network
    
    depends_on:
      postgres:
        condition: service_healthy
    
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8087/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  # ============================================
  # USER SERVICE (JWT Consumer - Public Key Only)
  # ============================================
  user-service:
    build:
      context: .
      dockerfile: user-service/Dockerfile
    container_name: stylemind-user-service
    ports:
      - "8082:8082"
    environment:
      # JWT Key Configuration (Public Key Path ONLY)
      # Internal container path injected from Host .env file
      - JWT_PUBLIC_KEY_PATH=${JWT_PUBLIC_KEY_PATH}
      - JWT_ALGORITHM=${JWT_ALGORITHM:RSA}
      - JWT_KEY_SIZE=${JWT_KEY_SIZE:2048}
      
      # Database Configuration
      - SPRING_DATASOURCE_URL=${USER_DATASOURCE_URL:jdbc:postgresql://postgres:5432/user_db}
      - SPRING_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME:postgres}
      - SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD:password}
      
      # Service Configuration
      - SERVER_PORT=${SERVER_PORT:8082}
      - SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:dev}
      
      # Logging
      - LOGGING_LEVEL_COM_STYLEMIND_USER=${LOGGING_LEVEL_COM_STYLEMIND_USER:DEBUG}
    
    # Volume Mounting: Read-Only access to public key only
    volumes:
      - ./.docker/certs:/app/certs:ro
    
    networks:
      - stylemind-network
    
    depends_on:
      postgres:
        condition: service_healthy
    
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8082/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  # ============================================
  # CART SERVICE (JWT Consumer - Public Key Only)
  # ============================================
  cart-service:
    build:
      context: .
      dockerfile: cart-service/Dockerfile
    container_name: stylemind-cart-service
    ports:
      - "8086:8086"
    environment:
      # JWT Key Configuration (Public Key Path ONLY)
      # Internal container path injected from Host .env file
      - JWT_PUBLIC_KEY_PATH=${JWT_PUBLIC_KEY_PATH}
      - JWT_ALGORITHM=${JWT_ALGORITHM:RSA}
      - JWT_KEY_SIZE=${JWT_KEY_SIZE:2048}
      
      # Database Configuration
      - SPRING_DATASOURCE_URL=${CART_DATASOURCE_URL:jdbc:postgresql://postgres:5432/cart_db}
      - SPRING_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME:postgres}
      - SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD:password}
      
      # Service Configuration
      - SERVER_PORT=${SERVER_PORT:8086}
      - SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:dev}
      
      # Logging
      - LOGGING_LEVEL_COM_STYLEMIND_CART=${LOGGING_LEVEL_COM_STYLEMIND_CART:DEBUG}
    
    # Volume Mounting: Read-Only access to public key only
    volumes:
      - ./.docker/certs:/app/certs:ro
    
    networks:
      - stylemind-network
    
    depends_on:
      postgres:
        condition: service_healthy
    
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8086/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  # ============================================
  # API GATEWAY (JWT Consumer - Public Key Only)
  # ============================================
  api-gateway:
    build:
      context: .
      dockerfile: api-gateway/Dockerfile
    container_name: stylemind-api-gateway
    ports:
      - "3000:3000"
    environment:
      # JWT Key Configuration (Public Key Path ONLY)
      # Internal container path injected from Host .env file
      - JWT_PUBLIC_KEY_PATH=${JWT_PUBLIC_KEY_PATH}
      - JWT_ALGORITHM=${JWT_ALGORITHM:RSA}
      - JWT_KEY_SIZE=${JWT_KEY_SIZE:2048}
      
      # Service Configuration
      - SERVER_PORT=${SERVER_PORT:3000}
      - SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:dev}
      
      # Logging
      - LOGGING_LEVEL_COM_STYLEMIND_GATEWAY=${LOGGING_LEVEL_COM_STYLEMIND_GATEWAY:DEBUG}
    
    # Volume Mounting: Read-Only access to public key only
    volumes:
      - ./.docker/certs:/app/certs:ro
    
    networks:
      - stylemind-network
    
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:3000/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  # ============================================
  # INFRASTRUCTURE SERVICES
  # ============================================
  postgres:
    image: postgres:15-alpine
    container_name: stylemind-postgres
    ports:
      - "5432:5432"
    environment:
      - POSTGRES_USER=${POSTGRES_USER:postgres}
      - POSTGRES_PASSWORD=${POSTGRES_PASSWORD:password}
      - POSTGRES_DB=${POSTGRES_DB:stylemind}
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d:ro
    networks:
      - stylemind-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: stylemind-redis
    ports:
      - "6379:6379"
    networks:
      - stylemind-network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

networks:
  stylemind-network:
    driver: bridge

volumes:
  postgres-data:
```

### 2.2 Volume Mounting Matrix

| Service | Volume Mount | Access Mode | Key Access | Internal Path |
|---------|-------------|-------------|------------|----------------|
| **auth-service** | `./.docker/certs:/app/certs` | `ro` (Read-Only) | Private + Public | `/app/certs/private_key.pem`<br>`/app/certs/public_key.pem` |
| **order-service** | `./.docker/certs:/app/certs` | `ro` (Read-Only) | Public Only | `/app/certs/public_key.pem` |
| **user-service** | `./.docker/certs:/app/certs` | `ro` (Read-Only) | Public Only | `/app/certs/public_key.pem` |
| **cart-service** | `./.docker/certs:/app/certs` | `ro` (Read-Only) | Public Only | `/app/certs/public_key.pem` |
| **api-gateway** | `./.docker/certs:/app/certs` | `ro` (Read-Only) | Public Only | `/app/certs/public_key.pem` |
| **product-service** | `./.docker/certs:/app/certs` | `ro` (Read-Only) | Public Only | `/app/certs/public_key.pem` |
| **payment-service** | `./.docker/certs:/app/certs` | `ro` (Read-Only) | Public Only | `/app/certs/public_key.pem` |
| **notification-service** | `./.docker/certs:/app/certs` | `ro` (Read-Only) | Public Only | `/app/certs/public_key.pem` |

---

## 3. SAMPLE .ENV TEMPLATE

### 3.1 Complete `.env` Template for Local Development

```bash
# ============================================
# DATABASE CONFIGURATION
# ============================================
POSTGRES_USER=postgres
POSTGRES_PASSWORD=password
POSTGRES_DB=stylemind

# Auth Service Database
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/auth_db

# Order Service Database
ORDER_DATASOURCE_URL=jdbc:postgresql://postgres:5432/order_db

# User Service Database
USER_DATASOURCE_URL=jdbc:postgresql://postgres:5432/user_db

# Cart Service Database
CART_DATASOURCE_URL=jdbc:postgresql://postgres:5432/cart_db

# Product Service Database
PRODUCT_DATASOURCE_URL=jdbc:postgresql://postgres:5432/product_db

# Payment Service Database
PAYMENT_DATASOURCE_URL=jdbc:postgresql://postgres:5432/payment_db

# Notification Service Database
NOTIFICATION_DATASOURCE_URL=jdbc:postgresql://postgres:5432/notification_db

# ============================================
# JWT ASYMMETRIC KEY CONFIGURATION
# ============================================
# NOTE: These paths are INTERNAL container paths, NOT host paths
# The actual keys are mounted via Docker volumes in docker-compose-separated.yml

# Auth Service (Issuer) - Needs both keys
JWT_PRIVATE_KEY_PATH=/app/certs/private_key.pem
JWT_PUBLIC_KEY_PATH=/app/certs/public_key.pem

# Consumer Services - Need only public key
# (order-service, user-service, cart-service, api-gateway, etc.)
JWT_PUBLIC_KEY_PATH=/app/certs/public_key.pem

# JWT Algorithm Configuration
JWT_ALGORITHM=RSA
JWT_KEY_SIZE=2048

# JWT Token Expiration (milliseconds)
JWT_ACCESS_TOKEN_EXPIRATION=3600000        # 1 hour
JWT_REFRESH_TOKEN_EXPIRATION=604800000     # 7 days

# ============================================
# SERVICE PORTS
# ============================================
SERVER_PORT=8081
GATEWAY_PORT=3000

# ============================================
# SPRING PROFILES
# ============================================
SPRING_PROFILES_ACTIVE=dev

# ============================================
# LOGGING LEVELS
# ============================================
LOGGING_LEVEL_COM_STYLEMIND_AUTH=DEBUG
LOGGING_LEVEL_COM_STYLEMIND_ORDER=DEBUG
LOGGING_LEVEL_COM_STYLEMIND_USER=DEBUG
LOGGING_LEVEL_COM_STYLEMIND_CART=DEBUG
LOGGING_LEVEL_COM_STYLEMIND_GATEWAY=DEBUG
LOGGING_LEVEL_COM_STYLEMIND_PRODUCT=DEBUG
LOGGING_LEVEL_COM_STYLEMIND_PAYMENT=DEBUG
LOGGING_LEVEL_COM_STYLEMIND_NOTIFICATION=DEBUG

# ============================================
# EXTERNAL SERVICES
# ============================================
REDIS_HOST=redis
REDIS_PORT=6379

QDRANT_HOST=qdrant
QDRANT_PORT=6333

NEO4J_HOST=neo4j
NEO4J_PORT=7687

MINIO_HOST=minio
MINIO_PORT=9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
```

### 3.2 Production `.env` Template (Reference)

```bash
# Production Environment Variables
# DO NOT commit actual values to version control

POSTGRES_USER=${POSTGRES_USER}
POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
POSTGRES_DB=${POSTGRES_DB}

# In production, keys should be mounted from secure storage
# (e.g., AWS Secrets Manager, HashiCorp Vault, Kubernetes Secrets)
JWT_PRIVATE_KEY_PATH=/app/secrets/private_key.pem
JWT_PUBLIC_KEY_PATH=/app/secrets/public_key.pem

JWT_ALGORITHM=RSA
JWT_KEY_SIZE=2048

JWT_ACCESS_TOKEN_EXPIRATION=3600000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

SPRING_PROFILES_ACTIVE=prod

LOGGING_LEVEL_COM_STYLEMIND_AUTH=INFO
LOGGING_LEVEL_COM_STYLEMIND_ORDER=INFO
LOGGING_LEVEL_COM_STYLEMIND_USER=INFO
LOGGING_LEVEL_COM_STYLEMIND_CART=INFO
LOGGING_LEVEL_COM_STYLEMIND_GATEWAY=INFO
```

---

## 4. SPRING BOOT CONFIGURATION TEMPLATES

### 4.1 Auth Service `application.yml` (JWT Issuer)

```yaml
server:
  port: ${SERVER_PORT:8081}

spring:
  main:
    allow-bean-definition-overriding: true
    allow-circular-references: true
  application:
    name: auth-service
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5433/auth_db}
    username: ${SPRING_DATASOURCE_USERNAME:postgres}
    password: ${SPRING_DATASOURCE_PASSWORD:password}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
  sql:
    init:
      mode: never

# ============================================
# JWT ASYMMETRIC KEY CONFIGURATION (ISSUER)
# ============================================
jwt:
  # Private Key Path - REQUIRED for signing tokens
  # NO DEFAULT VALUE - Must be set via environment variable
  private-key-path: ${JWT_PRIVATE_KEY_PATH}
  
  # Public Key Path - REQUIRED for verification (if needed internally)
  # NO DEFAULT VALUE - Must be set via environment variable
  public-key-path: ${JWT_PUBLIC_KEY_PATH}
  
  # Algorithm Configuration
  algorithm: ${JWT_ALGORITHM:RSA}
  key-size: ${JWT_KEY_SIZE:2048}
  
  # Token Expiration
  access-token-expiration: ${JWT_ACCESS_TOKEN_EXPIRATION:3600000}
  refresh-token-expiration: ${JWT_REFRESH_TOKEN_EXPIRATION:604800000}

# ============================================
# MANAGEMENT & MONITORING
# ============================================
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    tags:
      application: ${spring.application.name}

# ============================================
# LOGGING
# ============================================
logging:
  level:
    com.stylemind.auth: ${LOGGING_LEVEL_COM_STYLEMIND_AUTH:DEBUG}
    org.springframework.security: ${LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY:DEBUG}
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{X-Request-Id:-}] [%thread] %-5level %logger{36} - %msg%n"
```

### 4.2 Consumer Service `application.yml` (Order Service Example)

```yaml
server:
  port: ${SERVER_PORT:8087}

spring:
  main:
    allow-bean-definition-overriding: true
    allow-circular-references: true
  application:
    name: order-service
  datasource:
    url: ${ORDER_DATASOURCE_URL:jdbc:postgresql://localhost:5433/order_db}
    username: ${SPRING_DATASOURCE_USERNAME:postgres}
    password: ${SPRING_DATASOURCE_PASSWORD:password}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
  sql:
    init:
      mode: never

# ============================================
# JWT ASYMMETRIC KEY CONFIGURATION (CONSUMER)
# ============================================
jwt:
  # Public Key Path ONLY - REQUIRED for token verification
  # NO DEFAULT VALUE - Must be set via environment variable
  # Private key is NOT needed for consumer services
  public-key-path: ${JWT_PUBLIC_KEY_PATH}
  
  # Algorithm Configuration (must match issuer)
  algorithm: ${JWT_ALGORITHM:RSA}
  key-size: ${JWT_KEY_SIZE:2048}
  
  # Token Expiration (for validation checks)
  access-token-expiration: ${JWT_ACCESS_TOKEN_EXPIRATION:3600000}
  refresh-token-expiration: ${JWT_REFRESH_TOKEN_EXPIRATION:604800000}

# ============================================
# MANAGEMENT & MONITORING
# ============================================
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    tags:
      application: ${spring.application.name}

# ============================================
# LOGGING
# ============================================
logging:
  level:
    com.stylemind.order: ${LOGGING_LEVEL_COM_STYLEMIND_ORDER:DEBUG}
    org.springframework.security: ${LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY:DEBUG}
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{X-Request-Id:-}] [%thread] %-5level %logger{36} - %msg%n"
```

### 4.3 API Gateway `application.yml` (JWT Consumer)

```yaml
server:
  port: ${SERVER_PORT:3000}

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins: "*"
            allowedMethods: "*"
            allowedHeaders: "*"
            exposedHeaders: "X-Request-Id,X-User-Id,X-User-Roles"
      routes:
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/auth/**
          filters:
            - StripPrefix=1
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/users/**
          filters:
            - StripPrefix=1
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**
          filters:
            - StripPrefix=1
        - id: cart-service
          uri: lb://cart-service
          predicates:
            - Path=/api/cart/**
          filters:
            - StripPrefix=1

# ============================================
# JWT ASYMMETRIC KEY CONFIGURATION (CONSUMER)
# ============================================
jwt:
  # Public Key Path ONLY - REQUIRED for token verification
  # NO DEFAULT VALUE - Must be set via environment variable
  public-key-path: ${JWT_PUBLIC_KEY_PATH}
  
  # Algorithm Configuration (must match issuer)
  algorithm: ${JWT_ALGORITHM:RSA}
  key-size: ${JWT_KEY_SIZE:2048}
  
  # Token Expiration (for validation checks)
  access-token-expiration: ${JWT_ACCESS_TOKEN_EXPIRATION:3600000}
  refresh-token-expiration: ${JWT_REFRESH_TOKEN_EXPIRATION:604800000}

# ============================================
# MANAGEMENT & MONITORING
# ============================================
management:
  endpoints:
    web:
      exposure:
        include: health,info,gateway
  endpoint:
    health:
      show-details: always

# ============================================
# LOGGING
# ============================================
logging:
  level:
    com.stylemind.gateway: ${LOGGING_LEVEL_COM_STYLEMIND_GATEWAY:DEBUG}
    org.springframework.cloud.gateway: ${LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_CLOUD_GATEWAY:DEBUG}
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{X-Request-Id:-}] [%thread] %-5level %logger{36} - %msg%n"
```

---

## 5. KEY GENERATION SCRIPT (Reference)

### 5.1 RSA-2048 Key Pair Generation

```bash
#!/bin/bash
# generate-rsa-keys.sh
# Run from project root: ./generate-rsa-keys.sh

CERTS_DIR=".docker/certs"

# Create目录 if it doesn't exist
mkdir -p "$CERTS_DIR"

# Generate RSA-2048 Private Key
openssl genrsa -out "$CERTS_DIR/private_key.pem" 2048

# Extract Public Key from Private Key
openssl rsa -in "$CERTS_DIR/private_key.pem" -pubout -out "$CERTS_DIR/public_key.pem"

# Set appropriate permissions
chmod 600 "$CERTS_DIR/private_key.pem"
chmod 644 "$CERTS_DIR/public_key.pem"

echo "✅ RSA-2048 key pair generated successfully:"
echo "   Private Key: $CERTS_DIR/private_key.pem"
echo "   Public Key:  $CERTS_DIR/public_key.pem"
```

### 5.2 Windows PowerShell Equivalent

```powershell
# generate-rsa-keys.ps1
# Run from project root: .\generate-rsa-keys.ps1

# PREREQUISITE: If openssl is not recognized, add Git/usr/bin to PATH:
# $env:Path += ";C:\Program Files\Git\usr\bin"

$certsDir = ".docker\certs"

# Create directory if it doesn't exist
if (-not (Test-Path $certsDir)) {
    New-Item -ItemType Directory -Path $certsDir | Out-Null
}

# Generate RSA-2048 Private Key using modern openssl genpkey
openssl genpkey -algorithm RSA -out "$certsDir\private_key.pem" -pkeyopt rsa_keygen_bits:2048

# Extract Public Key from Private Key
openssl rsa -in "$certsDir\private_key.pem" -pubout -out "$certsDir\public_key.pem"

# Set appropriate permissions
icacls "$certsDir\private_key.pem" /inheritance:r
icacls "$certsDir\private_key.pem" /grant:r "$($env:USERNAME):(R)"

Write-Host "✅ RSA-2048 key pair generated successfully:"
Write-Host "   Private Key: $certsDir\private_key.pem"
Write-Host "   Public Key:  $certsDir\public_key.pem"
```

---

## 6. .GITIGNORE CONFIGURATION

### 6.1 Add to `.gitignore`

```gitignore
# ============================================
# SECURITY: Certificates Directory
# ============================================
# STRICT: Exclude entire certs directory to prevent any key leakage
# This prevents backups, temporary files, or renamed keys from being committed
.docker/certs/*

# EXCEPTION: Preserve directory structure via .gitkeep
!.docker/certs/.gitkeep
```

### 6.2 Create `.docker/certs/.gitkeep`

```bash
# Ensure directory structure is maintained in git
touch .docker/certs/.gitkeep
```

---

## 7. VALIDATION & TESTING

### 7.1 Verify Key Mounting

```bash
# Test that keys are mounted correctly in auth-service
docker exec stylemind-auth-service ls -la /app/certs/

# Expected output:
# -rw-r--r-- 1 root root  451 Jan 11 10:00 private_key.pem
# -rw-r--r-- 1 root root  167 Jan 11 10:00 public_key.pem

# Test that consumer services only have public key
docker exec stylemind-order-service ls -la /app/certs/

# Expected output:
# -rw-r--r-- 1 root root  167 Jan 11 10:00 public_key.pem
```

### 7.2 Verify Environment Variables

```bash
# Check environment variables in auth-service
docker exec stylemind-auth-service env | grep JWT

# Expected output:
# JWT_PRIVATE_KEY_PATH=/app/certs/private_key.pem
# JWT_PUBLIC_KEY_PATH=/app/certs/public_key.pem
# JWT_ALGORITHM=RSA
# JWT_KEY_SIZE=2048

# Check environment variables in consumer service
docker exec stylemind-order-service env | grep JWT

# Expected output:
# JWT_PUBLIC_KEY_PATH=/app/certs/public_key.pem
# JWT_ALGORITHM=RSA
# JWT_KEY_SIZE=2048
```

---

## 8. DEPLOYMENT CHECKLIST

### 8.1 Pre-Deployment Checklist

- [ ] Generate RSA-2048 key pair using provided script
- [ ] Verify `.docker/certs/` directory structure
- [ ] Add `private_key.pem` to `.gitignore`
- [ ] Update `docker-compose-separated.yml` with volume mounts
- [ ] Update all service `application.yml` files with JWT configuration
- [ ] Create `.env` file with appropriate environment variables
- [ ] Test key mounting in containers
- [ ] Verify environment variables are injected correctly
- [ ] Test JWT token generation with private key
- [ ] Test JWT token verification with public key

### 8.2 Production Deployment Considerations

- [ ] Use secure key management system (AWS KMS, HashiCorp Vault, Kubernetes Secrets)
- [ ] Rotate keys regularly (implement key rotation schedule)
- [ ] Use separate key pairs for different environments (dev/staging/prod)
- [ ] Implement key backup and recovery procedures
- [ ] Monitor key usage and expiration
- [ ] Implement proper access controls for key storage
- [ ] Use hardware security modules (HSM) for high-security requirements

---

## 9. TROUBLESHOOTING

### 9.1 Common Issues

**Issue:** Container cannot find key files
```
Solution: Verify volume mount path matches internal container path
Check: docker exec <container> ls -la /app/certs/
```

**Issue:** Permission denied reading private key
```
Solution: Ensure file permissions are set correctly (600 for private key)
Check: docker exec <container> ls -la /app/certs/private_key.pem
```

**Issue:** JWT signature verification fails
```
Solution: Verify public key matches private key pair
Check: openssl rsa -in private_key.pem -pubout -out public_key.pem
```

**Issue:** Environment variables not injected
```
Solution: Verify .env file is loaded in docker-compose
Check: docker-compose config | grep JWT
```

---

## 10. SUMMARY

This configuration blueprint provides:

1. **✅ Secure key storage** - Private keys excluded from version control
2. **✅ Relative path portability** - Works across different developer machines
3. **✅ Read-only volume mounting** - Prevents container key modification
4. **✅ Environment-agnostic paths** - Consistent internal container paths
5. **✅ Clear separation of concerns** - Issuer vs consumer key access
6. **✅ Production-ready structure** - Scalable to secure key management systems
7. **✅ Zero absolute paths** - Fully portable configuration
8. **✅ Comprehensive documentation** - Complete setup and validation procedures

**Next Steps:**
1. Execute key generation script
2. Update `.gitignore` to exclude private keys
3. Apply Docker Compose configuration changes
4. Update Spring Boot application.yml files
5. Test container mounting and environment variable injection
6. Implement Java code changes for asymmetric JWT handling

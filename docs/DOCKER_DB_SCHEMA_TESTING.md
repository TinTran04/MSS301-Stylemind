# Docker Database Operations & Schema Testing Guide

## 1. Docker Database Commands

### PostgreSQL Connection
```bash
# Kết nối vào container postgres
docker exec -it stylemind-postgres psql -U postgres

# Kết nối vào database cụ thể
docker exec -it stylemind-postgres psql -U postgres -d auth_db
docker exec -it stylemind-postgres psql -U postgres -d user_db
docker exec -it stylemind-postgres psql -U postgres -d product_db
docker exec -it stylemind-postgres psql -U postgres -d cart_db
docker exec -it stylemind-postgres psql -U postgres -d order_db
docker exec -it stylemind-postgres psql -U postgres -d payment_db
docker exec -it stylemind-postgres psql -U postgres -d notification_db
docker exec -it stylemind-postgres psql -U postgres -d ai_db
docker exec -it stylemind-postgres psql -U postgres -d inventory_db
```

### Xem Tables & Data
```bash
# Liệt kê tất cả tables trong database
docker exec -it stylemind-postgres psql -U postgres -d auth_db -c "\dt"

# Xem schema của table
docker exec -it stylemind-postgres psql -U postgres -d auth_db -c "\d users"

# Xem dữ liệu
docker exec -it stylemind-postgres psql -U postgres -d auth_db -c "SELECT * FROM users;"
docker exec -it stylemind-postgres psql -U postgres -d auth_db -c "SELECT id, email, role, created_at FROM users;"
```

### Backup & Restore
```bash
# Backup database
docker exec stylemind-postgres pg_dump -U postgres auth_db > auth_db_backup.sql

# Restore database
cat auth_db_backup.sql | docker exec -i stylemind-postgres psql -U postgres -d auth_db

# Backup all databases
docker exec stylemind-postgres pg_dumpall -U postgres > all_dbs_backup.sql
```

### Docker Compose Database Operations
```bash
# Restart postgres (giữ data)
docker compose restart postgres

# Restart postgres + xóa volume (reset hoàn toàn)
docker compose down -v postgres
docker compose up -d postgres

# Xem logs
docker logs stylemind-postgres -f
docker logs stylemind-postgres --tail 100
```

---

## 2. Database Schema Reference

### auth_db
```sql
-- Table: users
CREATE TABLE users (
    id VARCHAR(50) PRIMARY KEY,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    full_name VARCHAR(150),
    provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    provider_id VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_provider ON users(provider, provider_id);

-- Default users (password: admin123 / customer123)
INSERT INTO users (id, email, password_hash, full_name, provider, role) VALUES
('usr_admin', 'admin@stylemind.ai', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RK.PZvO.S', 'System Admin', 'LOCAL', 'ADMIN'),
('usr_customer', 'customer@stylemind.ai', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RK.PZvO.S', 'Test Customer', 'LOCAL', 'CUSTOMER');
```

### user_db
```sql
-- Table: customer_style_profiles
CREATE TABLE customer_style_profiles (
    user_id VARCHAR(50) PRIMARY KEY REFERENCES users(id),
    gender VARCHAR(20),
    age INT,
    height_cm DECIMAL(5, 2),
    weight_kg DECIMAL(5, 2),
    body_morphology VARCHAR(50),
    preferred_fit VARCHAR(30),
    style_personas JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table: delivery_addresses
CREATE TABLE delivery_addresses (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL REFERENCES users(id),
    recipient_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    address_line TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### product_db
```sql
-- Table: categories
CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_id INT REFERENCES categories(id),
    slug VARCHAR(150) UNIQUE NOT NULL
);

-- Table: products
CREATE TABLE products (
    id VARCHAR(50) PRIMARY KEY,
    category_id INT REFERENCES categories(id),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    base_price DECIMAL(12, 2) NOT NULL,
    aesthetic_style VARCHAR(50),
    target_demographic VARCHAR(20),
    seasonal_property VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table: product_variants
CREATE TABLE product_variants (
    id VARCHAR(50) PRIMARY KEY,
    product_id VARCHAR(50) NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku VARCHAR(100) UNIQUE NOT NULL,
    size VARCHAR(20) NOT NULL,
    color VARCHAR(50) NOT NULL,
    material VARCHAR(50),
    price_override DECIMAL(12, 2)
);

-- Table: product_images
CREATE TABLE product_images (
    id SERIAL PRIMARY KEY,
    product_id VARCHAR(50) NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    image_url VARCHAR(500) NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE
);

-- Sample categories
INSERT INTO categories (name, parent_id, slug) VALUES
('Áo', NULL, 'ao'),
('Quần', NULL, 'quan'),
('Áo thun', 1, 'ao-thun'),
('Áo sơ mi', 1, 'ao-so-mi'),
('Quần jeans', 2, 'quan-jeans'),
('Quần tây', 2, 'quan-tay');
```

### cart_db
```sql
-- Table: shopping_carts
CREATE TABLE shopping_carts (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) UNIQUE REFERENCES users(id),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table: cart_items
CREATE TABLE cart_items (
    id VARCHAR(50) PRIMARY KEY,
    cart_id VARCHAR(50) NOT NULL REFERENCES shopping_carts(id) ON DELETE CASCADE,
    variant_id VARCHAR(50) NOT NULL REFERENCES product_variants(id),
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
    is_ai_recommended BOOLEAN DEFAULT FALSE,
    source_bundle_id VARCHAR(50),
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### order_db
```sql
-- Table: orders
CREATE TABLE orders (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL REFERENCES users(id),
    total_amount DECIMAL(12, 2) NOT NULL,
    order_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    shipping_address TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table: order_items
CREATE TABLE order_items (
    id VARCHAR(50) PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    variant_id VARCHAR(50) NOT NULL REFERENCES product_variants(id),
    quantity INT NOT NULL CHECK (quantity > 0),
    price_at_purchase DECIMAL(12, 2) NOT NULL,
    is_ai_conversion BOOLEAN DEFAULT FALSE,
    source_bundle_id VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### payment_db
```sql
-- Table: transactions
CREATE TABLE transactions (
    id VARCHAR(50) PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL,
    user_id VARCHAR(50) NOT NULL REFERENCES users(id),
    amount DECIMAL(12, 2) NOT NULL,
    method VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    transaction_ref VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### notification_db
```sql
-- Table: notification_logs
CREATE TABLE notification_logs (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(50) REFERENCES users(id),
    type VARCHAR(30) NOT NULL,
    title VARCHAR(200),
    content TEXT,
    status VARCHAR(20) NOT NULL,
    sent_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### ai_db
```sql
-- Table: chat_sessions
CREATE TABLE chat_sessions (
    id UUID PRIMARY KEY,
    user_id VARCHAR(50) REFERENCES users(id),
    context_weather_temp DECIMAL(4, 1),
    context_weather_condition VARCHAR(30),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table: chat_messages
CREATE TABLE chat_messages (
    id VARCHAR(50) PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    sender_type VARCHAR(10) NOT NULL,
    message_text TEXT NOT NULL,
    has_product_block BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table: ai_curated_bundles
CREATE TABLE ai_curated_bundles (
    id VARCHAR(50) PRIMARY KEY,
    message_id VARCHAR(50) NOT NULL REFERENCES chat_messages(id),
    justification_summary TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table: ai_curated_bundle_items
CREATE TABLE ai_curated_bundle_items (
    bundle_id VARCHAR(50) NOT NULL REFERENCES ai_curated_bundles(id) ON DELETE CASCADE,
    product_id VARCHAR(50) NOT NULL REFERENCES products(id),
    PRIMARY KEY (bundle_id, product_id)
);

-- Table: ai_analytics_logs
CREATE TABLE ai_analytics_logs (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL REFERENCES users(id),
    bundle_id VARCHAR(50) NOT NULL REFERENCES ai_curated_bundles(id),
    interaction_type VARCHAR(30) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table: ai_index_jobs
CREATE TABLE ai_index_jobs (
    id VARCHAR(50) PRIMARY KEY,
    target_type VARCHAR(30) NOT NULL,
    target_id VARCHAR(50) NOT NULL,
    operation_type VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INT DEFAULT 0,
    last_error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### inventory_db
```sql
-- Table: inventory_items
CREATE TABLE inventory_items (
    id VARCHAR(50) PRIMARY KEY,
    variant_id VARCHAR(50) NOT NULL UNIQUE,
    quantity_available INT NOT NULL DEFAULT 0,
    quantity_reserved INT NOT NULL DEFAULT 0,
    reorder_level INT DEFAULT 10,
    location VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 3. Swagger 3 API Test Data

### Auth Service Endpoints

#### POST /api/auth/register
```json
{
  "email": "newuser@stylemind.ai",
  "password": "SecurePass123!",
  "name": "New User"
}
```

#### POST /api/auth/login
```json
{
  "email": "newuser@stylemind.ai",
  "password": "SecurePass123!"
}
```

#### GET /api/auth/me
```bash
# Cần header Authorization
curl -H "Authorization: Bearer <token>" http://localhost:8081/api/auth/me
```

#### POST /api/auth/logout
```bash
# Cần header Authorization
curl -X POST -H "Authorization: Bearer <token>" http://localhost:8081/api/auth/logout
```

### Sample Test Users (đã có sẵn)

| Email | Password | Role | ID |
|-------|----------|------|-----|
| admin@stylemind.ai | admin123 | ADMIN | usr_admin |
| customer@stylemind.ai | customer123 | CUSTOMER | usr_customer |

### Test Data cho Các Service Khác

#### User Service - Style Profile
```json
POST /api/users/me/style-profile
{
  "gender": "MALE",
  "age": 28,
  "height_cm": 175.5,
  "weight_kg": 70.2,
  "body_morphology": "ATHELETIC",
  "preferred_fit": "SLIM",
  "style_personas": ["CASUAL", "MINIMALIST", "STREETWEAR"]
}
```

#### Product Service - Tạo Product
```json
POST /api/products
{
  "category_id": 3,
  "name": "Áo thun basic trắng",
  "description": "Áo thun cotton 100% cơ bản",
  "base_price": 250000,
  "aesthetic_style": "MINIMALIST",
  "target_demographic": "YOUNG_ADULT",
  "seasonal_property": "ALL_SEASON"
}
```

#### Cart Service - Thêm vào giỏ
```json
POST /api/carts/items
{
  "variant_id": "variant-id-here",
  "quantity": 2
}
```

#### Order Service - Checkout
```json
POST /api/orders
{
  "shipping_address": "123 Đường ABC, Quận 1, TP.HCM",
  "payment_method": "COD"
}
```

---

## 4. Quick Commands for Testing

```bash
# 1. Check all services running
docker compose ps

# 2. Test auth register
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@stylemind.ai","password":"Test123!","name":"Test User"}'

# 3. Test auth login
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@stylemind.ai","password":"Test123!"}' | jq -r .data.token)
echo $TOKEN

# 4. Test protected endpoint
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/auth/me

# 5. View swagger docs
curl http://localhost:8081/v3/api-docs | jq .

# 6. Check database tables
docker exec -it stylemind-postgres psql -U postgres -d auth_db -c "\dt"
docker exec -it stylemind-postgres psql -U postgres -d auth_db -c "SELECT * FROM users;"

# 7. Reset database hoàn toàn
docker compose down -v postgres
docker compose up -d postgres
sleep 10
docker compose up -d auth-service
```

---

## 5. Environment Variables (.env)

```env
# BE/.env
JWT_SECRET=super-secure-stylemind-secret-key-signature-2026-xyz-32chars-min
X_INTERNAL_TOKEN=stylemind-internal-token-for-service-communication
LLM_API_KEY=
```

---

## 6. Ports Mapping

| Service | Container Port | Host Port |
|---------|---------------|-----------|
| postgres | 5432 | 5432 |
| redis | 6379 | 6379 |
| minio | 9000/9001 | 9000/9001 |
| qdrant | 6333/6334 | 6333/6334 |
| auth-service | 8081 | 8081 |
| api-gateway | 3000 | 3000 |
| user-service | 8082 | 8082 |
| product-service | 8083 | 8083 |
| cart-service | 8086 | 8086 |
| order-service | 8087 | 8087 |
| payment-service | 8088 | 8088 |
| notification-service | 8089 | 8089 |
| ai-agent-service | 8085 | 8085 |

---

## 7. Troubleshooting

### Auth Service Not Starting
```bash
# Check logs
docker logs auth-service --tail 50

# Common issues:
# - Circular dependency: check @Lazy annotations
# - JWT secret too short: must be >= 32 chars
# - DB connection: check postgres healthy
```

### Swagger UI Not Working
```bash
# API docs available at:
http://localhost:8081/v3/api-docs

# Swagger UI needs api-gateway running:
http://localhost:3000/swagger-ui/
```

### Database Not Initialized
```bash
# Force re-init
docker compose down -v postgres
docker compose up -d postgres
# Wait for healthy, then restart auth-service
docker compose up -d auth-service
```

---

*Created: 2026-06-18*
*Project: MSS301-Stylemind*
*Location: docs/DOCKER_DB_SCHEMA_TESTING.md*
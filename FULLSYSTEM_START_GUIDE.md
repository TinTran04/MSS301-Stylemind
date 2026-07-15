# HƯỚNG DẪN KHỞI ĐỘNG HỆ THỐNG STYLEMIND

## 1. KIẾN TRÚC TỔNG QUAN

```
Frontend (React + Vite, Port 5173) 
    ↓
API Gateway (Port 3000) 
    ↓
├── Auth Service (8081) - JWT Issuer
├── User Service (8082) 
├── Product Service (8083)
├── Cart Service (8086)
├── Order Service (8087)
├── Payment Service (8088)
├── Notification Service (8089)
└── AI Agent Service (8085)
```

**Luồng kết nối:**
- Frontend gọi API thông qua API Gateway (Port 3000)
- Gateway routing requests đến các microservices tương ứng
- Auth Service phát hành JWT tokens (RSA-2048)
- Các services khác verify JWT tokens bằng public key

---

## 2. HƯỚNG DẪN START BACKEND

### 2.1. Cấu hình Environment Variables

**BƯỚC 1:** Đảm bảo file `.env` tồn tại trong thư mục `BE/`

File `.env` chứa tất cả environment variables cần thiết cho hệ thống. Nếu file chưa tồn tại, tạo mới với các biến sau:

```env
# Database Configuration
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=password

# JWT Configuration (RSA Keys)
JWT_PUBLIC_KEY_PATH=/app/certs/public_key.pem
JWT_PRIVATE_KEY_PATH=/app/certs/private_key.pem
JWT_ALGORITHM=RSA
JWT_KEY_SIZE=2048
JWT_ACCESS_TOKEN_EXPIRATION=3600000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# Service Ports
SERVER_PORT=3000
SERVER_PORT_AUTH=8081
SERVER_PORT_USER=8082
SERVER_PORT_PRODUCT=8083
SERVER_PORT_AI=8085
SERVER_PORT_CART=8086
SERVER_PORT_ORDER=8087
SERVER_PORT_PAYMENT=8088
SERVER_PORT_NOTIFICATION=8089

# Service URLs (dùng container names cho Docker network)
AUTH_SERVICE_URL=http://auth-service:8081
USER_SERVICE_URL=http://user-service:8082
PRODUCT_SERVICE_URL=http://product-service:8083
AI_SERVICE_URL=http://ai-agent-service:8085
CART_SERVICE_URL=http://cart-service:8086
ORDER_SERVICE_URL=http://order-service:8087
PAYMENT_SERVICE_URL=http://payment-service:8088
NOTIFICATION_SERVICE_URL=http://notification-service:8089

# Infrastructure Configuration
REDIS_HOST=stylemind-redis
REDIS_PORT=6379
NEO4J_URI=bolt://neo4j:7687
NEO4J_USERNAME=neo4j
NEO4J_PASSWORD=password
QDRANT_HOST=qdrant
QDRANT_PORT=6333
S3_ENDPOINT=http://minio:9000
S3_ACCESS_KEY=admin
S3_SECRET_KEY=password
S3_BUCKET=stylemind-products

# Cloudinary Configuration (cho Product Service)
CLOUDINARY_CLOUD_NAME=donepkwh3
CLOUDINARY_API_KEY=846482493196337
CLOUDINARY_API_SECRET=u7pccgzUfXBh2wgPYC5lsnH2ViI
CLOUDINARY_FOLDER=stylemind/products

# Internal Service Token
INTERNAL_TOKEN=sm-secret-internal-service-token-key-2026
X_INTERNAL_TOKEN=sm-secret-internal-service-token-key-2026

# SePay Configuration (cho Payment Service)
SEPAY_BANK_SHORT_NAME=VietinBank
SEPAY_ACCOUNT_NUMBER=101876751836
SEPAY_ACCOUNT_NAME=NGUYEN MINH KHOI
SEPAY_BANK_HUB_PREFIX=SEVQR
SEPAY_PAYMENT_EXPIRE_MINUTES=15
SEPAY_QR_BASE_URL=https://img.vietqr.io/image
SEPAY_ENABLED=true
SEPAY_MODE=live
SEPAY_PAYMENT_CODE_PREFIX=STYLEMIND
SEPAY_WEBHOOK_AUTH_MODE=API_KEY
SEPAY_WEBHOOK_API_KEY=GZRG6C8WW7ALNAHQM1DYRIGOW0HZRQKM2VFUZBTEMUX9VSPJUTCSLVQYLKNRZOGQ

# Mail Configuration (cho Notification Service)
MAIL_ENABLED=true
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=khoiminhnguyen2k4@gmail.com
SPRING_MAIL_PASSWORD=mkgafyqqzzoeattk
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
APP_MAIL_FROM_ADDRESS=no-reply@stylemind.ai
APP_MAIL_FROM_NAME=StyleMind
MAIL_LOG_FALLBACK=true

# Auth-specific Configuration
APP_FRONTEND_BASE_URL=http://localhost:5173
AUTH_SETUP_TOKEN_EXPIRY_MINUTES=1440
AUTH_RESET_OTP_EXPIRY_MINUTES=10
AUTH_RESET_TOKEN_EXPIRY_MINUTES=30
AUTH_RESET_OTP_MAX_ATTEMPTS=5
AUTH_RESET_OTP_RESEND_COOLDOWN_SECONDS=60
AUTH_REGISTER_OTP_EXPIRY_MINUTES=10
AUTH_REGISTER_OTP_MAX_ATTEMPTS=5
AUTH_REGISTER_OTP_RESEND_COOLDOWN_SECONDS=60

# Order-specific Configuration
ORDER_PAYMENT_TIMEOUT_MINUTES=15
ORDER_TIMEOUT_JOB_INTERVAL_MS=300000

# Product-specific Configuration
PRODUCT_DEFAULT_CURRENCY=VND

# LLM Configuration
LLM_API_KEY=your-llm-api-key-here

# Eureka Configuration
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://discovery-service:8761/eureka
```

**BƯỚC 2:** Đảm bảo RSA keys tồn tại trong `.docker/certs/`

RSA keys được generate tự động khi build Docker image, nhưng bạn cũng có thể tạo thủ công:

```bash
cd BE
mkdir -p .docker/certs
openssl genrsa -out .docker/certs/private_key.pem 2048
openssl rsa -in .docker/certs/private_key.pem -pubout -out .docker/certs/public_key.pem
```

### 2.2. Khởi động Infrastructure (PostgreSQL, Redis, Neo4j, Qdrant, MinIO)

```bash
cd BE
docker compose --profile infra up -d
```

Đợi khoảng 15-30 giây để các infrastructure services khởi động hoàn tất.

### 2.3. Khởi động toàn bộ hệ thống Backend (Infrastructure + Services)

```bash
cd BE
docker compose --profile all up -d
```

Hoặc khởi động từng phần:
- **Infrastructure only:** `docker compose --profile infra up -d`
- **Application services only:** `docker compose --profile app up -d`
- **Tất cả:** `docker compose --profile all up -d`

### 2.4. Kiểm tra trạng thái containers

```bash
cd BE
docker compose ps
```

Đảm bảo tất cả containers có status "Up" hoặc "healthy".

### 2.5. Danh sách Services & Ports

| Service | Port | Mô tả |
|---------|------|-------|
| API Gateway | 3000 | Gateway routing & JWT verification |
| Auth Service | 8081 | Authentication & JWT Issuer |
| User Service | 8082 | User profiles & delivery addresses |
| Product Service | 8083 | Products, categories, variants |
| Cart Service | 8086 | Shopping cart management |
| Order Service | 8087 | Order processing & management |
| Payment Service | 8088 | Payment simulation |
| Notification Service | 8089 | Notification logging |
| AI Agent Service | 8085 | AI Stylist & chatbot |

### 2.6. Infrastructure Components

| Component | Port | Mô tả |
|-----------|------|-------|
| PostgreSQL | 5433-5440 | 9 databases cho các services |
| Redis | 6379 | Caching & session storage |
| Qdrant | 6333 | Vector DB cho product search |
| Neo4j | 7474, 7687 | Graph DB cho fashion taxonomy |
| MinIO | 9000, 9001 | S3-compatible image storage |

### 2.7. Cơ chế Fail-Fast

**QUAN TRỌNG:** Hệ thống sử dụng cơ chế fail-fast để dễ debug:

- **KHÔNG có default values** trong `application.yml` cho JWT key paths
- Nếu environment variables thiếu hoặc sai, service sẽ **crash ngay khi boot**
- JWT keys bắt buộc phải được mount từ `.docker/certs/` directory
- RSA keys được generate tự động khi build Docker image

**Environment variables bắt buộc:**
- `JWT_PRIVATE_KEY_PATH` - Auth Service only
- `JWT_PUBLIC_KEY_PATH` - Tất cả services
- `NEO4J_URI`, `NEO4J_USERNAME`, `NEO4J_PASSWORD` - AI Agent Service only

### 2.8. Rebuild Services

**Rebuild một service cụ thể sau khi thay đổi code:**

```bash
cd BE
docker compose up -d --build <service-name>
```

Ví dụ: Rebuild auth service
```bash
docker compose up -d --build auth-service
```

**Rebuild toàn bộ hệ thống:**

```bash
cd BE
docker compose --profile all up -d --build
```

**Rebuild và force-recreate containers (xóa container cũ và tạo mới):**

```bash
cd BE
docker compose --profile all up -d --build --force-recreate
```

**Rebuild từ đầu (xóa images và volumes):**

```bash
cd BE
docker compose down -v
docker compose --profile all up -d --build
```

Lưu ý: Lệnh này sẽ **XÓA TẤT CẢ DỮ LIỆU** trong databases.

---

## 3. HƯỚNG DẪN START FRONTEND

### 3.1. Framework: React + Vite

Frontend sử dụng React 18 với Vite 5 và TailwindCSS 4.

### 3.2. Cài đặt Dependencies

```bash
cd FE
npm install
```

### 3.3. Cấu hình Environment Variables

Copy file `.env.example` sang `.env`:

```bash
cp .env.example .env
```

Edit `.env` để cấu hình API Gateway endpoint:

```env
VITE_API_GATEWAY=http://localhost:3000/api
```

### 3.4. Khởi động Development Server

```bash
npm run dev
```

Frontend sẽ chạy tại: `http://localhost:5173`

---

## 4. TROUBLESHOOTING & CONNECTIVITY CHECK

### 4.1. Kiểm tra Health Status (Windows PowerShell)

```powershell
# API Gateway
curl.exe -s http://localhost:3000/actuator/health

# Auth Service
curl.exe -s http://localhost:8081/actuator/health

# User Service
curl.exe -s http://localhost:8082/actuator/health

# Product Service
curl.exe -s http://localhost:8083/actuator/health

# Cart Service
curl.exe -s http://localhost:8086/actuator/health

# Order Service
curl.exe -s http://localhost:8087/actuator/health

# Payment Service
curl.exe -s http://localhost:8088/actuator/health

# Notification Service
curl.exe -s http://localhost:8089/actuator/health

# AI Agent Service
curl.exe -s http://localhost:8085/actuator/health
```

### 4.2. Kiểm tra Gateway Routing

```powershell
# Test routing đến auth service (sẽ trả về lỗi auth nếu không có token)
curl.exe -s http://localhost:3000/api/v1/auth/actuator/health
```

Kết quả mong đợi: `{"success":false,"errorCode":"AUTH_TOKEN_INVALID","message":"Missing or invalid Authorization header"}`

### 4.3. Kiểm tra Docker Containers

```powershell
cd BE
docker compose ps
```

### 4.4. Xem Logs của Service

```powershell
# Xem logs của auth service
docker compose logs auth-service --tail 50

# Xem logs của tất cả services
docker compose logs --tail 20
```

### 4.5. Vấn đề Thường Gặp

**Service không start:**
- Kiểm tra environment variables trong file `.env`
- Đảm bảo RSA keys tồn tại trong `.docker/certs/`
- Xem logs để xác định lỗi cụ thể

**JWT keys không load được:**
- Kiểm tra permissions của file keys (chmod 644)
- Đảm bảo volume mount đúng: `./.docker/certs:/app/certs:ro`

**Neo4j connection failed:**
- Đảm bảo `NEO4J_URI` được set đúng: `bolt://neo4j:7687`
- Kiểm tra Neo4j container status: `docker compose logs neo4j`

**Frontend không kết nối được API:**
- Kiểm tra `.env` file có đúng `VITE_API_GATEWAY`
- Đảm bảo API Gateway đang chạy trên port 3000
- Kiểm tra CORS configuration trong API Gateway

---

## 5. DỪNG HỆ THỐNG

### 5.1. Dừng Backend

```bash
cd BE
docker compose down
```

Để dừng và xóa volumes (dữ liệu database):
```bash
cd BE
docker compose down -v
```

### 5.2. Dừng Frontend

```bash
# Ctrl+C trong terminal đang chạy npm run dev
```

---

## 6. TÀI LIỆU THAM KHẢO

- [Backend Architecture](BE/README.md)
- [Frontend Documentation](FE/README.md)
- [Docker Configuration](BE/docker-compose.yml)
- [Environment Variables](BE/.env)
- [JWT Implementation](docs/AGENT_WORKSPACE/ASYMMETRIC_JWT_IMPLEMENTATION_PLAN.md)

---

**Lưu ý:** Hướng dẫn này dành cho môi trường phát triển (development). Đối với production, cần cấu hình thêm security, monitoring, và scaling.

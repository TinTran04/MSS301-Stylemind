# StyleMind — Full Stack Start Guide

> **Tài liệu dành cho toàn team.** Mô tả đầy đủ cấu trúc dự án, các file cần xin từ main developer, và thứ tự khởi động toàn bộ stack.

---

## Tổng Quan Kiến Trúc

```
MSS301-Code/
├── MSS301-Stylemind/          ← Repo chính (Java BE + React FE)
│   ├── BE/                    ← Java Spring Boot microservices
│   │   ├── api-gateway/       ← Spring Cloud Gateway (port 3000)
│   │   ├── auth-service/      ← JWT, OTP, đăng ký/đăng nhập (port 8081)
│   │   ├── user-service/      ← Profile người dùng (port 8082)
│   │   ├── product-service/   ← Sản phẩm, danh mục (port 8083)
│   │   ├── cart-service/      ← Giỏ hàng (port 8086)
│   │   ├── order-service/     ← Đặt hàng (port 8087)
│   │   ├── payment-service/   ← SePay/VietQR (port 8088)
│   │   ├── notification-service/ ← Email/thông báo (port 8089)
│   │   └── docker-compose.yml ← Orchestration duy nhất
│   └── FE/                    ← React + Vite (port 5173)
└── MSS301-AI-Service-real/
    └── AI-stylist-recommendation/ ← Python FastAPI AI (port 8000)
```

**Network flow:**
```
Browser → Gateway :3000 → Java Services (Docker internal)
                        → Python AI :8000 (host.docker.internal)
```

---

## Yêu Cầu Hệ Thống

| Tool | Version | Link |
|---|---|---|
| Docker Desktop | ≥ 4.x | https://www.docker.com/products/docker-desktop |
| Node.js | ≥ 20.x | https://nodejs.org |
| Python + uv | ≥ 3.12 | https://docs.astral.sh/uv/getting-started/installation |
| Git | ≥ 2.x | https://git-scm.com |

---

## Bước 1 — Xin File Secrets Từ Main Developer

> ⚠️ Tất cả file dưới đây bị **gitignore** — không có trong repo, phải xin trực tiếp.

### 1.1 RSA Key Pair (QUAN TRỌNG NHẤT)

Xin 2 file `.pem` và đặt vào đúng đường dẫn:
```
MSS301-Stylemind/BE/.docker/certs/
├── private_key.pem    ← auth-service dùng để ký JWT
└── public_key.pem     ← tất cả services dùng để verify JWT
```

> ⚠️ Toàn team PHẢI dùng chung 1 cặp key. Nếu mỗi người tự gen key riêng, JWT sẽ không verify được giữa các service.

**Tạo thư mục nếu chưa có:**
```powershell
New-Item -ItemType Directory -Force -Path "BE\.docker\certs"
```

### 1.2 Checklist Đầy Đủ

| # | File / Biến | Mô tả | Ai cung cấp |
|---|---|---|---|
| 1 | `BE/.docker/certs/private_key.pem` | RSA private key | 🔴 Main dev |
| 2 | `BE/.docker/certs/public_key.pem` | RSA public key | 🔴 Main dev |
| 3 | `BE/.env` → `SPRING_DATASOURCE_PASSWORD` | Password PostgreSQL | 🔴 Main dev |
| 4 | `BE/.env` → `INTERNAL_TOKEN` | Token nội bộ giữa services | 🔴 Main dev |
| 5 | `BE/.env` → `CLOUDINARY_API_KEY/SECRET/CLOUD_NAME` | Upload ảnh sản phẩm | 🔴 Main dev |
| 6 | `BE/.env` → `SPRING_MAIL_USERNAME/PASSWORD` | Gmail App Password cho OTP | 🟠 Main dev |
| 7 | `BE/.env` → `SEPAY_WEBHOOK_API_KEY/ACCOUNT_NUMBER` | Thanh toán VietQR | 🟠 Main dev |
| 8 | `AI/.env` → `GEMINI_API_KEY` | Google Gemini API key | 🔴 Main dev / tự đăng ký |
| 9 | `AI/.env` → `FIRECRAWL_API_KEY` | Blog ingestion (có thể skip) | 🟡 Tự đăng ký |
| 10 | `FE/.env` | Chỉ chứa URL, không có secret | ✅ Tự tạo |

---

## Bước 2 — Tạo File `.env`

### 2.1 `BE/.env` — Backend Java Stack

Tạo file tại `MSS301-Stylemind/BE/.env`:

```env
# ============================================================
# DATABASE
# ============================================================
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=<xin_main_dev>

# ============================================================
# JWT RSA KEYS
# ============================================================
JWT_PUBLIC_KEY_PATH=/app/certs/public_key.pem
JWT_PRIVATE_KEY_PATH=/app/certs/private_key.pem
JWT_ALGORITHM=RSA
JWT_KEY_SIZE=2048
JWT_ACCESS_TOKEN_EXPIRATION=3600000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# ============================================================
# SERVICE PORTS
# ============================================================
SERVER_PORT=3000
SERVER_PORT_AUTH=8081
SERVER_PORT_USER=8082
SERVER_PORT_PRODUCT=8083
SERVER_PORT_AI=8085
SERVER_PORT_CART=8086
SERVER_PORT_ORDER=8087
SERVER_PORT_PAYMENT=8088
SERVER_PORT_NOTIFICATION=8089

# ============================================================
# SERVICE URLs (Docker internal network — không thay đổi)
# ============================================================
AUTH_SERVICE_URL=http://auth-service:8081
USER_SERVICE_URL=http://user-service:8082
PRODUCT_SERVICE_URL=http://product-service:8083
CART_SERVICE_URL=http://cart-service:8086
ORDER_SERVICE_URL=http://order-service:8087
PAYMENT_SERVICE_URL=http://payment-service:8088
NOTIFICATION_SERVICE_URL=http://notification-service:8089

# AI Stylist — Hybrid mode: Python chạy trên host machine
# Nếu containerize Python thì đổi thành: http://ai-stylist-service:8000
AI_SERVICE_URL=http://host.docker.internal:8000

# ============================================================
# REDIS
# ============================================================
REDIS_HOST=stylemind-redis
REDIS_PORT=6379

# ============================================================
# NEO4J (Knowledge Graph)
# ============================================================
NEO4J_URI=bolt://neo4j:7687
NEO4J_USERNAME=neo4j
NEO4J_PASSWORD=<xin_main_dev>

# ============================================================
# QDRANT (Vector Search)
# ============================================================
QDRANT_HOST=qdrant
QDRANT_PORT=6333

# ============================================================
# MINIO / S3 (Object Storage)
# ============================================================
S3_ENDPOINT=http://minio:9000
S3_ACCESS_KEY=<xin_main_dev>
S3_SECRET_KEY=<xin_main_dev>
S3_BUCKET=stylemind-products

# ============================================================
# CLOUDINARY (Upload ảnh sản phẩm)
# ============================================================
CLOUDINARY_CLOUD_NAME=<xin_main_dev>
CLOUDINARY_API_KEY=<xin_main_dev>
CLOUDINARY_API_SECRET=<xin_main_dev>
CLOUDINARY_FOLDER=stylemind/products

# ============================================================
# INTERNAL SERVICE TOKEN
# ============================================================
INTERNAL_TOKEN=<xin_main_dev>
X_INTERNAL_TOKEN=<xin_main_dev>

# ============================================================
# SEPAY / VIETQR (Thanh toán)
# ============================================================
SEPAY_BANK_SHORT_NAME=VietinBank
SEPAY_ACCOUNT_NUMBER=<xin_main_dev>
SEPAY_ACCOUNT_NAME=<xin_main_dev>
SEPAY_BANK_HUB_PREFIX=SEVQR
SEPAY_PAYMENT_EXPIRE_MINUTES=15
SEPAY_QR_BASE_URL=https://img.vietqr.io/image
SEPAY_ENABLED=true
SEPAY_MODE=live
SEPAY_PAYMENT_CODE_PREFIX=STYLEMIND
SEPAY_WEBHOOK_AUTH_MODE=API_KEY
SEPAY_WEBHOOK_API_KEY=<xin_main_dev>

# ============================================================
# MAIL — Dùng Gmail App Password (không phải mk Gmail thường)
# Tạo App Password: myaccount.google.com → Security → App passwords
# ============================================================
MAIL_ENABLED=true
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=<gmail_address>
SPRING_MAIL_PASSWORD=<16_char_app_password>
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
APP_MAIL_FROM_ADDRESS=no-reply@stylemind.ai
APP_MAIL_FROM_NAME=StyleMind
MAIL_LOG_FALLBACK=true

# ============================================================
# AUTH SERVICE
# ============================================================
APP_FRONTEND_BASE_URL=http://localhost:5173
AUTH_SETUP_TOKEN_EXPIRY_MINUTES=1440
AUTH_RESET_OTP_EXPIRY_MINUTES=10
AUTH_RESET_TOKEN_EXPIRY_MINUTES=30
AUTH_RESET_OTP_MAX_ATTEMPTS=5
AUTH_RESET_OTP_RESEND_COOLDOWN_SECONDS=60
AUTH_REGISTER_OTP_EXPIRY_MINUTES=10
AUTH_REGISTER_OTP_MAX_ATTEMPTS=5
AUTH_REGISTER_OTP_RESEND_COOLDOWN_SECONDS=60

# ============================================================
# ORDER SERVICE
# ============================================================
ORDER_PAYMENT_TIMEOUT_MINUTES=15
ORDER_TIMEOUT_JOB_INTERVAL_MS=300000

# ============================================================
# PRODUCT SERVICE
# ============================================================
PRODUCT_DEFAULT_CURRENCY=VND

# ============================================================
# EUREKA (Service Discovery)
# ============================================================
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://discovery-service:8761/eureka
```

### 2.2 `FE/.env` — Frontend React

Tạo file tại `MSS301-Stylemind/FE/.env` (tự tạo, không cần xin):

```env
VITE_API_BASE_URL=http://localhost:3000
VITE_APP_NAME=StyleMind
```

### 2.3 `AI-stylist-recommendation/.env` — Python AI Service

Tạo file tại `MSS301-AI-Service-real/AI-stylist-recommendation/.env`:

```env
# PostgreSQL riêng cho AI — port 5433 (tránh đụng Java BE ở 5432)
POSTGRES_HOST=localhost
POSTGRES_PORT=5433
POSTGRES_DB=ai_stylist
POSTGRES_USER=stylist
POSTGRES_PASSWORD=stylist123

# Neo4j — dùng chung container với Java BE
NEO4J_URI=bolt://localhost:7687
NEO4J_USER=neo4j
NEO4J_PASSWORD=password

# Gemini API — lấy tại: https://aistudio.google.com/app/apikey
GEMINI_API_KEY=<xin_main_dev>
GEMINI_MODEL=gemini-2.0-flash
GEMINI_EMBEDDING_MODEL=text-embedding-004

# Firecrawl — lấy tại: https://firecrawl.dev (chỉ cần cho blog ingestion)
FIRECRAWL_API_KEY=<xin_main_dev>
FIRECRAWL_BASE_URL=https://api.firecrawl.dev/v2
FIRECRAWL_TIMEOUT=120

# Qdrant — dùng chung container với Java BE
QDRANT_URL=http://localhost:6333
QDRANT_CONCEPT_COLLECTION=ai_stylist_concepts
QDRANT_PRODUCT_COLLECTION=ai_stylist_products

# Product Service (Java BE)
PRODUCT_SERVICE_BASE_URL=http://localhost:8083
PRODUCT_SERVICE_TIMEOUT=10

# App config
CONCEPT_SIMILARITY_THRESHOLD=0.65
APP_ENV=development
LOG_LEVEL=INFO
```

---

## Bước 3 — Khởi Động Stack

### 3.1 Nhanh nhất — Dùng script có sẵn

```powershell
# Từ thư mục gốc MSS301-Stylemind/
.\scripts\windows\full-up.ps1
```

Script này sẽ tự động chạy `docker compose --profile app up -d --build`.

### 3.2 Manual — Từng bước

#### Bước 3.2.1 — Start Infrastructure (DB + cache + search)

```powershell
cd "MSS301-Stylemind/BE"
docker compose --profile infra up -d
```

Bao gồm: Redis, Qdrant, Neo4j, MinIO, PostgreSQL (auth/user/product/cart/order/payment/notification)

Chờ tất cả healthy (30-60s):
```powershell
docker compose --profile infra ps
```

#### Bước 3.2.2 — Start Java Microservices

```powershell
# Vẫn ở thư mục BE/
docker compose --profile app up -d --build
```

Bao gồm: api-gateway (:3000), auth-service (:8081), user-service (:8082), product-service (:8083), cart-service (:8086), order-service (:8087), payment-service (:8088), notification-service (:8089)

#### Bước 3.2.3 — Start Python AI Service (terminal riêng)

```powershell
cd "MSS301-AI-Service-real/AI-stylist-recommendation"

# Lần đầu: init data vào Neo4j và Qdrant
uv run python scripts/init_concepts.py
uv run python scripts/init_graphdb.py --clear
uv run python scripts/init_qdrant.py --recreate

# Start service
uv run uvicorn ai_stylist.main:app --reload --host 0.0.0.0 --port 8000
```

#### Bước 3.2.4 — Start Frontend (terminal riêng)

```powershell
cd "MSS301-Stylemind/FE"
npm install        # chỉ cần lần đầu hoặc sau khi thay đổi package.json
npm run dev        # http://localhost:5173
```

---

## Bước 4 — Kiểm Tra Hoạt Động

```powershell
# 1. Gateway đang chạy
Invoke-RestMethod http://localhost:3000/actuator/health

# 2. Python AI service đang chạy
Invoke-RestMethod http://localhost:8000/health

# 3. Gateway → Python bridge (từ trong container)
docker exec stylemind-gateway curl -s http://host.docker.internal:8000/health

# 4. Frontend
# Mở browser: http://localhost:5173
```

---

## Dừng Stack

```powershell
# Dừng Java services (giữ nguyên volumes/data)
cd "MSS301-Stylemind/BE"
docker compose --profile app down

# Dừng infra
docker compose --profile infra down

# Hoặc dừng tất cả cùng lúc
docker compose --profile app --profile infra down
```

---

## Port Map

| Service | Port | Truy cập |
|---|---|---|
| **API Gateway** | `3000` | Entry point duy nhất cho FE và client |
| Frontend (dev) | `5173` | React Vite dev server |
| Python AI | `8000` | Chỉ local (gateway forward qua host.docker.internal) |
| auth-service | `8081` | Internal |
| user-service | `8082` | Internal |
| product-service | `8083` | Internal |
| cart-service | `8086` | Internal |
| order-service | `8087` | Internal |
| payment-service | `8088` | Internal |
| notification-service | `8089` | Internal |
| PostgreSQL (infra) | `5432` | Internal |
| PostgreSQL (AI) | `5433` | Local (AI service connect) |
| Redis | `6379` | Internal |
| Neo4j Browser | `7474` | http://localhost:7474 |
| Neo4j Bolt | `7687` | Internal |
| Qdrant | `6333` | Internal |
| MinIO Console | `9001` | http://localhost:9001 |

---

## Troubleshooting

### JWT / Auth không hoạt động
→ Kiểm tra `BE/.docker/certs/` có đủ 2 file `.pem` chưa.
→ Kiểm tra file `.pem` đúng cặp với nhau (không phải tự gen riêng).

### Gateway không forward được sang Python AI
→ Chạy: `docker exec stylemind-gateway curl http://host.docker.internal:8000/health`
→ Nếu fail trên Linux: kiểm tra `extra_hosts: host.docker.internal:host-gateway` trong `docker-compose.yml`.

### Service không start được (OOMKilled)
→ Docker Desktop cần ít nhất **8GB RAM** allocated.
→ Settings → Resources → Memory → tăng lên 8GB+.

### Frontend không gọi được API
→ Kiểm tra `FE/.env` có `VITE_API_BASE_URL=http://localhost:3000`.
→ Kiểm tra Gateway đang chạy: `docker ps | grep gateway`.
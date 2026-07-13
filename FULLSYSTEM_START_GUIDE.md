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

### 2.1. Khởi động toàn bộ hệ thống Backend

```bash
cd BE
docker compose -f docker-compose-separated.yml up -d
```

### 2.2. Danh sách Services & Ports

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

### 2.3. Infrastructure Components

| Component | Port | Mô tả |
|-----------|------|-------|
| PostgreSQL | 5433-5440 | 9 databases cho các services |
| Redis | 6379 | Caching & session storage |
| Qdrant | 6333 | Vector DB cho product search |
| Neo4j | 7474, 7687 | Graph DB cho fashion taxonomy |
| MinIO | 9000, 9001 | S3-compatible image storage |

### 2.4. Cơ chế Fail-Fast

**QUAN TRỌNG:** Hệ thống sử dụng cơ chế fail-fast để dễ debug:

- **KHÔNG có default values** trong `application.yml` cho JWT key paths
- Nếu environment variables thiếu hoặc sai, service sẽ **crash ngay khi boot**
- JWT keys bắt buộc phải được mount từ `.docker/certs/` directory
- RSA keys được generate tự động khi build Docker image

**Environment variables bắt buộc:**
- `JWT_PRIVATE_KEY_PATH` - Auth Service only
- `JWT_PUBLIC_KEY_PATH` - Tất cả services
- `NEO4J_URI`, `NEO4J_USERNAME`, `NEO4J_PASSWORD` - AI Agent Service only

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
docker compose -f docker-compose-separated.yml ps
```

### 4.4. Xem Logs của Service

```powershell
# Xem logs của auth service
docker compose -f docker-compose-separated.yml logs auth-service --tail 50

# Xem logs của tất cả services
docker compose -f docker-compose-separated.yml logs --tail 20
```

### 4.5. Vấn đề Thường Gặp

**Service không start:**
- Kiểm tra environment variables trong `docker-compose-separated.yml`
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
docker compose -f docker-compose-separated.yml down
```

### 5.2. Dừng Frontend

```bash
# Ctrl+C trong terminal đang chạy npm run dev
```

---

## 6. TÀI LIỆU THAM KHẢO

- [Backend Architecture](BE/README.md)
- [Frontend Documentation](FE/README.md)
- [Docker Configuration](BE/docker-compose-separated.yml)
- [JWT Implementation](docs/AGENT_WORKSPACE/ASYMMETRIC_JWT_IMPLEMENTATION_PLAN.md)

---

**Lưu ý:** Hướng dẫn này dành cho môi trường phát triển (development). Đối với production, cần cấu hình thêm security, monitoring, và scaling.

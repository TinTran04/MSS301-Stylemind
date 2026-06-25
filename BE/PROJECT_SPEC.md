# StyleMind Backend — Project Spec

## 1. Overview

**Tên sản phẩm:** StyleMind  
**Mã dự án:** MSS301-Stylemind  
**Loại:** E-commerce platform (thời trang)  
**Phạm vi BE:** Microservices backend chạy trên Docker Compose. FE tích hợp qua API Gateway port 3001.

**Out of scope (không implement):**
- Quản lý tồn kho / inventory tracking — hệ thống không theo dõi số lượng hàng tồn, không có inventory reservation trong order saga.

---

## 2. Services

### Infrastructure

| Service            | Port      | Mục đích                           |
|--------------------|-----------|-------------------------------------|
| stylemind-postgres | 5432      | PostgreSQL — 8 schemas riêng biệt  |
| stylemind-redis    | 6379      | Rate-limit (Gateway), cache         |
| stylemind-qdrant   | 6333/6334 | Vector store — AI semantic search  |
| stylemind-neo4j    | 7474/7687 | Graph DB — fashion rules (TBD)     |
| stylemind-minio    | 9000/9001 | S3-compatible — product images     |

### Application Services

| Service              | Port | Database      | Mục đích                          |
|----------------------|------|---------------|-----------------------------------|
| api-gateway          | 3001 | —             | Routing, JWT validate, rate-limit, CORS |
| auth-service         | 8081 | auth_db       | Login, register, JWT issue        |
| user-service         | 8082 | user_db       | Customer profile, addresses       |
| product-service      | 8083 | product_db    | Catalog, categories, images       |
| cart-service         | 8086 | cart_db       | Cart (guest + authenticated)      |
| order-service        | 8087 | order_db      | Orders, checkout flow             |
| payment-service      | 8088 | payment_db    | Transactions (COD + simulated)    |
| notification-service | 8089 | notification_db | Notification logs (stub)        |
| ai-agent-service     | 8085 | ai_db         | AI stylist chat, bundles, index   |

---

## 3. Admin Scope

### Đã có (implemented)

| Area            | Endpoints                                              | Service          |
|-----------------|--------------------------------------------------------|------------------|
| Products        | POST/PUT/DELETE `/api/products`, POST variants/images  | product-service  |
| Categories      | POST/PUT/DELETE `/api/categories`                      | product-service  |
| AI index jobs   | GET/retry `/api/admin/ai/index-jobs`                   | ai-agent-service |
| Notifications   | GET/POST/PUT `/api/notifications`                      | notification-service |

### Thiếu — cần implement

| Area              | Endpoints cần thêm                                   | Service       |
|-------------------|------------------------------------------------------|---------------|
| **User management** | `GET /api/admin/users` — danh sách users           | user-service  |
|                   | `GET /api/admin/users/{id}` — chi tiết user bất kỳ  | user-service  |
|                   | `PUT /api/admin/users/{id}/role` — đổi CUSTOMER/ADMIN | user-service |
|                   | `PUT /api/admin/users/{id}/enabled` — ban/unban      | user-service  |
| **Order management** | `GET /api/admin/orders` — tất cả orders (mọi user) | order-service |

---

## 4. Auth & Security Pattern

- **JWT** (HS256) validate tại Gateway → inject `X-User-Id`, `X-User-Roles`
- **X-Internal-Token** cho service-to-service qua Feign (`/internal/**` bị Gateway block với client)
- CORS: `DedupeResponseHeader` tại Gateway ngăn header trùng
- Tất cả services dùng chung `JWT_SECRET` (phải set env var; default fallback đã align)

---

## 5. Current State (cập nhật 2026-06-24)

Tất cả services UP, healthy:

```
api-gateway :3001 — CORS ok, routing ok, rate-limit on /api/ai-stylist/chat
auth-service :8081 — JWT issue + validate ok
user-service :8082 — customer profile/address ok
product-service :8083 — catalog ok; admin CRUD ok
cart-service :8086 — guest + auth cart ok; merge on login ok
order-service :8087 — order creation ok (saga partial — xem Known Issues)
payment-service :8088 — COD + online_simulated ok
notification-service :8089 — stub, log only
ai-agent-service :8085 — mock responses; vector search TBD
```

---

## 6. Known Issues / Tech Debt

| ID | Severity | Mô tả | File |
|----|----------|-------|------|
| H2 | High | Cart không được clear sau checkout — `mergeCart("")` là no-op vì cart-service không có "clear cart" endpoint | `order-service/.../OrderService.java:87` |
| H3 | High | Order saga thiếu compensation đầy đủ — payment thành công nhưng step sau fail → tiền đã trừ, cart không clear | `OrderService.java:67-90` |
| M3 | Medium | Giá tại order time đọc từ cart DTO (stale), chưa query authoritative từ product-service | `OrderService.java:109` |
| L4 | Low | Không có liveness/readiness probe riêng (chỉ có `/actuator/health` chung) | docker-compose.yml |

Xem chi tiết: `docs/ARCHITECTURE_AND_QUALITY_REVIEW.md`

---

## 7. Roadmap

### Sprint 0 — Infrastructure ✅
- [x] Bring services online
- [x] Verify gateway routes end-to-end + CORS fix
- [x] JWT secret align across all services
- [x] Feign timeout defaults (order-service, ai-agent-service)
- [ ] Clean obsolete compose warning (`version` field deprecated)

### Sprint 1 — Core Customer Flow (in progress)
- [x] FE auth (login/register) nối thật qua gateway
- [x] Product catalog nối thật
- [x] Cart (guest + auth) nối thật
- [ ] Checkout/order flow FE → BE end-to-end
- [ ] Order tracking FE
- [ ] Fix H2: thêm `DELETE /api/cart` endpoint + wire vào order-service

### Sprint 2 — Admin
- [ ] Admin user management (xem danh sách, ban, đổi role)
- [ ] Admin order management (xem tất cả orders)
- [ ] Swagger/OpenAPI accessible từ web (CORS cho `/v3/api-docs`)

### Phase 2 (TBD)
- [ ] AI stylist — Qdrant vector search thật
- [ ] Neo4j fashion graph rules
- [ ] Notification delivery (email/SMS)
- [ ] Inventory check integration (nếu cần)

---

## 8. Environment Variables

Các biến **bắt buộc** set trước khi deploy non-local:

| Variable                 | Dùng ở                    | Default (local only)                           |
|--------------------------|---------------------------|------------------------------------------------|
| `JWT_SECRET`             | Tất cả services           | `super-secure-stylemind-secret-key-signature-2026-xyz` |
| `X_INTERNAL_TOKEN`       | Feign calls giữa services | `sm-secret-internal-service-token-key-2026`    |
| `SPRING_DATASOURCE_PASSWORD` | Tất cả services       | `password`                                     |
| `S3_SECRET_KEY`          | product-service           | `password`                                     |
| `S3_ACCESS_KEY`          | product-service           | `admin`                                        |

> ⚠️ Default values là plain-text secrets — chỉ dùng local dev. Production phải set env vars thật.

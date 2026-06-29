# StyleMind

StyleMind là hệ thống thương mại điện tử thời trang theo kiến trúc microservice. Repository này là monorepo gồm frontend React/Vite và backend Java/Spring Boot.

Tài liệu dành cho developer mới:

- [Development Guide](docs/development.md)
- [Architecture](docs/architecture.md)
- [Auth Flow](docs/auth-flow.md)
- [API Contract](docs/api-contract.md)
- [Error Codes](docs/error-codes.md)
- [Local Infrastructure](docs/local-infrastructure.md)
- [Deployment Checklist](docs/deployment-checklist.md)

## Tổng Quan Kiến Trúc

```text
Browser / Frontend
        |
        v
API Gateway (Spring Cloud Gateway, WebFlux)
        |
        +--> Auth Service (Spring MVC, JPA, auth_db)
        |
        +--> User Profile Service (Spring MVC, JPA, user_profile_db)

Shared infrastructure:
- PostgreSQL per service database
- RabbitMQ for USER_REGISTERED events
- Redis for Gateway rate limiting
```

Frontend chỉ gọi API Gateway. Auth Service và User Profile Service không expose public khi chạy bằng Docker Compose.

## Trách Nhiệm Service

| Service | Port local | Public qua Docker | Trách nhiệm |
| --- | --- | --- | --- |
| `api-gateway` | `3000` | Có | Routing, JWT validation, authorization, request id, CORS, rate limiting, timeout, standardized gateway errors |
| `auth-service` | `8081` | Không | Register, login, refresh token lifecycle, logout, change password, email verification, forgot/reset password, account role/status, outbox events |
| `user-profile-service` (`BE/user-service`) | `8082` | Không | User profile, shipping addresses, consume `USER_REGISTERED`, processed event idempotency |
| `auth-postgres` | `5432` internal | Không | Database riêng của Auth Service: `auth_db` |
| `user-profile-postgres` | `5432` internal | Không | Database riêng của User Profile Service: `user_profile_db` |
| `rabbitmq` | `5672`, `15672` internal | Không | Event broker và DLQ |
| `redis` | `6379` internal | Không | Rate limiting/cache infrastructure |

Boundary quan trọng:

- Auth Service sở hữu `email`, `password_hash`, `role`, `account status`, `refresh token`.
- User Profile Service sở hữu `fullName`, `phone`, `avatarUrl`, `gender`, `dateOfBirth`, `address`.
- Không service nào truy cập database của service khác.
- Không tạo foreign key xuyên database.
- `USER_REGISTERED` không chứa password, phone, address hoặc refresh token.

## Yêu Cầu Môi Trường

- Git
- Node.js 18+ và npm
- Java 17
- Maven 3.9+
- Docker Desktop hoặc Docker Engine có Docker Compose v2
- OpenSSL, dùng để tạo JWT key pair local khi cần kiểm thử asymmetric JWT

## Clone Và Cấu Hình Environment

```powershell
git clone <repo-url>
cd MSS301-Stylemind

Copy-Item BE\.env.example BE\.env
Copy-Item BE\api-gateway\.env.example BE\api-gateway\.env
Copy-Item BE\auth-service\.env.example BE\auth-service\.env
Copy-Item BE\user-service\.env.example BE\user-service\.env
```

Các file `.env` chỉ dùng local và không được commit. Thay các giá trị `change-me-*` bằng secret local riêng của bạn.

## JWT Local

Local hiện đang dùng `JWT_SECRET` cho signing/verification mặc định. Gateway cũng có cấu hình đọc public key để chuẩn bị cho asymmetric JWT.

Tạo key pair local:

```bash
mkdir -p BE/secrets/local
openssl genrsa -out BE/secrets/local/jwt-private.pem 2048
openssl rsa -in BE/secrets/local/jwt-private.pem -pubout -out BE/secrets/local/jwt-public.pem
```

Khi service Auth được cấu hình asymmetric signing, dùng private key cho Auth và public key cho Gateway. Không commit thư mục `BE/secrets`.

Ví dụ biến môi trường Gateway:

```env
JWT_PUBLIC_KEY_PATH=/run/secrets/jwt-public.pem
```

Nếu chưa bật asymmetric signing trong Auth Service, giữ `JWT_SECRET` giống nhau giữa Auth Service và API Gateway.

## Chạy Docker Compose

```powershell
cd BE
docker compose --env-file .env up --build
```

Chạy nền:

```powershell
docker compose --env-file .env up -d --build
```

Dừng và giữ volume:

```powershell
docker compose down
```

Dừng và xóa dữ liệu local:

```powershell
docker compose down -v
```

## Migration

Backend dùng Flyway. Migration tự chạy khi service start vì `spring.flyway.enabled=true`.

Chạy riêng migration cho Auth bằng cách start service với database đang chạy:

```powershell
cd BE
docker compose --env-file .env up -d auth-postgres rabbitmq redis
mvn -pl auth-service -am spring-boot:run
```

Chạy riêng migration cho User Profile:

```powershell
cd BE
docker compose --env-file .env up -d user-profile-postgres rabbitmq redis
mvn -pl user-service -am spring-boot:run
```

## Chạy Từng Service

Khi chạy ngoài Docker, cần đảm bảo PostgreSQL/RabbitMQ/Redis đang chạy và env trỏ về host phù hợp.

```powershell
cd BE
mvn -pl api-gateway -am spring-boot:run
mvn -pl auth-service -am spring-boot:run
mvn -pl user-service -am spring-boot:run
```

Frontend:

```powershell
cd FE
npm install
npm run dev
```

## Test

Chạy toàn bộ backend test suite:

```powershell
cd BE
mvn test
```

Chạy ba service trong phạm vi Auth/Gateway/User Profile:

```powershell
cd BE
mvn -pl api-gateway,auth-service,user-service -am test
```

Frontend:

```powershell
cd FE
npm install
npm run build
```

## Swagger / OpenAPI

Khi chạy trực tiếp từng service:

- API Gateway: `http://localhost:3000/swagger-ui.html`
- Auth Service: `http://localhost:8081/swagger-ui.html`
- User Profile Service: `http://localhost:8082/swagger-ui.html`

Khi chạy Docker Compose mặc định, chỉ Gateway expose public. Auth/User Swagger không truy cập trực tiếp từ host trừ khi bạn tạm map port để debug local.

Static contract:

- `contracts/openapi/auth-service.yaml`
- `contracts/openapi/user-profile-service.yaml`
- `contracts/events/user-registered.schema.json`

## Health Endpoint

```powershell
curl http://localhost:3000/actuator/health
```

Nếu chạy service trực tiếp ngoài Docker:

```powershell
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

Trong Docker Compose, Auth/User là internal. Có thể kiểm tra qua container:

```powershell
docker compose exec auth-service curl -fsS http://localhost:8081/actuator/health
docker compose exec user-profile-service curl -fsS http://localhost:8082/actuator/health
```

## RabbitMQ Management UI

Docker Compose hiện chỉ expose RabbitMQ trong internal network. Để mở UI local khi debug, tạm map port `15672:15672` cho service `rabbitmq`, rồi mở:

```text
http://localhost:15672
```

User/password lấy từ `BE/.env`. Không dùng credential production cho local.

## Curl Nhanh

Register:

```bash
curl -i -X POST http://localhost:3000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"customer@example.com","password":"StrongPassword123!","fullName":"Nguyen Van A"}'
```

Login và lưu cookie refresh token:

```bash
curl -i -c cookies.txt -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"customer@example.com","password":"StrongPassword123!"}'
```

Refresh:

```bash
curl -i -b cookies.txt -c cookies.txt -X POST http://localhost:3000/api/auth/refresh
```

Get profile:

```bash
curl -i http://localhost:3000/api/users/me \
  -H "Authorization: Bearer <access-token>"
```

Create address:

```bash
curl -i -X POST http://localhost:3000/api/users/me/addresses \
  -H "Authorization: Bearer <access-token>" \
  -H "Content-Type: application/json" \
  -d '{"receiverName":"Nguyen Van A","receiverPhone":"+84901234567","province":"Ho Chi Minh","district":"District 1","ward":"Ben Nghe","streetAddress":"1 Le Loi"}'
```

## Lỗi Thường Gặp

- `401 INVALID_ACCESS_TOKEN`: token sai chữ ký, hết hạn, thiếu claim, hoặc dùng refresh token làm access token.
- `401 INVALID_CREDENTIALS`: email hoặc password sai. Auth cố tình không tiết lộ email có tồn tại hay không.
- `429 RATE_LIMIT_EXCEEDED`: Gateway rate limit đang chặn request. Chờ hết window hoặc reset Redis local.
- `PROFILE_NOT_FOUND`: profile chưa được tạo. Kiểm tra consumer `USER_REGISTERED`, RabbitMQ và outbox publisher.
- Service không connect database: kiểm tra `SPRING_DATASOURCE_URL`, user/password và container health.
- RabbitMQ không nhận event: kiểm tra exchange/queue/DLQ trong cấu hình `RABBITMQ_*`.
- CORS bị chặn: kiểm tra `CORS_ALLOWED_ORIGINS`, không dùng wildcard khi bật credentials.

Xem hướng dẫn chi tiết hơn trong [docs/development.md](docs/development.md).

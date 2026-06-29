# Development Guide

Tài liệu này giúp developer mới clone repo, cấu hình môi trường local, chạy service, chạy migration, chạy test và kiểm tra nhanh luồng Auth/Gateway/User Profile.

## 1. Repository Layout

```text
MSS301-Stylemind/
├── FE/                         # React 18 + Vite frontend
├── BE/                         # Maven multi-module backend
│   ├── api-gateway/            # Spring Cloud Gateway + WebFlux
│   ├── auth-service/           # Auth Service
│   ├── user-service/           # User Profile Service, container name user-profile-service
│   ├── common-lib/             # Shared config, exception, security, DTO
│   ├── docker-compose.yml      # Local infra and three-service stack
│   └── pom.xml                 # Maven parent
├── contracts/
│   ├── openapi/
│   └── events/
└── docs/
```

## 2. Architecture Summary

API Gateway là public entrypoint. Gateway xác thực JWT access token offline, xóa internal headers từ client, rồi thêm trusted headers cho downstream:

- `X-User-Id`
- `X-User-Role`
- `X-Request-Id`
- `X-Internal-Request`

Auth Service sở hữu tài khoản và token. User Profile Service sở hữu profile/address. Hai service có database riêng và giao tiếp bất đồng bộ qua RabbitMQ event `USER_REGISTERED`.

## 3. Prerequisites

- Git
- Java 17
- Maven 3.9+
- Node.js 18+ và npm
- Docker Desktop hoặc Docker Engine + Docker Compose v2
- OpenSSL nếu cần tạo JWT key pair local

Kiểm tra nhanh:

```powershell
java -version
mvn -version
node -v
npm -v
docker version
docker compose version
```

## 4. Environment Files

Copy file mẫu:

```powershell
Copy-Item BE\.env.example BE\.env
Copy-Item BE\api-gateway\.env.example BE\api-gateway\.env
Copy-Item BE\auth-service\.env.example BE\auth-service\.env
Copy-Item BE\user-service\.env.example BE\user-service\.env
```

Không commit `.env` hoặc secret thật.

Biến quan trọng:

| Biến | Dùng cho | Ghi chú |
| --- | --- | --- |
| `JWT_SECRET` | Auth, Gateway, User Profile | Local mặc định dùng HMAC secret giống nhau |
| `JWT_PUBLIC_KEY_PATH` | Gateway | Dùng khi verify asymmetric JWT |
| `INTERNAL_SERVICE_SECRET` / `INTERNAL_TOKEN` | Gateway và downstream | Chứng thực service-to-service |
| `SPRING_DATASOURCE_URL` | Auth/User | Mỗi service trỏ database riêng |
| `SPRING_RABBITMQ_*` | Auth/User/Gateway | RabbitMQ connection |
| `SPRING_DATA_REDIS_*` | Gateway | Redis rate limiting |
| `CORS_ALLOWED_ORIGINS` | Gateway | CSV origin, ví dụ `http://localhost:5173` |

## 5. JWT Key Pair Cho Local

Local mặc định đang dùng `JWT_SECRET`. Để chuẩn bị asymmetric JWT, tạo key pair:

```bash
mkdir -p BE/secrets/local
openssl genrsa -out BE/secrets/local/jwt-private.pem 2048
openssl rsa -in BE/secrets/local/jwt-private.pem -pubout -out BE/secrets/local/jwt-public.pem
```

Gateway có thể đọc public key:

```env
JWT_PUBLIC_KEY_PATH=/absolute/path/to/BE/secrets/local/jwt-public.pem
```

Lưu ý: Auth Service hiện vẫn dùng `JWT_SECRET` trong `common-lib` để ký token local. Chỉ bật public/private key khi Auth Service đã được cấu hình ký asymmetric tương ứng.

## 6. Docker Compose

Start toàn bộ stack local:

```powershell
cd BE
docker compose --env-file .env up --build
```

Start nền:

```powershell
docker compose --env-file .env up -d --build
```

Xem container:

```powershell
docker compose ps
```

Xem log:

```powershell
docker compose logs -f api-gateway
docker compose logs -f auth-service
docker compose logs -f user-profile-service
```

Stop:

```powershell
docker compose down
```

Reset sạch volume local:

```powershell
docker compose down -v
```

## 7. Local Ports

| Component | Host port mặc định | Docker public? | Ghi chú |
| --- | --- | --- | --- |
| API Gateway | `3000` | Có | Public entrypoint |
| Auth Service | `8081` | Không | Chỉ internal trong Compose; public nếu chạy trực tiếp |
| User Profile Service | `8082` | Không | Chỉ internal trong Compose; public nếu chạy trực tiếp |
| Auth PostgreSQL | `5432` internal | Không | `auth_db` |
| User Profile PostgreSQL | `5432` internal | Không | `user_profile_db` |
| RabbitMQ AMQP | `5672` internal | Không | Broker |
| RabbitMQ UI | `15672` internal | Không | Tạm expose khi debug |
| Redis | `6379` internal | Không | Rate limit |
| Frontend Vite | `5173` | N/A | Khi chạy `npm run dev` |

## 8. Migration

Auth/User dùng Flyway. Migration tự chạy khi service start.

Migration files:

- `BE/auth-service/src/main/resources/db/migration`
- `BE/user-service/src/main/resources/db/migration`

Chạy migration Auth bằng startup:

```powershell
cd BE
docker compose --env-file .env up -d auth-postgres rabbitmq redis
mvn -pl auth-service -am spring-boot:run
```

Chạy migration User Profile bằng startup:

```powershell
cd BE
docker compose --env-file .env up -d user-profile-postgres rabbitmq redis
mvn -pl user-service -am spring-boot:run
```

Validation migration được thực hiện bởi `spring.flyway.validate-on-migrate=true`.

## 9. Run Services Individually

Gateway:

```powershell
cd BE
mvn -pl api-gateway -am spring-boot:run
```

Auth:

```powershell
cd BE
mvn -pl auth-service -am spring-boot:run
```

User Profile:

```powershell
cd BE
mvn -pl user-service -am spring-boot:run
```

Frontend:

```powershell
cd FE
npm install
npm run dev
```

## 10. Tests

Full backend:

```powershell
cd BE
mvn test
```

Scope Auth/Gateway/User Profile:

```powershell
cd BE
mvn -pl api-gateway,auth-service,user-service -am test
```

Single module:

```powershell
cd BE
mvn -pl auth-service -am test
mvn -pl user-service -am test
mvn -pl api-gateway -am test
```

Frontend build check:

```powershell
cd FE
npm install
npm run build
```

## 11. Swagger / OpenAPI

Runtime Swagger UI khi chạy service trực tiếp:

- Gateway: `http://localhost:3000/swagger-ui.html`
- Auth: `http://localhost:8081/swagger-ui.html`
- User Profile: `http://localhost:8082/swagger-ui.html`

OpenAPI static contracts:

- `contracts/openapi/auth-service.yaml`
- `contracts/openapi/user-profile-service.yaml`

Trong Docker Compose mặc định, chỉ Gateway public. Auth/User Swagger không expose ra host.

## 12. RabbitMQ Management UI

Compose hiện chỉ expose RabbitMQ trong internal network. Khi cần debug UI, tạm thêm mapping:

```yaml
ports:
  - "15672:15672"
```

Sau đó mở:

```text
http://localhost:15672
```

Credential local nằm trong `BE/.env`.

## 13. Health Checks

Gateway:

```powershell
curl http://localhost:3000/actuator/health
```

Auth/User khi chạy trực tiếp:

```powershell
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

Auth/User trong Docker:

```powershell
cd BE
docker compose exec auth-service curl -fsS http://localhost:8081/actuator/health
docker compose exec user-profile-service curl -fsS http://localhost:8082/actuator/health
```

## 14. Example Curl Flow

Set base:

```bash
API=http://localhost:3000
```

Register:

```bash
curl -i -X POST "$API/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"email":"customer@example.com","password":"StrongPassword123!","fullName":"Nguyen Van A"}'
```

Login:

```bash
curl -i -c cookies.txt -X POST "$API/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"customer@example.com","password":"StrongPassword123!"}'
```

Copy `data.accessToken` from the response:

```bash
TOKEN="<access-token>"
```

Refresh using HttpOnly cookie saved by curl:

```bash
curl -i -b cookies.txt -c cookies.txt -X POST "$API/api/auth/refresh"
```

Get profile:

```bash
curl -i "$API/api/users/me" \
  -H "Authorization: Bearer $TOKEN"
```

Patch profile:

```bash
curl -i -X PATCH "$API/api/users/me" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Nguyen Van A","phone":"+84901234567","avatarUrl":"https://example.com/avatar.png","gender":"MALE","dateOfBirth":"1995-01-01"}'
```

Create address:

```bash
curl -i -X POST "$API/api/users/me/addresses" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"receiverName":"Nguyen Van A","receiverPhone":"+84901234567","province":"Ho Chi Minh","district":"District 1","ward":"Ben Nghe","streetAddress":"1 Le Loi"}'
```

Create second address:

```bash
curl -i -X POST "$API/api/users/me/addresses" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"receiverName":"Nguyen Van A","receiverPhone":"+84907654321","province":"Ha Noi","district":"Hoan Kiem","ward":"Hang Bac","streetAddress":"2 Hang Bac"}'
```

Set default:

```bash
curl -i -X PUT "$API/api/users/me/addresses/<addressId>/default" \
  -H "Authorization: Bearer $TOKEN"
```

Logout:

```bash
curl -i -b cookies.txt -c cookies.txt -X POST "$API/api/auth/logout" \
  -H "Authorization: Bearer $TOKEN"
```

## 15. Troubleshooting

### Docker Compose báo unhealthy

Kiểm tra log:

```powershell
cd BE
docker compose logs auth-postgres
docker compose logs user-profile-postgres
docker compose logs rabbitmq
docker compose logs redis
```

Nếu schema/volume cũ gây lỗi:

```powershell
docker compose down -v
docker compose --env-file .env up --build
```

### `PROFILE_NOT_FOUND` sau register

Profile được tạo từ RabbitMQ event `USER_REGISTERED`. Kiểm tra:

- Auth outbox publisher đang bật: `AUTH_OUTBOX_PUBLISHER_ENABLED=true`
- RabbitMQ healthy
- User Profile consumer đang bật: `USER_PROFILE_EVENTS_CONSUMER_ENABLED=true`
- Queue `user-profile.user-registered` không bị kẹt

### `INVALID_ACCESS_TOKEN`

Nguyên nhân thường gặp:

- Access token hết hạn.
- Dùng refresh token ở header `Authorization`.
- `JWT_SECRET` giữa Auth và Gateway không giống nhau.
- Gateway đang cấu hình public key nhưng Auth vẫn ký bằng HMAC secret.

### `RATE_LIMIT_EXCEEDED`

Gateway dùng Redis để rate limit login/register/forgot/reset/refresh. Reset Redis local:

```powershell
cd BE
docker compose restart redis
```

### CORS bị chặn

Kiểm tra `CORS_ALLOWED_ORIGINS`. Không dùng wildcard `*` khi `CORS_ALLOW_CREDENTIALS=true`.

### Không mở được RabbitMQ UI

Mặc định UI không public. Tạm expose `15672:15672` trong `BE/docker-compose.yml` khi debug local, rồi restart RabbitMQ.

### Swagger Auth/User không mở khi chạy Docker

Đúng theo thiết kế: Auth/User chỉ internal. Chạy service trực tiếp hoặc tạm map port khi cần debug.

## 16. Useful Commands

Validate Docker Compose:

```powershell
cd BE
docker compose --env-file .env config
```

Build backend modules:

```powershell
cd BE
mvn -DskipTests package
```

Run selected integration tests:

```powershell
cd BE
mvn -pl api-gateway,auth-service,user-service -am test
```

# Implementation Plan - Auth, API Gateway, User Profile

## 1. Hien trang repository

Repository hien tai la monorepo gom `FE/`, `BE/` va `docs/`.

Frontend:
- `FE/` la ung dung React 18 + Vite 5, dung JavaScript, React Router, Zustand, Axios, Tailwind CSS v4, Recharts, Lucide React va Framer Motion.
- Package manager la npm, co `package-lock.json`.
- Da co `apiClient.js` va `endpoints.js` tro ve API Gateway mac dinh `http://localhost:3000/api`.
- Nhieu feature API hien van tra mock data tu `FE/src/data` va `FE/src/features/**/*.api.js`; frontend chua duoc noi day du voi backend thuc te.

Backend:
- `BE/` la Maven multi-module Spring Boot 3.2.5, Java 17, Spring Cloud 2023.0.1.
- Parent Maven hien khai bao cac module: `common-lib`, `api-gateway`, `auth-service`, `user-service`, `product-service`, `cart-service`, `order-service`, `payment-service`, `notification-service`.
- `ai-agent-service` co folder, source, Dockerfile va `pom.xml`, nhung chua nam trong `<modules>` cua `BE/pom.xml`.
- `inventory_db` duoc tao trong script database va Dockerfiles co copy `inventory-service/pom.xml`, nhung khong co folder `BE/inventory-service`.
- Khong tim thay file test theo pattern pho bien `*Test.java`, `*Tests.java`, `*.test.*`, `*.spec.*`.

Build, Docker, migration:
- Backend build bang Maven.
- Frontend build bang Vite qua `npm run build`.
- Docker Compose trong `BE/docker-compose.yml` khai bao Postgres, Redis, Qdrant, Neo4j, MinIO va cac service backend.
- Tat ca Dockerfile backend dang copy `inventory-service/pom.xml` trong khi folder nay khong ton tai, nen Docker build co kha nang fail ngay o buoc copy.
- `BE/init-scripts` tao database rieng cho tung service va tao schema thuc te khong co FK xuyen database cho cac bang chinh nhu `user_db`, `cart_db`, `order_db`, `payment_db`, `notification_db`.
- Mot so tai lieu cu nhu `MICROSERVICE_ARCHITECTURE.md`, `DEPLOYMENT_GUIDE.md`, `DOCKER_DB_SCHEMA_TESTING.md`, `DATA_MODEL_DOCUMENTATION` van mo ta FK xuyen DB nhu `REFERENCES users(id)` hoac `REFERENCES product_variants(id)`. Diem nay khong phu hop boundary bat buoc.

Framework, ORM, database, broker, convention:
- Framework backend: Spring Boot, Spring MVC cho business services, Spring Cloud Gateway/WebFlux cho API Gateway.
- ORM: Spring Data JPA/Hibernate, entity/repository/service/controller theo convention Spring.
- Database chinh: PostgreSQL 15, chia database rieng theo service.
- AI/search/storage infrastructure: Qdrant, Neo4j, MinIO/S3-compatible storage.
- Cache/rate limit: Redis 7, hien duoc dung trong API Gateway cho AI chat rate limit.
- Message broker: chua co Kafka/RabbitMQ/AMQP trong dependency hoac Docker Compose. Event-driven broker chi nam trong future scope cua docs.
- Service-to-service: Spring Cloud OpenFeign trong mot so service, them `X-Internal-Token` qua `FeignClientConfig`.

Service va thu muc hien co trong `BE/`:
- `api-gateway`
- `auth-service`
- `user-service`
- `product-service`
- `cart-service`
- `order-service`
- `payment-service`
- `notification-service`
- `ai-agent-service`
- `common-lib`
- `init-scripts`

Ba service trong pham vi phu trach:
- Auth Service: da duoc tao tai `BE/auth-service`.
- API Gateway: da duoc tao tai `BE/api-gateway`.
- User Profile Service: da duoc tao tai `BE/user-service`.

## 2. Kien truc dang co

Kien truc tong the dang huong toi microservices:
- Frontend goi API Gateway.
- API Gateway route den tung service backend.
- Moi service co database Postgres rieng.
- Cac service chia package theo `controller`, `service`, `repository`, `entity`, `dto`, `feign`.
- `common-lib` chia se DTO response, exception handler, security, JWT utility, JPA auditing, Feign interceptor va utility chung.

Auth Service hien co:
- Entity `User` map bang `users`.
- Cac truong hien co: `id`, `email`, `password_hash`, `full_name`, `provider`, `provider_id`, `role`, `enabled`, timestamps.
- API hien co:
  - `POST /api/auth/login`
  - `POST /api/auth/register`
  - `GET /api/auth/me`
  - `POST /api/auth/logout`
- Chuc nang da co: register, login bang email/password, BCrypt password encoder, JWT access token, get current user, logout stateless.
- Chuc nang con thieu so voi boundary:
  - Chua co refresh token persistence/rotation/revocation.
  - Chua co account status ro rang ngoai boolean `enabled`.
  - Chua co endpoint quan tri khoa/mo tai khoan.
  - Chua co SSO implementation du provider/provider_id ton tai trong schema.
  - `full_name` dang nam trong Auth, trong khi boundary moi yeu cau User Profile Service so huu.

API Gateway hien co:
- Spring Cloud Gateway dung route trong ca Java `ApiGatewayApplication` va YAML `application.yml`.
- Co JWT global filter doc Authorization Bearer, extract `userId`, `role`, `email`, gan header downstream `X-User-Id`, `X-User-Roles`, `X-User-Email`.
- Co Redis rate limit filter cho `/api/ai-stylist/chat`, gioi han 5 request/phut theo userId hoac IP.
- Co route block `/internal/**`.
- Chuc nang da co: routing khai bao, authentication filter, request id cho request co auth, rate limiting AI chat.
- Chuc nang con thieu/chua on dinh:
  - RBAC theo route chua ro; filter moi xac thuc token, chua enforce role ADMIN/CUSTOMER theo path.
  - Route config bi duplicate giua Java bean va YAML.
  - Gateway dung `StripPrefix=1`, nhung downstream controllers cung map `/api/...`; request qua gateway co the bi forward sai path.
  - `PUBLIC_PATHS` cho phep `/api/auth/me`, trong khi endpoint nay can principal.
  - Dung `lb://service-name` va bien Eureka, nhung Docker Compose khong co `discovery-service` va cac service khong thay dependency Eureka client. Neu khong co discovery/static service instances, gateway co the khong resolve duoc service.
  - Header strip dang set `X-User-Id` va `X-User-Roles` thanh chuoi rong truoc khi gan lai, chua phai cach remove header ro rang.

User Profile Service hien co:
- Entity `CustomerStyleProfile` map bang `customer_style_profiles`.
- Entity `DeliveryAddress` map bang `delivery_addresses`.
- API hien co:
  - `GET /api/users/profile`
  - `PUT /api/users/profile`
  - `GET /api/users/addresses`
  - `POST /api/users/addresses`
  - `PUT /api/users/addresses/{addressId}`
  - `DELETE /api/users/addresses/{addressId}`
- Chuc nang da co: tao profile mac dinh khi GET/PUT neu chua co, cap nhat style profile, CRUD dia chi, dam bao address ownership bang `userId`.
- Chuc nang con thieu so voi boundary:
  - Chua co bang/profile entity so huu `full_name`, `phone`, `avatar`, `date_of_birth`.
  - `phone` hien chi la `phone_number` trong delivery address, chua phai phone cua profile nguoi dung.
  - Chua co avatar storage/presigned URL.
  - Chua co endpoint `GET/PUT /api/users/me` hoac tuong duong cho profile identity.
  - Chua co admin endpoint xem danh sach user/profile trong pham vi User Profile.

## 3. Kien truc muc tieu

Boundary bat buoc:
- Auth Service so huu duy nhat: `email`, `password_hash`, `role`, `account_status`, `refresh_token`.
- User Profile Service so huu duy nhat: `full_name`, `phone`, `avatar`, `date_of_birth`, `address`.
- API Gateway chi lam: routing, authentication, authorization, rate limiting, request processing.
- Khong service nao truy cap truc tiep database cua service khac.
- Khong tao foreign key xuyen database.

Muc tieu cho Auth Service:
- `auth_db` chi luu thong tin dinh danh va truy cap.
- Bang `users` nen duoc dieu chinh ve boundary: `id`, `email`, `password_hash`, `role`, `account_status`, `provider`, `provider_id`, timestamps.
- Refresh token nen co bang rieng trong `auth_db`, vi du `refresh_tokens`, gom token hash, user id, expiry, revoked flag, created/revoked metadata.
- API public: register, login, refresh token.
- API protected: logout, revoke refresh token, current auth identity neu can.
- Response auth co the tra `userId`, `email`, `role`, `accountStatus`, nhung khong so huu/tra `fullName` nhu source of truth.

Muc tieu cho User Profile Service:
- `user_db` luu profile identity va dia chi, khong FK sang `auth_db`.
- Can bo sung entity/bang user profile, vi du `user_profiles` gom `user_id`, `full_name`, `phone`, `avatar_url`, `date_of_birth`, timestamps.
- `customer_style_profiles` giu phan style/measurement.
- `delivery_addresses` giu dia chi va phone nguoi nhan hang.
- Khi can xac minh user ton tai, goi Auth Service qua API noi bo hoac tin vao identity da duoc Gateway xac thuc, khong query `auth_db`.

Muc tieu cho API Gateway:
- La entrypoint duy nhat cua frontend.
- Chuan hoa mot convention path:
  - Hoac Gateway giu `/api` va downstream controllers map `/api/...`, khong `StripPrefix=1`.
  - Hoac Gateway strip `/api` va downstream controllers map `/{resource}`. Chi chon mot convention.
- Gateway tao/propagate `X-Request-Id` cho tat ca request.
- Gateway remove header identity tu client truoc khi tu gan header tin cay cho downstream.
- Gateway enforce RBAC theo route, vi du `/api/admin/**` can ADMIN.
- Gateway block public `/internal/**`.
- Redis dung cho rate limit. Neu can service discovery, them discovery service/Eureka client dung cach; neu khong, route truc tiep qua Docker DNS URL nhu `http://auth-service:8081`.

## 4. Danh sach task theo thu tu trien khai

1. Chot convention routing giua Gateway va service controllers.
   - Quyet dinh giu `/api` trong downstream hay strip tai gateway.
   - Loai bo route duplicate: chon Java `RouteLocator` hoac YAML, khong duy tri ca hai.

2. Sua nen build/deploy truoc khi implement business.
   - Dong bo `BE/pom.xml` voi cac folder service thuc te.
   - Quyet dinh dua `ai-agent-service` vao modules hoac tach khoi parent.
   - Xu ly reference `inventory-service/pom.xml` trong Dockerfiles hoac tao module inventory neu that su can.
   - Dong bo `docker-compose.yml` voi co che route: discovery service hay direct URLs.

3. Dieu chinh data ownership trong migration/docs truoc khi code tiep.
   - Loai `full_name` khoi Auth target schema.
   - Bo moi FK xuyen database trong tai lieu schema.
   - Giu cross-service references duoi dang id string, validation qua API/service contract.

4. Auth Service phase 1.
   - Chuan hoa schema Auth theo boundary: email, password, role, account status.
   - Them refresh token model va endpoint refresh/logout revoke.
   - Chuan hoa response login/register de khong so huu profile fields.
   - Them test cho register/login/refresh/logout/me.

5. User Profile Service phase 1.
   - Them `user_profiles` cho full name, phone, avatar, date of birth.
   - Cap nhat API profile identity rieng voi style profile.
   - Giu address CRUD trong User Profile Service.
   - Them validation ownership theo `X-User-Id`/principal tu Gateway.
   - Them test cho profile identity, style profile, address CRUD.

6. API Gateway phase 1.
   - Fix route path convention.
   - Fix public/protected paths, dac biet `/api/auth/me`.
   - Them authorization theo role cho route admin/customer.
   - Remove incoming identity headers dung cach truoc khi gan header moi.
   - Dam bao request id duoc tao cho ca public request.
   - Them integration test hoac smoke test route gateway den auth/user.

7. Tach common-lib thanh cac concern nho hon hoac giam auto-config coupling.
   - Security config hien can `UserDetailsService`, nhung chi Auth Service co implementation.
   - Cac service downstream nen validate JWT/header theo cach phu hop ma khong can query user repository cua Auth.
   - Can tranh common-lib keo web + JPA + security vao moi service neu service khong can.

8. Noi frontend tung buoc.
   - Doi `auth.api.js` tu mock sang API Gateway sau khi Auth/Gateway on dinh.
   - Doi `profile.api.js` sang User Profile API sau khi boundary moi co schema/API.
   - Giu cac feature ngoai pham vi tiep tuc mock cho den khi service tuong ung san sang.

9. Them verification pipeline.
   - Backend: Maven compile/test theo module.
   - Frontend: lint/build.
   - Docker: build tung service va compose infrastructure.

## 5. Rui ro hoac diem chua nhat quan

- Boundary mismatch: Auth Service hien dang so huu `full_name`, trai voi boundary moi yeu cau User Profile Service so huu.
- Refresh token mismatch: config co `refresh-token-expiration`, `JwtUtil` co ham generate refresh token, nhung Auth Service chua luu/tra/rotate/revoke refresh token.
- Gateway path mismatch: `StripPrefix=1` lam `/api/auth/login` thanh `/auth/login`, trong khi Auth Controller map `/api/auth/login`.
- Gateway route duplication: routes ton tai ca trong Java bean va `application.yml`, de lech behavior.
- Gateway discovery mismatch: su dung `lb://...` va bien Eureka, nhung Docker Compose khong co `discovery-service`; chua thay Eureka client dependency trong business services.
- Common security coupling: `common-lib` khai bao `SecurityConfig` va `JwtAuthenticationFilter` can `UserDetailsService`; chi `auth-service` implement `UserDetailsService`. Cac service khac scan `com.stylemind.common` co rui ro fail startup hoac coupling vao Auth model.
- Docker build risk: Dockerfiles copy `inventory-service/pom.xml` nhung folder khong ton tai.
- Parent module mismatch: `ai-agent-service` co source nhung chua nam trong parent Maven modules.
- Docs mismatch: mot so docs van mo ta FK xuyen database, trong khi boundary bat buoc cam dieu nay.
- API docs mismatch: API contract dung `full_name` trong Auth response; can doi sang profile source of truth.
- Test gap: khong thay automated tests hien tai cho backend/frontend.
- Gateway public path risk: `/api/auth/me` dang nam trong public paths nhung controller can authenticated principal.
- Header trust risk: Gateway dang set identity headers thanh empty string thay vi remove ro rang; downstream can chi tin header do Gateway tao.
- Admin route risk: `/api/admin/**` hien route ve `product-service`, trong khi docs co admin user/order/AI endpoints thuoc service khac.
- Data side effect risk: `GET /api/users/profile` tao default profile neu chua co; can can nhac vi GET nen idempotent va khong tao side effect bat ngo.

## 6. Gia dinh duoc su dung

- Muc tieu gan nhat la chuan bi Auth Service, API Gateway va User Profile Service cho microservice deployment, chua trien khai feature moi trong buoc nay.
- PostgreSQL van la database chinh cho Auth/User Profile.
- Redis duoc giu cho Gateway rate limit.
- Chua dua Kafka/RabbitMQ vao MVP vi repository hien khong co broker dependency/runtime.
- Frontend se tiep tuc duoc migrate tu mock API sang API Gateway theo tung feature.
- Service-to-service chi duoc phep qua HTTP/OpenFeign/internal API, khong query database cua service khac.
- Cross-service references nhu `user_id`, `variant_id`, `bundle_id` chi la id tham chieu logic, khong FK xuyen database.
- Boundary moi cua nguoi phu trach uu tien hon cac docs cu neu co mau thuan.

## Danh sach file da doc quan trong

- `README.md`
- `FE/package.json`
- `FE/package-lock.json`
- `FE/vite.config.js`
- `FE/.env.example`
- `FE/src/services/apiClient.js`
- `FE/src/services/endpoints.js`
- `FE/src/features/auth/auth.api.js`
- `FE/src/features/profile/profile.api.js`
- `BE/pom.xml`
- `BE/docker-compose.yml`
- `BE/*/pom.xml`
- `BE/*/Dockerfile`
- `BE/init-scripts/00-create-databases.sh`
- `BE/init-scripts/01-auth-db.sql`
- `BE/init-scripts/02-user-db.sql`
- `BE/init-scripts/03-product-db.sql`
- `BE/init-scripts/05-cart-db.sql`
- `BE/init-scripts/06-order-db.sql`
- `BE/init-scripts/07-payment-db.sql`
- `BE/init-scripts/08-ai-db.sql`
- `BE/init-scripts/09-notification-db.sql`
- `BE/auth-service/src/main/java/com/stylemind/auth/AuthServiceApplication.java`
- `BE/auth-service/src/main/java/com/stylemind/auth/controller/AuthController.java`
- `BE/auth-service/src/main/java/com/stylemind/auth/service/AuthService.java`
- `BE/auth-service/src/main/java/com/stylemind/auth/entity/User.java`
- `BE/auth-service/src/main/java/com/stylemind/auth/repository/UserRepository.java`
- `BE/auth-service/src/main/java/com/stylemind/auth/dto/RegisterRequest.java`
- `BE/auth-service/src/main/resources/application.yml`
- `BE/api-gateway/src/main/java/com/stylemind/gateway/ApiGatewayApplication.java`
- `BE/api-gateway/src/main/java/com/stylemind/gateway/filter/JwtAuthenticationFilter.java`
- `BE/api-gateway/src/main/java/com/stylemind/gateway/filter/RateLimitFilter.java`
- `BE/api-gateway/src/main/resources/application.yml`
- `BE/user-service/src/main/java/com/stylemind/user/UserServiceApplication.java`
- `BE/user-service/src/main/java/com/stylemind/user/controller/UserProfileController.java`
- `BE/user-service/src/main/java/com/stylemind/user/service/UserProfileService.java`
- `BE/user-service/src/main/java/com/stylemind/user/entity/CustomerStyleProfile.java`
- `BE/user-service/src/main/java/com/stylemind/user/entity/DeliveryAddress.java`
- `BE/user-service/src/main/java/com/stylemind/user/dto/StyleProfileRequest.java`
- `BE/user-service/src/main/java/com/stylemind/user/dto/DeliveryAddressRequest.java`
- `BE/user-service/src/main/resources/application.yml`
- `BE/common-lib/src/main/java/com/stylemind/common/config/SecurityConfig.java`
- `BE/common-lib/src/main/java/com/stylemind/common/security/JwtAuthenticationFilter.java`
- `BE/common-lib/src/main/java/com/stylemind/common/security/InternalAuthFilter.java`
- `BE/common-lib/src/main/java/com/stylemind/common/security/JwtUtil.java`
- `BE/common-lib/src/main/java/com/stylemind/common/feign/FeignClientConfig.java`
- `docs/DATA_MODEL_DOCUMENTATION`
- `docs/MICROSERVICE_ARCHITECTURE.md`
- `docs/API_CONTRACT.md`
- `docs/MIGRATION_ROADMAP.md`
- `docs/DEPLOYMENT_GUIDE.md`
- `docs/DOCKER_DB_SCHEMA_TESTING.md`
- `docs/PROJECT_ANALYSIS.md`

## File da tao hoac sua

- `docs/implementation-plan.md`

## Cac lenh nen chay de xac minh repository

```bash
# Kiem tra frontend
cd FE
npm install
npm run lint
npm run build

# Kiem tra backend parent modules
cd ../BE
mvn clean test

# Kiem tra rieng cac module trong pham vi phu trach
mvn clean test -pl common-lib,auth-service,api-gateway,user-service -am

# Kiem tra Docker Compose config
docker compose config

# Build thu cac image trong pham vi phu trach
docker compose build auth-service api-gateway user-service

# Chay infrastructure truoc
docker compose up -d postgres redis

# Kiem tra database da tao
docker exec -it stylemind-postgres psql -U postgres -d auth_db -c "\dt"
docker exec -it stylemind-postgres psql -U postgres -d user_db -c "\dt"
```

## Trang thai

Chua bat dau trien khai chuc nang. Tai lieu nay chi la buoc audit va lap ke hoach.

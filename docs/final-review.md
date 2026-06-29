# Final Review - Auth, API Gateway, User Profile

Ngay review: 2026-06-24

## Tinh nang da hoan thanh

- Auth Service co persistence rieng cho account, refresh token, email verification token, password reset token va transactional outbox.
- Auth Service da co register, login, refresh rotation, logout, logout-all, change-password, forgot/reset password, verify email va admin status/role endpoints.
- User Profile Service co profile co ban, address management, default address policy, processed events va consumer idempotent cho `USER_REGISTERED`.
- API Gateway route dung scope:
  - `/api/auth/**` den Auth Service.
  - `/api/admin/users/{userId}/status` va `/api/admin/users/{userId}/role` den Auth Service.
  - `/api/users/**` va admin user read endpoints den User Profile Service.
- API Gateway da xoa client-supplied internal headers va tu gan `X-Request-Id`, `X-User-Id`, `X-User-Role`, `X-Token-Id`, `X-Token-Type`.
- Response envelope chung da co `success`, `data`, `requestId` va nested `error.code/message`; cac truong legacy duoc giu lai de tranh break code cu.
- OpenAPI/Auth docs da duoc can chinh voi implementation hien tai:
  - Register tra account summary, khong phat token.
  - Login/refresh tra access token va `expiresInSeconds`; refresh token tra qua HttpOnly cookie.
  - JWT `type` dung lowercase: `access`, `refresh`, `password_reset`, `email_verification`.
- Spotless Maven plugin version da duoc sua tu `2.42.19` sang `2.43.0` de formatter/check resolve duoc.
- Gateway log security test da duoc on dinh hoa bang timeout test client dai hon, khong thay doi production filter.

## Test da chay

| Lenh | Ket qua |
| --- | --- |
| `mvn -pl api-gateway,auth-service,user-service -am spotless:apply` | PASS |
| `mvn -pl api-gateway,auth-service,user-service -am spotless:check` | PASS |
| `mvn -pl api-gateway,auth-service,user-service -am compile test-compile` | PASS |
| `mvn -pl api-gateway -Dtest=GatewayCrossCuttingFilterTest#accessLogDoesNotContainTokenPasswordOrCookie test` | PASS |
| `mvn -pl api-gateway,auth-service,user-service -am test` | PASS |
| `mvn -DskipTests package` | PASS |
| `mvn test` | PASS |
| `docker compose config` | PASS |
| `Get-Content contracts/events/user-registered.schema.json \| ConvertFrom-Json` | PASS |
| `git diff --check` | PASS, chi co canh bao CRLF tren Windows |
| `docker compose build api-gateway auth-service user-profile-service` | FAIL do Docker Desktop/Linux engine khong chay |

## Ket qua chi tiet

- Full backend `mvn test` da pass tren 10 module Maven. Cac module ngoai scope khong co test thi Maven bao `No tests to run`.
- Flyway validation/migration da duoc chay trong Spring test context:
  - Auth Service validate va migrate 2 migration.
  - User Profile Service validate va migrate 3 migration.
- Docker Compose static config hop le; chi Gateway expose public port `3000`.
- Docker image build chua chay duoc do loi moi truong:
  - `open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified`

## File chinh da sua

- `BE/pom.xml`
- `BE/common-lib/src/main/java/com/stylemind/common/dto/ApiResponse.java`
- `BE/api-gateway/src/main/resources/application.yml`
- `BE/api-gateway/src/main/java/com/stylemind/gateway/filter/JwtAuthenticationFilter.java`
- `BE/api-gateway/src/main/java/com/stylemind/gateway/support/GatewayHeaders.java`
- `BE/api-gateway/src/test/java/com/stylemind/gateway/filter/GatewayCrossCuttingFilterTest.java`
- `BE/auth-service/src/main/java/com/stylemind/auth/controller/AuthController.java`
- `BE/auth-service/src/main/java/com/stylemind/auth/controller/AdminAuthController.java`
- `BE/auth-service/src/main/java/com/stylemind/auth/service/AuthService.java`
- `BE/user-service/src/main/java/com/stylemind/user/controller/UserProfileController.java`
- `BE/user-service/src/main/java/com/stylemind/user/controller/AdminUserProfileController.java`
- `BE/user-service/src/main/java/com/stylemind/user/service/UserProfileService.java`
- `contracts/openapi/auth-service.yaml`
- `docs/api-contract.md`
- `docs/architecture.md`

## Van de con ton tai

- Docker image build chua duoc xac minh vi Docker daemon khong active trong moi truong hien tai.
- Chua co Checkstyle/PMD/SpotBugs rieng trong repo; `spotless:check`, compile va test dang la cac buoc lint/quality kha dung duoc hien tai.
- OpenAPI YAML chua duoc validate bang parser OpenAPI chuyen dung trong repo. JSON event schema da parse OK bang PowerShell.
- Auth/Gateway hien van dung HMAC JWT secret qua `JwtUtil`; architecture muc tieu uu tien asymmetric signing private/public key. Day la technical debt bao mat can tach rieng neu muon dat dung muc tieu production.
- `ApiResponse` van giu cac field legacy `message`, `errorCode`, `timestamp` ngoai envelope moi. Huu ich de tuong thich, nhung can don dep khi client da migrate.
- Test E2E hien la Spring integration/E2E-style tests trong Maven, chua phai black-box E2E tren Docker Compose that.
- Git bao canh bao LF se duoc chuyen sang CRLF tren Windows. Khong phai loi diff, nhung nen thong nhat `.gitattributes` neu team muon line ending on dinh.

## Technical debt

- Can chuyen JWT sang asymmetric key pair theo dung architecture production, dong bo `JWT_PUBLIC_KEY_PATH`/`JWT_PRIVATE_KEY_PATH`.
- Nen them OpenAPI validation tool vao CI de bat contract drift tu dong.
- Nen them Docker build/test job trong CI khi Docker daemon san sang.
- Nen disable `spring.jpa.open-in-view` cho service API neu khong can lazy rendering.
- Nen xem lai debug log cua Gateway test/application de giam log noise trong CI.

## Co y chua trien khai

- Khong them business domain ngoai Auth, Gateway va User Profile.
- Khong them service discovery moi, ORM moi, framework moi hoac shared database.
- Khong publish refresh token trong JSON response cho web flow; refresh token duoc gan bang HttpOnly cookie theo policy hien tai.
- Khong chay production-like Docker E2E vi Docker engine khong kha dung trong may hien tai.

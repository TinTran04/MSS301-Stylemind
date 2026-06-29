# Security Review - Auth Service, API Gateway, User Profile Service

Ngay review: 2026-06-23

Pham vi:

- `BE/auth-service`
- `BE/api-gateway`
- `BE/user-service`
- `BE/common-lib` cho cac security component dang duoc ba service dung chung
- `BE/docker-compose.yml`

Lenh da chay:

```powershell
cd BE
mvn test
```

Ket qua: `BUILD SUCCESS`.

## Tom tat thay doi da sua

- Gioi han public route cua Auth Service theo contract, khong con `permitAll("/api/auth/**")`.
- Them internal-header authentication cho request tu API Gateway xuong downstream service.
- JWT servlet filter kiem tra `type=access`, `jti`, `exp`, signature va dung `sub=userId`.
- Auth Service `loadUserByUsername` ho tro subject la account UUID, phu hop JWT payload hien tai.
- Giam ro ri error response: khong tra stack trace/framework exception message cho bad request, constraint violation va bad credentials.
- CORS common config khong con wildcard; Gateway chi expose `X-Request-Id`, khong expose internal user headers.
- Them rate limit cho `POST /api/auth/reset-password` theo IP va hash cua reset token.
- Legacy delivery address update/delete dung query theo ca `addressId` va `userId`.
- Loai bo default JWT/internal secret co dang secret that trong source, chuyen sang placeholder/env.

## Bang review chi tiet

| # | Issue | Severity | File lien quan | Cach sua / danh gia | Trang thai |
|---|---|---|---|---|---|
| 1 | Password co bi log hoac tra response khong | High | `AuthService.java`, `AuthResponse.java`, `RegisterResponse.java`, `GlobalExceptionHandler.java`, `AccessLogFilter.java` | Password chi duoc hash bang `PasswordEncoder`; response khong co `password_hash`. Error handler khong tra raw framework message; access log khong log body/header token. | Da sua/Dat |
| 2 | Refresh token co luu plain text khong | Critical | `AuthService.java`, `RefreshToken.java`, `RefreshTokenRepository.java` | Refresh token raw chi tra client qua HttpOnly cookie/body bi `@JsonIgnore`; DB chi luu `token_hash`. | Dat |
| 3 | JWT secret/private key co bi commit khong | High | `application.yml`, `.env.example`, `docker-compose.yml` | Da thay default `super-secure...` va `sm-secret...` bang placeholder `change-me...`; secret that phai doc tu environment/secret manager. | Da sua |
| 4 | JWT co kiem tra type, exp, signature khong | Critical | `JwtAccessTokenValidator.java`, `JwtAuthenticationFilter.java`, `JwtUtil.java` | Gateway verify signature offline va exp qua JJWT; kiem tra `type=access`, `sub`, `role`, `jti`. Common JWT filter cung kiem tra `type=access` va `jti`. | Da sua/Dat |
| 5 | Gateway co tin `X-User-Id`/`X-User-Role` tu client khong | Critical | `JwtAuthenticationFilter.java`, `GatewayHeaders.java` | Gateway xoa internal headers client gui, sau do tu them header tu JWT da verify. | Dat |
| 6 | Downstream co tin `X-User-Id`/`X-User-Role` gia mao khong | Critical | `InternalAuthFilter.java`, `UserProfileController.java` | `InternalAuthFilter` yeu cau `X-Internal-Token`/`X-Internal-Service-Secret` hop le khi co internal/user header, va tao `UserPrincipal` tin cay. | Da sua |
| 7 | User Profile co lay `userId` tu body khong | High | `UserProfileController.java`, `UserProfileRequest.java`, `AddressRequest.java` | `/api/users/me` lay user tu principal/internal context; request DTO chan unknown fields nhu `userId`, `email`, `role`. | Dat |
| 8 | Address ownership co enforce trong query khong | Critical | `AddressRepository.java`, `UserProfileService.java`, `DeliveryAddressRepository.java` | Address moi dung `findByIdAndUserIdForUpdate`; legacy delivery address update/delete da doi sang `findByIdAndUserId`. | Da sua |
| 9 | SQL injection/raw query khong an toan | High | Repository cua Auth/User Profile | Khong thay native SQL hoac string-concatenated query trong ba service; JPQL co bind param. Migration SQL static. | Dat |
| 10 | CORS qua rong | Medium | `SecurityConfig.java`, `api-gateway/application.yml` | Common CORS chuyen tu wildcard sang env `CORS_ALLOWED_ORIGINS`; Gateway chi allow origin cau hinh, khong wildcard voi credentials, chi expose `X-Request-Id`. | Da sua |
| 11 | Error response lo stack trace/noi bo | High | `GatewayErrorResponseWriter.java`, `GlobalExceptionHandler.java` | Gateway tra response chuan, khong stack trace. Common handler khong tra exception message cho bad request/constraint/bad credentials/generic. | Da sua |
| 12 | Rate limiting login/register/reset | Medium | `RateLimitFilter.java`, `GatewayRateLimitProperties.java`, `api-gateway/application.yml` | Da co login/register/forgot/refresh; bo sung reset-password theo IP va hash token identifier. Redis unavailable theo policy `fail-open`. | Da sua |
| 13 | Docker expose service noi bo | High | `BE/docker-compose.yml` | Chi `api-gateway` map public port. Auth, User Profile, PostgreSQL, RabbitMQ, Redis dung `expose` trong internal network. | Dat |
| 14 | Shared database/cross-database query | Critical | `docker-compose.yml`, migrations/repositories | Auth dung `auth_db`, User Profile dung `user_profile_db`; khong thay repository query DB service khac; khong foreign key xuyen DB. | Dat |
| 15 | Race condition email registration | High | `accounts` migration, `AuthService.java` | DB unique constraint tren `accounts.email`; service bat `DataIntegrityViolationException` tra `EMAIL_ALREADY_EXISTS`. | Dat |
| 16 | Race condition refresh rotation | High | `RefreshTokenRepository.java`, `AuthService.java` | `findByTokenHash` dung `PESSIMISTIC_WRITE`; token cu bi revoke trong transaction; reuse revoke token cua user. | Dat |
| 17 | Race condition default address | High | `AddressRepository.java`, `UserProfileService.java`, `db/postgresql/V3__enforce_single_default_address.sql` | Service lock address theo user khi set default; PostgreSQL partial unique index chan hon mot default address/user. | Dat |
| 18 | Race condition outbox publishing | Medium | `OutboxEventRepository.java`, `OutboxPublisherService.java` | Publisher lay PENDING bang pessimistic lock; chi mark PUBLISHED sau publish confirm; failure khong xoa event. | Dat |
| 19 | Refresh cookie flags | High | `AuthController.java`, `auth-service/application.yml` | Cookie `HttpOnly`, `SameSite=Strict`, `Path=/api/auth`; `Secure` doc tu `AUTH_REFRESH_TOKEN_COOKIE_SECURE`. Local default hien la `false`, production bat buoc set `true`. | Can cau hinh prod |
| 20 | Secret doc tu environment/secret manager | High | `application.yml`, `docker-compose.yml`, `.env.example` | Secret doc tu env var; placeholder khong phai secret that. Production can secret manager hoac env protected. | Da sua/Can cau hinh prod |
| 21 | Log mask authorization/cookies/password | High | `AccessLogFilter.java`, `JwtAuthenticationFilter.java`, `GlobalExceptionHandler.java`, `LogSafeEmailSender.java` | Access log chi log method/route/status/duration/userId. JWT failure log class/path, khong log token. Email sender dev khong log raw token. | Da sua/Dat |
| 22 | Auth protected endpoints bi permitAll | Critical | `SecurityConfig.java`, `AuthController.java` | Da thay `permitAll("/api/auth/**")` bang danh sach public endpoint theo contract; logout/logout-all/change-password/me can authentication. | Da sua |
| 23 | Auth Service JWT subject mismatch | High | `AuthService.java`, `JwtAuthenticationFilter.java`, `JwtUtil.java` | Token co `sub=userId`; Auth `loadUserByUsername` nay tim account theo UUID truoc, fallback email; `JwtUtil.validateToken` chap nhan `UserPrincipal.userId`. | Da sua |

## Ghi chu kien truc va rui ro con lai

- JWT signing hien tai van la HMAC secret theo convention repository. Gateway co ho tro public key validation, nhung Auth Service chua phat hanh asymmetric JWT. Neu yeu cau production la asymmetric signing, can them keypair/private-key signing rieng cho Auth Service trong mot prompt rieng.
- `AUTH_REFRESH_TOKEN_COOKIE_SECURE` dang co local default `false` de dev HTTP hoat dong. Moi moi truong production/staging HTTPS phai set `AUTH_REFRESH_TOKEN_COOKIE_SECURE=true`.
- Redis rate limit dang co policy `fail-open=true` theo config. Day la trade-off availability; neu muon strict security hon, set `GATEWAY_RATE_LIMIT_FAIL_OPEN=false`.
- `UserProfileController` van co fallback doc `X-User-Id`, nhung fallback nay nam sau `InternalAuthFilter`; request gia mao internal/user header khong co internal token se bi tu choi truoc controller.
- Cac service khac ngoai pham vi cung dung `common-lib`; thay doi common security da duoc verify bang `mvn test` toan bo backend.

## File quan trong da review

- `BE/auth-service/src/main/java/com/stylemind/auth/service/AuthService.java`
- `BE/auth-service/src/main/java/com/stylemind/auth/controller/AuthController.java`
- `BE/auth-service/src/main/resources/application.yml`
- `BE/auth-service/src/main/resources/db/migration/V1__create_auth_persistence.sql`
- `BE/api-gateway/src/main/java/com/stylemind/gateway/filter/JwtAuthenticationFilter.java`
- `BE/api-gateway/src/main/java/com/stylemind/gateway/filter/RateLimitFilter.java`
- `BE/api-gateway/src/main/java/com/stylemind/gateway/error/GatewayErrorResponseWriter.java`
- `BE/api-gateway/src/main/resources/application.yml`
- `BE/user-service/src/main/java/com/stylemind/user/controller/UserProfileController.java`
- `BE/user-service/src/main/java/com/stylemind/user/service/UserProfileService.java`
- `BE/user-service/src/main/java/com/stylemind/user/repository/AddressRepository.java`
- `BE/user-service/src/main/java/com/stylemind/user/repository/DeliveryAddressRepository.java`
- `BE/user-service/src/main/resources/db/postgresql/V3__enforce_single_default_address.sql`
- `BE/common-lib/src/main/java/com/stylemind/common/config/SecurityConfig.java`
- `BE/common-lib/src/main/java/com/stylemind/common/security/InternalAuthFilter.java`
- `BE/common-lib/src/main/java/com/stylemind/common/security/JwtAuthenticationFilter.java`
- `BE/common-lib/src/main/java/com/stylemind/common/security/JwtUtil.java`
- `BE/common-lib/src/main/java/com/stylemind/common/exception/GlobalExceptionHandler.java`
- `BE/docker-compose.yml`

## File da tao hoac sua trong buoc review nay

- `docs/security-review.md`
- `BE/common-lib/src/main/java/com/stylemind/common/config/SecurityConfig.java`
- `BE/common-lib/src/main/java/com/stylemind/common/security/InternalAuthFilter.java`
- `BE/common-lib/src/main/java/com/stylemind/common/security/JwtAuthenticationFilter.java`
- `BE/common-lib/src/main/java/com/stylemind/common/security/JwtUtil.java`
- `BE/common-lib/src/main/java/com/stylemind/common/exception/GlobalExceptionHandler.java`
- `BE/common-lib/src/main/java/com/stylemind/common/feign/FeignClientConfig.java`
- `BE/api-gateway/src/main/java/com/stylemind/gateway/config/GatewayRateLimitProperties.java`
- `BE/api-gateway/src/main/java/com/stylemind/gateway/filter/RateLimitFilter.java`
- `BE/api-gateway/src/main/resources/application.yml`
- `BE/auth-service/src/main/java/com/stylemind/auth/service/AuthService.java`
- `BE/auth-service/src/main/resources/application.yml`
- `BE/user-service/src/main/java/com/stylemind/user/repository/DeliveryAddressRepository.java`
- `BE/user-service/src/main/java/com/stylemind/user/service/UserProfileService.java`
- `BE/user-service/src/main/resources/application.yml`

## Lenh nen chay lai

```powershell
cd BE
mvn test
docker compose config
```

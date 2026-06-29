# Deployment Checklist

Checklist này dành cho môi trường staging/production. Không dùng secret local hoặc credential mẫu trong production.

## Secrets And Keys

- [ ] Tất cả `change-me-*` đã được thay bằng secret thật từ secret manager hoặc cơ chế quản lý secret của nền tảng deploy.
- [ ] `JWT_SECRET` production đủ dài và khác local nếu còn dùng HMAC.
- [ ] Production JWT key pair đã được tạo, private key chỉ cấp cho Auth Service.
- [ ] Gateway chỉ có public key hoặc verification secret cần thiết.
- [ ] Không commit private key, `.env`, database password, RabbitMQ password hoặc Redis password.
- [ ] `INTERNAL_SERVICE_SECRET` production đã được rotate và chỉ cấp cho Gateway/downstream service cần thiết.

## Cookie And Auth Security

- [ ] Refresh token cookie bật `HttpOnly`.
- [ ] Refresh token cookie bật `Secure` trong production.
- [ ] `SameSite` phù hợp với frontend deployment.
- [ ] Cookie `Path` giới hạn, hiện dùng `/api/auth`.
- [ ] Access token TTL ngắn, mặc định 10-15 phút.
- [ ] Refresh token rotation đã bật và được test.
- [ ] Logout/logout-all revoke refresh token đúng chính sách.

## CORS And Gateway

- [ ] `CORS_ALLOWED_ORIGINS` là whitelist domain production, không dùng wildcard khi bật credentials.
- [ ] Gateway expose public duy nhất.
- [ ] Auth Service và User Profile Service nằm trong private/internal network.
- [ ] Gateway xóa header do client giả mạo: `X-User-Id`, `X-User-Role`, `X-Request-Id`, `X-Internal-Request`.
- [ ] Gateway tự tạo/forward `X-Request-Id`.
- [ ] Gateway rate limiting kết nối Redis production.
- [ ] Timeout downstream phù hợp.
- [ ] Không retry tự động request POST không idempotent.

## Database

- [ ] Mỗi service dùng database riêng.
- [ ] Auth Service trỏ `auth_db`.
- [ ] User Profile Service trỏ `user_profile_db`.
- [ ] Không có foreign key xuyên database.
- [ ] Flyway migration đã chạy thành công trước khi nhận traffic.
- [ ] `spring.jpa.hibernate.ddl-auto=validate` trong production.
- [ ] Backup database đã cấu hình.
- [ ] Restore backup đã được test.
- [ ] Index quan trọng đã tồn tại: email unique, refresh token hash, outbox status, profile/address user id.

## RabbitMQ

- [ ] RabbitMQ production credential đã thay.
- [ ] Exchange `stylemind.events` tồn tại hoặc được service khai báo.
- [ ] Queue `user-profile.user-registered` tồn tại.
- [ ] DLQ `user-profile.user-registered.dlq` tồn tại.
- [ ] Retry policy và DLQ routing đã được kiểm tra.
- [ ] Publisher confirm bật cho Auth outbox publisher.
- [ ] Outbox event không chứa password, phone, address hoặc token.
- [ ] Consumer idempotency bằng `processed_events` đã bật.

## Redis

- [ ] Redis production reachable từ Gateway.
- [ ] Redis có auth/TLS nếu nền tảng hỗ trợ hoặc yêu cầu.
- [ ] Gateway rate-limit fail-open/fail-closed được quyết định rõ theo môi trường.
- [ ] Monitoring Redis memory/eviction được bật.

## Network And TLS

- [ ] HTTPS bắt buộc ở public ingress/load balancer.
- [ ] HTTP public được redirect sang HTTPS.
- [ ] Internal service network không expose Auth/User/PostgreSQL/RabbitMQ/Redis ra internet.
- [ ] Security group/firewall chỉ cho phép traffic cần thiết.
- [ ] RabbitMQ management UI không public hoặc được bảo vệ bằng VPN/allowlist.

## Observability

- [ ] Log có `requestId`, method, route, status, duration.
- [ ] Log không chứa password, access token, refresh token, cookie hoặc secret.
- [ ] Metrics actuator/prometheus được cấu hình theo policy.
- [ ] Health/readiness endpoint được platform kiểm tra.
- [ ] Alert cho service down, database down, RabbitMQ queue backlog, DLQ growth, Redis unavailable.
- [ ] Error response không lộ stack trace, URL nội bộ, database error hoặc secret.

## Release Validation

- [ ] `docker compose config` hoặc manifest/k8s validation pass.
- [ ] Backend compile/test pass: `mvn test`.
- [ ] Smoke test Auth qua Gateway pass: register/login/refresh/logout.
- [ ] Smoke test User Profile qua Gateway pass: GET/PATCH profile, create address.
- [ ] Customer không gọi được admin API.
- [ ] Token hết hạn/sai chữ ký bị 401.
- [ ] Client giả `X-User-Role: ADMIN` không bypass được Gateway.
- [ ] Duplicate `USER_REGISTERED` không tạo profile trùng.
- [ ] Concurrent refresh không tạo hai refresh token hợp lệ.
- [ ] Concurrent set-default không tạo hai default address.

## Rollback

- [ ] Có image/version trước đó để rollback.
- [ ] Migration được phân loại backward-compatible hoặc có kế hoạch rollback dữ liệu.
- [ ] Backup gần nhất đã được xác nhận trước deploy.
- [ ] Feature flag hoặc config switch đã chuẩn bị cho thay đổi rủi ro cao.

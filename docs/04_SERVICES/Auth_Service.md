# auth-service Specification

## 1. Overview

| Field | Value |
|---|---|
| Service | `auth-service` |
| Port | `8081` |
| Database | `auth_db` |
| Responsibility | Identity, credentials, access control, JWT issuing |

## 2. Owned Data

- Account
- Email
- Password hash
- Provider
- Role (`CUSTOMER`, `ADMIN`)
- Account status (`ACTIVE`, `DISABLED`)
- Password setup/reset token and OTP

The service generates `user_id` at registration and is its source of truth. It
does not store display name, style profile, preferences, or delivery addresses.

## 3. Main APIs

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/auth/register` | Đăng ký |
| POST | `/api/v1/auth/login` | Đăng nhập |
| GET | `/api/v1/auth/me` | Current user |
| POST | `/api/v1/auth/forgot-password` | Generic recovery response; gửi OTP nếu hợp lệ |
| POST | `/api/v1/auth/verify-reset-otp` | Xác thực OTP và cấp reset token một lần |
| POST | `/api/v1/auth/reset-password` | Đổi mật khẩu bằng reset token |

## 4. Architecture Layers

```text
controller
application/service
domain/entity
domain/repository
dto/request
dto/response
mapper
exception
infrastructure/client
infrastructure/config
```

## 5. Requirements

- API response phải theo format chuẩn.
- Validate request DTO.
- Không expose entity trực tiếp ra API.
- Có global exception handler.
- Có transaction boundary rõ ràng.
- Có unit/integration tests cho critical flow.
- Password, OTP, setup token, và reset token chỉ được lưu dưới dạng BCrypt hash.
- OTP/reset token có expiry, bị xóa sau khi dùng, và hash không xuất hiện trong response/log.
- Forgot-password luôn trả cùng một generic message, bất kể email có tồn tại hay không.

# auth-service Specification

## 1. Overview

| Field | Value |
|---|---|
| Service | `auth-service` |
| Port | `8081` |
| Database | `auth_db` |
| Responsibility | Register, login, JWT issuing |

## 2. Owned Data

- Account
- Email
- Password hash
- Provider
- Role

## 3. Main APIs

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Đăng ký |
| POST | `/api/auth/login` | Đăng nhập |
| GET | `/api/auth/me` | Current user |

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

# notification-service Specification

## 1. Overview

| Field | Value |
|---|---|
| Service | `notification-service` |
| Port | `8089` |
| Database | `notification_db` |
| Responsibility | Notification logs, stub delivery |

## 2. Owned Data

- Notification logs

## 3. Main APIs

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/notifications` | User notifications |
| POST | `/internal/notifications` | Create notification log |
| GET | `/api/admin/notifications` | Admin notification logs |

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

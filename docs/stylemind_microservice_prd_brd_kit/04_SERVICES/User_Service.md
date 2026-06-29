# user-service Specification

## 1. Overview

| Field | Value |
|---|---|
| Service | `user-service` |
| Port | `8082` |
| Database | `user_db` |
| Responsibility | Customer profile, style profile, addresses |

## 2. Owned Data

- Customer profile
- Style profile
- Delivery addresses

## 3. Main APIs

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/users/profile` | Lấy profile |
| PUT | `/api/users/profile` | Cập nhật profile |
| GET | `/api/users/addresses` | Danh sách địa chỉ |
| POST | `/api/users/addresses` | Thêm địa chỉ |
| PUT | `/api/users/addresses/{id}` | Cập nhật địa chỉ |
| DELETE | `/api/users/addresses/{id}` | Xóa địa chỉ |

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

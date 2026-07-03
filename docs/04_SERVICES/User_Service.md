# user-service Specification

## 1. Overview

| Field | Value |
|---|---|
| Service | `user-service` |
| Port | `8082` |
| Database | `user_db` |
| Responsibility | Style profile, preferences, delivery addresses |

## 2. Owned Data

- Profile reference (`user_id`)
- Style profile and preferences
- Delivery addresses

The service does not store email, password, login, role, or account status.

## 3. Main APIs

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/users/style-profile` | Lấy hoặc lazy-init profile shell |
| PUT | `/api/v1/users/style-profile` | Cập nhật style profile |
| GET | `/api/v1/users/addresses` | Lazy-init profile shell và lấy danh sách địa chỉ |
| POST | `/api/v1/users/addresses` | Thêm địa chỉ |
| PUT | `/api/v1/users/addresses/{id}` | Cập nhật địa chỉ |
| DELETE | `/api/v1/users/addresses/{id}` | Xóa địa chỉ |

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
- `user_id` phải lấy từ authenticated principal do gateway/JWT cung cấp, không lấy từ request body.
- Profile shell chỉ được tạo khi truy cập profile/addresses lần đầu; registration không gọi chéo service.

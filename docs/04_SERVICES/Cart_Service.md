# cart-service Specification

## 1. Overview

| Field | Value |
|---|---|
| Service | `cart-service` |
| Port | `8086` |
| Database | `cart_db` |
| Responsibility | Guest/authenticated cart, merge cart |

## 2. Owned Data

- Carts
- Cart items

## 3. Main APIs

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/cart` | Lấy cart |
| POST | `/api/v1/cart/items` | Thêm item |
| PUT | `/api/v1/cart/items/{itemId}` | Cập nhật quantity |
| DELETE | `/api/v1/cart/items/{itemId}` | Xóa item |
| POST | `/api/v1/cart/merge` | Merge guest cart |
| DELETE | `/api/v1/cart` | Clear cart |

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

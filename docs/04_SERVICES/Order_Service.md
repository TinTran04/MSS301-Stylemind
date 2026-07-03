# order-service Specification

## 1. Overview

| Field | Value |
|---|---|
| Service | `order-service` |
| Port | `8087` |
| Database | `order_db` |
| Responsibility | Order creation and checkout flow |

## 2. Owned Data

- Orders
- Order items

## 3. Main APIs

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/orders` | Tạo order |
| GET | `/api/v1/orders` | Order list |
| GET | `/api/v1/orders/{id}` | Order detail |
| GET | `/api/v1/admin/orders` | Admin order list |
| PUT | `/api/v1/admin/orders/{id}/status` | Update status |

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

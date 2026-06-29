# payment-service Specification

## 1. Overview

| Field | Value |
|---|---|
| Service | `payment-service` |
| Port | `8088` |
| Database | `payment_db` |
| Responsibility | COD and simulated payment transactions |

## 2. Owned Data

- Transactions

## 3. Main APIs

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/payments` | Tạo transaction |
| GET | `/api/payments/{id}` | Payment detail |
| POST | `/internal/payments/confirm` | Internal confirm |

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

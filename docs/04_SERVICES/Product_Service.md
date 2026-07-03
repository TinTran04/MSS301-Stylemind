# product-service Specification

## 1. Overview

| Field | Value |
|---|---|
| Service | `product-service` |
| Port | `8083` |
| Database | `product_db` |
| Responsibility | Catalog, categories, variants, images |

## 2. Owned Data

- Categories
- Products
- Variants
- Images

## 3. Main APIs

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/products` | Product listing |
| GET | `/api/v1/products/{id}` | Product detail |
| GET | `/api/v1/categories` | Category listing |
| POST | `/api/v1/admin/products` | Tạo product |
| PUT | `/api/v1/admin/products/{id}` | Cập nhật product |
| DELETE | `/api/v1/admin/products/{id}` | Xóa product |

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

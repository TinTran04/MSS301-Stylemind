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
| GET | `/api/products` | Product listing |
| GET | `/api/products/{id}` | Product detail |
| GET | `/api/categories` | Category listing |
| POST | `/api/admin/products` | Tạo product |
| PUT | `/api/admin/products/{id}` | Cập nhật product |
| DELETE | `/api/admin/products/{id}` | Xóa product |

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

# product-service

**Port:** `8083` &nbsp;|&nbsp; **Database:** `product_db`

## Purpose
Catalog: danh mục, sản phẩm, biến thể (variant), hình ảnh. Cung cấp **giá authoritative** cho cart/order qua internal API.

## Owns (dữ liệu service này sở hữu)
- Category, Product (status ACTIVE/INACTIVE), Variant (unique SKU), Image, Price.

## Does NOT own
- Không sở hữu cart/order.

## API — Public / Customer
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/v1/categories` | Public categories |
| GET | `/api/v1/products` | Public listing (search/filter/pagination) |
| GET | `/api/v1/products/{id}` | Product detail |

## API — Admin (role ADMIN)
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/v1/admin/categories` | Tạo category |
| PUT | `/api/v1/admin/categories/{id}` | Cập nhật category |
| DELETE | `/api/v1/admin/categories/{id}` | Xóa category |
| POST | `/api/v1/admin/products` | Tạo product |
| PUT | `/api/v1/admin/products/{id}` | Cập nhật product |
| PATCH | `/api/v1/admin/products/{id}/status` | Đổi trạng thái product |

## API — Internal (`X-Internal-Token`, frontend cấm gọi)
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/internal/v1/products/variants/{variantId}` | Variant snapshot (giá authoritative) |

## Key business rules
- Public chỉ thấy sản phẩm `ACTIVE`.
- Variant SKU không trùng.
- Internal variant API chỉ gọi bằng `X-Internal-Token`; trả giá authoritative dùng cho cart/order.
- Giá do service này quyết định, không tin giá client gửi.

## Dependencies
- **Gọi ra:** —
- **Được gọi bởi:** gateway (catalog), cart-service (validate variant), order-service (lấy giá).

## Notes
Snapshot nên gồm: variantId, productId, name, sku, price, currency, active.

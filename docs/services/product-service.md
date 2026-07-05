# product-service

**Port:** `8083` &nbsp;|&nbsp; **Database:** `product_db`

## Purpose
Catalog: danh mục, sản phẩm, biến thể (variant), hình ảnh. Cung cấp **giá authoritative** cho cart/order qua internal API.

## Owns (dữ liệu service này sở hữu)
- Category, Product (status ACTIVE/INACTIVE/DISCONTINUED), Variant (unique SKU), Image metadata, Price.

## Does NOT own
- Không sở hữu cart/order.

## API — Public / Customer
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/v1/categories` | Public root categories; `parentId` trả direct children |
| GET | `/api/v1/products` | Public ACTIVE listing; search/category/price/sort/pagination |
| GET | `/api/v1/products/{id}` | ACTIVE product detail |
| GET | `/api/v1/products/{productId}/variants` | Variants của ACTIVE product |

## API — Admin (role ADMIN)
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/v1/admin/categories` | Danh sách phẳng toàn bộ category |
| POST | `/api/v1/admin/categories` | Tạo category |
| PUT | `/api/v1/admin/categories/{id}` | Cập nhật category |
| DELETE | `/api/v1/admin/categories/{id}` | Xóa category nếu không có child/product |
| GET | `/api/v1/admin/products` | List/search/filter/pagination cho admin |
| GET | `/api/v1/admin/products/summary` | Tổng số product theo trạng thái |
| GET | `/api/v1/admin/products/{id}` | Product detail cho admin |
| POST | `/api/v1/admin/products` | Tạo product |
| PUT | `/api/v1/admin/products/{id}` | Cập nhật product |
| PATCH | `/api/v1/admin/products/{id}/status` | Đổi trạng thái product |
| DELETE | `/api/v1/admin/products/{id}` | Soft-delete bằng trạng thái INACTIVE |
| POST | `/api/v1/admin/products/{productId}/variants` | Tạo variant |
| PUT | `/api/v1/admin/products/{productId}/variants/{variantId}` | Cập nhật variant |
| DELETE | `/api/v1/admin/products/{productId}/variants/{variantId}` | Xóa variant |
| POST | `/api/v1/admin/products/{productId}/images` | Upload image multipart lên Cloudinary |
| DELETE | `/api/v1/admin/products/{productId}/images/{imageId}` | Xóa image metadata và best-effort cloud asset |

Image upload dùng `multipart/form-data`, field `file`, query `isPrimary` (default
`false`). Image response gồm `id`, `imageUrl`, nullable `publicId`, và
`isPrimary`.

## API — Internal (`X-Internal-Token`, frontend cấm gọi)
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/internal/v1/products/variants/{variantId}` | Variant snapshot (giá authoritative) |

## Key business rules
- Public chỉ thấy sản phẩm `ACTIVE`.
- Variant SKU không trùng.
- Product create/update nhận category có thật hoặc `null`; response trả cả `categoryId` và `categoryName`.
- Không xóa category đang có category con hoặc product tham chiếu; trả `409` với `CATEGORY_HAS_CHILDREN` hoặc `CATEGORY_IN_USE`.
- Product được tạo trước; image và variant được quản lý sau bằng product subresource. Không có aggregate create API và không tạo inventory/stock.
- Image chỉ nhận JPEG, PNG, WebP, GIF hoặc AVIF, tối đa 10 MB.
- Image lưu `imageUrl` và nullable `publicId`; dữ liệu ảnh seed/legacy không có `publicId` vẫn hợp lệ.
- Cloudinary credential chỉ tồn tại ở backend. Frontend không upload trực tiếp và không được biết `CLOUDINARY_API_SECRET`.
- Khi thay primary image: upload ảnh mới trước, đánh dấu primary mới, rồi xóa ảnh cũ best-effort. Lỗi cleanup không rollback product/image mới.
- Public UI chỉ hiển thị dữ liệu product/category/image/variant thật; thiếu image dùng empty state trung tính, không dùng ảnh giả.
- Internal variant API chỉ gọi bằng `X-Internal-Token`; trả giá authoritative dùng cho cart/order.
- Giá do service này quyết định, không tin giá client gửi.

## Dependencies
- **Gọi ra:** Cloudinary Upload API (product image upload/delete).
- **Được gọi bởi:** gateway (catalog), cart-service (validate variant), order-service (lấy giá).

## Notes
Snapshot nên gồm: variantId, productId, name, sku, price, currency, active.

Cloudinary environment:
`CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`,
`CLOUDINARY_FOLDER` (default `stylemind/products`).

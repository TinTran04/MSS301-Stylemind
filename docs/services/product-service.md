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
| GET | `/api/v1/categories` | Public: **danh sách phẳng toàn bộ category** (gồm cả con) để customer lọc; `parentId` trả direct children |
| GET | `/api/v1/products` | Public listing sản phẩm ACTIVE có ít nhất một variant; search/category/price/sort/pagination |
| GET | `/api/v1/products/{id}` | Detail sản phẩm ACTIVE có ít nhất một variant |
| GET | `/api/v1/products/{productId}/variants` | Variants của sản phẩm ACTIVE có ít nhất một variant |

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
| POST | `/api/v1/admin/products` | Tạo thông tin cơ bản của product ở trạng thái INACTIVE |
| PUT | `/api/v1/admin/products/{id}` | Cập nhật product |
| PATCH | `/api/v1/admin/products/{id}/status` | Đổi trạng thái product; ACTIVE yêu cầu có variant |
| DELETE | `/api/v1/admin/products/{id}` | Soft-delete bằng trạng thái INACTIVE |
| POST | `/api/v1/admin/products/{productId}/variants` | Tạo variant |
| PUT | `/api/v1/admin/products/{productId}/variants/{variantId}` | Cập nhật variant |
| DELETE | `/api/v1/admin/products/{productId}/variants/{variantId}` | Xóa variant; không được xóa final variant của ACTIVE product |
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
- Public chỉ thấy sản phẩm `ACTIVE` có ít nhất một variant.
- **Customer Shop category filter** dùng `GET /api/v1/categories` (không param) → danh sách phẳng thật gồm cả category con (products gắn với category lá), không hardcode; `parentId` vẫn để drill-down. Admin quản lý category qua `/api/v1/admin/categories` (không đổi).
- Variant SKU không trùng.
- Product create/update nhận category có thật hoặc `null`; response trả cả `categoryId` và `categoryName`.
- Không xóa category đang có category con hoặc product tham chiếu; trả `409` với `CATEGORY_HAS_CHILDREN` hoặc `CATEGORY_IN_USE`.
- Product được tạo trước; image và variant được quản lý sau bằng product subresource. Không có aggregate create API và không tạo inventory/stock.
- `POST /api/v1/admin/products` giữ nguyên request/`ProductResponse`; service luôn tạo product ở trạng thái `INACTIVE`, kể cả khi client gửi `ACTIVE`.
- Admin Add Product là flow Product Info → Variants → Images / Finish. Step 2 và Step 3 dùng các variant/image subresource hiện có; không thay đổi schema.
- Product chỉ được chuyển sang `ACTIVE` sau khi có ít nhất một variant. Vi phạm trả HTTP `409`, `errorCode=PRODUCT_REQUIRES_VARIANT`, message `Cannot activate a product without variants. Add at least one variant before publishing it.`
- Nếu ACTIVE product chỉ còn đúng một variant, xóa variant đó trả HTTP `409`, `errorCode=LAST_ACTIVE_VARIANT`, message `Cannot delete the last variant of an active product. Deactivate the product before deleting its final variant.`
- Đóng flow sau Step 1 không xóa product; product tiếp tục tồn tại ở trạng thái `INACTIVE`.
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

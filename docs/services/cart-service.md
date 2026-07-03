# cart-service

**Port:** `8086` &nbsp;|&nbsp; **Database:** `cart_db`

## Purpose
Giỏ hàng cho guest và customer; merge guest cart sau login; clear sau checkout.

## Owns (dữ liệu service này sở hữu)
- Cart (một chính/user), CartItem (variantId, quantity, cờ aiRecommended tùy chọn).

## Does NOT own
- **Không** sở hữu giá (lấy từ product-service); không đụng product_db.

## API — Public / Customer
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/v1/cart` | Cart hiện tại |
| POST | `/api/v1/cart/items` | Thêm item |
| PUT | `/api/v1/cart/items/{itemId}` | Cập nhật quantity |
| DELETE | `/api/v1/cart/items/{itemId}` | Xóa item |
| DELETE | `/api/v1/cart` | Clear cart |
| POST | `/api/v1/cart/merge` | Merge guest cart |

## API — Admin (role ADMIN)
_(không có)_

## API — Internal (`X-Internal-Token`, frontend cấm gọi)
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/internal/v1/cart/users/{userId}` | Internal get cart (order-service dùng) |
| DELETE | `/internal/v1/cart/users/{userId}` | Internal clear cart |

## Key business rules
- Add item validate variant qua product-service.
- Quantity > 0; item trùng thì cộng dồn.
- Merge không tạo duplicate variant.
- Checkout thành công → cart rỗng.
- Frontend cấm gọi `/internal/v1/cart/**`.

## Dependencies
- **Gọi ra:** product-service (validate variant).
- **Được gọi bởi:** gateway (cart pages), order-service (get/clear khi checkout).

## Notes
Guest cart có thể giữ ở client tới khi login rồi gọi merge.

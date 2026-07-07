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
- Add item validate variant qua product-service: variant phải tồn tại, product phải `ACTIVE`, và variant phải `active=true` với `stockQuantity > 0` — hết hàng hoặc bị vô hiệu hóa trả `400 VARIANT_OUT_OF_STOCK`, message "Biến thể này đã hết hàng." Không cho thêm vào giỏ một variant hết hàng ngay cả khi product vẫn ACTIVE.
- Quantity > 0; item trùng thì cộng dồn.
- Merge không tạo duplicate variant.
- Checkout thành công → cart rỗng.
- Frontend cấm gọi `/internal/v1/cart/**`.
- **Cart item display**: mỗi item trong `GET /api/v1/cart` được enrich bằng snapshot từ product-service (`GET /internal/v1/products/variants/{variantId}`) — trả `productName`, `size`, `color`, `material`, `imageUrl`, giá hiệu lực (`variant.product.basePrice`). Đây là **giá hiển thị**, không phải nguồn giá cho order; order-service tự lấy lại giá authoritative từ product-service khi checkout và lưu `price_at_purchase`.
- Nếu product-service không tìm thấy variant hoặc variant không còn `ACTIVE`, item được đánh dấu `available: false` kèm `unavailableMessage`; item đó không cộng vào `totalAmount` và không hiển thị giá giả.
- `totalAmount` = tổng (`unitPrice × quantity`) của các item còn `available`.
- **Merge sau login**: `POST /api/v1/cart/merge` yêu cầu JWT (400 `AUTH_REQUIRED` nếu chưa đăng nhập); body `{ guestSessionId }`. Item trùng `variantId` cộng dồn quantity vào item của user; item khác variant được chuyển nguyên sang cart của user (không tạo duplicate). Sau khi merge thành công, guest cart bị xóa.

## Dependencies
- **Gọi ra:** product-service (validate variant).
- **Được gọi bởi:** gateway (cart pages), order-service (get/clear khi checkout).

## Notes
Guest cart có thể giữ ở client tới khi login rồi gọi merge.

# Error Codes — StyleMind

## Auth Errors

| Code | Meaning |
|---|---|
| AUTH_INVALID_CREDENTIALS | Email hoặc password không đúng |
| AUTH_EMAIL_ALREADY_EXISTS | Email đã tồn tại |
| AUTH_TOKEN_EXPIRED | JWT hết hạn |
| AUTH_UNAUTHORIZED | Chưa đăng nhập |
| AUTH_FORBIDDEN | Không đủ quyền |

## Product Errors

| Code | Meaning |
|---|---|
| PRODUCT_NOT_FOUND | Không tìm thấy sản phẩm |
| PRODUCT_VARIANT_NOT_FOUND | Không tìm thấy biến thể |
| CATEGORY_NOT_FOUND | Không tìm thấy danh mục |
| PRODUCT_INVALID_PRICE | Giá sản phẩm không hợp lệ |

## Cart Errors

| Code | Meaning |
|---|---|
| CART_NOT_FOUND | Không tìm thấy giỏ hàng |
| CART_ITEM_NOT_FOUND | Không tìm thấy item trong cart |
| CART_EMPTY | Giỏ hàng rỗng |
| CART_INVALID_QUANTITY | Quantity không hợp lệ |

## Order Errors

| Code | Meaning |
|---|---|
| ORDER_NOT_FOUND | Không tìm thấy đơn hàng |
| ORDER_CREATE_FAILED | Tạo order thất bại |
| ORDER_INVALID_STATUS | Trạng thái order không hợp lệ |
| ORDER_PRICE_CHANGED | Giá sản phẩm đã thay đổi |

## Payment Errors

| Code | Meaning |
|---|---|
| PAYMENT_FAILED | Thanh toán thất bại |
| PAYMENT_NOT_FOUND | Không tìm thấy transaction |
| PAYMENT_METHOD_NOT_SUPPORTED | Payment method không hỗ trợ |

## AI Errors

| Code | Meaning |
|---|---|
| AI_CHAT_FAILED | AI chat failed |
| AI_INDEX_JOB_NOT_FOUND | Không tìm thấy index job |
| AI_RATE_LIMITED | AI endpoint bị rate limit |

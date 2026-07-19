# API Catalog — Toàn bộ endpoint

Quy ước: public/admin = `/api/v1`; nội bộ = `/internal/v1` (frontend cấm gọi).

## Auth (auth-service)
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/v1/auth/register` | Bắt đầu đăng ký → gửi OTP email (chưa tạo account) |
| POST | `/api/v1/auth/register/verify-otp` | Xác thực OTP đăng ký → tạo account ACTIVE |
| POST | `/api/v1/auth/register/resend-otp` | Gửi lại OTP đăng ký (có cooldown) |
| POST | `/api/v1/auth/login` | Đăng nhập → JWT |
| GET | `/api/v1/auth/me` | User hiện tại |
| POST | `/api/v1/auth/forgot-password` | Yêu cầu reset |
| POST | `/api/v1/auth/verify-reset-otp` | Xác thực OTP/token |
| POST | `/api/v1/auth/reset-password` | Đặt lại mật khẩu |
| POST | `/api/v1/auth/password/setup` | Thiết lập mật khẩu lần đầu từ link email admin tạo account |

## Admin Account (auth-service)
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/v1/admin/accounts` | Danh sách (search/filter) |
| GET | `/api/v1/admin/users/summary` | Dashboard: user counts (total/customers/admins) |
| POST | `/api/v1/admin/accounts` | Tạo account |
| PATCH | `/api/v1/admin/accounts/{userId}/status` | Enable/disable |
| PATCH | `/api/v1/admin/accounts/{userId}/role` | Cập nhật role |
| DELETE | `/api/v1/admin/accounts/{userId}` | Xóa (chặn self & last-admin) |

## User Profile (user-service)
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/v1/users/style-profile` | Lấy style profile |
| PUT | `/api/v1/users/style-profile` | Upsert style profile |
| GET | `/api/v1/users/addresses` | Danh sách địa chỉ |
| POST | `/api/v1/users/addresses` | Tạo địa chỉ |
| PUT | `/api/v1/users/addresses/{id}` | Cập nhật địa chỉ |
| DELETE | `/api/v1/users/addresses/{id}` | Xóa địa chỉ |

## Product (product-service)
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/v1/categories` | Public: full flat category list (incl. children); `parentId` → direct children |
| GET | `/api/v1/products` | ACTIVE listing có ít nhất một variant; search/filter/sort/pagination (`targetDemographic` optional) |
| GET | `/api/v1/products/{id}` | ACTIVE product detail có ít nhất một variant |
| GET | `/api/v1/products/{productId}/variants` | Variants của ACTIVE product có ít nhất một variant |
| GET | `/api/v1/admin/categories` | Toàn bộ category cho admin |
| POST | `/api/v1/admin/categories` | Tạo category |
| PUT | `/api/v1/admin/categories/{id}` | Cập nhật category |
| DELETE | `/api/v1/admin/categories/{id}` | Xóa category; `409` nếu đang được dùng/có child |
| GET | `/api/v1/admin/products` | Admin product list/search/filter |
| GET | `/api/v1/admin/products/summary` | Product counts |
| GET | `/api/v1/admin/products/{id}` | Admin product detail |
| POST | `/api/v1/admin/products` | Tạo basic product ở trạng thái INACTIVE; request/response giữ nguyên |
| PUT | `/api/v1/admin/products/{id}` | Cập nhật product |
| PATCH | `/api/v1/admin/products/{id}/status` | Đổi trạng thái; ACTIVE yêu cầu ít nhất một variant |
| DELETE | `/api/v1/admin/products/{id}` | Soft-delete product |
| POST | `/api/v1/admin/products/{productId}/variants` | Tạo variant |
| PUT | `/api/v1/admin/products/{productId}/variants/{variantId}` | Cập nhật variant |
| DELETE | `/api/v1/admin/products/{productId}/variants/{variantId}` | Xóa variant; chặn final variant của ACTIVE product |
| POST | `/api/v1/admin/products/{productId}/images` | Upload image multipart |
| DELETE | `/api/v1/admin/products/{productId}/images/{imageId}` | Xóa product image |
| GET | `/internal/v1/products/variants/{variantId}` | Variant snapshot (giá authoritative) |

Product create/update nhận `categoryIds` (list, many-to-many qua bảng
`product_categories`) và trả `ProductResponse` với `categories` (list
`{id, name}`) thay cho `categoryId`/`categoryName` cũ (không thêm
`effectivePrice`). Admin tạo variants và images qua các subresource sau khi nhận
`productId`. Product không còn field `aestheticStyle`/`seasonalProperty`.

`targetDemographic` là enum tiếng Anh `MALE`/`FEMALE`/`UNISEX` (API
request/response và DB đều dùng giá trị này); Vietnamese `Nam`/`Nữ`/`Unisex`
chỉ là label hiển thị ở frontend. Public product list hỗ trợ filter
`targetDemographic` cùng giá trị enum này để FE hiển thị bộ lọc
`Tất cả / Nam / Nữ / Unisex` mà không cần schema hay path mới; giá trị filter
không hợp lệ được bỏ qua (không lọc) thay vì lỗi.

Product conflict responses dùng error envelope chuẩn:
- Activate product chưa có variant: HTTP `409`, `PRODUCT_REQUIRES_VARIANT`,
  `Cannot activate a product without variants. Add at least one variant before publishing it.`
- Xóa final variant của ACTIVE product: HTTP `409`, `LAST_ACTIVE_VARIANT`,
  `Cannot delete the last variant of an active product. Deactivate the product before deleting its final variant.`

## Cart (cart-service)
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/v1/cart` | Cart hiện tại |
| POST | `/api/v1/cart/items` | Thêm item |
| PUT | `/api/v1/cart/items/{itemId}` | Cập nhật quantity |
| DELETE | `/api/v1/cart/items/{itemId}` | Xóa item |
| DELETE | `/api/v1/cart` | Clear cart |
| POST | `/api/v1/cart/merge` | Merge guest cart |
| GET | `/internal/v1/cart/users/{userId}` | Internal get cart |
| DELETE | `/internal/v1/cart/users/{userId}` | Internal clear cart |

## Order (order-service)
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/v1/orders` | Tạo order từ cart; nhận `paymentMethod=cod|sepay`, hỗ trợ header `Idempotency-Key` |
| GET | `/api/v1/orders` | Danh sách order của customer |
| GET | `/api/v1/orders/{id}` | Chi tiết order |
| PATCH | `/api/v1/orders/{id}/cancel` | Hủy order/payment đang chờ (PENDING/PAYMENT_PENDING); kết thúc checkout attempt hiện tại, lần checkout sau dùng Idempotency-Key mới |
| GET | `/api/v1/admin/orders` | Admin danh sách |
| GET | `/api/v1/admin/orders/summary` | Dashboard: order + revenue counts |
| GET | `/api/v1/admin/orders/{id}` | Admin chi tiết |
| PATCH | `/api/v1/admin/orders/{id}/status` | Admin đổi trạng thái (validate transition) |
| POST | `/internal/v1/orders/{orderId}/payment-status` | Internal callback từ payment-service về trạng thái webhook |

## Payment (payment-service)
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/internal/v1/payments/cod` | Tạo COD transaction |
| POST | `/internal/v1/payments/sepay` | Tạo SePay transaction + VietQR |
| GET | `/internal/v1/payments/orders/{orderId}` | Payment theo order |
| POST | `/internal/v1/payments/orders/{orderId}/expire` | Expire pending SePay payment khi order timeout |
| POST | `/api/v1/payments/webhook/sepay` | Webhook SePay (verify + idempotent, đúng path public duy nhất; không JWT) |

## Notification (notification-service)
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/internal/v1/notifications` | Tạo notification log |
| GET | `/api/v1/notifications` | Customer list |
| GET | `/api/v1/notifications/{id}` | Detail |
| GET | `/api/v1/admin/notifications` | Admin logs |
| GET | `/api/v1/admin/notifications/summary` | Dashboard: failed notification count |
| POST | `/api/v1/admin/notifications/{id}/retry` | Retry failed |

## AI Stylist (ai-agent-service)
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/v1/ai-stylist/chat` | Gửi message |
| GET | `/api/v1/ai-stylist/history` | Chat history |
| GET | `/api/v1/ai-stylist/bundles` | AI bundles |
| GET | `/api/v1/admin/ai/index-jobs` | Admin xem index jobs |
| POST | `/api/v1/admin/ai/index-jobs` | Admin tạo index job |

## Payment / checkout notes
- Frontend chỉ được checkout qua `POST /api/v1/orders`; **không** gọi `payment-service` trực tiếp và **không** gọi `/internal/v1/**`.
- Admin order detail trả `availableTransitions` từ `OrderStatus` và `statusHistory` từ `order_status_audit_log` khi có bản ghi; item `priceAtPurchase` là giá snapshot, còn metadata catalog trong admin detail là enrichment tùy khả dụng.
- Admin status update gửi body `{ "orderStatus": "<allowed-status>" }` qua Gateway, yêu cầu role `ADMIN`; transition không hợp lệ trả `409` và không ghi audit.
- `/api/v1/payments/webhook/sepay` là public từ góc nhìn JWT/gateway, nhưng payment-service vẫn bắt buộc xác thực webhook bằng SePay API key.
- Internal payment/order endpoints yêu cầu `X-Internal-Token`.
- SePay chỉ mark order paid khi **đúng số tiền** và **đúng `SEVQR STYLEMIND <reference>` đã normalize/exact-match**; chỉ `SEVQR` không được match và `contains(...)` không được phép dùng để đối soát.
- SePay Dashboard phải gọi endpoint qua HTTPS public; `localhost` không thể nhận webhook. Khi hủy hoặc hết hạn, order-service expire/cancel payment trước khi đổi order state; webhook đến trễ chỉ ghi event và không revive order.

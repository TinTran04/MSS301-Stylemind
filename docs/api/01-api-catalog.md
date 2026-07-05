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

## Admin Account (auth-service)
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/v1/admin/accounts` | Danh sách (search/filter) |
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
| GET | `/api/v1/categories` | Public root categories / children by `parentId` |
| GET | `/api/v1/products` | ACTIVE listing; search/filter/sort/pagination |
| GET | `/api/v1/products/{id}` | ACTIVE product detail |
| GET | `/api/v1/products/{productId}/variants` | ACTIVE product variants |
| GET | `/api/v1/admin/categories` | Toàn bộ category cho admin |
| POST | `/api/v1/admin/categories` | Tạo category |
| PUT | `/api/v1/admin/categories/{id}` | Cập nhật category |
| DELETE | `/api/v1/admin/categories/{id}` | Xóa category; `409` nếu đang được dùng/có child |
| GET | `/api/v1/admin/products` | Admin product list/search/filter |
| GET | `/api/v1/admin/products/summary` | Product counts |
| GET | `/api/v1/admin/products/{id}` | Admin product detail |
| POST | `/api/v1/admin/products` | Tạo product |
| PUT | `/api/v1/admin/products/{id}` | Cập nhật product |
| PATCH | `/api/v1/admin/products/{id}/status` | Đổi trạng thái product |
| DELETE | `/api/v1/admin/products/{id}` | Soft-delete product |
| POST | `/api/v1/admin/products/{productId}/variants` | Tạo variant |
| PUT | `/api/v1/admin/products/{productId}/variants/{variantId}` | Cập nhật variant |
| DELETE | `/api/v1/admin/products/{productId}/variants/{variantId}` | Xóa variant |
| POST | `/api/v1/admin/products/{productId}/images` | Upload image multipart |
| DELETE | `/api/v1/admin/products/{productId}/images/{imageId}` | Xóa product image |
| GET | `/internal/v1/products/variants/{variantId}` | Variant snapshot (giá authoritative) |

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
| POST | `/api/v1/orders` | Tạo order từ cart |
| GET | `/api/v1/orders` | Danh sách order của customer |
| GET | `/api/v1/orders/{id}` | Chi tiết order |
| GET | `/api/v1/admin/orders` | Admin danh sách |
| GET | `/api/v1/admin/orders/{id}` | Admin chi tiết |
| PATCH | `/api/v1/admin/orders/{id}/status` | Admin đổi trạng thái (validate transition) |

## Payment (payment-service)
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/internal/v1/payments/cod` | Tạo COD transaction |
| POST | `/internal/v1/payments/sepay` | Tạo SePay transaction + VietQR |
| GET | `/internal/v1/payments/orders/{orderId}` | Payment theo order |
| POST | `/api/v1/payments/webhook/sepay` | Webhook SePay (verify + idempotent) |
| GET | `/api/v1/payments/{transactionId}` | Payment detail |
| GET | `/api/v1/admin/payments` | Admin payment logs |

## Notification (notification-service)
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/internal/v1/notifications` | Tạo notification log |
| GET | `/api/v1/notifications` | Customer list |
| GET | `/api/v1/notifications/{id}` | Detail |
| GET | `/api/v1/admin/notifications` | Admin logs |
| POST | `/api/v1/admin/notifications/{id}/retry` | Retry failed |

## AI Stylist (ai-agent-service)
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/v1/ai-stylist/chat` | Gửi message |
| GET | `/api/v1/ai-stylist/history` | Chat history |
| GET | `/api/v1/ai-stylist/bundles` | AI bundles |
| GET | `/api/v1/admin/ai/index-jobs` | Admin xem index jobs |
| POST | `/api/v1/admin/ai/index-jobs` | Admin tạo index job |

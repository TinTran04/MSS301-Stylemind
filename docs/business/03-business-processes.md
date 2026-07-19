# Business Processes

## 1. Đăng ký / đăng nhập / quên mật khẩu
Flow: `Customer → Frontend → API Gateway → auth-service → auth_db`.

Rules: email không trùng; password hash; login sai không tiết lộ email/password sai cụ thể; user disable không login được; forgot-password không tiết lộ email tồn tại hay không; OTP/reset token có hạn & dùng một lần; password mới phải hash.

## 2. Xem & tìm kiếm sản phẩm
Flow: `Frontend → Gateway → product-service → product_db`.
Rules: public chỉ thấy sản phẩm `ACTIVE`; detail có category/variants/images/giá; admin CRUD sản phẩm.

## 3. Giỏ hàng
Rules: một user một cart chính; item trùng thì cộng quantity; quantity > 0; checkout xong clear cart; cart-service validate variant qua product-service (không đụng product_db).

## 4. Checkout & tạo đơn (orchestration + saga)
Flow: `order-service (orchestrator) → cart-service → product-service → payment-service → notification-service`.
Rules: giá lấy từ product-service (authoritative), không lấy từ cart; lưu `price_at_purchase`; COD → CONFIRMED ngay, SePay → PAYMENT_PENDING đến khi webhook PAID; checkout thành công clear cart; notification fail KHÔNG rollback order. Timeout tự động chuyển SePay `PAYMENT_PENDING -> EXPIRED`; customer hủy payment pending thì payment expire trước rồi order mới chuyển `CANCELLED`. Chi tiết: `architecture/05-checkout-saga.md`.

## 5. Payment
| Method | Bản chất | Xác nhận |
|---|---|---|
| COD | Trả khi nhận hàng | Order CONFIRMED ngay; thu tiền khi giao. |
| SEPAY | Chuyển khoản VietQR (Open Banking) | Khách quét QR → SePay bắn webhook → đối soát → PAID. |

## 6. Admin operations
Quản lý account user (+ self-protection), product/category/variants/images, order (theo state machine), notification logs (+ retry), AI index jobs. Thao tác nhạy cảm nên có audit log.

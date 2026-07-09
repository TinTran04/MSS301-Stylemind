# Checkout Orchestration & Saga

## Orchestration là gì
order-service là **nhạc trưởng** chủ động gọi cart → product → payment → notification. Dễ hiểu, dễ debug, hợp luồng checkout tuyến tính. (Choreography — không nhạc trưởng, chạy theo event — chưa cần cho MVP.)

## Saga là gì & vì sao cần
Mỗi service có DB riêng → **không** dùng được 1 transaction ACID xuyên service. Saga chia giao dịch lớn thành chuỗi local transaction; bước nào lỗi thì chạy **compensation** (bù trừ) thay vì rollback.

## Saga của StyleMind (SePay)
| Bước | Hành động | Compensation nếu lỗi |
|---|---|---|
| 1 | Lấy cart (cart-service) | Chưa tạo gì → không cần bù |
| 2 | Lấy giá authoritative (product-service) | Chưa tạo order → dừng |
| 3 | Tạo Order (PENDING) + items + price_at_purchase | Hủy order nếu bước sau fail nặng |
| 4 | Tạo payment + sinh VietQR → PAYMENT_PENDING | Hủy payment; Order → CANCELLED |
| 5 | Chờ webhook SePay (bất đồng bộ) | Timeout (vd 15') → Order = EXPIRED/CANCELLED |
| 6 | Webhook PAID → Order = PAID | Webhook FAILED/sai tiền → Order = FAILED |
| 7 | Clear cart + notification sau khi đã PAID/CONFIRMED | Notification fail **KHÔNG** rollback order (log/retry) |

Luồng **COD** đơn giản: tạo Order (PENDING) → payment COD → Order = CONFIRMED → clear cart → notification. Không có bước chờ webhook.

## Hardening đang áp dụng
- Frontend checkout chỉ gọi `POST /api/v1/orders`; frontend **không** tự tạo payment SePay và **không** tự xác nhận thanh toán.
- `order-service` là orchestrator: lấy cart, lấy giá authoritative, tạo order rồi mới gọi `payment-service` qua `/internal/v1/payments/cod|sepay`.
- COD: order chuyển thẳng `CONFIRMED`, clear cart ngay.
- SePay: order chỉ chuyển `PAYMENT_PENDING -> PAID` khi webhook/IPN đã xác thực và đối soát thành công.
- Webhook **không** tự đẩy `PAID -> PROCESSING`.
- `payment-service` đối soát bằng **exact normalized transferContent** hoặc token `STYLEMIND <payment-token>` có boundary rõ ràng; cấm `contains(...)`.
- Duplicate webhook cùng `gateway_transaction_id` là **no-op**: trả success nhưng không mark paid lần hai, không callback order-service lần hai.
- Timeout job của `order-service` đổi `PAYMENT_PENDING -> EXPIRED`, sau đó gọi `payment-service` expire transaction tương ứng.
- Late webhook sau khi order/payment đã EXPIRED chỉ được log thành event review (`LATE_AFTER_EXPIRY`), **không** revive order về `PAID`.

## Idempotency checkout
- FE gửi `Idempotency-Key` trên `POST /api/v1/orders`.
- `order-service` giữ khóa theo `userId + idempotencyKey` trong bảng `checkout_idempotency`.
- Cùng key đang xử lý → trả `409 CHECKOUT_IN_PROGRESS`.
- Cùng key đã thành công → trả lại order/payment hiện có, không tạo order/payment SePay thứ hai.

## Ghi chú schema local
- Hiện tại `order-service` và `payment-service` vẫn dùng **init-scripts** (`BE/init-scripts/06-order-db.sql`, `07-payment-db.sql`) cho local Docker schema.
- Flyway cho hai service này là việc riêng của một task hạ tầng khác, chưa bật trong pass SePay này.

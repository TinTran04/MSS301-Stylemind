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
| 3 | Tạo Order theo phương thức thanh toán: COD bắt đầu `PENDING`, SePay bắt đầu `PAYMENT_PENDING`; lưu items + `price_at_purchase` | Hủy order nếu payment initialization fail |
| 4 | Tạo payment transaction; SePay sinh VietQR | Nếu payment initialization fail, order chuyển `CANCELLED` qua state service |
| 5 | Chờ webhook SePay (bất đồng bộ) | Timeout job chỉ chuyển order `PAYMENT_PENDING -> EXPIRED` sau khi payment-service expire thành công |
| 6 | Webhook match đúng content + amount → payment `PAID`, order `PAID` | Match đúng transaction nhưng sai amount → payment/order `FAILED`; không match hoặc late webhook chỉ ghi event, không callback order |
| 7 | Clear cart + notification sau khi đã PAID/CONFIRMED | Notification fail **KHÔNG** rollback order (log/retry) |

Luồng **COD** đơn giản: tạo Order `PENDING` → payment COD transaction `PENDING` (thu tiền khi giao) → Order `CONFIRMED` → clear cart → notification. Không có bước chờ webhook; payment transaction COD không tự chuyển `PAID` trong luồng checkout này.

## Hardening đang áp dụng
- Frontend checkout chỉ gọi `POST /api/v1/orders`; frontend **không** tự tạo payment SePay và **không** tự xác nhận thanh toán.
- `order-service` là orchestrator: lấy cart, lấy giá authoritative, tạo order rồi mới gọi `payment-service` qua `/internal/v1/payments/cod|sepay`.
- COD: order chuyển thẳng `CONFIRMED`, clear cart ngay.
- SePay: order chỉ chuyển `PAYMENT_PENDING -> PAID` khi webhook/IPN đã xác thực và đối soát thành công.
- Webhook **không** tự đẩy `PAID -> PROCESSING`.
- `payment-service` lưu và đưa vào VietQR cùng một giá trị **`SEVQR STYLEMIND <payment-token>`**, đối soát bằng exact normalized transferContent hoặc token có boundary rõ ràng; chỉ prefix `SEVQR` không được coi là thanh toán và cấm `contains(...)`.
- `transactions.transfer_content` là reference đầy đủ được lưu; order item chỉ lưu `variant_id`, `quantity`, `price_at_purchase` và các cờ AI. Product name/image/SKU/size/color/material trên admin detail là catalog enrichment hiện tại, không phải snapshot lịch sử.
- Duplicate webhook cùng `gateway_transaction_id` là **no-op**: trả success nhưng không mark paid lần hai, không callback order-service lần hai.
- Timeout job của `order-service` đổi `PAYMENT_PENDING -> EXPIRED`, sau đó gọi `payment-service` expire transaction tương ứng.
- Late webhook sau khi order/payment đã EXPIRED hoặc CANCELLED chỉ được log thành event review (`LATE_AFTER_EXPIRY`), **không** revive order về `PAID`.

## Idempotency checkout
- FE gửi `Idempotency-Key` trên `POST /api/v1/orders`.
- `order-service` giữ khóa theo `userId + idempotencyKey` trong bảng `checkout_idempotency`.
- Cùng key đang xử lý → trả `409 CHECKOUT_IN_PROGRESS`.
- Cùng key đã thành công → trả lại order/payment hiện có, không tạo order/payment SePay thứ hai.
- Hủy một đơn `PAYMENT_PENDING` kết thúc checkout attempt hiện tại. FE xóa `lastOrder`, polling và key của attempt đó trước khi rời màn hình.
- Lần checkout tiếp theo tạo key mới và chỉ hiển thị order trả về từ request `POST /api/v1/orders` mới. `CANCELLED`, `EXPIRED` và `FAILED` không được dùng làm kết quả checkout hiện tại.
- Khi customer hủy `PAYMENT_PENDING`, order-service expire payment qua internal endpoint trước rồi mới chuyển order sang `CANCELLED`; nếu payment-service không xác nhận được, order không bị chuyển một mình. Webhook đến sau `CANCELLED`/`EXPIRED` chỉ được ghi nhận để review, không đổi lại `PAID`.

## Ghi chú schema local
- Hiện tại `order-service` và `payment-service` vẫn dùng **init-scripts** (`BE/init-scripts/06-order-db.sql`, `07-payment-db.sql`) cho local Docker schema.
- Flyway cho hai service này là việc riêng của một task hạ tầng khác, chưa bật trong pass SePay này.

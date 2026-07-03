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
| 7 | Clear cart + notification | Notification fail **KHÔNG** rollback order (log/retry) |

Luồng **COD** đơn giản: tạo Order (PENDING) → payment COD → Order = CONFIRMED → clear cart → notification. Không có bước chờ webhook.

> **Sprint:** saga cơ bản (orchestration + timeout/expire + clear cart) làm NGAY ở Sprint 3, nếu không đơn SePay treo PAYMENT_PENDING mãi. Sprint 5 chỉ hardening: outbox pattern, retry, compensation nâng cao.

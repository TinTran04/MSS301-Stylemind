# order-service

**Port:** `8087` &nbsp;|&nbsp; **Database:** `order_db`

## Purpose
**Orchestrator** của checkout + quản lý order (customer & admin) + **Order State Machine**.

## Owns (dữ liệu service này sở hữu)
- Order (status theo state machine), OrderItem (price_at_purchase, cờ AI conversion).

## Does NOT own
- Không sở hữu payment record (thuộc payment-service).

## API — Public / Customer
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/v1/orders` | Tạo order từ cart (orchestration) |
| GET | `/api/v1/orders` | Danh sách order của customer |
| GET | `/api/v1/orders/{id}` | Chi tiết order của customer |
| PATCH | `/api/v1/orders/{id}/cancel` | Hủy order/payment đang chờ (PENDING/PAYMENT_PENDING) |

## API — Admin (role ADMIN)
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/v1/admin/orders` | Admin danh sách order |
| GET | `/api/v1/admin/orders/summary` | Admin dashboard: order + revenue counts (aggregates only) |
| GET | `/api/v1/admin/orders/{id}` | Admin chi tiết |
| PATCH | `/api/v1/admin/orders/{id}/status` | Admin đổi trạng thái (validate transition → 409) |

## API — Internal (`X-Internal-Token`, frontend cấm gọi)
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/internal/v1/orders/{orderId}/payment-status` | payment-service callback trạng thái SePay webhook |

## Key business rules
- Lấy giá **authoritative** từ product-service; không lấy từ cart DTO.
- Lưu `price_at_purchase`; total tính từ product-service.
- COD → CONFIRMED ngay; SePay → PAYMENT_PENDING đến khi webhook PAID.
- Webhook SePay chỉ đổi `PAYMENT_PENDING -> PAID`; không tự đẩy sang `PROCESSING`.
- Checkout thành công → clear cart; notification fail KHÔNG rollback order.
- Customer có thể hủy order đang chờ thanh toán qua `PATCH /api/v1/orders/{id}/cancel`; nếu order đang `PAYMENT_PENDING`, order-service phải gọi payment-service expire payment thành công trước rồi mới chuyển order sang `CANCELLED`, tránh trạng thái order hủy nhưng payment vẫn `PENDING`.
- SePay quá hạn → EXPIRED/CANCELLED (timeout job).
- Timeout job expire transaction SePay trước rồi mới đổi order sang `EXPIRED`; nếu payment-service lỗi, job giữ order `PAYMENT_PENDING` để retry lần sau.
- Customer chỉ xem order của mình; admin xem tất cả.
- MỌI đổi trạng thái đi qua `OrderStatusService.changeStatus()`.
- `POST /api/v1/orders` hỗ trợ `Idempotency-Key`; cùng key đang xử lý trả `409`, cùng key đã thành công trả lại order cũ thay vì tạo order/payment mới.
- **Dashboard revenue rule:** doanh thu chỉ tính order đã thanh toán & đang xử lý/hoàn tất — `PAID, CONFIRMED, PROCESSING, SHIPPED, COMPLETED`; KHÔNG tính `PENDING`/`PAYMENT_PENDING` (chưa trả) hay `CANCELLED`/`EXPIRED`/`FAILED`. Summary chỉ trả count/sum, không lộ dữ liệu order.

## Dependencies
- **Gọi ra:** cart-service (get/clear), product-service (giá), payment-service (tạo payment), notification-service.
- **Được gọi bởi:** gateway (order pages/admin), payment-service (báo status change).

## Notes
- Hiện tại `order-service` vẫn dùng **init-script** `BE/init-scripts/06-order-db.sql` cho local Docker schema; chưa bật Flyway trong service này.
- Xem `architecture/04-order-state-machine.md` và `architecture/05-checkout-saga.md`.

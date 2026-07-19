# payment-service

**Port:** `8088` &nbsp;|&nbsp; **Database:** `payment_db`

## Purpose
Xử lý thanh toán **COD** và **SePay VietQR** (Open Banking). Nhận **webhook** từ SePay, đối soát, cập nhật trạng thái và báo lại order-service.

## Owns (dữ liệu service này sở hữu)
- Payment (method COD/SEPAY, status PENDING/PAID/FAILED/EXPIRED/CANCELLED; refund flow có thể dùng REFUNDED), transaction log, VietQR reference (nội dung CK duy nhất).

## Does NOT own
- Không sở hữu order (thuộc order-service).

## API — Public / Customer
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/v1/payments/webhook/sepay` | Webhook SePay (verify + idempotent, không JWT) |

## API — Internal (`X-Internal-Token`, frontend cấm gọi)
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/internal/v1/payments/cod` | Tạo COD transaction |
| POST | `/internal/v1/payments/sepay` | Tạo SePay transaction + sinh VietQR |
| GET | `/internal/v1/payments/orders/{orderId}` | Payment theo order |
| POST | `/internal/v1/payments/orders/{orderId}/expire` | Expire pending SePay payment khi order timeout |

## Key business rules
- SePay là Open Banking (webhook), KHÔNG phải cổng thẻ redirect.
- Webhook **verify** API key SePay trước khi xử lý.
- **Idempotent** ở mức event: `payment_webhook_events` có unique key theo `(provider, gateway_transaction_id)`; duplicate delivery trả success nhưng không reprocess.
- **Đối soát**: `transactions.transfer_content` là reference chuẩn, không có `payment_code`; giá trị được lưu và đưa vào QR là `SEVQR STYLEMIND <token>`. Webhook lần lượt kiểm tra các field `code`, `content`, `description` và `referenceCode` bằng exact normalized match hoặc bounded payment token có reference StyleMind + đúng số tiền mới set `PAID`; chỉ `SEVQR` không đủ để match. Sai tiền sau khi đã match transaction → transaction `FAILED` và callback order `FAILED`; transfer không match chỉ ghi event `NO_MATCHING_ORDER`, không đổi order. Không dùng `contains(...)`.
- Nếu không tìm thấy transaction pending, webhook event vẫn được lưu với `processed=false` và `error_message` chẩn đoán `transfer_content` không khớp. Sai số tiền hoặc webhook đến sau expiration/cancellation cũng được lưu `processed=false`; không revive payment/order.
- Cấm dùng `contains(...)` / `indexOf(...)` để match order reference.
- Late webhook sau khi payment đã `EXPIRED` hoặc `CANCELLED` chỉ được log event `LATE_AFTER_EXPIRY`, không revive payment/order.
- Báo lại order-service khi trạng thái thay đổi.
- Không log secret/API key/chữ ký.
- URL cấu hình trong SePay Dashboard phải là HTTPS public: `https://<public-host>/api/v1/payments/webhook/sepay`. `localhost` không thể được SePay gọi; local development cần ngrok hoặc Cloudflare Tunnel.
- Docker Compose yêu cầu `SEPAY_WEBHOOK_API_KEY` phải được set (không để trống); nếu thiếu, compose fail-fast thay vì chạy payment-service với key rỗng.
- Cấu hình QR dùng `SEPAY_BANK_SHORT_NAME`, `SEPAY_ACCOUNT_NUMBER`, `SEPAY_ACCOUNT_NAME`, `SEPAY_BANK_HUB_PREFIX=SEVQR` và `SEPAY_PAYMENT_CODE_PREFIX=STYLEMIND`; không dùng các biến `VIETQR_*` cũ.

## Dependencies
- **Gọi ra:** order-service (báo status change).
- **Được gọi bởi:** order-service (tạo payment), SePay (webhook), gateway (payment detail/admin).

## Local schema / config notes
- Hiện tại `payment-service` vẫn dùng **init-script** `BE/init-scripts/07-payment-db.sql` cho local Docker schema; chưa bật Flyway trong service này.
- Các env vars chính cho SePay/VietQR:
  - `SEPAY_ENABLED`
  - `SEPAY_MODE`
  - `SEPAY_BANK_SHORT_NAME`
  - `SEPAY_ACCOUNT_NUMBER`
  - `SEPAY_ACCOUNT_NAME`
  - `SEPAY_BANK_HUB_PREFIX`
  - `SEPAY_PAYMENT_CODE_PREFIX`
  - `SEPAY_QR_BASE_URL`
  - `SEPAY_WEBHOOK_AUTH_MODE`
  - `SEPAY_WEBHOOK_API_KEY`
  - `SEPAY_PAYMENT_EXPIRE_MINUTES`

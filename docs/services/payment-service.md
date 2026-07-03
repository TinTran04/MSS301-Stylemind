# payment-service

**Port:** `8088` &nbsp;|&nbsp; **Database:** `payment_db`

## Purpose
Xử lý thanh toán **COD** và **SePay VietQR** (Open Banking). Nhận **webhook** từ SePay, đối soát, cập nhật trạng thái và báo lại order-service.

## Owns (dữ liệu service này sở hữu)
- Payment (method COD/SEPAY, status PENDING/PAID/FAILED), transaction log, VietQR reference (nội dung CK duy nhất).

## Does NOT own
- Không sở hữu order (thuộc order-service).

## API — Public / Customer
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/v1/payments/webhook/sepay` | Webhook SePay (verify + idempotent, không JWT) |
| GET | `/api/v1/payments/{transactionId}` | Xem payment detail |

## API — Admin (role ADMIN)
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/v1/admin/payments` | Admin xem payment logs |

## API — Internal (`X-Internal-Token`, frontend cấm gọi)
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/internal/v1/payments/cod` | Tạo COD transaction |
| POST | `/internal/v1/payments/sepay` | Tạo SePay transaction + sinh VietQR |
| GET | `/internal/v1/payments/orders/{orderId}` | Payment theo order |

## Key business rules
- SePay là Open Banking (webhook), KHÔNG phải cổng thẻ redirect. Bỏ thuật ngữ 'simulated online payment'.
- Webhook **verify** chữ ký/API key SePay trước khi xử lý.
- **Idempotent**: lưu `transactionId` SePay; đã xử lý thì bỏ qua.
- **Đối soát**: khớp nội dung CK (order reference) + đúng số tiền mới set PAID; sai → FAILED.
- Báo lại order-service khi trạng thái thay đổi.
- Không log secret/API key/chữ ký.

## Dependencies
- **Gọi ra:** order-service (báo status change).
- **Được gọi bởi:** order-service (tạo payment), SePay (webhook), gateway (payment detail/admin).

## Notes
Dev dùng SePay sandbox + tài khoản test; demo dùng tài khoản thật (gói FREE hỗ trợ webhook).

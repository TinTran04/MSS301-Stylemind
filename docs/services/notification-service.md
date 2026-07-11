# notification-service

**Port:** `8089` &nbsp;|&nbsp; **Database:** `notification_db`

## Purpose
Lưu và quản lý notification logs; hỗ trợ customer xem thông báo và admin xem/retry.

## Owns (dữ liệu service này sở hữu)
- Notification log (status SENT/FAILED/PENDING).

## Does NOT own
- MVP có thể stub gửi email/SMS thật.

## API — Public / Customer
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/v1/notifications` | Customer notification list |
| GET | `/api/v1/notifications/{id}` | Notification detail |

## API — Admin (role ADMIN)
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/v1/admin/notifications` | Admin logs |
| GET | `/api/v1/admin/notifications/summary` | Admin dashboard: failed notification count |
| POST | `/api/v1/admin/notifications/{id}/retry` | Retry failed |

## API — Internal (`X-Internal-Token`, frontend cấm gọi)
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/internal/v1/notifications` | Tạo notification log |

## Key business rules
- Được gọi trong checkout; **fail không ảnh hưởng order** (caller log rồi đi tiếp).
- Customer chỉ xem notification của mình.
- **Email sender:** From = `app.mail.from-name` (mặc định `StyleMind`) + `app.mail.from-address` (mặc định `spring.mail.username`), qua `MimeMessageHelper.setFrom(address, name)` → hiển thị `StyleMind <account>`. Không đổi SMTP auth; UTF-8 giữ nguyên cho nội dung tiếng Việt.
- Không log OTP/secret (OTP chỉ trong htmlContent; `content` redact `[PROTECTED]`).

## Dependencies
- **Gọi ra:** —
- **Được gọi bởi:** order-service (tạo log khi checkout), gateway.

## Notes
Retry là cơ chế đơn giản ở MVP; nâng cấp outbox/queue ở phase sau.

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
| POST | `/api/v1/admin/notifications/{id}/retry` | Retry failed |

## API — Internal (`X-Internal-Token`, frontend cấm gọi)
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/internal/v1/notifications` | Tạo notification log |

## Key business rules
- Được gọi trong checkout; **fail không ảnh hưởng order** (caller log rồi đi tiếp).
- Customer chỉ xem notification của mình.

## Dependencies
- **Gọi ra:** —
- **Được gọi bởi:** order-service (tạo log khi checkout), gateway.

## Notes
Retry là cơ chế đơn giản ở MVP; nâng cấp outbox/queue ở phase sau.

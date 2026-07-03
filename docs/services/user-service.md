# user-service

**Port:** `8082` &nbsp;|&nbsp; **Database:** `user_db`

## Purpose
Sở hữu **profile mua sắm**: style profile và địa chỉ giao hàng. Tham chiếu `userId` do auth-service sinh ra.

## Owns (dữ liệu service này sở hữu)
- Style profile, delivery addresses, sở thích.

## Does NOT own
- **KHÔNG** giữ password/role/login (thuộc auth-service).

## API — Public / Customer
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/v1/users/style-profile` | Lấy style profile (lazy-init nếu chưa có) |
| PUT | `/api/v1/users/style-profile` | Upsert style profile |
| GET | `/api/v1/users/addresses` | Danh sách địa chỉ |
| POST | `/api/v1/users/addresses` | Tạo địa chỉ |
| PUT | `/api/v1/users/addresses/{id}` | Cập nhật địa chỉ |
| DELETE | `/api/v1/users/addresses/{id}` | Xóa địa chỉ |

## API — Admin (role ADMIN)
_(không có)_

## API — Internal (`X-Internal-Token`, frontend cấm gọi)
_(không có)_

## Key business rules
- **Lazy-init**: lần đầu customer truy cập style-profile → get-or-create theo `userId`.
- Một user chỉ có một default address.
- Đọc `userId` từ gateway-injected header/JWT, không từ body.

## Dependencies
- **Gọi ra:** —
- **Được gọi bởi:** gateway (profile pages).

## Notes
MVP dùng lazy-init (Cách B). Có thể nâng lên event-driven khi có message broker.

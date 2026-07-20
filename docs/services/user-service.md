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
| GET | `/api/v1/users/administrative/provinces` | Danh sách tỉnh/thành phố active |
| GET | `/api/v1/users/administrative/provinces/{provinceCode}/wards` | Danh sách phường/xã thuộc tỉnh/thành phố |

Địa chỉ mới bắt buộc gửi `recipientName`, `phoneNumber`, `provinceCode`,
`wardCode` và `addressLine`. User Service kiểm tra quan hệ tỉnh/phường bằng
dữ liệu local đã pin (`v4.0.0`), xác thực số điện thoại Việt Nam bằng
libphonenumber và lưu số hợp lệ ở dạng E.164. Địa chỉ cũ không có mã hành chính
giữ nguyên dữ liệu nhưng có `validationStatus=LEGACY_UNVERIFIED` và không được
dùng cho checkout cho đến khi cập nhật.

## API — Admin (role ADMIN)
_(không có)_

## API — Internal (`X-Internal-Token`, frontend cấm gọi)
| GET | `/internal/v1/users/{userId}/addresses/{addressId}` | Kiểm tra ownership và trả địa chỉ `VALID` cho order-service |

## Key business rules
- **Lazy-init**: lần đầu customer truy cập style-profile → get-or-create theo `userId`.
- Một user chỉ có một default address.
- Chỉ địa chỉ `VALID` mới đủ điều kiện checkout; ownership được kiểm tra theo `userId` trên URL nội bộ.
- Đọc `userId` từ gateway-injected header/JWT, không từ body.

## Dependencies
- **Gọi ra:** —
- **Được gọi bởi:** gateway (profile pages).

## Notes
MVP dùng lazy-init (Cách B). Có thể nâng lên event-driven khi có message broker.

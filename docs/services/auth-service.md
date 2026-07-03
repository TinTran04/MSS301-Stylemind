# auth-service

**Port:** `8081` &nbsp;|&nbsp; **Database:** `auth_db`

## Purpose
Sở hữu **identity**: đăng ký, đăng nhập, quên/đặt lại mật khẩu, và **quản lý account user cho admin** (kèm self-protection). Là source of truth của `userId`.

## Owns (dữ liệu service này sở hữu)
- Account: email, password hash, role (CUSTOMER/ADMIN), status (ACTIVE/DISABLED).
- Reset token/OTP (chỉ lưu hash, có expiry, dùng một lần).

## Does NOT own
- Không sở hữu style profile / địa chỉ (thuộc user-service).

## API — Public / Customer
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/v1/auth/register` | Đăng ký |
| POST | `/api/v1/auth/login` | Đăng nhập → JWT |
| GET | `/api/v1/auth/me` | User hiện tại |
| POST | `/api/v1/auth/forgot-password` | Yêu cầu reset (message chung) |
| POST | `/api/v1/auth/verify-reset-otp` | Xác thực OTP/token |
| POST | `/api/v1/auth/reset-password` | Đặt lại mật khẩu |

## API — Admin (role ADMIN)
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/v1/admin/accounts` | Danh sách (search/filter keyword,role,status) |
| POST | `/api/v1/admin/accounts` | Tạo account |
| PATCH | `/api/v1/admin/accounts/{userId}/status` | Enable/disable |
| PATCH | `/api/v1/admin/accounts/{userId}/role` | Cập nhật role |
| DELETE | `/api/v1/admin/accounts/{userId}` | Xóa (chặn self & last-admin) |

## API — Internal (`X-Internal-Token`, frontend cấm gọi)
_(không có)_

## Key business rules
- Password hash (BCrypt); login sai trả message chung; disabled user không login.
- forgot-password không tiết lộ email tồn tại; OTP/token có hạn, dùng một lần, chỉ lưu hash.
- Không trả password/OTP/reset token trong response hay log.
- **Self-protection:** admin không tự disable/delete/hạ role chính mình; không disable/delete/hạ **admin cuối cùng** còn ACTIVE → **409**.
- So sánh `targetUserId` với admin id lấy từ JWT/gateway header.

## Dependencies
- **Gọi ra:** —
- **Được gọi bởi:** gateway (auth & admin account), gián tiếp là mọi service dùng `userId`.

## Notes
Cân nhắc bảng `audit_log` cho hành động admin phá hủy. Xem `architecture/03-auth-user-boundary.md`.

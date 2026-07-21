# Auth ↔ User Boundary (Identity vs Profile)

Câu hỏi kinh điển: "user" nằm ở service nào? Tách theo **bounded context** — mỗi service một mối quan tâm.

| Tiêu chí | auth-service (Identity) | user-service (Profile) |
|---|---|---|
| Trả lời câu hỏi | "Bạn có được vào không?" | "Bạn là ai khi mua sắm?" |
| Dữ liệu sở hữu | email, password hash, role, account status, reset token/OTP | basic profile và địa chỉ giao hàng |
| Database | auth_db | user_db |
| Admin account mgmt | **THUỘC ĐÂY** | Không |
| Giữ password/role/login? | Có | **TUYỆT ĐỐI KHÔNG** |

## Ai sinh userId?
`auth-service` là **source of truth** của identity → sinh `userId` khi register. `user-service` chỉ lưu profile **tham chiếu** tới `userId`.

## Basic profile và địa chỉ

`user_profiles` giữ các trường basic profile được xác nhận, hiện là `display_name`. Delivery address nằm riêng trong `delivery_addresses`; đọc danh sách địa chỉ không tạo thêm profile shell. Auth Service vẫn là source of truth của identity và email.

Địa chỉ giao hàng được quản lý qua public User Service API bằng principal từ JWT. Order Service chỉ nhận `addressId`, sau đó gọi internal User Service để lấy dữ liệu authoritative và lưu snapshot bất biến cho Order.

> **Nguyên tắc vàng:** user-service không bao giờ giữ password/role/login. Nếu thấy mình định để password ở user-service → ranh giới đang sai.

# Auth ↔ User Boundary (Identity vs Profile)

Câu hỏi kinh điển: "user" nằm ở service nào? Tách theo **bounded context** — mỗi service một mối quan tâm.

| Tiêu chí | auth-service (Identity) | user-service (Profile) |
|---|---|---|
| Trả lời câu hỏi | "Bạn có được vào không?" | "Bạn là ai khi mua sắm?" |
| Dữ liệu sở hữu | email, password hash, role, account status, reset token/OTP | style profile, địa chỉ, sở thích |
| Database | auth_db | user_db |
| Admin account mgmt | **THUỘC ĐÂY** | Không |
| Giữ password/role/login? | Có | **TUYỆT ĐỐI KHÔNG** |

## Ai sinh userId?
`auth-service` là **source of truth** của identity → sinh `userId` khi register. `user-service` chỉ lưu profile **tham chiếu** tới `userId`.

## Khi nào tạo profile? (2 cách)
| Cách | Mô tả | MVP? |
|---|---|---|
| A. Event-driven | Register → auth phát event `UserRegistered` → user-service nghe & tạo profile rỗng. Cần Kafka/RabbitMQ. | Phase sau |
| B. Lazy-init ⭐ | Register chỉ tạo account. Lần đầu mở style-profile → user-service **get-or-create** theo userId. Không cần broker. | **Khuyên dùng** |

> **Nguyên tắc vàng:** user-service không bao giờ giữ password/role/login. Nếu thấy mình định để password ở user-service → ranh giới đang sai.

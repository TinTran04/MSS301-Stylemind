# Security Architecture

## Auth flow
1. Login tại auth-service → trả **JWT**.
2. Frontend gắn JWT vào mọi request qua Gateway.
3. Gateway **validate JWT**, chặn admin path, rồi **inject** `X-User-Id` / `X-User-Roles` xuống service.
4. Service tin context từ gateway; không tự đọc header user do client gửi.

## Nguyên tắc chính
| Chủ đề | Yêu cầu |
|---|---|
| AuthN | JWT; password hash (BCrypt); login sai trả message chung. |
| AuthZ | RBAC (CUSTOMER/ADMIN); `/api/v1/admin/**` yêu cầu ADMIN. |
| Internal | `/internal/v1/**` chỉ gọi bằng `X-Internal-Token`; frontend cấm gọi. |
| Webhook | `/api/v1/payments/webhook/sepay` verify chữ ký/API key SePay + idempotent (không JWT). |
| Admin self-protection | Không tự khóa/xóa/hạ quyền; bảo vệ admin cuối cùng. |
| Secrets | Không log password/OTP/reset token/secret SePay; để trong env/secret manager. |
| Rate limit | Login & forgot-password nên có rate limit. |
| Audit | Thao tác admin phá hủy nên ghi audit log. |

Danh sách requirement có mã (SEC-01..): `requirements/03-security-requirements.md`.

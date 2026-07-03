# MVP Acceptance Criteria

| Area | Criteria |
|---|---|
| Auth | Register, login, me, forgot/verify/reset password hoạt động. |
| Admin Account | Xem/tạo/disable/enable/role; self-protection & last-admin enforce. |
| Product | Public list/detail + admin product/category. |
| Cart | Guest/auth/merge, add/update/remove/clear. |
| Checkout | Tạo order từ cart; orchestration + timeout/expire. |
| Order | Customer xem order; admin xem & đổi trạng thái theo state machine. |
| Payment | COD & SePay VietQR (webhook confirm, idempotent). |
| Notification | Order tạo log; admin xem logs. |
| AI Stylist | Chat + gợi ý cơ bản. |
| Security | CUSTOMER không vào admin; frontend không gọi internal; webhook verify. |
| Deployment | Chạy local bằng Docker Compose hoặc IntelliJ. |

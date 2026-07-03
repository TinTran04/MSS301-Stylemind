# BRD — Business Requirements

## Phạm vi nghiệp vụ — In Scope
| Nhóm | Chức năng |
|---|---|
| Customer Shopping | Xem/chi tiết/tìm kiếm/filter sản phẩm, cart, checkout, theo dõi đơn. |
| Authentication | Register, login, JWT, forgot password, verify OTP/reset token, reset password. |
| User Profile | Hồ sơ khách, style profile, địa chỉ giao hàng. |
| Product Catalog | Danh mục, sản phẩm, biến thể, hình ảnh. |
| Cart | Guest cart, auth cart, merge sau login, update/remove/clear. |
| Order | Tạo đơn, xem danh sách/chi tiết, admin cập nhật trạng thái theo state machine. |
| Payment | COD và SePay VietQR (xác nhận webhook). |
| Notification | Notification logs, customer/admin notification. |
| AI Stylist | AI chat, gợi ý sản phẩm/outfit, bundles, AI index jobs. |
| Admin Management | Product/category, **account user (+ self-protection)**, status, role, order, notification, AI jobs. |

## Out of Scope (MVP)
| Ngoài phạm vi | Lý do |
|---|---|
| Full inventory tracking | MVP chưa quản lý tồn kho thật. |
| Inventory reservation | Chưa giữ hàng trong checkout saga → compensation nhẹ (chỉ đổi trạng thái). |
| Cổng thẻ quốc tế production | COD + SePay VietQR đủ cho demo. |
| Email/SMS delivery production | Notification stub/log trước, gửi thật ở phase sau. |
| AI recommendation production-grade | MVP dùng mock/partial; hoàn thiện sau. |

> **Lưu ý v2.0:** Admin **account management** và **order management** thuộc phạm vi hiện tại, KHÔNG phải phần mở rộng. Thanh toán online dùng **SePay thật**.

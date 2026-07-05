# PRD — Product Requirements

## Product Vision
StyleMind giúp khách mua sắm nhanh hơn, chọn sản phẩm phù hợp hơn và nhận tư vấn phối đồ cá nhân hóa từ AI stylist. Phục vụ 2 nhóm: **Customer** (mua sắm + AI) và **Admin** (vận hành).

## Product Goals
| Goal | Mô tả | Success Criteria |
|---|---|---|
| G1 | Mua hàng end-to-end | Browse → Cart → Checkout → Payment (COD/SePay) → Tracking. |
| G2 | Auth đầy đủ | Register, login, me, forgot/verify/reset password hoạt động. |
| G3 | Admin quản lý account | Xem/tạo/khóa/mở/role; tuân thủ self-protection. |
| G4 | Admin quản lý catalog | Category CRUD có conflict guard; product tạo/cập nhật; unique-SKU variant; image upload/replace qua backend; public catalog chỉ dùng dữ liệu thật. |
| G5 | Admin quản lý order | Xem list/detail; cập nhật trạng thái theo state machine. |
| G6 | Payment MVP | COD và SePay VietQR (webhook confirm). |
| G7 | AI stylist | Chat + gợi ý sản phẩm/outfit. |
| G8 | Microservices ổn định | Chạy local bằng IntelliJ hoặc Docker Compose. |

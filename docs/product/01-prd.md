# PRD — Product Requirements

## Product Vision
StyleMind giúp khách mua sắm nhanh hơn, chọn sản phẩm phù hợp hơn và nhận tư vấn phối đồ cá nhân hóa từ AI stylist. Phục vụ 2 nhóm: **Customer** (mua sắm + AI) và **Admin** (vận hành).

## Product Goals
| Goal | Mô tả | Success Criteria |
|---|---|---|
| G1 | Mua hàng end-to-end | Browse → Cart → Checkout → Payment (COD/SePay) → Tracking. |
| G2 | Auth đầy đủ | Register, login, me, forgot/verify/reset password hoạt động. |
| G3 | Admin quản lý account | Xem/tạo/khóa/mở/role; tuân thủ self-protection. |
| G4 | Admin quản lý catalog | Category CRUD có conflict guard; guided product create; unique-SKU variant; image upload/replace qua backend; ACTIVE product luôn có variant; public catalog chỉ dùng dữ liệu thật. |
| G5 | Admin quản lý order | Xem list/detail; cập nhật trạng thái theo state machine. |
| G6 | Payment MVP | COD và SePay VietQR (webhook confirm). |
| G7 | AI stylist | Chat + gợi ý sản phẩm/outfit. |
| G8 | Microservices ổn định | Chạy local bằng IntelliJ hoặc Docker Compose. |

## Admin Product Creation
- Add Product là flow Product Info → Variants → Images / Finish.
- Product Info tạo basic product bằng API hiện có và luôn lưu `INACTIVE`.
- Admin dùng variant/image subresource hiện có sau khi nhận `productId`; không có aggregate create API và không đổi `ProductResponse`.
- Finish/Publish chỉ khả dụng sau khi có ít nhất một persisted variant.
- Product không có variant không được publish hoặc hiển thị cho customer.
- ACTIVE product không được xóa final variant; admin phải deactivate trước.
- Admin Product Management phải chuyển conflict/validation/network errors thành hướng dẫn hành động, đồng thời giữ drawer và dữ liệu nhập cho lỗi có thể phục hồi.
- `PRODUCT_REQUIRES_VARIANT` hướng admin thêm variant; `LAST_ACTIVE_VARIANT` hướng admin deactivate trước; duplicate SKU yêu cầu một SKU khác.
- Image upload failure sau create là partial success và không rollback product.

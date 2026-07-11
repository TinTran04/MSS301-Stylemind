# PRD — Product Requirements

## Product Vision
StyleMind giúp khách mua sắm nhanh hơn, chọn sản phẩm phù hợp hơn và nhận tư vấn phối đồ cá nhân hóa từ AI stylist. Phục vụ 2 nhóm: **Customer** (mua sắm + AI) và **Admin** (vận hành).

## Product Goals
| Goal | Mô tả | Success Criteria |
|---|---|---|
| G1 | Mua hàng end-to-end | Browse → Cart → Checkout → Payment (COD/SePay) → Tracking. |
| G2 | Auth đầy đủ | Register, login, me, forgot/verify/reset password hoạt động. |
| G3 | Admin quản lý account | Xem/tạo/khóa/mở/role; tuân thủ self-protection. |
| G4 | Admin quản lý catalog | Category CRUD có conflict guard; guided product create; variant là tổ hợp size/color/material cụ thể với unique SKU và stock riêng; image upload/replace qua backend; ACTIVE product luôn có variant; public catalog chỉ dùng dữ liệu thật. |
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
- `PRODUCT_REQUIRES_VARIANT` hướng admin thêm variant; `LAST_ACTIVE_VARIANT` hướng admin deactivate trước; duplicate SKU yêu cầu một SKU khác; `DUPLICATE_VARIANT` (trùng size+color+material) yêu cầu kiểm tra lại phân loại.
- Image upload failure sau create là partial success và không rollback product.

## Product Variant & Stock
- Mỗi variant là một tổ hợp cụ thể **size + color + material (tùy chọn)** với SKU riêng, `priceOverride` tùy chọn, `stockQuantity` và `active` riêng — không phải danh sách size/color độc lập.
- Admin quản lý stock trực tiếp trên từng variant (thêm/sửa/xóa, xem và chỉnh số lượng tồn kho).
- Customer chỉ được chọn tổ hợp size/color thực sự tồn tại trong dữ liệu variant; nhãn màu/kích cỡ trùng lặp do khác hoa/thường (`trắng`/`Trắng`) được gộp khi hiển thị nhưng vẫn khớp đúng variant gốc khi thêm vào giỏ.
- Tổ hợp hết hàng (`stockQuantity = 0`) hoặc bị vô hiệu hóa (`active = false`) hiển thị nhưng bị disable, nhãn "Hết hàng"; Add to Cart chỉ bật khi đã chọn variant hợp lệ và còn hàng.

## Customer Browsing
- Shop cho phép lọc sản phẩm theo `targetDemographic` với 4 chế độ hiển thị: `Tất cả`, `Nam`, `Nữ`, `Unisex`.
- Product card cart icon trên storefront là shortcut đi tới trang chi tiết sản phẩm để chọn variant, không phải quick-add trực tiếp vào giỏ.

# 3. Change Log

## v2.0 — thay đổi so với bản gốc
| # | Thay đổi | Ảnh hưởng |
|---|---|---|
| 1 | Thanh toán online dùng **SePay thật** (VietQR/Open Banking), xác nhận qua **Webhook** thay cho "sandbox payment" mô phỏng. | payment-service, order flow, checkout saga, notification |
| 2 | Cập nhật 3 điểm quan trọng: admin scope (có account user), auth đầy đủ, thuật ngữ payment → SePay. | business, product |
| 3 | **Admin self-protection**: không tự khóa/xóa/hạ quyền; bảo vệ admin cuối cùng. | auth-service |
| 4 | Chuẩn hóa **API versioning** `/api/v1/...`, `/internal/v1/...`. | toàn bộ API, gateway, frontend |
| 5 | Bổ sung **Order State Machine** chặn admin cập nhật trạng thái không hợp lệ. | order-service |
| 6 | Định nghĩa **Checkout orchestration + Saga**; saga cơ bản đưa vào Sprint 3. | order-service, roadmap |
| 7 | Làm rõ **ranh giới auth-service ↔ user-service** (Identity vs Profile) + lazy-init. | auth-service, user-service |
| 8 | Hoàn thiện catalog: category conflict guard, Cloudinary image metadata/upload, variant wiring và product-first public listing không dùng dữ liệu giả. | product-service, frontend |
| 9 | Add Product chuyển sang guided flow; product mới mặc định INACTIVE, chỉ ACTIVE khi có variant và chặn xóa final variant của ACTIVE product. | product-service, frontend, docs |
| 10 | Admin Product Management hiển thị lỗi thân thiện, field guidance và recovery action; recoverable errors giữ nguyên drawer/form, image upload failure là partial success. | frontend, docs |
| 10 | Customer Shop category filter hiển thị **toàn bộ category thật** (gồm cả con) từ `GET /api/v1/categories`; endpoint public không param nay trả danh sách phẳng thay vì chỉ root. "Tất cả" là option FE để clear filter; không hardcode category. | product-service, frontend, docs |

Chi tiết kỹ thuật của mục 4–8 nằm trong thư mục `architecture/` và `decisions/`.

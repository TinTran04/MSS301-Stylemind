# Frontend Requirements

Stack: ReactJS/Vite. Frontend chỉ gọi API qua Gateway (`VITE_API_BASE_URL`, vd `http://localhost:3001`), path dạng `/api/v1/...`.

## Route structure
```
/
├── /login
├── /register
├── /forgot-password
├── /verify-reset-otp
├── /reset-password
├── /products
├── /products/:id
├── /cart
├── /checkout          (chọn COD/SePay; SePay → màn VietQR)
├── /orders
├── /orders/:id
├── /profile
├── /ai-stylist
└── /admin
    ├── /dashboard
    ├── /accounts
    ├── /products
    ├── /categories
    ├── /orders
    ├── /payments
    ├── /notifications
    └── /ai-pipeline
```

## Rules
- Chỉ gọi `VITE_API_BASE_URL`; không gọi port service (8081, 8083, 8087...); không gọi `/internal/v1/**`.
- Không tự gửi `X-User-Id`/`X-User-Roles` (gateway inject).
- Màn checkout SePay hiển thị VietQR + hướng dẫn; **poll** `GET /api/v1/orders/{id}` (hoặc SSE) tới khi PAID/EXPIRED.
- Sau khi order PAID/CONFIRMED → clear hoặc refetch cart.
- `/cart` hiển thị tên/ảnh/size/color/material/giá thật từ `variant`/`variant.product` do cart-service enrich từ product-service; "Product"/"One Size"/"Default" chỉ là fallback khi dữ liệu thật sự thiếu, không phải hành vi mặc định. Item có `available: false` hiển thị "This item is no longer available." thay vì giá `$0`. Giá trên `/cart` chỉ để hiển thị; checkout/order vẫn dùng giá authoritative từ product-service.
- UI payment ghi "Thanh toán qua SePay (VietQR)", không dùng "Simulated Online Payment".
- CUSTOMER không thấy admin menu; chỉ ADMIN vào `/admin/**`.
- Shop (`/shop`) load category từ `GET /api/v1/categories` (qua gateway), render danh sách phẳng thật gồm cả category con; **không hardcode**. "Tất cả" là option FE để clear filter; chọn category lọc theo `category=<id>`. Lỗi tải category → giữ "Tất cả" + thông báo thân thiện, không crash.
- `/admin/products` Add Product dùng ba bước: Product Info → Variants → Images / Finish.
- Step 1 gọi create product API hiện có với `INACTIVE`, giữ drawer mở và lưu `productId`; Step 2/3 tái sử dụng variant/image API hiện có.
- Continue, Finish và Publish bị disable tới khi có ít nhất một persisted variant. Backend `409` vẫn là guard authoritative.
- Đóng drawer sau Step 1 giữ product ở trạng thái `INACTIVE` và cảnh báo product chưa có variant.
- Publish gọi status API qua Gateway; UI hiển thị message từ `PRODUCT_REQUIRES_VARIANT` hoặc `LAST_ACTIVE_VARIANT`.
- Product actions phải hiển thị lỗi thân thiện, có title/message/action rõ ràng; không dùng raw JSON, stack trace hoặc backend technical message làm primary copy.
- Recoverable errors giữ drawer mở và không xóa form state: `PRODUCT_REQUIRES_VARIANT` đưa admin về Variants, `LAST_ACTIVE_VARIANT` hướng dẫn deactivate, duplicate SKU giữ draft và đánh dấu trường SKU.
- Product/variant validation hiển thị field-level guidance. Category load failure disable create-product và cho phép retry.
- Image upload failure sau khi product đã tồn tại là partial success: giữ Images step, selected image và cho phép retry; không rollback product. Nếu backend trả `IMAGE_STORAGE_NOT_CONFIGURED` (Cloudinary chưa cấu hình), hiển thị message riêng ("Chưa cấu hình dịch vụ lưu ảnh...") thay vì message upload thất bại chung.
- Friendly error hiển thị theo từng section trong drawer thay vì một alert dùng chung: lỗi variant (add/update/delete, `LAST_ACTIVE_VARIANT`, duplicate SKU) hiển thị ngay trong khu vực Variants, cạnh danh sách variant; lỗi image hiển thị trong khu vực Images. Toast góc màn hình chỉ dùng cho thông báo cấp trang (load list, category, create/update/delete thành công), không dùng cho lỗi variant vốn đã hiển thị inline.
- Xóa variant/product/category dùng dialog xác nhận theo style dự án (component `Modal` dùng chung), không dùng `window.confirm`/`window.alert` của trình duyệt.
- Admin Category Management (`/admin/products` → "Danh mục") chỉ gọi Gateway (`/api/v1/admin/categories`), không gọi thẳng product-service (`localhost:8083`) và không gọi `/internal/v1/**`. Local dev CORS cho phép origin `http://localhost:5173` qua Gateway (`spring.cloud.gateway.globalcors`); Gateway dedupe các header CORS trùng lặp do từng service tự thêm (`Access-Control-Allow-Origin/Credentials/Expose-Headers`, `Vary`). Category APIs vẫn yêu cầu role ADMIN.
- Category load/create/update/delete failure hiển thị thông báo thân thiện tiếng Việt trong Manage Categories drawer, không hiện lỗi CORS/kỹ thuật thô cho admin.
- Toàn bộ Admin Dashboard (User/Order/Notification/AI Pipeline Management, không chỉ Product Management) dùng dialog xác nhận theo style dự án (`components/admin/AdminConfirmDialog`, dựng trên `Modal` dùng chung) cho hành động phá hủy/tác động lớn (xóa tài khoản, vô hiệu hóa tài khoản, đổi trạng thái đơn hàng, gửi lại thông báo, tạo AI index job); không dùng `window.confirm`/`window.alert` của trình duyệt.
- Lỗi có thể khôi phục (self-protection, last-admin, invalid order transition, retry thất bại...) hiển thị inline cạnh khu vực/hàng/action gây lỗi (vd. panel thao tác user, khu vực Change Status trong Order drawer, cạnh từng dòng thông báo), không đóng drawer/modal, không xóa dữ liệu form. Toast góc màn hình chỉ dùng cho thông báo thành công hoặc lỗi tải trang cấp cao (list/dashboard). Admin-facing messages đều bằng tiếng Việt (`features/admin/admin-error-messages.js` chuẩn hóa 401/403/network cho các trang chưa có helper riêng).

## Checklist chất lượng mỗi màn
- Loading / Error / Empty state đầy đủ.
- API client tách riêng (một axios instance + interceptor gắn JWT).
- Server state qua TanStack Query/SWR; validation form rõ ràng.
- Admin: dropdown đổi trạng thái order chỉ hiện transition hợp lệ (đồng bộ với state machine); xử lý 409 thân thiện.

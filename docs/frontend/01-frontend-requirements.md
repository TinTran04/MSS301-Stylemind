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
- Sau khi login thành công, frontend gọi `POST /api/v1/cart/merge` qua Gateway (JWT tự đính qua apiClient) để gộp guest cart vào cart của user. Guest session id trong `localStorage` chỉ bị xóa khi merge **thành công**; nếu merge lỗi, giữ nguyên guest session id (để lần đăng nhập sau vẫn thử gộp lại) và hiển thị "Chưa thể đồng bộ giỏ hàng. Hệ thống chưa thể chuyển giỏ hàng tạm thời vào tài khoản của bạn. Vui lòng thử lại sau." — không mất guest cart âm thầm. Cart của user vẫn tải được bình thường dù merge lỗi.
- Khi logout (`auth.store.js`'s `logout()`), ngoài xóa auth token/session còn xóa cart state trong bộ nhớ (`cart.store.js`'s `resetLocalCart()` — chỉ reset state cục bộ, không gọi API xóa cart trên server) và xóa guest session id còn sót trong `localStorage`, để cart/badge của user cũ không hiện lại và phiên guest mới sau logout được tạo lại từ đầu.
- UI payment ghi "Thanh toán qua SePay (VietQR)", không dùng "Simulated Online Payment".
- CUSTOMER không thấy admin menu; chỉ ADMIN vào `/admin/**`.
- Shop (`/shop`) load category từ `GET /api/v1/categories` (qua gateway), render danh sách phẳng thật gồm cả category con; **không hardcode**. "Tất cả" là option FE để clear filter; chọn category lọc theo `category=<id>`. Lỗi tải category → giữ "Tất cả" + thông báo thân thiện, không crash.
- `/admin/products` Add Product dùng ba bước: Product Info → Variants → Images / Finish.
- Step 1 gọi create product API hiện có với `INACTIVE`, giữ drawer mở và lưu `productId`; Step 2/3 tái sử dụng variant/image API hiện có.
- Continue, Finish và Publish bị disable tới khi có ít nhất một persisted variant. Backend `409` vẫn là guard authoritative.
- Đóng drawer sau Step 1 giữ product ở trạng thái `INACTIVE` và cảnh báo product chưa có variant.
- Publish gọi status API qua Gateway; UI hiển thị message từ `PRODUCT_REQUIRES_VARIANT` hoặc `LAST_ACTIVE_VARIANT`.
- Toàn bộ Admin Dashboard hiển thị text tiếng Việt cho sidebar, header, filter, table, badge, button, empty state và alert; các enum/backend payload values (vd. `ACTIVE`, `PENDING`, `ADMIN`, `GOOGLE`) vẫn giữ nguyên, chỉ đổi lớp hiển thị.
- Product actions phải hiển thị lỗi thân thiện, có title/message/action rõ ràng; không dùng raw JSON, stack trace hoặc backend technical message làm primary copy.
- Recoverable errors giữ drawer mở và không xóa form state: `PRODUCT_REQUIRES_VARIANT` đưa admin về Variants, `LAST_ACTIVE_VARIANT` hướng dẫn deactivate, duplicate SKU giữ draft và đánh dấu trường SKU.
- Product/variant validation hiển thị field-level guidance. Category load failure disable create-product và cho phép retry.
- Image upload failure sau khi product đã tồn tại là partial success: giữ Images step, selected image và cho phép retry; không rollback product. Nếu backend trả `IMAGE_STORAGE_NOT_CONFIGURED` (Cloudinary chưa cấu hình), hiển thị message riêng ("Chưa cấu hình dịch vụ lưu ảnh...") thay vì message upload thất bại chung.
- Friendly error hiển thị theo từng section trong drawer thay vì một alert dùng chung: lỗi variant (add/update/delete, `LAST_ACTIVE_VARIANT`, duplicate SKU) hiển thị ngay trong khu vực Variants, cạnh danh sách variant; lỗi image hiển thị trong khu vực Images. Toast góc màn hình chỉ dùng cho thông báo cấp trang (load list, category, create/update/delete thành công), không dùng cho lỗi variant vốn đã hiển thị inline.
- Xóa variant/product/category dùng dialog xác nhận theo style dự án (component `Modal` dùng chung), không dùng `window.confirm`/`window.alert` của trình duyệt.
- Admin Category Management (`/admin/products` → "Danh mục") chỉ gọi Gateway (`/api/v1/admin/categories`), không gọi thẳng product-service (`localhost:8083`) và không gọi `/internal/v1/**`. Local dev CORS cho phép origin `http://localhost:5173` qua Gateway (`spring.cloud.gateway.globalcors`); Gateway dedupe các header CORS trùng lặp do từng service tự thêm (`Access-Control-Allow-Origin/Credentials/Expose-Headers`, `Vary`). Category APIs vẫn yêu cầu role ADMIN.
- Category load/create/update/delete failure hiển thị thông báo thân thiện tiếng Việt trong Manage Categories drawer, không hiện lỗi CORS/kỹ thuật thô cho admin.
- Admin Product Management variant form quản lý biến thể như tổ hợp size/color/material/SKU cụ thể, gồm "SKU", "Kích cỡ", "Màu sắc", "Chất liệu", "Giá ghi đè", "Số lượng tồn kho", "Trạng thái"; validate: SKU/Kích cỡ/Màu sắc bắt buộc, "Số lượng tồn kho phải lớn hơn hoặc bằng 0.", "Giá ghi đè phải lớn hơn 0 nếu được nhập.". Trùng SKU hoặc trùng tổ hợp size+color+material hiện lỗi thân thiện ("Biến thể này đã tồn tại. Vui lòng kiểm tra lại kích cỡ, màu sắc và chất liệu.") ngay trong khu vực Variants.
- Product Detail (customer) chọn variant theo flow size-trước: tất cả size luôn hiển thị và luôn có thể bấm (kể cả khi một size khác đang được chọn — size khác chỉ mờ đi, không disable); màu chỉ hiển thị sau khi đã chọn size, và chỉ gồm màu thuộc size đó (nhãn trùng do khác hoa/thường như `trắng`/`Trắng` được gộp làm một nút nhưng vẫn ánh xạ đúng giá trị gốc khi resolve variant). Chưa chọn size, khu vực màu hiện "Vui lòng chọn kích cỡ trước." Bấm lại đúng size đang chọn sẽ bỏ chọn (size trở lại bình thường, màu/`variantId` bị xóa); bấm sang size khác sẽ chuyển thẳng sang size đó và tự xóa màu/`variantId` đã chọn trước đó (không giữ lựa chọn cũ). Màu hết hàng hiện nhãn "Hết hàng" và bị disable. Add to Cart chỉ bật khi đã resolve được một `variantId` hợp lệ và còn hàng: chưa chọn size hiện "Vui lòng chọn kích cỡ.", đã chọn size nhưng chưa chọn màu hiện "Vui lòng chọn màu sắc.", tổ hợp hết hàng hiện "Biến thể này đã hết hàng." Add to Cart luôn gửi `variantId` đã resolve, không tự đoán/tạo tổ hợp.
- Toàn bộ Admin Dashboard (User/Order/Notification/AI Pipeline Management, không chỉ Product Management) dùng dialog xác nhận theo style dự án (`components/admin/AdminConfirmDialog`, dựng trên `Modal` dùng chung) cho hành động phá hủy/tác động lớn (xóa tài khoản, vô hiệu hóa tài khoản, đổi trạng thái đơn hàng, gửi lại thông báo, tạo AI index job); không dùng `window.confirm`/`window.alert` của trình duyệt.
- Lỗi có thể khôi phục (self-protection, last-admin, invalid order transition, retry thất bại...) hiển thị inline cạnh khu vực/hàng/action gây lỗi (vd. panel thao tác user, khu vực Change Status trong Order drawer, cạnh từng dòng thông báo), không đóng drawer/modal, không xóa dữ liệu form. Toast góc màn hình chỉ dùng cho thông báo thành công hoặc lỗi tải trang cấp cao (list/dashboard). Admin-facing messages đều bằng tiếng Việt (`features/admin/admin-error-messages.js` chuẩn hóa 401/403/network cho các trang chưa có helper riêng).

## Checklist chất lượng mỗi màn
- Loading / Error / Empty state đầy đủ.
- API client tách riêng (một axios instance + interceptor gắn JWT).
- Server state qua TanStack Query/SWR; validation form rõ ràng.
- Admin: dropdown đổi trạng thái order chỉ hiện transition hợp lệ (đồng bộ với state machine); xử lý 409 thân thiện.

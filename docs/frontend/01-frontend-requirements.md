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
- Image upload failure sau khi product đã tồn tại là partial success: giữ Images step, selected image và cho phép retry; không rollback product.

## Checklist chất lượng mỗi màn
- Loading / Error / Empty state đầy đủ.
- API client tách riêng (một axios instance + interceptor gắn JWT).
- Server state qua TanStack Query/SWR; validation form rõ ràng.
- Admin: dropdown đổi trạng thái order chỉ hiện transition hợp lệ (đồng bộ với state machine); xử lý 409 thân thiện.

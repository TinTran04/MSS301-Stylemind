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

## Checklist chất lượng mỗi màn
- Loading / Error / Empty state đầy đủ.
- API client tách riêng (một axios instance + interceptor gắn JWT).
- Server state qua TanStack Query/SWR; validation form rõ ràng.
- Admin: dropdown đổi trạng thái order chỉ hiện transition hợp lệ (đồng bộ với state machine); xử lý 409 thân thiện.

# PRD Overview — StyleMind

## 1. Product Vision

StyleMind là fashion e-commerce platform có AI stylist assistant, giúp người dùng mua sắm và nhận tư vấn phối đồ cá nhân hóa.

## 2. Product Goals

| Goal | Mô tả | Success Criteria |
|---|---|---|
| G1 | Mua hàng end-to-end | Browse → Cart → Checkout → Order tracking |
| G2 | Auth ổn định | Register/login/me hoạt động |
| G3 | Catalog usable | Product listing/detail/admin CRUD hoạt động |
| G4 | AI stylist có UI/API | Chat UI và API hoạt động |
| G5 | Local stack chạy được | Docker Compose start được hệ thống |
| G6 | Gateway là entry point | Frontend không gọi service trực tiếp |

## 3. Product Modules

```text
Auth
User Profile
Product Catalog
Cart
Order
Payment
Notification
AI Stylist
Admin
```

## 4. Product Constraints

- MVP không có full inventory.
- Payment là COD/simulated payment.
- Notification là log/stub.
- AI có thể mock trong MVP.
- Local deployment bằng Docker Compose.

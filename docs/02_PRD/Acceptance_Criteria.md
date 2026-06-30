# MVP Acceptance Criteria — StyleMind

## 1. Frontend

- React app gọi API Gateway thật.
- Core flow không còn phụ thuộc mock data.
- Có loading/error/empty state.
- Có route guard cho authenticated/admin pages.

## 2. Auth

- Register/login/me hoạt động.
- JWT được gửi qua `Authorization: Bearer <token>`.
- Login sai trả về error response chuẩn.

## 3. Product

- Product listing có pagination.
- Product detail hiển thị variants/images/category.
- Admin CRUD product/category hoạt động.
- Product service là source of truth cho product price.

## 4. Cart

- Guest cart hoạt động.
- Authenticated cart hoạt động.
- Guest cart merge sau login.
- Cart clear sau checkout thành công.

## 5. Order/Payment

- Customer tạo order từ cart.
- Payment COD và simulated payment hoạt động.
- Customer xem order list/detail.
- Payment/order failure có trạng thái rõ ràng.

## 6. AI

- AI chat UI hoạt động.
- AI chat API trả response chuẩn.
- Chat history được lưu.
- Admin tạo/xem AI index jobs.

## 7. Deployment

- `docker-compose up -d` chạy được toàn bộ local stack.
- Các service có health endpoint.
- Gateway route tới các service chính.

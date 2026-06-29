# Business Processes — StyleMind

## 1. Register/Login

```text
Customer → Frontend → API Gateway → auth-service → auth_db
```

### Rules

- User đăng ký bằng email/password.
- User đăng nhập để nhận JWT.
- Frontend gửi JWT trong `Authorization` header.
- API Gateway validate JWT và inject identity headers.

## 2. Product Browsing

```text
Guest/Customer → Frontend → API Gateway → product-service → product_db
```

### Rules

- Guest và Customer đều xem được catalog.
- Listing hỗ trợ pagination/filter/sort.
- Product detail trả về product, variants, images, category.

## 3. Cart Flow

```text
Guest Cart → Login → Merge into Customer Cart → Checkout → Clear Cart
```

### Rules

- Guest có thể thêm sản phẩm vào cart.
- Sau login, guest cart merge vào authenticated cart.
- Sau checkout thành công, cart phải được clear.

## 4. Checkout Flow

```text
Frontend
  → API Gateway
  → order-service
  → cart-service
  → product-service
  → payment-service
  → notification-service
```

### Rules

- Order service tạo order từ cart.
- Product price nên lấy từ product-service.
- Payment service xử lý COD/simulated payment.
- Notification service lưu log notification.
- Nếu một bước lỗi, cần compensation logic.

## 5. AI Stylist Flow

```text
Customer
  → AI Chat UI
  → API Gateway
  → ai-agent-service
  → Qdrant / Neo4j / product-service
```

### Rules

- AI nhận style request.
- AI truy xuất product candidates.
- AI gọi product-service để lấy product info authoritative.
- AI trả về natural language recommendation và product/outfit suggestions.

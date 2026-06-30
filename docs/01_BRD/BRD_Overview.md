# BRD Overview — StyleMind

## 1. Executive Summary

StyleMind là nền tảng thương mại điện tử thời trang kết hợp trợ lý AI stylist. Khách hàng có thể xem sản phẩm, quản lý giỏ hàng, checkout, theo dõi đơn hàng và nhận gợi ý phối đồ từ AI. Admin có thể quản lý catalog, category, notification, AI index jobs và các chức năng quản trị mở rộng.

Backend được thiết kế theo microservices bằng Spring Boot. Frontend dùng React/Vite. Toàn bộ request từ frontend đi qua API Gateway.

## 2. Business Objectives

| Mục tiêu | Mô tả |
|---|---|
| Bán hàng thời trang online | Hỗ trợ browse, cart, checkout, order tracking |
| Cá nhân hóa trải nghiệm | AI stylist gợi ý outfit/sản phẩm theo phong cách |
| Hỗ trợ admin vận hành | Quản lý product, category, AI index, notification |
| Sẵn sàng mở rộng | Tách service theo business capability |
| Chuẩn bị nền AI | Dùng Qdrant cho vector search và Neo4j cho fashion graph |

## 3. Business Value

- Tăng khả năng khám phá sản phẩm.
- Tăng conversion bằng recommendation.
- Tách biệt nghiệp vụ giúp team phát triển song song.
- Dễ mở rộng sang inventory, payment gateway thật, email/SMS, AI retrieval thật.

## 4. Stakeholders

| Stakeholder | Vai trò |
|---|---|
| Customer | Người mua hàng |
| Guest | Người xem sản phẩm chưa đăng nhập |
| Admin | Người quản trị hệ thống |
| Development Team | Xây dựng frontend/backend/infrastructure |
| Product Owner | Quản lý scope và ưu tiên nghiệp vụ |
| AI/ML Team | Phát triển recommendation và stylist assistant |

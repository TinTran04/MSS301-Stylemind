# 1. Tổng quan dự án

## 1.1. StyleMind là gì
StyleMind là nền tảng thương mại điện tử thời trang kết hợp trợ lý **AI stylist**. Khách hàng có thể xem/tìm kiếm sản phẩm, quản lý giỏ hàng, đặt hàng, thanh toán, theo dõi đơn hàng và nhận gợi ý phối đồ cá nhân hóa. Bên cạnh đó, **admin** vận hành toàn bộ nền tảng: quản lý sản phẩm, danh mục, tài khoản người dùng, đơn hàng, thông báo và AI pipeline.

Hệ thống theo mô hình **microservices**: mỗi service phụ trách một nhóm nghiệp vụ và sở hữu database riêng (*database per service*). Frontend ReactJS giao tiếp backend qua **API Gateway**.

## 1.2. Thông tin nhanh
| Thuộc tính | Giá trị |
|---|---|
| Mô hình | Microservices Spring Boot + ReactJS |
| Frontend | ReactJS / Vite |
| Backend | Spring Boot Microservices |
| Gateway | API Gateway (`:3000`) |
| Database | Database per service |
| Payment | COD và SePay (VietQR – Open Banking, xác nhận qua Webhook) |
| API versioning | `/api/v1/...` (public & admin), `/internal/v1/...` (nội bộ) |

## 1.3. Mục tiêu kinh doanh
| Mục tiêu | Mô tả |
|---|---|
| Nền tảng bán hàng thời trang hiện đại | Browse → cart → checkout → theo dõi đơn. |
| Cá nhân hóa trải nghiệm | AI stylist gợi ý sản phẩm/outfit theo phong cách, dịp, sở thích, ngân sách. |
| Hỗ trợ vận hành admin | Quản lý catalog, **account user**, order, notification, AI index jobs. |
| Sẵn sàng mở rộng | Mỗi service phát triển/deploy/scale/bảo trì độc lập. |
| Tích hợp AI | Chuẩn bị kiến trúc cho semantic search, vector DB, knowledge graph ở phase sau. |

## 1.4. Business capability → Service
| Capability | Service |
|---|---|
| Identity & Access (+ admin account) | auth-service |
| User Profile | user-service |
| Product Catalog | product-service |
| Shopping Cart | cart-service |
| Order Management (orchestration + state machine) | order-service |
| Payment (COD + SePay webhook) | payment-service |
| Notification | notification-service |
| AI Stylist | ai-agent-service |
| Gateway / Security / Routing | api-gateway |

## 1.5. Phạm vi MVP
**In scope:** Customer shopping, Authentication đầy đủ, User profile, Product catalog, Cart, Order, Payment (COD + SePay), Notification, AI stylist, Admin management (account + order + catalog + notification + AI jobs).

**Out of scope (MVP):** full inventory tracking, inventory reservation, cổng thẻ quốc tế production, email/SMS delivery production, AI recommendation production-grade. Chi tiết: `business/01-brd.md`.

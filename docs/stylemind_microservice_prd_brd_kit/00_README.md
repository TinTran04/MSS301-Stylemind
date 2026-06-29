# StyleMind Documentation Kit

**Project:** StyleMind  
**Project code:** MSS301-Stylemind  
**Domain:** Fashion e-commerce platform with AI stylist assistant  
**Architecture:** React/Vite frontend + Spring Boot microservices backend behind API Gateway  
**Deployment target:** Local/Docker Compose MVP environment

## Mục đích bộ kit

Bộ kit này chia nhỏ BRD/PRD và tài liệu kỹ thuật theo từng mục để team có thể dùng trực tiếp trong phân tích, thiết kế, phát triển, review và báo cáo dự án.

## Cấu trúc tài liệu

```text
stylemind_microservice_prd_brd_kit/
├── 00_README.md
├── 01_BRD/
├── 02_PRD/
├── 03_ARCHITECTURE/
├── 04_SERVICES/
├── 05_API_CONTRACT/
├── 06_FRONTEND/
├── 07_DATABASE/
├── 08_SECURITY/
├── 09_TESTING/
├── 10_DEPLOYMENT/
├── 11_OBSERVABILITY/
├── 12_ROADMAP/
└── 13_TEMPLATES/
```

## Cách sử dụng

1. Đọc `01_BRD/BRD_Overview.md` để hiểu business context.
2. Đọc `02_PRD/PRD_Overview.md` để hiểu product scope và functional requirements.
3. Đọc `03_ARCHITECTURE/System_Architecture.md` để nắm kiến trúc tổng thể.
4. Dùng thư mục `04_SERVICES/` để chia việc backend theo từng microservice.
5. Dùng `05_API_CONTRACT/` làm nền cho OpenAPI/Swagger.
6. Dùng `12_ROADMAP/` để quản lý sprint và phase tiếp theo.

## Nguyên tắc thiết kế chính

- API Gateway là public entry point duy nhất.
- Mỗi microservice sở hữu database/schema riêng.
- Service không truy cập trực tiếp database của service khác.
- Cross-service communication thông qua REST/internal API hoặc event sau này.
- JWT được validate tại API Gateway.
- Backend service tin identity headers do Gateway inject.
- Internal APIs dùng `/internal/**` và phải được bảo vệ.

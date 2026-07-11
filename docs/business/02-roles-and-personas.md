# Roles & Permissions

| Role | Mô tả | Quyền chính |
|---|---|---|
| **Guest** | Chưa đăng nhập | Xem sản phẩm, xem danh mục, dùng guest cart. |
| **Customer** | Đã đăng ký | Login, quản lý profile, cart, checkout, theo dõi đơn, dùng AI stylist. |
| **Admin** | Quản trị hệ thống | Quản lý account user, product, category, order, payment log, notification, AI jobs. **Không** tự khóa/xóa/hạ quyền chính mình. |

## Nguyên tắc phân quyền
- RBAC 2 role: `CUSTOMER`, `ADMIN`.
- Mọi API `/api/v1/admin/**` yêu cầu role `ADMIN`; CUSTOMER gọi → **403**.
- Admin guard đặt ở API Gateway + kiểm tra lại ở service.
- Admin self-protection (xem `services/auth-service.md` và `requirements/03-security-requirements.md`).

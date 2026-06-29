# User Roles — StyleMind

## 1. Guest

Người dùng chưa đăng nhập.

### Main Actions

- Browse products.
- View product details.
- Add products to guest cart.
- Register/login.

### Constraints

- Không checkout nếu chưa đăng nhập.
- Không truy cập profile/order/AI history riêng tư.

## 2. Customer

Người dùng đã đăng ký.

### Main Actions

- Login/logout.
- Manage profile.
- Manage style profile.
- Manage delivery addresses.
- Manage cart.
- Checkout.
- Track orders.
- Use AI stylist.

### Authorization

Customer APIs cần authenticated user.

## 3. Admin

Người quản trị hệ thống.

### Main Actions

- Manage products.
- Manage categories.
- Manage AI index jobs.
- Manage notifications.
- Later: manage users and orders.

### Authorization

Admin APIs phải yêu cầu role `ADMIN`.

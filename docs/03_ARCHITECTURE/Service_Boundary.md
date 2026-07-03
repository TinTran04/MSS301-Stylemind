# Service Boundary — StyleMind

## 1. Boundary Mapping

| Business Capability | Service | Data Owner |
|---|---|---|
| Identity & Access | auth-service | User ID, email, password hash, role, account status, reset credentials |
| Customer Profile | user-service | User ID reference, style profile, preferences, delivery addresses |
| Product Catalog | product-service | Products, categories, variants, images |
| Shopping Cart | cart-service | Carts, cart items |
| Order Management | order-service | Orders, order items |
| Payment | payment-service | Transactions |
| Notification | notification-service | Notification logs |
| AI Stylist | ai-agent-service | Chat, bundles, analytics, index jobs |

## 2. Boundary Rules

### Allowed

- Service gọi service khác qua REST/internal API.
- Service publish event sau này nếu cần.
- Service cache dữ liệu read-only nếu có TTL/invalidation rule.
- user-service lazy-init profile shell on first authenticated profile/address read.

### Not Allowed

- Service query trực tiếp DB của service khác.
- Frontend gọi trực tiếp backend service bỏ qua gateway.
- Public client gọi `/internal/v1/**`.
- Backend service tin identity header nếu request không đến từ gateway.
- auth-service lưu profile fields hoặc user-service lưu credentials/access-control fields.
- Registration gọi user-service để tạo profile; MVP không cần cross-service call hoặc broker cho flow này.

## 3. Common Anti-patterns

| Anti-pattern | Tác hại |
|---|---|
| Shared database | Làm mất data ownership |
| Distributed monolith | Service tách code nhưng deploy/change phụ thuộc nhau |
| Chatty service calls | Tăng latency và failure surface |
| No timeout/retry | Dễ cascading failure |
| No contract testing | FE/BE dễ lệch API |

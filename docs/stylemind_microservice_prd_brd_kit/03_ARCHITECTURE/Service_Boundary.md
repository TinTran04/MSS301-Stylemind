# Service Boundary — StyleMind

## 1. Boundary Mapping

| Business Capability | Service | Data Owner |
|---|---|---|
| Identity & Access | auth-service | Account, password hash, role |
| Customer Profile | user-service | Profile, style profile, addresses |
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

### Not Allowed

- Service query trực tiếp DB của service khác.
- Frontend gọi trực tiếp backend service bỏ qua gateway.
- Public client gọi `/internal/**`.
- Backend service tin identity header nếu request không đến từ gateway.

## 3. Common Anti-patterns

| Anti-pattern | Tác hại |
|---|---|
| Shared database | Làm mất data ownership |
| Distributed monolith | Service tách code nhưng deploy/change phụ thuộc nhau |
| Chatty service calls | Tăng latency và failure surface |
| No timeout/retry | Dễ cascading failure |
| No contract testing | FE/BE dễ lệch API |

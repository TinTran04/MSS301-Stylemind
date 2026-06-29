# Data Ownership — StyleMind

## 1. Service Database Ownership

| Service | Database | Data Owned |
|---|---|---|
| auth-service | auth_db | Account, email, password hash, provider, role |
| user-service | user_db | Profile, style profile, addresses |
| product-service | product_db | Categories, products, variants, images |
| cart-service | cart_db | Carts, cart items |
| order-service | order_db | Orders, order items |
| payment-service | payment_db | Transactions |
| notification-service | notification_db | Notification logs |
| ai-agent-service | ai_db | Chat sessions, messages, bundles, analytics, index jobs |

## 2. Data Access Rules

- Service chỉ đọc/ghi database của chính nó.
- Cross-service read phải qua API.
- Cross-service write nên qua API hoặc event.
- Không join database giữa services.
- Không share JPA entity giữa services.

## 3. Migration

- Dùng Flyway hoặc Liquibase cho mỗi service.
- Migration script nằm trong service repo.
- Không để một service migrate schema của service khác.

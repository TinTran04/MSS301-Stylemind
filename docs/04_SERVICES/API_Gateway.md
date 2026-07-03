# api-gateway Specification

## 1. Responsibility

API Gateway là public entry point cho frontend.

## 2. Main Responsibilities

- Route request tới backend services.
- Validate JWT.
- Inject identity headers:
  - `X-User-Id`
  - `X-User-Roles`
- Block external access to `/internal/v1/**`.
- Handle CORS centrally.
- Rate limit sensitive endpoints.
- Add correlation ID.

## 3. Public Routes

| Route | Target |
|---|---|
| `/api/v1/auth/**` | auth-service |
| `/api/v1/users/**` | user-service |
| `/api/v1/products/**` | product-service |
| `/api/v1/categories/**` | product-service |
| `/api/v1/cart/**` | cart-service |
| `/api/v1/orders/**` | order-service |
| `/api/v1/payments/**` | payment-service |
| `/api/v1/notifications/**` | notification-service |
| `/api/v1/ai-stylist/**` | ai-agent-service |
| `/api/v1/admin/**` | Multiple services |

## 4. Security Rules

- Validate JWT for protected routes.
- Admin routes require role `ADMIN`.
- `/internal/v1/**` must never be exposed publicly.
- Use Redis for rate limit/cache when needed.

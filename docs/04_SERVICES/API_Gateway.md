# api-gateway Specification

## 1. Responsibility

API Gateway là public entry point cho frontend.

## 2. Main Responsibilities

- Route request tới backend services.
- Validate JWT.
- Inject identity headers:
  - `X-User-Id`
  - `X-User-Roles`
- Block external access to `/internal/**`.
- Handle CORS centrally.
- Rate limit sensitive endpoints.
- Add correlation ID.

## 3. Public Routes

| Route | Target |
|---|---|
| `/api/auth/**` | auth-service |
| `/api/users/**` | user-service |
| `/api/products/**` | product-service |
| `/api/categories/**` | product-service |
| `/api/cart/**` | cart-service |
| `/api/orders/**` | order-service |
| `/api/payments/**` | payment-service |
| `/api/notifications/**` | notification-service |
| `/api/ai-stylist/**` | ai-agent-service |
| `/api/admin/**` | Multiple services |

## 4. Security Rules

- Validate JWT for protected routes.
- Admin routes require role `ADMIN`.
- `/internal/**` must never be exposed publicly.
- Use Redis for rate limit/cache when needed.

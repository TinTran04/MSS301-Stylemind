# CodeGraph Workflow

## Current Index

The repository has an existing CodeGraph index under `.codegraph/`. The installed CLI is CodeGraph `0.9.9`; the last inspected index reported 401 files, 5,711 nodes, 10,343 edges, and an up-to-date status before this change.

## Non-destructive update workflow

After source changes, update the existing index with:

```bash
codegraph sync .
```

Do not run `codegraph uninit`, remove `.codegraph`, or rebuild the index destructively. If the sync command is unavailable or fails, document the failure and use `rg`/source inspection as the fallback rather than resetting the graph.

## Relevant relationships

- `cart-service` owns shopping-cart creation and item mutation.
- `cart-service` connects to `postgres-cart` through the Compose network.
- `auth-service` calls `notification-service` through `NotificationInternalClient`.
- Auth notification routing is configuration-driven by `notification.service.url`, with Docker DNS supplied through `NOTIFICATION_SERVICE_URL`.
- The notification email endpoint is `/internal/v1/notifications/email` and remains protected by the shared `X-Internal-Token` check.
- `api-gateway` routes `/api/v1/auth/**` to `auth-service`.
- `JwtAuthenticationFilter` bypasses JWT for the exact pre-authentication paths `/api/v1/auth/register/verify-otp` and `/api/v1/auth/register/resend-otp`.
- The Cart `/api/v1/cart` prefix is still treated as public by the Gateway filter and needs a separate authentication review.

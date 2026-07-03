# Endpoint Catalog — StyleMind

## Auth

| Method | Endpoint | Owner |
|---|---|---|
| POST | `/api/v1/auth/register` | auth-service |
| POST | `/api/v1/auth/login` | auth-service |
| GET | `/api/v1/auth/me` | auth-service |

## Users

| Method | Endpoint | Owner |
|---|---|---|
| GET | `/api/v1/users/profile` | user-service |
| PUT | `/api/v1/users/profile` | user-service |
| GET | `/api/v1/users/addresses` | user-service |
| POST | `/api/v1/users/addresses` | user-service |

## Products

| Method | Endpoint | Owner |
|---|---|---|
| GET | `/api/v1/products` | product-service |
| GET | `/api/v1/products/{id}` | product-service |
| GET | `/api/v1/categories` | product-service |

## Cart

| Method | Endpoint | Owner |
|---|---|---|
| GET | `/api/v1/cart` | cart-service |
| POST | `/api/v1/cart/items` | cart-service |
| PUT | `/api/v1/cart/items/{itemId}` | cart-service |
| DELETE | `/api/v1/cart/items/{itemId}` | cart-service |
| POST | `/api/v1/cart/merge` | cart-service |
| DELETE | `/api/v1/cart` | cart-service |

## Orders

| Method | Endpoint | Owner |
|---|---|---|
| POST | `/api/v1/orders` | order-service |
| GET | `/api/v1/orders` | order-service |
| GET | `/api/v1/orders/{id}` | order-service |

## Payments

| Method | Endpoint | Owner |
|---|---|---|
| POST | `/api/v1/payments` | payment-service |
| GET | `/api/v1/payments/{id}` | payment-service |

## Notifications

| Method | Endpoint | Owner |
|---|---|---|
| GET | `/api/v1/notifications` | notification-service |

## AI Stylist

| Method | Endpoint | Owner |
|---|---|---|
| POST | `/api/v1/ai-stylist/chat` | ai-agent-service |
| GET | `/api/v1/ai-stylist/history` | ai-agent-service |
| GET | `/api/v1/ai-stylist/bundles` | ai-agent-service |

## Admin

| Method | Endpoint | Owner |
|---|---|---|
| POST | `/api/v1/admin/products` | product-service |
| PUT | `/api/v1/admin/products/{id}` | product-service |
| DELETE | `/api/v1/admin/products/{id}` | product-service |
| GET | `/api/v1/admin/orders` | order-service |
| GET | `/api/v1/admin/notifications` | notification-service |
| POST | `/api/v1/admin/ai/index-jobs` | ai-agent-service |

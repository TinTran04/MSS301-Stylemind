# Endpoint Catalog — StyleMind

## Auth

| Method | Endpoint | Owner |
|---|---|---|
| POST | `/api/auth/register` | auth-service |
| POST | `/api/auth/login` | auth-service |
| GET | `/api/auth/me` | auth-service |

## Users

| Method | Endpoint | Owner |
|---|---|---|
| GET | `/api/users/profile` | user-service |
| PUT | `/api/users/profile` | user-service |
| GET | `/api/users/addresses` | user-service |
| POST | `/api/users/addresses` | user-service |

## Products

| Method | Endpoint | Owner |
|---|---|---|
| GET | `/api/products` | product-service |
| GET | `/api/products/{id}` | product-service |
| GET | `/api/categories` | product-service |

## Cart

| Method | Endpoint | Owner |
|---|---|---|
| GET | `/api/cart` | cart-service |
| POST | `/api/cart/items` | cart-service |
| PUT | `/api/cart/items/{itemId}` | cart-service |
| DELETE | `/api/cart/items/{itemId}` | cart-service |
| POST | `/api/cart/merge` | cart-service |
| DELETE | `/api/cart` | cart-service |

## Orders

| Method | Endpoint | Owner |
|---|---|---|
| POST | `/api/orders` | order-service |
| GET | `/api/orders` | order-service |
| GET | `/api/orders/{id}` | order-service |

## Payments

| Method | Endpoint | Owner |
|---|---|---|
| POST | `/api/payments` | payment-service |
| GET | `/api/payments/{id}` | payment-service |

## Notifications

| Method | Endpoint | Owner |
|---|---|---|
| GET | `/api/notifications` | notification-service |

## AI Stylist

| Method | Endpoint | Owner |
|---|---|---|
| POST | `/api/ai-stylist/chat` | ai-agent-service |
| GET | `/api/ai-stylist/history` | ai-agent-service |
| GET | `/api/ai-stylist/bundles` | ai-agent-service |

## Admin

| Method | Endpoint | Owner |
|---|---|---|
| POST | `/api/admin/products` | product-service |
| PUT | `/api/admin/products/{id}` | product-service |
| DELETE | `/api/admin/products/{id}` | product-service |
| GET | `/api/admin/orders` | order-service |
| GET | `/api/admin/notifications` | notification-service |
| POST | `/api/admin/ai/index-jobs` | ai-agent-service |

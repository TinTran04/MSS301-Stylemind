# Admin Order Detail Page Design

## Context

The admin order list currently opens a narrow detail drawer from the eye
action. The existing API contract is `GET /api/v1/admin/orders/{orderId}`.
It returns the order ID, user ID, total amount, order status, allowed status
transitions, shipping address string, order item IDs/variant IDs/quantities/
purchase prices, timestamps, and basic payment response fields.

The dedicated page will remain read-only. It will use the existing admin
layout and the API Gateway. It will not call `/internal/v1/**` or service
ports from the browser.

## Field Matrix

| UI field | Current API field | Stored snapshot | External lookup | Missing | Decision |
| --- | --- | --- | --- | --- | --- |
| Order ID, timestamps | `id`, `createdAt`, `updatedAt` | Yes | No | No | Render directly |
| Current order status | `orderStatus` | Yes | No | No | Central status mapping |
| Customer user ID | `userId` | Yes | No | No | Render as secondary data |
| Customer email | None | No | Auth user record | No | Add optional admin-detail enrichment |
| Customer full name/phone | None | No | Auth entity does not store them | Yes | Show `Chưa có thông tin` |
| Shipping address | `shippingAddress` | Yes, as one string | No | No | Render snapshot unchanged |
| Product name/image | None | No | Product variant snapshot endpoint | No | Add optional variant enrichment |
| Variant attributes | `variantId` only | ID only | Product variant snapshot endpoint | No | Add optional current catalog details |
| Quantity and unit price | `items[].quantity`, `items[].priceAtPurchase` | Yes | No | No | Never replace purchase price |
| Subtotal | None | Derivable from item snapshots | No | No | Derive from quantity x purchase price and label |
| Shipping/discount/tax | None | Not stored/exposed | No | Yes | Do not fabricate rows |
| Payment status and amount | `paymentStatus`, `totalAmount` | Status/order amount | Payment endpoint | No | Render supported fields |
| Payment reference/transfer content/expiry | Existing flat payment fields | Payment service | No | No | Render only when present |
| Paid time/gateway transaction ID | None | Payment service stores them | Payment endpoint | No | Add optional payment enrichment |
| Status history | `statusHistory[]` | `order_status_audit_log` / `OrderStatusAuditLog` | No | No | Render the returned timeline; keep an empty state only when the API returns no records |

## Backend Contract Extension

Extend the existing `OrderResponse` with optional, additive fields used only by
the admin detail response:

- `customerEmail`
- `itemDetails[]` containing product/variant catalog details for each stored
  `variantId`
- `paymentMethod`, `paymentReference`, `gatewayTransactionId`, and `paidAt`

The order item's `priceAtPurchase` remains the source of truth for displayed
prices. Variant enrichment is clearly presented as catalog detail for the
stored variant ID, not as a purchase-time snapshot. Missing external data is
represented by null and handled by the frontend.

The existing `/api/v1/admin/orders` list response remains compatible. The
detail method may perform best-effort enrichment while keeping the base order
and item response available if an optional lookup fails.

## Frontend Structure

- `AdminOrderDetailPage`: fetches the admin detail endpoint and owns loading,
  error, not-found, forbidden, retry, and back-navigation state.
- `OrderDetailHeader`: title, shortened ID, copy action, timestamps, order
  status, and payment status.
- `OrderItemsSection`: responsive item list with image fallback, variant
  attributes, quantity, snapshot price, and line total.
- `OrderPriceSummary`: subtotal and total only when supported.
- `OrderCustomerCard`: email and user ID, with safe fallbacks.
- `OrderShippingAddressCard`: stored shipping-address snapshot.
- `OrderPaymentCard`: supported payment fields, with order/payment status kept
  separate.
- `OrderStatusTimeline`: renders `statusHistory` when returned by the existing
  admin detail response and an empty state when the list is empty.
- `OrderDetailSkeleton` and `OrderDetailErrorState`: shaped loading and retry
  states.

## Route and List State

Add `/admin/orders/:orderId` inside the existing `RequireAdmin` and
`AdminLayout` route tree. The list eye action navigates with the current list
query string as a `from` value. The detail page decodes that safe relative
list URL and uses it for the back link, falling back to `/admin/orders`.

The existing list filters remain local state for this focused change. The
detail page preserves the captured list URL when entered from the list and
supports direct navigation without relying on browser history.

## Responsive and Accessibility Behavior

Desktop uses a main content column with a narrower information sidebar. Below
the medium breakpoint, sections stack and item rows become compact cards. The
page uses one meaningful `h1`, semantic headings, keyboard-accessible buttons,
accessible eye/copy labels, image alt text, visible focus styles, and text plus
status labels rather than color alone.

## Test Strategy

Frontend tests cover route navigation, detail rendering, loading/error states,
safe optional fields, snapshot price rendering, separated order/payment
status, and back-link query preservation using the repository's existing test
conventions. Backend tests cover additive detail enrichment and compatibility
only if backend code is changed. Playwright verifies the real admin list to
detail journey through the frontend and Gateway, direct navigation/refresh,
invalid ID, authorization, desktop/mobile layout, and prohibited network
targets.

## Explicit Non-Goals

The page does not change order status, cancel orders, refund payments, edit
customer data, edit addresses, or synthesize status history.

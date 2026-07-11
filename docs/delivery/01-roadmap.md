# Roadmap

## Sprint 1 — Auth, Gateway, Product Public
Register/login/me; forgot/reset password; gateway routing (`/api/v1`); product listing/detail; category listing.

## Sprint 2 — User, Cart
Style profile (lazy-init); delivery address; guest cart; auth cart; merge cart; cart item management.

## Sprint 3 — Checkout, Order, Payment (+ Saga cơ bản)
Create order (orchestration); product authoritative price; COD; **SePay VietQR (QR + webhook + idempotency)**; **Order State Machine**; saga cơ bản (timeout/expire + clear cart); customer order list/detail.

## Sprint 4 — Admin Scope
Admin account (+ self-protection); admin product/category; guided Add Product (basic INACTIVE → variants → images/publish); admin variant/image; admin order (theo state machine); admin notification.

## Sprint 5 — Hardening & AI
AI stylist chat; AI index jobs; notification retry; saga nâng cao (outbox/retry/compensation); logging/metrics/tracing; contract/integration tests.

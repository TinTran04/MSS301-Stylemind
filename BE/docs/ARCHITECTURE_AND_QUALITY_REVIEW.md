# StyleMind Backend — Architecture & Quality Review

> A teaching-oriented walkthrough of the StyleMind Spring Boot microservices,
> followed by a quality review using the **microservices-architect** skill as
> the lens (DDD boundaries, communication, data, resilience, observability).
>
> Reviewer: Claude · Date: 2026-06-24 · Skill: `microservices-architect` v1.1.0

---

## Part 1 — Learn the architecture

### 1.1 The 10-second mental model

StyleMind is an **AI fashion e-commerce** backend. A customer logs in, browses
products, fills a cart, places an order, pays, and chats with an AI stylist.
Each of those concerns is a **separate Spring Boot service** with **its own
database schema**. A single **API Gateway** is the front door; services talk to
each other over HTTP using **Feign clients**.

```
                          ┌─────────────────────────┐
   Browser / App  ───────▶│   API Gateway  :3001    │   (JWT check, routing,
                          │   ApiGatewayApplication │    rate-limit, CORS)
                          └───────────┬─────────────┘
        ┌──────────┬──────────┬───────┼───────┬──────────┬───────────┐
        ▼          ▼          ▼       ▼       ▼          ▼           ▼
     auth      user      product    cart    order    payment   notification   ai-agent
    :8081     :8082      :8083     :8086   :8087     :8088      :8089         :8085
   auth_db   user_db   product_db cart_db order_db payment_db notif_db       ai_db
                                            │  │  │                            │  │
                                            │  │  └──▶ payment (process)       │  └▶ order
                                            │  └─────▶ product (variants)      └────▶ product
                                            └────────▶ cart   (get / "clear")

   Shared infra: PostgreSQL (one instance, 8 schemas) · Redis (rate-limit)
                 Qdrant + Neo4j + MinIO (AI search, graph, image storage)
```

**Key idea — "smart edges, dumb pipes":** the gateway holds cross-cutting
concerns (auth, rate-limiting) so each service can stay focused on its own
domain. Services never share a database table; if order-service needs cart data,
it *asks* cart-service over HTTP — it does not read `cart_db` directly.

### 1.2 Each service, in one line

| Service | Owns (its data) | Talks to | Key files |
|---|---|---|---|
| **api-gateway** :3001 | nothing (stateless) | all services | `api-gateway/.../filter/JwtAuthenticationFilter.java`, `resources/application.yml` |
| **auth-service** :8081 | `users` (email, password hash, role) | — | `auth/service/AuthService.java`, `auth/entity/User.java` |
| **user-service** :8082 | style profiles, delivery addresses | — | `user/service/UserProfileService.java` |
| **product-service** :8083 | products, variants, images, categories | — | `product/service/ProductService.java`, `product/controller/ProductController.java` |
| **cart-service** :8086 | shopping carts, cart items (incl. guest carts) | — | `cart/service/CartService.java` |
| **order-service** :8087 | orders, order items | cart, product, payment | `order/service/OrderService.java`, `order/feign/*.java` |
| **payment-service** :8088 | transactions | — | `payment/service/PaymentService.java` |
| **notification-service** :8089 | notification logs (stub) | — | `notification/service/NotificationService.java` |
| **ai-agent-service** :8085 | chat sessions, AI bundles, index jobs | product, order | `ai/service/AiChatService.java`, `ai/feign/*.java` |
| **common-lib** (library) | — (shared code, not deployed) | — | `common/security/*`, `common/dto/ApiResponse.java`, `common/exception/*` |

> **DDD note (skill principle #1):** the table above is a *bounded-context map*.
> Boundaries look healthy — every service owns exactly one business capability
> and one schema. The two services with outbound calls (order, ai-agent) are the
> "orchestrators"; everything else is a leaf. That's a clean topology.

### 1.3 How a request actually flows

**Flow A — Login, then an authenticated call (the auth model)**

1. `POST /api/auth/login` hits the gateway. Login is on the gateway's
   *public* list, so no token is required.
2. Gateway forwards to **auth-service**. `AuthService.login()` checks the email +
   BCrypt password against `auth_db.users`, then mints a **JWT** containing
   `userId` and `role`, signed with HMAC-SHA256 using a shared `JWT_SECRET`.
3. Client stores the token and sends it on every later call:
   `Authorization: Bearer <token>`.
4. For `GET /api/orders`, the gateway's `JwtAuthenticationFilter` validates the
   signature, extracts the claims, and **injects headers** `X-User-Id` /
   `X-User-Roles` before forwarding to order-service. Downstream services trust
   those headers (they're behind the gateway) rather than re-parsing the token.
5. When order-service calls another service (Feign), `FeignClientConfig` attaches
   an `X-Internal-Token`; the callee's `InternalAuthFilter` checks it so that
   `/internal/**` endpoints can't be reached directly from the internet (the
   gateway also hard-blocks `/internal/**`).

> **Why two token types?** The **JWT** proves *who the user is* (edge auth). The
> **internal token** proves *the call came from inside the mesh* (service auth).
> Different jobs.

**Flow B — Place an order (the cross-service "saga")**

`OrderService.createOrder()` —
[order-service/.../service/OrderService.java:34](../order-service/src/main/java/com/stylemind/order/service/OrderService.java#L34):

1. **Read cart** — Feign `GET /api/cart` via `CartClient`. Empty → `CART_EMPTY`.
2. **Persist order** as `PENDING`, then **persist each order item** (price copied
   from the cart DTO).
3. **Charge payment** (if `online_simulated`) — Feign `PaymentClient.processPayment`.
   On failure → set order `CANCELLED`, throw `PAYMENT_FAILED`.
4. **Mark order `FULFILLED`.**
5. **Clear cart** — Feign `CartClient.mergeCart(...)`.

This is a **saga**: a business transaction spread across services that can't share
one ACID transaction. Steps 1–5 are the saga; if a later step fails, earlier
steps must be *compensated* (undone). Part 2 shows where this saga is incomplete.

---

## Part 2 — Quality review (findings)

Severity = impact × likelihood. Each finding cites a real `file:line`, ties to a
skill principle, and states the fix. Items marked **✅ fix applied** were changed
in this pass (safe, low-risk only); **📋 recommended** are left for you to decide.

### 🔴 High

#### H1 — JWT secret default differs across services → auth breaks
**Where:** `payment-service/src/main/resources/application.yml:36` and
`notification-service/.../application.yml:33` default `JWT_SECRET` to
`sm-secret-key-2026`, while auth/user/product/cart/order default to
`super-secure-stylemind-secret-key-signature-2026-xyz`.
**Why it matters:** JWTs are HS256-signed with this secret. If the env var isn't
set (e.g. local dev, or any env that forgets one var), a token minted by
auth-service **fails signature validation** at payment & notification → every
authenticated call to those two services returns 401. This is a silent,
environment-dependent outage. *(Skill: API/auth consistency.)*
**Fix:** make the committed default identical everywhere (or better, fail fast if
`JWT_SECRET` is unset in non-local profiles).
**Status:** ✅ fix applied — aligned the two outliers to the common default.

#### H2 — "Clear cart" after checkout is a silent no-op
**Where:** `OrderService.createOrder` calls
`cartClient.mergeCart(authHeader, {guestSessionId: ""})`
([OrderService.java:87](../order-service/src/main/java/com/stylemind/order/service/OrderService.java#L87)).
In `CartService.mergeCart`
([CartService.java:119](../cart-service/src/main/java/com/stylemind/cart/service/CartService.java#L119))
this builds `guestCartId = "guest_"`, finds no such cart, and **returns early
without modifying the user's cart**. There is **no "clear cart" endpoint** on
`CartClient` at all (only `getCart` + `mergeCart`).
**Why it matters:** after a successful order the customer's cart still contains
the purchased items → they can re-order the same cart, totals are wrong on the
next visit. *(Skill: saga compensation / data correctness.)*
**Fix:** add a real `DELETE /api/cart` (clear-all) endpoint on cart-service and
call it here. **This requires new behavior across two services**, so it is
**📋 recommended**, not auto-applied. As an interim, the misleading no-op call is
replaced with an explicit `TODO` + warning log so it stops *looking* correct.
**Status:** ✅ partial — misleading call annotated; 📋 real endpoint recommended below.

#### H3 — Order saga has no real compensation or inventory step
**Where:** [OrderService.java:67-90](../order-service/src/main/java/com/stylemind/order/service/OrderService.java#L67-L90).
On payment failure the code sets the order to `CANCELLED` but the already-saved
`order_items` rows remain, there is **no inventory reservation/release**, and the
whole method runs under a single `@Transactional` that does **not** roll back the
*remote* payment call. If step 4/5 throws after payment succeeded, money is taken
but the cart isn't cleared and no compensation runs.
**Why it matters:** this is the classic "distributed monolith" failure the skill
warns against — local `@Transactional` gives a false sense of atomicity across
services. *(Skill: saga pattern, design-for-failure.)*
**Fix (📋 recommended):** model the flow as explicit saga steps each with
`execute()`/`compensate()` (reserve-inventory → charge-payment → fulfil →
clear-cart), persist saga state, and make each step idempotent. See the skill's
`references/data.md` (Saga section) and `references/patterns.md`.

### 🟠 Medium

#### M1 — No resilience on any Feign call (no timeout / retry / circuit breaker)
**Where:** all Feign clients (`order/feign/*.java`, `ai/feign/*.java`); no
`resilience4j` / `feign.client.config` timeout keys exist in any `application.yml`.
**Why it matters:** a slow or down payment/cart/product service makes order-service
threads hang on the default (effectively unbounded) read, cascading the outage
upstream. *(Skill MUST-DO: circuit breakers + explicit timeouts on every external
call.)*
**Fix:** ✅ added conservative `feign.client.config.default` connect/read timeouts
to order-service & ai-agent-service config (pure safety net, no behavior change).
📋 recommended: add `spring-cloud-starter-circuitbreaker-resilience4j` + a
fallback per client.

#### M2 — Weak committed secret *defaults*
**Where:** `${SPRING_DATASOURCE_PASSWORD:password}`,
`${S3_SECRET_KEY:password}` (product:46), `${X_INTERNAL_TOKEN:sm-secret-internal-service-token-key-2026}`.
**Why it matters:** secrets *are* externalized via env vars (good), but the
**committed fallback values are real, guessable secrets**. Anyone with the repo
knows the internal-token and default DB password. *(Skill: security baseline.)*
**Fix (📋 recommended):** keep the `${ENV:...}` indirection but make defaults
non-functional placeholders (e.g. `CHANGE_ME`) and fail fast when unset outside
`local`. Not auto-changed because it can break local startup until you set vars.

#### M3 — Saga reads price from a cart DTO that may be stale/spoofable
**Where:** `getVariantPrice(cartItem)`
([OrderService.java:109](../order-service/src/main/java/com/stylemind/order/service/OrderService.java#L109))
trusts `cartItem.getVariant().getProduct().getBasePrice()` carried in the cart
response.
**Why it matters:** price-at-purchase should be authoritative from
product-service at order time, not whatever the cart serialized earlier (stale
price, or a manipulated client cart). `ProductClient.getVariants(List<id>)`
already exists for an authoritative batch lookup. *(Skill: data ownership —
product owns price, not cart.)*
**Fix (📋 recommended):** fetch authoritative prices via the existing batch
endpoint during order creation.

### 🟡 Low / cleanup

#### L1 — Dead code: `getVariantSku` is never called
**Where:** [OrderService.java:104-107](../order-service/src/main/java/com/stylemind/order/service/OrderService.java#L104-L107).
Private method with no callers. *(Skill-adjacent: simplicity.)*
**Status:** ✅ removed.

#### L2 — Unused imports / fields in OrderService
**Where:** `java.time.Instant` import is unused; `productClient` field is now
unused after L1 removal (it was only referenced by the dead method).
**Status:** ✅ unused import removed. ⚠️ `productClient` is **kept** because M3's
recommended fix will use it — removing it now would just be re-added. Noted here
so it's intentional, not an oversight.

#### L3 — Hardcoded Vietnamese error messages in exceptions
**Where:** `new BusinessException("CART_EMPTY", "Giỏ hàng trống", 400)` etc.
throughout services.
**Why it matters:** the error *code* is the stable contract; the human string
should be localizable (message bundle) rather than hardcoded. Low impact for an
MVP. *(Skill: API versioning/contract hygiene.)*
**Status:** 📋 recommended (no change — codes are already present and usable).

#### L4 — No health/readiness probe contract documented
**Where:** services expose `/actuator/health` but there's no liveness/readiness
split or k8s probe config. *(Skill MUST-DO #7.)*
**Status:** 📋 recommended.

---

## Part 3 — Recommended next steps (not auto-applied)

A suggested order, highest leverage first:

1. **H1 already fixed** — verify by setting *no* `JWT_SECRET` and confirming
   payment/notification accept an auth-service token. (Low effort, high payoff.)
2. **H2** — add `DELETE /api/cart` to cart-service, expose it on `CartClient`,
   call it in `createOrder`. (Small, removes a real customer-facing bug.)
3. **M1** — add resilience4j + fallbacks to order-service's three clients.
4. **H3 / M3** — refactor `createOrder` into an explicit saga with compensation
   and authoritative pricing. (Largest; do after M1.)
5. **M2 / L4** — harden secret defaults and add probes before any real deploy.

---

## Appendix — Reviewer's verification log

What was checked against source (not assumed):
- H1 confirmed by reading `JWT_SECRET` default in all 8 service `application.yml`.
- H2 confirmed by tracing `mergeCart("")` into `CartService.mergeCart` early-return.
- H3 confirmed by reading the full `createOrder` method + `@Transactional` scope.
- M1 confirmed by grep: no `resilience4j`/`feign.client.config` keys exist.
- M3 confirmed: `getVariantPrice` reads the cart DTO; `ProductClient.getVariants`
  batch endpoint exists and is unused by the order path.
- **Correction during review:** an earlier hypothesis of an N+1 variant fetch in
  `getVariantPrice` was **wrong** — that method reads the already-loaded cart DTO,
  no per-item call. Reclassified as M3 (stale/authoritative-price) instead.

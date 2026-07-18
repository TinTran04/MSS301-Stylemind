# Docker Service URL Matrix

This matrix records the Docker-first runtime contract. Values below are hostnames on the Compose network, not host-machine loopback addresses.

| Caller | Target | Variable/property | Docker value | Internal auth |
|--------|--------|------------------|--------------|---------------|
| API Gateway | auth-service | `AUTH_SERVICE_URL` | `http://auth-service:8081` | Gateway routing/JWT rules |
| API Gateway | user-service | `USER_SERVICE_URL` | `http://user-service:8082` | Gateway routing/JWT rules |
| API Gateway | product-service | `PRODUCT_SERVICE_URL` | `http://product-service:8083` | Gateway routing/JWT rules |
| API Gateway | cart-service | `CART_SERVICE_URL` | `http://cart-service:8086` | Gateway routing/JWT rules |
| API Gateway | order-service | `ORDER_SERVICE_URL` | `http://order-service:8087` | Gateway routing/JWT rules |
| API Gateway | payment-service | `PAYMENT_SERVICE_URL` | `http://payment-service:8088` | Gateway routing/JWT rules |
| API Gateway | notification-service | `NOTIFICATION_SERVICE_URL` | `http://notification-service:8089` | Gateway routing/JWT rules |
| API Gateway | ai-agent-service | `AI_SERVICE_URL` | `http://ai-agent-service:8085` | Gateway routing/JWT rules |
| auth-service | notification-service | `NOTIFICATION_SERVICE_URL` | `http://notification-service:8089` | `X-Internal-Token` |
| cart-service | product-service | `PRODUCT_SERVICE_URL` | `http://product-service:8083` | `X-Internal-Token` |
| order-service | auth-service | `AUTH_SERVICE_URL` | `http://auth-service:8081` | `X-Internal-Token` |
| order-service | notification-service | `NOTIFICATION_SERVICE_URL` | `http://notification-service:8089` | `X-Internal-Token` |
| order-service | cart-service | `CART_SERVICE_URL` | `http://cart-service:8086` | `X-Internal-Token` |
| order-service | product-service | `PRODUCT_SERVICE_URL` | `http://product-service:8083` | `X-Internal-Token` |
| order-service | payment-service | `PAYMENT_SERVICE_URL` | `http://payment-service:8088` | `X-Internal-Token` |
| payment-service | order-service | `ORDER_SERVICE_URL` | `http://order-service:8087` | `X-Internal-Token` |
| ai-agent-service | product-service | `PRODUCT_SERVICE_URL` | `http://product-service:8083` | `X-Internal-Token` |
| ai-agent-service | order-service | `ORDER_SERVICE_URL` | `http://order-service:8087` | `X-Internal-Token` |

## Internal-token binding per service (verified 2026-07-19)

Every service's `internal.token` Spring property is consumed by the same shared `common-lib`
classes (`FeignClientConfig` as sender, `InternalAuthFilter` as receiver), always sent/checked as
the `X-Internal-Token` header, with the same hardcoded fallback default
(`sm-secret-internal-service-token-key-2026`) if the property resolves to nothing. What differs per
service is which environment variable that property is explicitly bound to in `application.yml`,
and which environment variables Compose actually injects into that service's container. These two
were verified independently by reading source and the resolved Compose config; they are not
currently consistent across all seven services.

| Service | `internal.token` source in `application.yml` | Env vars Compose injects into this container | Effective value given current `.env` |
|---|---|---|---|
| auth-service | no explicit override (Spring's built-in relaxed binding of the `INTERNAL_TOKEN` env var) | `INTERNAL_TOKEN` | `.env` `INTERNAL_TOKEN` |
| order-service | `${INTERNAL_TOKEN:default}` (changed 2026-07-19, currently uncommitted in git but built into the running container) | `INTERNAL_TOKEN` | `.env` `INTERNAL_TOKEN` |
| notification-service | `${X_INTERNAL_TOKEN:default}` (unchanged) | `X_INTERNAL_TOKEN`, now aliased in Compose to `${INTERNAL_TOKEN}` (fixed 2026-07-19) | `.env` `INTERNAL_TOKEN` (same value as auth/order, since the fix) |
| product-service | `${X_INTERNAL_TOKEN:default}` | `INTERNAL_TOKEN` only (`X_INTERNAL_TOKEN` is not injected) | hardcoded default |
| cart-service | `${X_INTERNAL_TOKEN:default}` | `INTERNAL_TOKEN` only | hardcoded default |
| payment-service | `${X_INTERNAL_TOKEN:default}` | `INTERNAL_TOKEN` only | hardcoded default |
| ai-agent-service | `${X_INTERNAL_TOKEN:default}` | `INTERNAL_TOKEN` only | hardcoded default |

`.env` defines both `INTERNAL_TOKEN` and `X_INTERNAL_TOKEN` as distinct 128-character values. A
read-only comparison script confirmed they are **different** without printing either value.

### Resulting call-pair risk (updated 2026-07-19 after the order↔notification fix)

| Caller → Target | Caller's effective token | Target's effective token | Match? | Status |
|---|---|---|---|---|
| order-service → auth-service | `.env INTERNAL_TOKEN` | `.env INTERNAL_TOKEN` | MATCH | Runtime-verified (HTTP 200 read-only probe, 2026-07-19) |
| order-service → notification-service | `.env INTERNAL_TOKEN` | `.env INTERNAL_TOKEN` (via alias) | MATCH | **Fixed and runtime-verified 2026-07-19**: direct probe went 403 → 200 after `notification-service`'s `X_INTERNAL_TOKEN` Compose mapping was aliased to `${INTERNAL_TOKEN}` |
| auth-service → notification-service | `.env INTERNAL_TOKEN` | `.env INTERNAL_TOKEN` (via alias) | MATCH | Fixed by the same change; not independently re-probed for this specific pair (IMPLEMENTED, RUNTIME VERIFICATION PENDING) |
| cart-service → product-service | hardcoded default | hardcoded default | MATCH (coincidental) | Not independently retested this session |
| ai-agent-service → product-service | hardcoded default | hardcoded default | MATCH (coincidental) | Not independently retested this session |
| payment-service → order-service | hardcoded default | `.env INTERNAL_TOKEN` | MISMATCH | NEEDS VERIFICATION — this is the call that marks an order PAID after a SePay webhook; **not fixed by this session's change** |
| ai-agent-service → order-service | hardcoded default | `.env INTERNAL_TOKEN` | MISMATCH | NEEDS VERIFICATION — not fixed by this session's change |
| order-service → product-service / cart-service / payment-service | `.env INTERNAL_TOKEN` | hardcoded default | MISMATCH | NEEDS VERIFICATION — not fixed by this session's change |

This table reflects two changes on the same day (2026-07-19): the `order-service ↔ auth-service`
fix, and the `order-service`/`auth-service ↔ notification-service` fix (Compose-only alias). The
remaining MISMATCH rows above (`payment-service → order-service` and the `ai-agent-service`/
`order-service → product/cart/payment` pairs) were deliberately **not** touched, per the fix's
explicit scope of "do not modify the working SePay or payment-to-order flow" — see KNOWN_ISSUES.md
for the full write-up and evidence, and DOCKER_AND_DATABASE_RUNBOOK.md for read-only diagnostic commands
that do not print secret values.

## Configuration rules

- Docker Compose supplies the variables above explicitly. A container must not use `localhost` to reach another container.
- `INTERNAL_TOKEN` is the configuration variable. The HTTP header remains `X-Internal-Token`.
- `application-local.yml` may retain `localhost` for IntelliJ/local-process runs.
- Browser URLs and host-side curl commands may use published ports such as `http://localhost:3000`.
- Same-container healthchecks may use `localhost`.
- Missing production service URL or internal-token values should fail at startup rather than silently selecting a loopback fallback.

## Evidence snapshot

On 2026-07-19, the resolved Compose configuration and recreated `payment-service` container reported `ORDER_SERVICE_URL=http://order-service:8087`; Docker DNS resolved `order-service`, and the payment actuator health check returned HTTP 200. Secret values were not printed. A controlled SePay webhook replay remains pending.

Also on 2026-07-19, the Order-to-Auth internal email lookup was verified after
standardizing both Compose services on `INTERNAL_TOKEN`: both containers had a
set matching value and the read-only internal request returned HTTP 200. This
verifies the internal-auth boundary only; notification delivery during a real
`ORDER_PAID` event remains unverified in this pass.

On 2026-07-18 21:12:42 (before the `INTERNAL_TOKEN` standardization above), a real SePay webhook
was authenticated by `payment-service` and, per a read-only `order_db` query, the corresponding
order reached `order_status = PAID` about 0.5s later. This proves the full webhook path worked
under the *previous* internal-token configuration, in which `order-service` still defaulted to the
same hardcoded token as `payment-service`. No webhook or notification call has since been observed
in logs against the rebuilt `order-service`/`auth-service` containers, so whether this path still
works under the current `INTERNAL_TOKEN` binding is unverified — see the call-pair risk table
above.

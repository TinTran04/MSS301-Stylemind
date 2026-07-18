# Known Issues

## 2026-07-19 (fix) Order→Notification 403 on ORDER_PAID email

**Status:** RESOLVED for `order-service → notification-service`. `auth-service →
notification-service` (registration OTP email) shares the identical root cause and the same fix
resolves it too, but that specific pair was not independently re-probed after the fix — treat as
IMPLEMENTED, RUNTIME VERIFICATION PENDING for that one pair specifically.

**Exact throw location:** `com.stylemind.common.security.InternalAuthFilter.doFilterInternal`
(`InternalAuthFilter.java:33`), the `if (token == null || !token.equals(internalToken))` check —
confirmed via the full stack trace in notification-service's log
(`Servlet.service() ... threw exception` → `BusinessException: Không có quyền truy cập tài nguyên
này` → `InternalAuthFilter.doFilterInternal:33`).

**Root cause (Case D - tokens differ):** identical mechanism to the already-fixed
order-to-auth issue below, affecting a different pair. `order-service` sends `X-Internal-Token`
sourced from `.env INTERNAL_TOKEN`; `notification-service`'s `InternalAuthFilter` compared against
its own `internal.token`, sourced from `.env X_INTERNAL_TOKEN` — a different value. Confirmed with
live evidence, not just source reading:
- Log correlation (request id `30f94034-5062-4eba-83bf-ec17d4536f81`, order
  `65bbe29343d54ec69ada49a5743ba18d`): `order-service` retried 3x, each attempt `[403] ... POST ...
  /internal/v1/notifications/email`, matched 1:1 with 3 `BusinessException` stack traces in
  `notification-service`'s log at the same timestamps (2026-07-18 21:59:45-46).
- Token presence check (no values printed): `order-service` has `INTERNAL_TOKEN=SET`,
  `X_INTERNAL_TOKEN=NOT_SET`; `notification-service` has the reverse.
  `order_token == notification_token` comparison: **MISMATCH**.
- Direct controlled probe from `order-service` to
  `http://notification-service:8089/internal/v1/notifications/email` using order-service's own
  effective token: **HTTP 403 before the fix**.
- `NotificationClient.class.getAnnotation(FeignClient.class).configuration()` is empty, ruling out
  "Case A" (client not applying the shared interceptor) - `com.stylemind.common` is in
  `OrderServiceApplication`'s `scanBasePackages`, so the global `internalRequestInterceptor` bean
  applies to every Feign client automatically, `NotificationClient` included.

**Fix (Case C - Compose-only compatibility mapping, no Java/production source changed):**
`BE/docker-compose.yml`, `notification-service.environment.X_INTERNAL_TOKEN` changed from
`${X_INTERNAL_TOKEN}` to `${INTERNAL_TOKEN}`, so notification-service's existing
`internal.token: ${X_INTERNAL_TOKEN:default}` binding (unchanged) now resolves to the same
canonical `.env INTERNAL_TOKEN` value that `auth-service`/`order-service` send. This mirrors the
compatibility-mapping pattern already used elsewhere in this file. `/internal/v1/**` remains
protected; no path was made public; no token was hardcoded; no user/admin role was granted.

**Tests added (TDD, RED confirmed before the fix, GREEN confirmed after):**
- `BE/common-lib/src/test/java/com/stylemind/common/security/InternalAuthFilterTest.java` (new) -
  valid token accepted without a user JWT, missing token rejected, wrong token rejected,
  non-`/internal/v1/**` paths bypass the check. 4/4 pass (characterization coverage for a
  previously-untested shared class; the filter logic itself was not changed).
- `BE/order-service/src/test/java/com/stylemind/order/feign/ServiceUrlConfigurationTest.java` -
  new test `dockerComposeGivesNotificationServiceTheSameInternalTokenValueAsOrderAndAuth`
  asserts the compose block contains `X_INTERNAL_TOKEN: ${INTERNAL_TOKEN}`. **Failed before the
  fix** (expected string not found, actual was `X_INTERNAL_TOKEN: ${X_INTERNAL_TOKEN}`) and
  **passed after**. Also added `notificationClient_doesNotOverrideFeignConfigurationAwayFromThe
  GlobalInterceptor` (passed both before and after - rules out Case A).
- `BE/order-service/src/test/java/com/stylemind/order/service/OrderServiceTest.java` - new test
  `updateOrderStatusFromPayment_notificationClientThrows_orderRemainsPaid` asserts a notification
  exception does not revert or cancel the order (passed both before and after - characterization
  of already-correct behavior, not the bug itself).

**Runtime verification after the fix:** `notification-service` recreated
(`--no-deps --force-recreate`, Compose-env-only change, no rebuild needed); `order-service` was
not recreated (its own config did not change in this fix). The identical direct controlled probe
that returned 403 before the fix returned **HTTP 200** after
(`{"success":true,"message":"Gửi email nội bộ thành công",...}`), using order-service's real
effective token against the real endpoint. A live end-to-end SePay bank-transfer replay (a human
scanning the VietQR code with a real bank app) was **not** performed - that is outside what this
agent can safely automate and was not requested to be faked. The 403-causing configuration is
fixed and independently proven via the same header/token/endpoint the real `ORDER_PAID` path uses.

**Follow-up still open:** `payment-service → order-service` (marks the order PAID) and
`ai-agent-service → order-service`/`order-service → product-service`/`cart-service`/`payment-service`
remain source-verified MISMATCH risks from the entry below and were **not** touched by this fix -
see "Internal-token binding inconsistent across services after the order/auth fix".

## 2026-07-19 (documentation sync — findings, partially superseded by the fix above)

### Internal-token binding inconsistent across services after the order/auth fix

**Status:** NEEDS VERIFICATION for the remaining pairs (`payment-service → order-service`,
`ai-agent-service → order-service`, `order-service → product-service`/`cart-service`/
`payment-service`). The `order-service ↔ notification-service` and `auth-service ↔
notification-service` pairs are now fixed - see the entry above.

**Root cause:** The same-day fix below ("Order-to-Auth email lookup returned 403") changed
`order-service` (and `auth-service`, via its Compose mapping) to resolve `internal.token` from
`INTERNAL_TOKEN`. `product-service`, `cart-service`, `payment-service`, and `ai-agent-service`
still resolve `internal.token` from `X_INTERNAL_TOKEN` in their own `application.yml`, but Compose
only injects `INTERNAL_TOKEN` into those four containers — so they now silently fall back to the
shared hardcoded default value baked into `common-lib` rather than using any `.env` value.
`notification-service` originally still correctly received and used `X_INTERNAL_TOKEN`, but that no
longer matched `auth-service`/`order-service`, which moved to `INTERNAL_TOKEN`. `.env`'s
`INTERNAL_TOKEN` and `X_INTERNAL_TOKEN` values were confirmed different (without printing either).
**Update:** the `notification-service` side of this was fixed on 2026-07-19 - see the "Order→
Notification 403" entry above. `notification-service`'s Compose mapping now aliases
`X_INTERNAL_TOKEN` to the canonical `.env INTERNAL_TOKEN` value, so it matches
`auth-service`/`order-service` again.

**Impact still open (not touched by the notification fix):** `payment-service → order-service`
(the call that marks an order `PAID` after a SePay webhook), `ai-agent-service → order-service`,
and `order-service → product-service`/`cart-service`/`payment-service` would still return 403 from
`InternalAuthFilter`, because `product-service`, `cart-service`, `payment-service`, and
`ai-agent-service` still default to the shared hardcoded token instead of any `.env` value.
`cart-service ↔ product-service` and `ai-agent-service → product-service` remain coincidentally
matched because both still default to the same hardcoded value. Full per-pair table:
ENVIRONMENT_MATRIX.md.

**Evidence for and against:** A real SePay webhook completed the full PAID path successfully on
2026-07-18 21:12, but that was *before* `order-service`/`auth-service` were rebuilt with the new
binding (rebuild confirmed at 21:34 the same day via container startup logs). No webhook or
internal notification call has been observed in logs since that rebuild. This is therefore a
source-verified risk, not a confirmed failure — do not mark it RESOLVED or BROKEN without a
controlled webhook/notification replay against the current build.

**Suggested next step (not performed here — this is a documentation-only task):** apply the same
`INTERNAL_TOKEN` binding used by `order-service`/`auth-service` consistently to `product-service`,
`cart-service`, `payment-service`, `ai-agent-service`, and `notification-service`, then re-verify
every pair in ENVIRONMENT_MATRIX.md.

### Gateway DEBUG logging exposes Authorization headers and user-identity headers

**Status:** CONFIRMED via live log inspection, 2026-07-19.

`api-gateway`'s `application.yml` sets `org.springframework.cloud.gateway: DEBUG`. At this level,
Gateway's observation filters log full inbound request headers, including a live customer JWT
Bearer token and the SePay webhook's static `Authorization: Apikey ...` value, plus the raw
`X-User-Id`/`X-User-Roles`/`X-User-Email` headers as sent by the client (before
`JwtAuthenticationFilter` strips them from the request it forwards downstream). This was observed
directly in current container logs and is a live credential/PII exposure risk in application logs.
Not fixed here (documentation-only task); see DOCKER_AND_DATABASE_RUNBOOK.md for a redacted way to
inspect Gateway logs safely.

### Plaintext SePay webhook API key committed to `BE/PAYMENT_REDIRECT_ISSUE.md`

**Status:** CONFIRMED, needs credential rotation and file redaction by the user.

`BE/PAYMENT_REDIRECT_ISSUE.md` (added in commit `4bbf1a7c`, pushed to `origin/VoKhai`) contains the
plaintext `SEPAY_WEBHOOK_API_KEY` value in two places. This file is outside `docs/AGENT_WORKSPACE`
and was **not** modified by this documentation task (out of the allowed scope
`docs/AGENT_WORKSPACE/**`). Recorded here only as a pointer so a future agent or the user does not
re-discover it from scratch; the key itself is not reproduced in this file. Recommended action:
rotate the SePay webhook API key and redact the committed file (both outside this task's scope).

## 2026-07-19

### Order-to-Auth email lookup returned 403

**Status:** RESOLVED for the verified internal endpoint probe; full `ORDER_PAID`
notification delivery remains runtime-pending.

**Root cause:** Compose supplied `${INTERNAL_TOKEN}` to Order Service but
`${X_INTERNAL_TOKEN}` to Auth Service. The shared `X-Internal-Token` header was
therefore rejected by Auth's internal authentication filter.

**Resolution:** Both services now use the canonical `INTERNAL_TOKEN` mapping.
The internal endpoint remains protected and no payment or order status was
changed by this fix.

**Verification:** The focused Compose regression test passed after the fix; the
rebuilt containers reported a set matching token without exposing its value;
the read-only Order-to-Auth email lookup returned HTTP 200; and resolved Compose
validation passed. A real SePay webhook replay was not run.

**Follow-up (same session):** this fix only changed `order-service` and `auth-service`. It was not
propagated to `product-service`, `cart-service`, `payment-service`, `ai-agent-service`, or
`notification-service`, which creates a new, separate NEEDS VERIFICATION risk — see "Internal-token
binding inconsistent across services after the order/auth fix" above.

## 2026-07-17

### Registration OTP verification blocked by API Gateway

**Status:** RESOLVED. Runtime verification confirms the Gateway no longer returns 401 for the verify-OTP path.

**Root cause:** `JwtAuthenticationFilter` used exact public-path matching and contained `/api/v1/auth/register`, but not `/api/v1/auth/register/verify-otp`. The request had no Bearer token, so the filter returned HTTP 401 before routing to Auth Service. `SecurityConfig.anyExchange().permitAll()` did not bypass the custom GlobalFilter.

**Evidence:**

- Gateway matched route `auth-service` with `/api/v1/auth/**` and downstream URI `http://auth-service:8081`.
- Auth Service received no request through Gateway before the fix.
- A direct Auth Service request returned HTTP 400 with `REGISTER_OTP_INVALID`, not HTTP 401.
- The verify-OTP path was absent from `PUBLIC_EXACT_PATHS`.

**Impact:** Users could receive a registration OTP but could not complete verification through the frontend Gateway path. The frontend's generic HTTP 401 handling could redirect the user to Login before Auth Service evaluated the OTP.

**Resolution:** Added exact public paths for `/api/v1/auth/register/verify-otp` and `/api/v1/auth/register/resend-otp`. No wildcard Auth allowlist, fake JWT, or filter disablement was introduced. Gateway regression tests cover public registration/login paths and protected Auth rejection.

**Verification:**

- `mvn -pl api-gateway test`: 10 tests passed.
- `docker compose --profile all up -d --build --force-recreate api-gateway`: Gateway image built and `stylemind-gateway` started on port 3000.
- Invalid-OTP probe through `http://localhost:3000/api/v1/auth/register/verify-otp` returned HTTP 400 `REGISTER_OTP_INVALID`.
- Auth Service logs confirmed the verify-OTP request arrived through Gateway.
- Unauthenticated `GET /api/v1/auth/me` still returned HTTP 401.
- A fresh successful account-creation flow was not executed because no valid current OTP was available.

### Cart API broadly bypasses Gateway JWT validation

**Status:** NEEDS VERIFICATION.

`JwtAuthenticationFilter` currently treats every path beginning with `/api/v1/cart` as public. It has not been confirmed whether this is intentional or whether Cart Service independently validates authentication for every operation. Investigate separately and do not change Cart security as part of the OTP fix.

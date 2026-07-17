# Known Issues

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

# Gateway and Auth Authentication Guide

## Pre-authentication public endpoints

These verified endpoints are used before a user has an access token:

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/register/verify-otp`
- `POST /api/v1/auth/register/resend-otp`
- `POST /api/v1/auth/password/setup`
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/verify-reset-otp`
- `POST /api/v1/auth/reset-password`

They are public at the Gateway because the browser may not have a Bearer token yet. Public at the Gateway does not mean unrestricted business success. Auth Service still validates request fields, OTP lifecycle, password rules, rate limits, and other business rules.

For registration verification, Auth Service may return HTTP 400 with `REGISTER_OTP_INVALID` when the OTP is invalid, expired, or replaced. Gateway authentication failure is different: HTTP 401 with `AUTH_TOKEN_INVALID` means the custom Gateway JWT filter rejected the request before downstream processing.

The SePay webhook path is also public from the user-JWT perspective, but payment-service applies its own webhook authentication. It is not a customer registration endpoint.

## Gateway filter behavior

`JwtAuthenticationFilter` is a Spring Cloud Gateway `GlobalFilter`. It checks the exact `PUBLIC_EXACT_PATHS` list before forwarding requests. `SecurityConfig.anyExchange().permitAll()` does not automatically bypass this filter.

Do not replace exact public paths with any of these broad alternatives:

- `path.startsWith("/api/v1/auth")`
- permitting `/api/v1/auth/**`
- disabling `JwtAuthenticationFilter`
- requiring login before registration verification
- sending a fake Authorization header from the frontend

Protected Auth endpoints, including `/api/v1/auth/me`, must continue to return HTTP 401 when the JWT is missing or invalid.

## Maintenance checklist for new pre-auth endpoints

1. Add the endpoint to Auth Controller.
2. Configure Auth Service security for the endpoint.
3. Add the exact path to the Gateway public list when no JWT exists yet.
4. Add a Gateway filter regression test without an Authorization header.
5. Verify the request reaches Auth Service through the Gateway.
6. Verify protected endpoints remain protected.

## Troubleshooting registration OTP 401

1. Inspect the browser Network request URL, method, status, payload, and response. Preserve the browser log when a redirect occurs.
2. Check API Gateway logs and confirm route mapping.
3. Check Auth Service logs and confirm whether the request arrived.
4. Inspect `JwtAuthenticationFilter.PUBLIC_EXACT_PATHS`.
5. Call Auth Service directly with a safe test payload to distinguish Gateway failure from OTP validation.
6. Rebuild only API Gateway after changing Gateway source.
7. Register again and use a fresh OTP before it expires.

Interpretation:

| Observation | Meaning |
|---|---|
| Gateway 401 and no Auth log | Gateway filter blocked the request. |
| Direct Auth 400 `REGISTER_OTP_INVALID` | Auth endpoint is reachable, but the OTP is invalid, expired, or replaced. |
| Gateway 400 `REGISTER_OTP_INVALID` with Auth log | Gateway forwarding works; investigate OTP lifecycle. |
| Gateway 2xx with Auth log | OTP verification completed successfully according to the response. |

## Separate Cart security finding

The current filter uses a prefix check for `/api/v1/cart`. This means all paths beginning with that prefix may bypass Gateway JWT validation. The behavior is recorded as `NEEDS VERIFICATION`; inspect Cart Service authentication separately before changing it.

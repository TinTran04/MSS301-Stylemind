# Auth Flow — StyleMind

## 1. Login Flow

```text
1. User submits email/password
2. Frontend calls POST /api/v1/auth/login
3. API Gateway routes to auth-service
4. auth-service validates credentials
5. auth-service returns JWT
6. Frontend stores token in tab-scoped sessionStorage
7. The shared Axios interceptor sends the token in future requests
```

Only the Axios request interceptor reads the JWT to attach the
`Authorization: Bearer ...` header.

## 2. Protected API Flow

```text
1. Frontend sends Authorization header
2. API Gateway validates JWT
3. Gateway extracts subject/roles
4. Gateway injects identity headers
5. Backend service reads trusted identity headers
```

## 3. Admin Authorization

Admin APIs require:

```text
X-User-Roles contains ADMIN
```

## 4. Frontend Route Guard

- Customer route: require authenticated user.
- Admin route: require authenticated user and role `ADMIN`.
- Unauthenticated user redirects to `/login`.

## 5. Password Reset

```text
1. Frontend calls POST /api/v1/auth/forgot-password
2. auth-service always returns the same generic response
3. Eligible local accounts receive an OTP; only its BCrypt hash and expiry are stored
4. POST /api/v1/auth/verify-reset-otp clears the OTP and returns a short-lived reset token
5. Only the reset-token hash is stored
6. POST /api/v1/auth/reset-password hashes the new password and clears all reset state
```

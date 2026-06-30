# Auth Flow — StyleMind

## 1. Login Flow

```text
1. User submits email/password
2. Frontend calls POST /api/auth/login
3. API Gateway routes to auth-service
4. auth-service validates credentials
5. auth-service returns JWT
6. Frontend stores token
7. Frontend sends token in future requests
```

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

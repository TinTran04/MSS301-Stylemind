# Architecture Contract - Auth, API Gateway, User Profile

## Scope

This document defines the target contract architecture for the services currently owned by this workstream:

- `api-gateway`
- `auth-service`
- `user-profile-service` (current repository folder: `BE/user-service`)

This is a contract-only design step. It does not implement business logic.

## Technology Baseline

- Frontend: JavaScript, React 18, Vite, npm.
- Backend: Java 17, Spring Boot 3.2.5, Spring Cloud.
- API Gateway: Spring Cloud Gateway + WebFlux.
- Security: Spring Security + JWT.
- ORM: Spring Data JPA / Hibernate.
- Database: PostgreSQL 15.
- Cache/rate limit: Redis 7.
- Repository: monorepo with `FE/`, `BE/`, `docs/`, `contracts/`.

No new framework, ORM, or repository structure is introduced by this contract.

## Service Boundaries

### Auth Service

Auth Service owns account and credential data only:

- `email`
- `password_hash`
- `role`
- `account_status`
- `refresh_token` records
- identity provider metadata, if SSO is added later

Auth Service must not store profile-owned fields such as `fullName`, `phone`, `avatar`, `dateOfBirth`, or addresses. The registration request may carry `fullName` only as command input so Auth can publish the `USER_REGISTERED` event. Auth must not persist `fullName` as source of truth.

### User Profile Service

User Profile Service owns customer profile data:

- `fullName`
- `phone`
- `avatarUrl`
- `dateOfBirth`
- addresses

It stores `userId` as an external identity reference. It must not create a foreign key to `auth_db.users`.

### API Gateway

API Gateway owns no business data. It performs:

- routing
- JWT authentication
- route-level authorization
- request id creation/propagation
- request header normalization
- Redis-backed rate limiting
- public blocking of `/internal/**`

API Gateway must strip incoming identity headers from clients before writing trusted downstream headers.

Trusted downstream headers:

- `X-Request-Id`
- `X-User-Id`
- `X-User-Role`
- `X-Token-Id`
- `X-Token-Type`

## Data Ownership Rules

- One service owns one database schema/database area.
- No service may query another service database directly.
- No foreign key may cross service-owned databases.
- Cross-service identifiers such as `userId` are logical references only.
- Cross-service validation must be done by API calls, trusted gateway context, or events.

## External API Routing

The external public API path keeps the `/api` prefix:

- `/api/auth/**` routes to Auth Service.
- `/api/users/**` routes to User Profile Service.
- `/api/admin/users/{userId}/status` routes to Auth Service.
- `/api/admin/users/{userId}/role` routes to Auth Service.
- `GET /api/admin/users` and `GET /api/admin/users/{userId}` route to User Profile Service unless an aggregation/BFF service is introduced later.

Important routing decision:

- The target convention is to keep `/api` in downstream controllers and avoid `StripPrefix=1`.
- If the team chooses stripping later, downstream controllers must be changed consistently. Mixed conventions are not allowed.

## Authentication Model

JWT access tokens are bearer tokens. Minimum payload:

```json
{
  "sub": "user-id",
  "role": "CUSTOMER",
  "type": "access",
  "jti": "token-id",
  "iat": 1710000000,
  "exp": 1710003600
}
```

Allowed roles:

- `CUSTOMER`
- `STAFF`
- `ADMIN`

Allowed account statuses:

- `PENDING`
- `ACTIVE`
- `LOCKED`
- `DISABLED`

Token types:

- `access`
- `refresh`
- `password_reset`
- `email_verification`

## Response Standard

All HTTP APIs in this workstream use the same response envelope.

Success:

```json
{
  "success": true,
  "data": {},
  "requestId": "uuid"
}
```

Failure:

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Safe message"
  },
  "requestId": "uuid"
}
```

## Event Contract

When a customer registers successfully, Auth Service publishes `USER_REGISTERED`.

The event contains:

- `eventId`
- `eventType`
- `occurredAt`
- `data.userId`
- `data.fullName`

The event must not contain:

- password
- password hash
- phone
- address
- refresh token

The current repository has no message broker dependency. Until a broker is selected, this contract treats events as durable integration messages that can later be delivered by a broker, outbox, or synchronous bridge without changing payload shape.

# Security Model — StyleMind

## 1. Gateway-centered Security

```text
Frontend sends JWT
  → API Gateway validates JWT
  → Gateway injects X-User-Id and X-User-Roles
  → Backend services trust gateway headers
```

## 2. Required Headers

### Public authenticated requests

```http
Authorization: Bearer <jwt>
```

### Gateway to service

```http
X-User-Id: <user-id>
X-User-Roles: CUSTOMER,ADMIN
X-Correlation-Id: <correlation-id>
```

### Internal service-to-service

```http
X-Internal-Token: <internal-token>
```

## 3. Security Requirements

| ID | Requirement |
|---|---|
| SEC-01 | JWT validation at API Gateway |
| SEC-02 | Admin APIs require role `ADMIN` |
| SEC-03 | Block external access to `/internal/**` |
| SEC-04 | Use internal token for service-to-service |
| SEC-05 | Avoid logging tokens/passwords |
| SEC-06 | Rate limit auth and AI endpoints |
| SEC-07 | Add audit logs for destructive admin actions |
| SEC-08 | Centralized CORS at gateway |

## 4. Production Hardening

- Replace local secrets with environment variables.
- Rotate JWT/internal secrets.
- Use HTTPS.
- Use secure cookie strategy if refresh token is introduced.
- Add mTLS for service-to-service in production if needed.

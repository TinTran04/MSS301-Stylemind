# Error Codes

All errors use the standard failure envelope:

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

Messages must be safe for clients. Do not leak password validity details, token internals, database names, stack traces, or infrastructure secrets.

## Common

| Code | HTTP | Message |
| --- | ---: | --- |
| `VALIDATION_ERROR` | 400 | Request data is invalid. |
| `INVALID_REQUEST` | 400 | Request is invalid. |
| `UNAUTHORIZED` | 401 | Authentication is required. |
| `FORBIDDEN` | 403 | You do not have permission to access this resource. |
| `RESOURCE_NOT_FOUND` | 404 | Resource was not found. |
| `CONFLICT` | 409 | Resource state conflicts with the request. |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many requests. |
| `INTERNAL_ERROR` | 500 | An unexpected error occurred. |
| `SERVICE_UNAVAILABLE` | 503 | Service is temporarily unavailable. |

## Auth

| Code | HTTP | Message |
| --- | ---: | --- |
| `AUTH_EMAIL_ALREADY_EXISTS` | 409 | Email is already registered. |
| `AUTH_INVALID_CREDENTIALS` | 401 | Email or password is incorrect. |
| `AUTH_ACCOUNT_PENDING` | 403 | Account email verification is required. |
| `AUTH_ACCOUNT_LOCKED` | 403 | Account is locked. |
| `AUTH_ACCOUNT_DISABLED` | 403 | Account is disabled. |
| `AUTH_TOKEN_MISSING` | 401 | Authentication token is missing. |
| `AUTH_TOKEN_INVALID` | 401 | Authentication token is invalid. |
| `AUTH_TOKEN_EXPIRED` | 401 | Authentication token has expired. |
| `AUTH_TOKEN_REVOKED` | 401 | Authentication token has been revoked. |
| `AUTH_REFRESH_TOKEN_INVALID` | 401 | Refresh token is invalid. |
| `AUTH_REFRESH_TOKEN_EXPIRED` | 401 | Refresh token has expired. |
| `AUTH_PASSWORD_REUSE_NOT_ALLOWED` | 400 | New password cannot match the previous password. |
| `AUTH_PASSWORD_RESET_TOKEN_INVALID` | 400 | Password reset token is invalid. |
| `AUTH_EMAIL_VERIFICATION_TOKEN_INVALID` | 400 | Email verification token is invalid. |
| `AUTH_ROLE_INVALID` | 400 | Role is invalid. |
| `AUTH_STATUS_INVALID` | 400 | Account status is invalid. |

## User Profile

| Code | HTTP | Message |
| --- | ---: | --- |
| `USER_PROFILE_NOT_FOUND` | 404 | User profile was not found. |
| `USER_PROFILE_ALREADY_EXISTS` | 409 | User profile already exists. |
| `USER_PROFILE_INVALID_PHONE` | 400 | Phone number is invalid. |
| `USER_PROFILE_INVALID_AVATAR_URL` | 400 | Avatar URL is invalid. |
| `ADDRESS_NOT_FOUND` | 404 | Address was not found. |
| `ADDRESS_LIMIT_EXCEEDED` | 400 | Address limit has been reached. |
| `ADDRESS_DEFAULT_REQUIRED` | 400 | A default address is required. |
| `ADDRESS_ACCESS_DENIED` | 403 | You do not have permission to access this address. |

## Gateway

| Code | HTTP | Message |
| --- | ---: | --- |
| `GATEWAY_ROUTE_NOT_FOUND` | 404 | Route was not found. |
| `GATEWAY_INTERNAL_PATH_BLOCKED` | 403 | Internal endpoint is not publicly accessible. |
| `GATEWAY_UPSTREAM_TIMEOUT` | 504 | Upstream service timed out. |
| `GATEWAY_UPSTREAM_UNAVAILABLE` | 503 | Upstream service is unavailable. |
| `GATEWAY_INVALID_IDENTITY_HEADERS` | 400 | Identity headers are not allowed from clients. |


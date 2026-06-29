# API Contract - Auth, Gateway, User Profile

## Base URL

Local gateway base URL:

```text
http://localhost:3000/api
```

All paths below are public-facing API Gateway paths. Gateway routes the request to the owning service.

## Headers

Client request headers:

- `Authorization: Bearer <accessToken>` for protected endpoints.
- `X-Request-Id: <uuid>` optional. Gateway creates one when absent.

Trusted downstream headers set by API Gateway:

- `X-Request-Id`
- `X-User-Id`
- `X-User-Role`
- `X-Token-Id`
- `X-Token-Type`

Clients must not send identity headers. Gateway must remove or overwrite them.

## Standard Response

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

## Enums

Roles:

- `CUSTOMER`
- `STAFF`
- `ADMIN`

Account statuses:

- `PENDING`
- `ACTIVE`
- `LOCKED`
- `DISABLED`

JWT token types:

- `access`
- `refresh`
- `password_reset`
- `email_verification`

## Auth Endpoints

### POST /api/auth/register

Public.

Request:

```json
{
  "email": "customer@example.com",
  "password": "StrongPassword123!",
  "fullName": "Customer Name"
}
```

Response data:

```json
{
  "id": "11111111-1111-1111-1111-111111111111",
  "email": "customer@example.com",
  "role": "CUSTOMER",
  "status": "PENDING",
  "emailVerified": false,
  "createdAt": "2026-06-24T10:00:00Z"
}
```

Notes:

- Auth does not persist `fullName`; it is passed into `USER_REGISTERED`.
- Register does not issue tokens. Clients must call login after the account is eligible to authenticate.

### POST /api/auth/login

Public.

Request:

```json
{
  "email": "customer@example.com",
  "password": "StrongPassword123!"
}
```

Response data:

```json
{
  "accessToken": "jwt-access-token",
  "tokenType": "Bearer",
  "expiresInSeconds": 900,
  "user": {
    "id": "11111111-1111-1111-1111-111111111111",
    "email": "customer@example.com",
    "role": "CUSTOMER",
    "status": "ACTIVE"
  }
}
```

The refresh token is returned as an HttpOnly cookie for web clients and is not included in JSON.

### POST /api/auth/refresh

Public.

Request:

```json
{
  "refreshToken": "jwt-refresh-token"
}
```

Response data:

```json
{
  "accessToken": "new-jwt-access-token",
  "tokenType": "Bearer",
  "expiresInSeconds": 900
}
```

The rotated refresh token is returned as an HttpOnly cookie.

### POST /api/auth/logout

Protected.

Request:

```json
{
  "refreshToken": "jwt-refresh-token"
}
```

Response data:

```json
{
  "revoked": true
}
```

### POST /api/auth/logout-all

Protected.

Response data:

```json
{
  "revokedSessions": 3
}
```

### PUT /api/auth/change-password

Protected.

Request:

```json
{
  "currentPassword": "OldPassword123!",
  "newPassword": "NewPassword123!"
}
```

Response data:

```json
{
  "changed": true
}
```

### POST /api/auth/forgot-password

Public.

Request:

```json
{
  "email": "customer@example.com"
}
```

Response data:

```json
{
  "accepted": true
}
```

### POST /api/auth/reset-password

Public.

Request:

```json
{
  "token": "password-reset-token",
  "newPassword": "NewPassword123!"
}
```

Response data:

```json
{
  "reset": true
}
```

### POST /api/auth/verify-email

Public.

Request:

```json
{
  "token": "email-verification-token"
}
```

Response data:

```json
{
  "verified": true,
  "accountStatus": "ACTIVE"
}
```

## User Profile Endpoints

### GET /api/users/me

Protected.

Response data:

```json
{
  "userId": "usr_123",
  "fullName": "Customer Name",
  "phone": "+84901234567",
  "avatarUrl": "https://cdn.example.com/avatar/usr_123.png",
  "dateOfBirth": "1998-02-10",
  "createdAt": "2026-06-22T06:00:00Z",
  "updatedAt": "2026-06-22T06:00:00Z"
}
```

### PATCH /api/users/me

Protected.

Request:

```json
{
  "fullName": "Customer Name",
  "phone": "+84901234567",
  "avatarUrl": "https://cdn.example.com/avatar/usr_123.png",
  "dateOfBirth": "1998-02-10"
}
```

Response data is the updated profile.

### GET /api/users/me/addresses

Protected.

Response data:

```json
[
  {
    "addressId": "68a8c8ec-df4a-4c04-98f9-2ef7a21d80e2",
    "receiverName": "Customer Name",
    "receiverPhone": "+84901234567",
    "province": "Ho Chi Minh",
    "district": "District 1",
    "ward": "Ben Nghe",
    "streetAddress": "123 Nguyen Hue",
    "isDefault": true
  }
]
```

### POST /api/users/me/addresses

Protected.

Request contains `receiverName`, `receiverPhone`, `province`, `district`, `ward`, `streetAddress`, and optional `isDefault`.

Response data is the created address.

### PATCH /api/users/me/addresses/{addressId}

Protected.

Request contains partial address fields. Ownership is checked by both `addressId` and authenticated user id.

Response data is the updated address.

### DELETE /api/users/me/addresses/{addressId}

Protected.

Response data:

```json
{
  "deleted": true
}
```

### PUT /api/users/me/addresses/{addressId}/default

Protected.

Setting a new default address unsets the previous default in the same transaction.

Response data:

```json
{
  "addressId": "68a8c8ec-df4a-4c04-98f9-2ef7a21d80e2",
  "isDefault": true
}
```

Delete-default policy: when the current default address is deleted and other addresses remain, the oldest remaining address becomes default.

## Admin Endpoints

### GET /api/admin/users

Protected, `ADMIN`.

Owning service: User Profile Service for profile read model.

Query parameters:

- `page`
- `size`
- `search`

Response data:

```json
{
  "items": [
    {
      "userId": "usr_123",
      "fullName": "Customer Name",
      "phone": "+84901234567",
      "avatarUrl": null,
      "dateOfBirth": "1998-02-10"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1
}
```

### GET /api/admin/users/{userId}

Protected, `ADMIN`.

Owning service: User Profile Service for profile and addresses.

Response data:

```json
{
  "profile": {
    "userId": "usr_123",
    "fullName": "Customer Name",
    "phone": "+84901234567",
    "avatarUrl": null,
    "dateOfBirth": "1998-02-10"
  },
  "addresses": []
}
```

### PATCH /api/admin/users/{userId}/status

Protected, `ADMIN`.

Owning service: Auth Service.

Request:

```json
{
  "accountStatus": "LOCKED",
  "reason": "Manual admin action"
}
```

Response data:

```json
{
  "userId": "usr_123",
  "accountStatus": "LOCKED"
}
```

### PATCH /api/admin/users/{userId}/role

Protected, `ADMIN`.

Owning service: Auth Service.

Request:

```json
{
  "role": "STAFF",
  "reason": "Promoted to support team"
}
```

Response data:

```json
{
  "userId": "usr_123",
  "role": "STAFF"
}
```

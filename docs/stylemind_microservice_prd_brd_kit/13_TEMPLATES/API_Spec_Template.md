# API Spec Template

## API Name

`METHOD /api/path`

## Owner Service

`service-name`

## Description

Describe what this API does.

## Authentication

| Required | Role |
|---|---|
| Yes/No | Guest/Customer/Admin |

## Request Headers

```http
Authorization: Bearer <token>
X-Correlation-Id: <id>
```

## Request Body

```json
{}
```

## Response Body

```json
{
  "success": true,
  "message": "Thao tác thành công",
  "data": {},
  "timestamp": "2026-06-14T20:21:40.000Z"
}
```

## Error Codes

| Code | Meaning |
|---|---|

## Validation Rules

- Rule 1
- Rule 2

## Acceptance Criteria

- Criteria 1
- Criteria 2

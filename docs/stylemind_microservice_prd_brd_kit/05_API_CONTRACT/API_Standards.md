# API Standards — StyleMind

## 1. Base Rule

Tất cả frontend calls đi qua API Gateway.

```text
Frontend → API Gateway → Backend Services
```

## 2. Success Response

```json
{
  "success": true,
  "message": "Thao tác thành công",
  "data": {},
  "timestamp": "2026-06-14T20:21:40.000Z"
}
```

## 3. Error Response

```json
{
  "success": false,
  "errorCode": "ERROR_CODE_STRING",
  "message": "Mô tả lỗi",
  "timestamp": "2026-06-14T20:21:40.000Z"
}
```

## 4. Pagination Response

```json
{
  "success": true,
  "message": "Lấy dữ liệu thành công",
  "data": {
    "items": [],
    "page": 0,
    "size": 20,
    "totalItems": 100,
    "totalPages": 5
  },
  "timestamp": "2026-06-14T20:21:40.000Z"
}
```

## 5. HTTP Status Code

| Status | Use Case |
|---|---|
| 200 | Success |
| 201 | Created |
| 204 | No content |
| 400 | Validation/business error |
| 401 | Unauthenticated |
| 403 | Forbidden |
| 404 | Not found |
| 409 | Conflict |
| 429 | Rate limited |
| 500 | Unexpected server error |

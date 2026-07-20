# API Conventions

## Versioning
| Loại | Prefix |
|---|---|
| Public / Customer | `/api/v1/...` |
| Admin | `/api/v1/admin/...` |
| Internal (service-to-service) | `/internal/v1/...` |

Lý do version: khi cần breaking change, ra `/api/v2` mà `/api/v1` cũ vẫn sống → **backward compatibility**, không phá client cũ.

## HTTP method & status code
- `GET` đọc, `POST` tạo, `PUT` thay toàn bộ, `PATCH` sửa một phần, `DELETE` xóa.
- 200 OK, 201 Created, 204 No Content, 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found, **409 Conflict** (vd transition trạng thái không hợp lệ, admin self-action), 422 nếu tách validation.

## Error response (đề xuất format chuẩn)
```json
{
  "timestamp": "2026-01-01T10:00:00Z",
  "status": 409,
  "error": "InvalidOrderStatusTransition",
  "message": "Cannot transition COMPLETED -> PENDING",
  "path": "/api/v1/admin/orders/123/status",
  "traceId": "abc-123"
}
```
Hiện thực bằng `@RestControllerAdvice` để mọi lỗi có cùng shape.

## Pagination / filtering / sorting
- Query params: `?page=0&size=20&sort=createdAt,desc&status=ACTIVE&keyword=shirt`.
- Response list nên kèm metadata phân trang (totalElements, totalPages, page, size).

## Idempotency
- Webhook SePay **bắt buộc idempotent**: lưu `transactionId` của SePay, đã xử lý thì bỏ qua.

## OpenAPI/Swagger
- Mỗi service expose springdoc; cập nhật servers/paths theo `/api/v1`.

# Non-functional Requirements

| Nhóm | Yêu cầu |
|---|---|
| Security | JWT, RBAC, admin guard, internal token, webhook verification, không expose internal APIs ra frontend. |
| Scalability | Service scale độc lập. |
| Maintainability | Tách service theo business capability. |
| Reliability | Health check, timeout, retry hợp lý cho service-to-service; idempotent webhook. |
| Observability | Logging, metrics, tracing, correlation ID. |
| Data Consistency | Chấp nhận eventual consistency (order/payment/cart/notification), điều phối bằng saga. |
| API Governance | REST, versioning `/api/v1`, response format chuẩn, OpenAPI/Swagger. |

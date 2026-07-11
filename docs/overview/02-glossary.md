# 2. Glossary — Thuật ngữ

Bảng thuật ngữ để cả team hiểu thống nhất.

| Thuật ngữ | Giải thích ngắn |
|---|---|
| **API Gateway** | Cửa ngõ duy nhất frontend gọi vào; lo routing, CORS, validate JWT, chặn admin, inject user context. |
| **Database per service** | Mỗi microservice có DB riêng; service khác không truy cập trực tiếp DB đó. |
| **Bounded Context** | Ranh giới một "vùng nghiệp vụ" trong DDD; mỗi service nên gói gọn một bounded context. |
| **Authoritative price** | Giá "nguồn sự thật" lấy từ product-service; cart/order KHÔNG tự tin giá client gửi. |
| **Internal token** (`X-Internal-Token`) | Bí mật dùng cho gọi nội bộ service-to-service (`/internal/v1/**`); frontend không có. |
| **Orchestration** | Một service "nhạc trưởng" (order-service) chủ động gọi lần lượt các service khác. |
| **Choreography** | Không có nhạc trưởng; mỗi service tự phản ứng theo event. (Chưa dùng ở MVP.) |
| **Saga** | Chuỗi local transaction thay cho 1 transaction ACID xuyên service; lỗi giữa chừng thì chạy compensation. |
| **Compensation** | Hành động bù trừ để hoàn tác bước đã làm (vì không rollback xuyên service được). |
| **Eventual consistency** | Dữ liệu giữa các service sẽ nhất quán "sau một lúc", không tức thời. |
| **Idempotency** | Gọi nhiều lần cho cùng một input chỉ tạo một hiệu ứng (quan trọng cho webhook SePay). |
| **VietQR** | Chuẩn mã QR chuyển khoản ngân hàng tại VN. |
| **Open Banking (SePay)** | SePay theo dõi biến động số dư ngân hàng và bắn Webhook khi tiền về — không phải cổng quẹt thẻ. |
| **Webhook** | HTTP callback do bên thứ ba (SePay) gọi vào backend khi có sự kiện (tiền về). |
| **State machine** | Tập trạng thái + các bước chuyển hợp lệ; chặn nhảy trạng thái lung tung. |
| **Lazy-init (profile)** | Tạo profile lần đầu user chạm vào, thay vì tạo sẵn lúc đăng ký. |
| **RBAC** | Phân quyền theo vai trò (CUSTOMER, ADMIN). |
| **JWT** | Token đăng nhập; gateway validate rồi inject user id/roles xuống service. |
| **DTO / Entity / Mapper** | DTO: dữ liệu vào/ra API; Entity: bản ghi DB; Mapper: chuyển đổi giữa hai cái. |

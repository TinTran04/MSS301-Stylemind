# api-gateway

**Port:** `3000` &nbsp;|&nbsp; **Database:** `—`

## Purpose
Cửa ngõ duy nhất cho frontend. Lo routing tới các service, CORS, validate JWT, admin guard, và inject user context.

## Owns (dữ liệu service này sở hữu)
- Route config, CORS policy, JWT validation logic.

## Does NOT own
- Không sở hữu dữ liệu nghiệp vụ; không có DB.

## API — Public / Customer
_(không có)_

## API — Admin (role ADMIN)
_(không có)_

## API — Internal (`X-Internal-Token`, frontend cấm gọi)
_(không có)_

## Key business rules
- Validate JWT trên mọi request cần auth.
- Chặn `/api/v1/admin/**` nếu không phải ADMIN.
- Inject `X-User-Id`, `X-User-Roles` xuống downstream; loại bỏ 2 header này nếu client tự gửi.
- Chỉ cho phép đúng `POST /api/v1/payments/webhook/sepay` đi qua mà không cần JWT; payment-service vẫn kiểm tra API key SePay.
- SePay phải gọi URL HTTPS công khai dạng `https://<public-host>/api/v1/payments/webhook/sepay`; `localhost` chỉ dùng để kiểm tra nội bộ và không thể nhận callback từ SePay. Khi phát triển local, dùng ngrok hoặc Cloudflare Tunnel, không hardcode URL tunnel vào source.
- Không lộ port service ra ngoài.

## Dependencies
- **Gọi ra:** toàn bộ service (routing).
- **Được gọi bởi:** frontend.

## Notes
Là điểm đặt lý tưởng cho rate limit và correlation-id (sinh traceId đầu vào).

# ai-agent-service

**Port:** `8085` &nbsp;|&nbsp; **Database:** `ai_db`

## Purpose
AI stylist: chat, gợi ý sản phẩm/outfit, bundles, và AI index jobs cho admin.

## Owns (dữ liệu service này sở hữu)
- Chat history, recommendation, index jobs.

## Does NOT own
- Không sở hữu catalog/order.

## API — Public / Customer
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/v1/ai-stylist/chat` | Gửi message cho AI |
| GET | `/api/v1/ai-stylist/history` | Chat history |
| GET | `/api/v1/ai-stylist/bundles` | AI bundles |

## API — Admin (role ADMIN)
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/v1/admin/ai/index-jobs` | Admin xem index jobs |
| POST | `/api/v1/admin/ai/index-jobs` | Admin tạo index job |

## API — Internal (`X-Internal-Token`, frontend cấm gọi)
_(không có)_

## Key business rules
- MVP có thể trả mock/partial recommendation.
- Giữ **response DTO ổn định** để 'add to cart from recommendation' và frontend không phải sửa khi AI thật lên.

## Dependencies
- **Gọi ra:** (tùy chọn) product-service để lấy sản phẩm gợi ý.
- **Được gọi bởi:** gateway (AI pages/admin).

## Notes
Semantic search / vector DB / knowledge graph là phase sau.

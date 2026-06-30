# ai-agent-service Specification

## 1. Overview

| Field | Value |
|---|---|
| Service | `ai-agent-service` |
| Port | `8085` |
| Database | `ai_db` |
| Responsibility | AI chat, recommendations, bundles, index jobs |

## 2. Owned Data

- Chat sessions
- Messages
- AI bundles
- Analytics logs
- Index jobs

## 3. Main APIs

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/ai-stylist/chat` | Chat với AI |
| GET | `/api/ai-stylist/history` | Chat history |
| GET | `/api/ai-stylist/bundles` | AI bundles |
| POST | `/api/admin/ai/index-jobs` | Tạo index job |
| GET | `/api/admin/ai/index-jobs` | Index job list |

## 4. Architecture Layers

```text
controller
application/service
domain/entity
domain/repository
dto/request
dto/response
mapper
exception
infrastructure/client
infrastructure/config
```

## 5. Requirements

- API response phải theo format chuẩn.
- Validate request DTO.
- Không expose entity trực tiếp ra API.
- Có global exception handler.
- Có transaction boundary rõ ràng.
- Có unit/integration tests cho critical flow.

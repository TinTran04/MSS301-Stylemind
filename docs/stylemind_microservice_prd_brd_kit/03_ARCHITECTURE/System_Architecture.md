# System Architecture — StyleMind

## 1. High-Level Architecture

```text
Frontend Client
     |
     v
API Gateway
     |
     +--> auth-service
     +--> user-service
     +--> product-service
     +--> cart-service
     +--> order-service
     +--> payment-service
     +--> notification-service
     +--> ai-agent-service
```

## 2. Supporting Infrastructure

```text
PostgreSQL  -> business databases/schemas
Redis       -> gateway rate limit/cache
Qdrant      -> vector search for AI semantic retrieval
Neo4j       -> fashion knowledge graph
MinIO       -> product image object storage
```

## 3. Design Principles

- Microservices split by business capability.
- API Gateway is the single public entry point.
- Each service owns its data.
- No cross-service direct database access.
- Internal APIs protected by internal token.
- Eventual consistency accepted for distributed workflows.

## 4. Architecture Style

StyleMind dùng microservices cho backend. Tuy nhiên MVP cần giữ triển khai đơn giản:

- REST-first communication.
- Docker Compose local deployment.
- Không ép event-driven toàn bộ hệ thống ngay từ đầu.
- Saga/compensation chỉ tập trung vào checkout/order/payment.

# StyleMind Backend — Project Spec

## 1. Overview

**Tên sản phẩm:** StyleMind  
**Mã dự án:** MSS301-Stylemind  
**Loại:** E-commerce platform (thời trang)  
**Phạm vi BE:** Microservices backend chạy trên Docker Compose, frontend/WebApp không thuộc phạm vi backend này.

## 2. Service Inventory

| Service              | Port | Role                     |
|----------------------|------|---------------------------|
| stylemind-postgres   | 5432 | Database                 |
| stylemind-redis      | 6379 | Cache / Session          |
| stylemind-qdrant     | 6333/6334 | Vector store      |
| stylemind-neo4j      | 7474/7687 | Graph DB          |
| stylemind-minio      | 9000/9001 | Object storage    |
| api-gateway          | 3001 | API gateway              |
| auth-service         | 8081 | Authentication           |
| user-service         | 8082 | Customer profile         |
| product-service      | 8083 | Catalog                  |
| cart-service         | 8086 | Cart / promotions        |
| order-service        | 8087 | Orders                   |
| payment-service      | 8088 | Payments                 |
| notification-service | 8089 | Notifications            |

## 3. Current State

Docker Compose: healthy
- Postgres, Redis, Qdrant, Neo4j, Minio: RUNNING
- api-gateway: UP on 3001
- auth-service: 8081
- user-service: 8082
- product-service: 8083
- cart-service: 8086
- order-service: 8087
- payment-service: 8088
- notification-service: 8089

Gateway findings:
- Build successful.
- Health: UP (components include Redis UP; discovery client is UNKNOWN).
- Route inspection actuator endpoint returns 404; routing workflow needs endpoint-aware confirmation.

## 4. Roadmap

Sprint 0
- [x] Bring services online
- [ ] Verify gateway routes end-to-end
- [ ] Clean obsolete compose warning

## 5. Notes

- Port 3001 is used for api-gateway.
- Use JWT + internal token pattern for service-to-service calls.

# Docker Compose Guide — StyleMind

## 1. Deployment Target

MVP local environment dùng Docker Compose.

## 2. Infrastructure Containers

| Container | Purpose |
|---|---|
| PostgreSQL | Business databases/schemas |
| Redis | Gateway rate limit/cache |
| Qdrant | AI vector database |
| Neo4j | Fashion knowledge graph |
| MinIO | Product image object storage |

## 3. Startup Order

```text
1. PostgreSQL, Redis, Qdrant, Neo4j, MinIO
2. API Gateway, Auth, User, Product
3. Cart, Order, Payment, Notification
4. AI Agent, Frontend
```

## 4. Run Command

```bash
docker-compose up -d
```

## 5. Environment Rules

- Put environment variables in `.env`.
- Do not commit `.env`.
- Use local default only for development.
- Replace secrets before non-local deployment.

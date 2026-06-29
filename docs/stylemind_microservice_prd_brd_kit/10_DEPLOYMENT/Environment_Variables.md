# Environment Variables — StyleMind

## Common

```env
SPRING_PROFILES_ACTIVE=local
JWT_SECRET=change-me
INTERNAL_TOKEN=change-me
```

## PostgreSQL

```env
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_USER=stylemind
POSTGRES_PASSWORD=stylemind
```

## Redis

```env
REDIS_HOST=redis
REDIS_PORT=6379
```

## AI Infrastructure

```env
QDRANT_URL=http://qdrant:6333
NEO4J_URI=bolt://neo4j:7687
NEO4J_USERNAME=neo4j
NEO4J_PASSWORD=change-me
```

## MinIO

```env
MINIO_ENDPOINT=http://minio:9000
MINIO_ACCESS_KEY=stylemind
MINIO_SECRET_KEY=change-me
MINIO_BUCKET_PRODUCTS=product-images
```

## Frontend

```env
VITE_API_BASE_URL=http://localhost:3001
```

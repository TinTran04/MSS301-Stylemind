# Health Checks — StyleMind

## 1. Basic Health Endpoint

Each Spring Boot service should expose:

```text
/actuator/health
/actuator/info
/actuator/metrics
```

## 2. Readiness Checks

Readiness should verify:

- Database connection.
- Redis connection if used.
- Required downstream dependency if critical.
- Migration completed.

## 3. Liveness Checks

Liveness should verify:

- Application process alive.
- JVM responsive.
- No deadlock critical state.

## 4. Docker Compose Healthcheck Example

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
  interval: 10s
  timeout: 5s
  retries: 5
```

## 5. Gateway Health

Gateway health should verify:

- Gateway process alive.
- Redis available if rate limiting required.
- Routes configured.

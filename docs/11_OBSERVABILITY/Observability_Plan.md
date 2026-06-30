# Observability Plan — StyleMind

## 1. Goals

- Detect service downtime.
- Trace checkout flow across services.
- Measure latency/error rate.
- Debug production issues with correlation ID.
- Monitor AI endpoint cost/latency later.

## 2. Logging

Required fields:

```text
timestamp
level
service
traceId
spanId
correlationId
userId
message
error
```

## 3. Metrics

| Metric | Purpose |
|---|---|
| http_server_requests_seconds | API latency |
| http_requests_total | Request count |
| error_rate | Error ratio |
| jvm_memory_used | JVM memory |
| db_connection_pool | Database health |
| checkout_success_total | Business KPI |
| payment_failed_total | Business KPI |

## 4. Tracing

Use OpenTelemetry.

Critical traces:

- Login flow.
- Product listing.
- Add to cart.
- Checkout.
- Payment.
- AI chat.

## 5. Tools

| Area | Tool |
|---|---|
| Metrics | Prometheus |
| Dashboard | Grafana |
| Tracing | Jaeger/Tempo |
| Logs | Loki/ELK |
| Spring metrics | Actuator + Micrometer |

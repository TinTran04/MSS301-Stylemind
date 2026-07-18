# Docker Service URL Matrix

This matrix records the Docker-first runtime contract. Values below are hostnames on the Compose network, not host-machine loopback addresses.

| Caller | Target | Variable/property | Docker value | Internal auth |
|--------|--------|------------------|--------------|---------------|
| API Gateway | auth-service | `AUTH_SERVICE_URL` | `http://auth-service:8081` | Gateway routing/JWT rules |
| API Gateway | user-service | `USER_SERVICE_URL` | `http://user-service:8082` | Gateway routing/JWT rules |
| API Gateway | product-service | `PRODUCT_SERVICE_URL` | `http://product-service:8083` | Gateway routing/JWT rules |
| API Gateway | cart-service | `CART_SERVICE_URL` | `http://cart-service:8086` | Gateway routing/JWT rules |
| API Gateway | order-service | `ORDER_SERVICE_URL` | `http://order-service:8087` | Gateway routing/JWT rules |
| API Gateway | payment-service | `PAYMENT_SERVICE_URL` | `http://payment-service:8088` | Gateway routing/JWT rules |
| API Gateway | notification-service | `NOTIFICATION_SERVICE_URL` | `http://notification-service:8089` | Gateway routing/JWT rules |
| API Gateway | ai-agent-service | `AI_SERVICE_URL` | `http://ai-agent-service:8085` | Gateway routing/JWT rules |
| auth-service | notification-service | `NOTIFICATION_SERVICE_URL` | `http://notification-service:8089` | `X-Internal-Token` |
| cart-service | product-service | `PRODUCT_SERVICE_URL` | `http://product-service:8083` | `X-Internal-Token` |
| order-service | auth-service | `AUTH_SERVICE_URL` | `http://auth-service:8081` | `X-Internal-Token` |
| order-service | notification-service | `NOTIFICATION_SERVICE_URL` | `http://notification-service:8089` | `X-Internal-Token` |
| order-service | cart-service | `CART_SERVICE_URL` | `http://cart-service:8086` | `X-Internal-Token` |
| order-service | product-service | `PRODUCT_SERVICE_URL` | `http://product-service:8083` | `X-Internal-Token` |
| order-service | payment-service | `PAYMENT_SERVICE_URL` | `http://payment-service:8088` | `X-Internal-Token` |
| payment-service | order-service | `ORDER_SERVICE_URL` | `http://order-service:8087` | `X-Internal-Token` |
| ai-agent-service | product-service | `PRODUCT_SERVICE_URL` | `http://product-service:8083` | `X-Internal-Token` |
| ai-agent-service | order-service | `ORDER_SERVICE_URL` | `http://order-service:8087` | `X-Internal-Token` |

## Configuration rules

- Docker Compose supplies the variables above explicitly. A container must not use `localhost` to reach another container.
- `INTERNAL_TOKEN` is the configuration variable. The HTTP header remains `X-Internal-Token`.
- `application-local.yml` may retain `localhost` for IntelliJ/local-process runs.
- Browser URLs and host-side curl commands may use published ports such as `http://localhost:3000`.
- Same-container healthchecks may use `localhost`.
- Missing production service URL or internal-token values should fail at startup rather than silently selecting a loopback fallback.

## Evidence snapshot

On 2026-07-19, the resolved Compose configuration and recreated `payment-service` container reported `ORDER_SERVICE_URL=http://order-service:8087`; Docker DNS resolved `order-service`, and the payment actuator health check returned HTTP 200. Secret values were not printed. A controlled SePay webhook replay remains pending.

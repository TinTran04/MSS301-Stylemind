# Docker Runtime Runbook

## Start the application profile

From `BE/`, use the existing Compose file and preserve the current volumes:

```bash
docker compose --env-file .env --profile all up -d --build
```

The Compose file is `BE/docker-compose.yml`. The `.env` file used by this project is `BE/.env`.

## Safe checks

```bash
docker compose ps
docker compose exec order-service sh -c 'printf "%s\\n" "$PRODUCT_SERVICE_URL"'
docker compose exec payment-service sh -c 'printf "%s\\n" "$ORDER_SERVICE_URL"'
docker compose exec order-service getent hosts product-service
docker compose exec payment-service getent hosts order-service
```

Do not print token values. For internal authentication, check only whether `INTERNAL_TOKEN` is set. Internal requests use the `X-Internal-Token` header.

## Preserve database data

`docker compose down -v` is destructive because it removes named volumes. Do not use it for routine restarts or configuration changes. To stop application containers while preserving volumes, target the application services explicitly or use `docker compose stop`.

The project currently relies on service-specific init scripts and existing database volumes. Init scripts do not retroactively update an already-created database. Schema changes for existing volumes require a reviewed, non-destructive manual SQL patch; never reset the volume to hide a schema problem.

## Networking diagnosis

If order checkout reports a Product snapshot or price failure, inspect the order container's `PRODUCT_SERVICE_URL` first. In Docker it must be `http://product-service:8083`, not `http://localhost:8083`. If the URL is correct, check Docker DNS and the Product actuator endpoint before investigating QR generation.

If payment callbacks fail, inspect payment-service's `ORDER_SERVICE_URL`, which must be `http://order-service:8087`. A 401/403 after connectivity succeeds indicates internal-token mismatch, not a DNS failure.

Payment Service callback configuration is injected explicitly by the
`payment-service` Compose environment block:

```yaml
ORDER_SERVICE_URL: ${ORDER_SERVICE_URL}
```

The payment Feign client consumes `${ORDER_SERVICE_URL}` directly. `.env`
defines the substitution value, but a value in `.env` is not automatically
placed inside a container unless Compose maps it under that service. After
changing this mapping, recreate only the application service that consumes it:

```bash
docker compose --env-file .env --profile all up -d --no-deps --force-recreate payment-service
```

Do not claim the SePay callback workflow is fully verified from configuration
alone. A controlled webhook test must still confirm payment and order status
updates, and must show no internal-token 401/403 response.

## Local IntelliJ mode

When a service runs directly from IntelliJ, its local profile may call host-published ports through `localhost`. That is a different runtime topology from Docker and should not be copied into Compose environment values.

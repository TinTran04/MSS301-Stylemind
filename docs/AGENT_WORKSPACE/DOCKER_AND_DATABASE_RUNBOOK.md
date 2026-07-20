# Docker Runtime Runbook

## 2026-07-20 structured address schema

User Service creates/extends its administrative and address schema through Flyway
`V3__structured_vietnamese_addresses.sql`. Order Service has no Flyway history in this repository;
fresh databases use `BE/init-scripts/06-order-db.sql`, while existing Order volumes require the
reviewed non-destructive patch
`docs/database/manual-patches/2026-07-20-structured-shipping-snapshot.sql`.

The patch only adds missing snapshot columns/indexes. Do not reset volumes, infer legacy province or
ward values, or backfill historical orders without a separately reviewed migration plan.

For Docker checkout, Order Service must resolve User Service through
`USER_SERVICE_URL=http://user-service:8082`. The browser must continue using Gateway routes and must
never call the protected User Service address lookup directly.

The User Service Compose block must also include `INTERNAL_TOKEN: ${INTERNAL_TOKEN}`. Without this
mapping, Order Service can resolve `user-service` and reach the network, but User Service's
`InternalAuthFilter` returns 403 before ownership validation. A 403 at this boundary is therefore
first an internal-token configuration check, not proof that the address belongs to another user.

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

For Order Service calls to Auth Service, both Compose services must receive the
same `INTERNAL_TOKEN` value. The shared Feign interceptor sends it as
`X-Internal-Token`, and Auth's `InternalAuthFilter` validates that header on
`/internal/v1/**`. A safe check is:

```bash
docker compose exec order-service sh -c 'test -n "$INTERNAL_TOKEN" && echo SET || echo NOT_SET'
docker compose exec auth-service sh -c 'test -n "$INTERNAL_TOKEN" && echo SET || echo NOT_SET'
```

Compare values only in memory and report `MATCH` or `MISMATCH`; never print the
token. A read-only Order-to-Auth email lookup returning HTTP 200 proves network
reachability and internal authentication for that endpoint. A 403 with a
healthy endpoint indicates mismatched token configuration.

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

## Diagnosing internal-token 403s across any service pair (generalized 2026-07-19)

The order-service/auth-service check above is one instance of a general pattern: **every**
`/internal/v1/**` call is guarded by the same `internal.token` property, but each service binds
that property to a different environment variable in its own `application.yml`
(`INTERNAL_TOKEN` or `X_INTERNAL_TOKEN`), and Compose independently decides which of those
variables it actually injects into each container. A 403 on any internal call can come from either
side disagreeing. To check any caller/target pair without ever printing a token value:

```bash
# 1. Confirm which env var each container actually has set (name only, not value):
docker compose exec <caller>  sh -c 'test -n "$INTERNAL_TOKEN" && echo INTERNAL_TOKEN_SET; test -n "$X_INTERNAL_TOKEN" && echo X_INTERNAL_TOKEN_SET'
docker compose exec <target>  sh -c 'test -n "$INTERNAL_TOKEN" && echo INTERNAL_TOKEN_SET; test -n "$X_INTERNAL_TOKEN" && echo X_INTERNAL_TOKEN_SET'

# 2. Cross-reference which one each service's application.yml actually binds `internal.token` to
#    (see ENVIRONMENT_MATRIX.md "Internal-token binding per service" for the current, verified
#    answer per service - do not assume it is the same variable for every service).

# 3. Compare the two .env values referenced above without printing either:
python3 - <<'PY'
vals = {}
with open('.env') as f:
    for line in f:
        line = line.strip()
        if line.startswith('INTERNAL_TOKEN=') or line.startswith('X_INTERNAL_TOKEN='):
            k, _, v = line.partition('=')
            vals[k] = v.strip().strip('"').strip("'")
print('EQUAL' if vals.get('INTERNAL_TOKEN') == vals.get('X_INTERNAL_TOKEN') else 'DIFFERENT')
PY
```

A `403`/`AUTH_ACCESS_DENIED` from `InternalAuthFilter` after DNS/connectivity already succeeds
means the two sides' effective `internal.token` values differ; a `Connection refused` or timeout
means DNS/networking is the problem instead, and the internal-token check above is not yet
relevant.

## Gateway DEBUG logging can print Authorization headers and identity headers

`api-gateway`'s `application.yml` currently sets `org.springframework.cloud.gateway: DEBUG` and
`com.stylemind.gateway: DEBUG`. At this level, Spring Cloud Gateway's request/response observation
filters log the full set of inbound headers, including `Authorization` (both the SePay webhook's
static API key and customer JWTs) and the client-sent `X-User-Id`/`X-User-Roles`/`X-User-Email`
headers (the custom `JwtAuthenticationFilter` strips these from the *mutated* request forwarded
downstream, but the DEBUG log captures the original inbound request before that mutation). This was
observed directly in current container logs. When inspecting Gateway logs for any reason:

```bash
docker compose logs --tail=300 --no-color api-gateway | \
  sed -E 's/(Bearer|Apikey) [A-Za-z0-9._-]+/\1 <REDACTED>/g'
```

Never paste unredacted Gateway DEBUG output into a shared document, chat, or issue tracker. Do not
lower this log level as part of a documentation task; treat it as a known, reportable risk.

## Local IntelliJ mode

When a service runs directly from IntelliJ, its local profile may call host-published ports through `localhost`. That is a different runtime topology from Docker and should not be copied into Compose environment values.

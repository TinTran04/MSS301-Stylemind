# StyleMind — Backend

E-commerce (fashion) platform backend for **MSS301-StyleMind**. A Spring Boot microservices system
behind a single API Gateway, orchestrated with Docker Compose. The frontend talks to everything
through the gateway on port **3000**.

---

## Tech stack

- **Java 17**, **Spring Boot 3.2.5**, **Spring Cloud Gateway**
- **Maven** multi-module reactor (`pom.xml` → 10 modules)
- **PostgreSQL** (8 schemas), **Redis** (rate-limit/cache), **Qdrant** (vector search),
  **Neo4j** (fashion graph, WIP), **MinIO** (S3-compatible image storage)
- **Docker Compose** for infra and full-stack runs

---

## Prerequisites

| Tool | Needed for | Notes |
|------|------------|-------|
| Docker + Docker Compose | Always (infra, and/or full stack) | Postgres, Redis, etc. run as containers |
| JDK 17 + Maven | Only when running services from an IDE (mode B) | Match Java 17 exactly |

You do **not** need a local JDK if you only run the full stack in Docker (mode A).

---

## Repository layout

```
BE/
├── pom.xml                     # Maven reactor (parent)
├── common-lib/                 # shared DTOs, security, exceptions, correlation-id filter
├── api-gateway/                # routing, JWT validate, rate-limit, CORS  (port 3000)
├── auth-service/               # login, register, JWT issue               (8081)
├── user-service/               # customer profile, addresses              (8082)
├── product-service/            # catalog, categories, images              (8083)
├── ai-agent-service/           # AI stylist chat, bundles, index          (8085)
├── cart-service/               # guest + authenticated cart               (8086)
├── order-service/              # orders, checkout flow                    (8087)
├── payment-service/            # transactions (COD + SePay sandbox)       (8088)
├── notification-service/       # notification logs / email                (8089)
├── init-scripts/               # DB bootstrap SQL (runs once, on first volume creation)
├── docker-compose.infra.yml    # infra only  → use with IDE
├── docker-compose.full.yml     # infra + all services → all-Docker
├── Makefile                    # convenience targets
└── .env.example                # copy to .env
```

---

## Services & ports

### Infrastructure (`docker-compose.infra.yml`)

| Service | Port | Purpose |
|---------|------|---------|
| stylemind-postgres | 5432 | PostgreSQL — 8 schemas |
| stylemind-redis | 6379 | Gateway rate-limit, cache |
| stylemind-qdrant | 6333 / 6334 | Vector store (AI semantic search) |
| stylemind-neo4j | 7474 / 7687 | Graph DB (fashion rules, WIP) |
| stylemind-minio | 9000 / 9001 | Object storage (product images); console on 9001 |

### Application services

| Service | Port | Database | Purpose |
|---------|------|----------|---------|
| api-gateway | 3000 | — | Routing, JWT validate, rate-limit, CORS |
| auth-service | 8081 | auth_db | Login, register, JWT issue |
| user-service | 8082 | user_db | Customer profile, addresses |
| product-service | 8083 | product_db | Catalog, categories, images |
| ai-agent-service | 8085 | ai_db | AI stylist chat, bundles, index |
| cart-service | 8086 | cart_db | Cart (guest + authenticated) |
| order-service | 8087 | order_db | Orders, checkout flow |
| payment-service | 8088 | payment_db | Transactions (COD + SePay sandbox) |
| notification-service | 8089 | notification_db | Notification logs / email |

> There is **no** discovery/Eureka service. Some compose blocks set `EUREKA_*` env vars — they are
> unused and safe to ignore. Routing is done entirely by the gateway's static route table.

---

## 1. Environment setup

```bash
cd BE
cp .env.example .env
```

Key variables (see `.env.example` and `PROJECT_SPEC.md` §8):

| Variable | Used by | Meaning |
|----------|---------|---------|
| `JWT_PRIVATE_KEY_PATH` | auth-service | RSA-2048 private key for token signing |
| `JWT_PUBLIC_KEY_PATH` | all services | RSA-2048 public key for token verification |
| `X_INTERNAL_TOKEN` | Feign service-to-service calls | guards `/internal/v1/**` |
| `SEPAY_WEBHOOK_API_KEY` | payment-service | SePay sends `Authorization: Apikey <this>`; webhooks rejected if it doesn't match |
| `LLM_API_KEY` | ai-agent-service | external LLM key |

> ⚠️ The defaults are plaintext dev-only secrets. Set real values via env vars for any non-local
> deployment.

---

## 2. Run mode A — full stack in Docker

Runs infra **and** every service in containers. Best for a quick end-to-end run without an IDE.

```bash
cd BE
make full-up        # docker compose -f docker-compose.full.yml up -d --build
make ps             # list running containers
make logs           # tail all service logs
make full-down      # stop everything
```

Gateway is then reachable at `http://localhost:3000`.

---

## 3. Run mode B — infra in Docker + services in IntelliJ

The typical dev workflow: run only the databases/infra in Docker, and start the Spring Boot services
from IntelliJ so you can debug them.

```bash
cd BE
make infra-up       # docker compose -f docker-compose.infra.yml up -d
make check-ports    # verify what's listening
```

Then, in IntelliJ, run the applications you need (Run → each `*Application` main class):

- **Always start `ApiGatewayApplication`** (port 3000) — the frontend only talks to the gateway.
- **Login** needs `AuthServiceApplication` (8081).
- **Register** additionally needs `NotificationServiceApplication` (8089) — it sends the OTP/verification email.
- Start other services (product, cart, order, payment, …) as the feature you're testing requires.

Service URLs and the gateway's Redis/route hosts **default to `localhost`**, so no per-service
environment variables are needed for IDE runs. (In Docker, `docker-compose.full.yml` overrides those
to container hostnames.)

Stop infra with `make infra-down` when done.

---

## 4. First login & smoke test

The database is seeded with a default admin (see `init-scripts/01-auth-db.sql`):

- **Email:** `admin@stylemind.ai`
- **Password:** `admin123`

Verify the gateway and a login round-trip:

```bash
# Gateway health — expect HTTP 200
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:3000/actuator/health

# Login through the gateway — expect a JWT in the response
curl -s -X POST http://localhost:3000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@stylemind.ai","password":"admin123"}'
```

CORS is open to all origins at the gateway, so the frontend dev server (`http://localhost:5173`) can
call it directly.

---

## 5. Database notes

- The 8 schemas and seed data are created by `init-scripts/*.sql`, which Postgres runs **only once —
  when the data volume is first created**.
- If you add a table or column to an entity, update the matching `init-scripts/*.sql` **and** apply it
  to any existing volume, e.g.:
  ```bash
  docker exec -i stylemind-postgres psql -U postgres -d payment_db < init-scripts/07-payment-db.sql
  ```
  (Re-running is safe: table DDL uses `CREATE TABLE IF NOT EXISTS`. Note this does **not** add missing
  columns to an existing table — drop the empty table first, or recreate the volume.)
- To rebuild the databases from scratch (destroys all data, re-runs every init script):
  ```bash
  docker compose -f docker-compose.full.yml down -v && make infra-up
  ```

---

## 6. Running tests

From `BE/`:

```bash
mvn test
```

> **Known local issue:** on some setups `mvn` fails at compile with a Lombok/JDK mismatch
> (`ExceptionInInitializerError: TypeTag :: UNKNOWN`) when a JDK 21 toolchain is picked up. If you hit
> this, run the reactor tests through a pinned JDK 17 Maven image instead:
> ```bash
> docker run --rm -v "$PWD":/build -v maven-repo-cache:/root/.m2 \
>   -w /build maven:3.9.6-eclipse-temurin-17 mvn test
> ```

---

## 7. Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `Connection to localhost:5432 refused` on service start | Infra not running | `make infra-up` |
| `Schema-validation: missing table [...]` | Stale volume predates a newer init script | Apply the table's DDL from `init-scripts/*.sql` to the volume, or `down -v` to rebuild |
| `Schema-validation: missing column [...]` | Existing table predates a new column | Drop the (empty) table and re-run its init script, or `down -v` to rebuild |
| Gateway: `Unable to connect to Redis` / routes to `*-service` host fail | Running the gateway from the IDE with Docker-hostname defaults | Fixed — defaults now point to `localhost`; ensure Redis is up (`make infra-up`) |
| Frontend "can't connect to backend" | Gateway and/or auth-service not running | Start `ApiGatewayApplication` + `AuthServiceApplication` (mode B), or `make full-up` |
| Register fails but login works | notification-service not running | Start `NotificationServiceApplication` (8089) |

`make check-ports` gives a quick view of which infra/service ports are currently up.

---

## Further documentation

- [`PROJECT_SPEC.md`](PROJECT_SPEC.md) — services, admin scope, security pattern, known issues, roadmap
- [`docs/`](docs/) — architecture and requirements docs
- [`AGENTS.md`](AGENTS.md) — Developer blueprint for AI agents and developers

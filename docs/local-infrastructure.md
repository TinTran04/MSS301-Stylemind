# Local Infrastructure - Auth, Gateway, User Profile

This document covers the local infrastructure for:

- API Gateway
- Auth Service
- User Profile Service
- Auth PostgreSQL
- User Profile PostgreSQL
- RabbitMQ
- Redis

## Start Local Stack

Run from the backend directory:

```bash
cd BE
cp .env.example .env
docker compose up -d --build
```

Only API Gateway is published to the host:

```text
http://localhost:3000
```

Auth Service, User Profile Service, PostgreSQL, RabbitMQ, and Redis are internal Docker network services.

## Stop Local Stack

```bash
cd BE
docker compose down
```

To reset local databases and rerun SQL init scripts:

```bash
cd BE
docker compose down -v
docker compose up -d auth-postgres user-profile-postgres
```

## Migration Commands

The current repository uses SQL init scripts, not Flyway or Liquibase.

Auth database migration/init:

```bash
cd BE
docker compose up -d auth-postgres
```

User Profile database migration/init:

```bash
cd BE
docker compose up -d user-profile-postgres
```

SQL files:

- `BE/init-scripts/01-auth-db.sql`
- `BE/init-scripts/02-user-db.sql`

Important: PostgreSQL Docker init scripts run only when the named volume is empty. Use `docker compose down -v` to force a full local reset.

## Development Commands

Compile the scoped backend modules:

```bash
cd BE
mvn clean test -pl common-lib,auth-service,api-gateway,user-service -am
```

Run infrastructure only:

```bash
cd BE
docker compose up -d auth-postgres user-profile-postgres rabbitmq redis
```

Run services locally through Docker Compose:

```bash
cd BE
docker compose up -d auth-service user-profile-service api-gateway
```

Because only API Gateway is published to the host, database, Redis, RabbitMQ,
Auth Service, and User Profile Service are reachable only from the internal
Docker network. Running a Spring Boot service directly with `mvn spring-boot:run`
requires separate local infrastructure or a local-only Compose override that
publishes the required internal ports.

Build the local Docker images:

```bash
cd BE
docker compose build api-gateway auth-service user-profile-service
```

Validate Compose:

```bash
cd BE
docker compose config
```

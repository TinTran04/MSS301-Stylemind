# Local Development Guide

This guide explains how to run the StyleMind backend locally, depending on whether you want to debug code in your IDE or run the full stack via Docker.

## Two Development Modes

We support two distinct modes of execution to avoid port conflicts and database connection issues. **Do not run both modes at the same time.**

### Mode 1: Local IntelliJ Development
This mode runs the infrastructure (databases, Redis, MinIO) in Docker, but leaves the ports open for you to run Spring Boot services from your IDE (e.g., IntelliJ IDEA).

1. **Start Infrastructure Only:**
   ```bash
   cd BE
   make infra-up
   ```
2. **Run Services in IntelliJ:**
   Set the active profile for **every** service to `local` before running:
   ```text
   SPRING_PROFILES_ACTIVE=local
   ```
   *In IntelliJ: Edit Run Configuration -> Environment variables -> `SPRING_PROFILES_ACTIVE=local`*
3. **Run Frontend:**
   ```bash
   cd ../FE
   npm run dev
   ```
   *Make sure your `.env` has `VITE_API_BASE_URL=http://localhost:3001`*

### Mode 2: Full Docker Mode
This mode spins up the entire stack, including all Java services, inside Docker. Use this when you are working purely on the frontend or testing the final Docker images.

1. **Stop IntelliJ Services:** Ensure no Spring Boot services are running locally on your machine.
2. **Start Full Stack:**
   ```bash
   cd BE
   make full-up
   ```
   *(Wait 1-2 minutes for all services to register with Eureka).*

## Make Commands Cheat Sheet
From the `BE` folder, you can use:
- `make infra-up` : Start only databases and infrastructure.
- `make infra-down` : Stop infrastructure.
- `make full-up` : Start everything (databases + all Java microservices).
- `make full-down` : Stop everything.
- `make logs` : Tail logs for the full stack.
- `make check-ports` : Quickly verify which ports are currently in use on your host machine.

## Troubleshooting

### 1. Port Already in Use (e.g., port 8088)
**Cause:** You are trying to start a service in IntelliJ, but it's already running in Docker (or vice-versa).
**Fix:**
- Run `make check-ports` to see what is running.
- If you want to use IntelliJ, make sure you ran `make infra-down` or `make full-down` to kill any rogue containers, then just run `make infra-up`.

### 2. Login Timeout (10000ms exceeded)
**Cause:** The Gateway or Auth Service is unreachable, or the frontend is calling the wrong port.
**Fix:**
- Verify frontend is calling the Gateway (`localhost:3001`), NOT `localhost:8081`.
- Ensure the API Gateway has successfully started and registered its routes.
- Verify the `local` profile is active. If a service connects to `postgres` instead of `localhost`, it will time out.

### 3. Service Cannot Connect to Postgres/Auth-Service
**Cause:** The service is using the `docker` profile (default) while running in IntelliJ, trying to connect to DNS names like `postgres` or `auth-service` which only exist inside the Docker network.
**Fix:** Add `SPRING_PROFILES_ACTIVE=local` to your run configuration.

## Verifying Login
When running either mode, you can verify the system is up by calling the Gateway:
```bash
curl -v -X POST http://localhost:3001/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"your_password"}'
```
You can also bypass the Gateway for debugging by calling the Auth Service directly (only accessible on localhost in local mode):
```bash
curl -v -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"your_password"}'
```

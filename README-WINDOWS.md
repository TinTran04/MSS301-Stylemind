# StyleMind on Windows

Windows users do not need GNU Make. `make full-up` is only a convenience
wrapper around the Docker Compose command below.

## Supported environments

- PowerShell 5.1 or PowerShell 7
- Command Prompt
- Git Bash, optionally
- Docker Desktop with the WSL2 backend enabled

## Prerequisites

Install Docker Desktop, Git, Java 17 or newer, Maven (or a Maven Wrapper if
one is added later), Node.js/npm, and ngrok if testing SePay webhooks.

Run these checks in PowerShell or Command Prompt:

```text
docker --version
docker compose version
java -version
mvn -version
node --version
npm --version
ngrok version
```

The backend Maven parent declares Java 17. The frontend does not pin a Node.js
engine, so use a current Node.js LTS release supported by Vite 5.

## Docker commands

Run the commands from the repository root unless noted otherwise. The Compose
files are under `BE`.

The exact replacement for `make full-up` is:

```powershell
docker compose -f .\BE\docker-compose.full.yml up -d --build
```

Before the first full-stack start, create a local backend environment file and
fill in your own SePay values. Do not commit this file:

```powershell
Copy-Item .\BE\.env.example .\BE\.env
```

`BE\.env` must provide `SEPAY_BANK_SHORT_NAME`, `SEPAY_ACCOUNT_NUMBER`,
`SEPAY_ACCOUNT_NAME`, and `SEPAY_WEBHOOK_API_KEY`. The full Compose file fails
fast when any required SePay value is missing; this prevents accidentally
starting a payment service that cannot generate a valid QR or authenticate a
webhook.

Start infrastructure only, for Spring Boot services launched from IntelliJ:

```powershell
docker compose -f .\BE\docker-compose.infra.yml up -d
```

Start the full stack:

```powershell
docker compose -f .\BE\docker-compose.full.yml up -d --build
```

Rebuild every service or only one service:

```powershell
docker compose -f .\BE\docker-compose.full.yml build
docker compose -f .\BE\docker-compose.full.yml build payment-service
```

View logs, one service log, restart a service, and list containers:

```powershell
docker compose -f .\BE\docker-compose.full.yml logs -f
docker compose -f .\BE\docker-compose.full.yml logs -f payment-service
docker compose -f .\BE\docker-compose.full.yml restart payment-service
docker compose -f .\BE\docker-compose.full.yml ps
```

Stop the stack while preserving named database and infrastructure volumes:

```powershell
docker compose -f .\BE\docker-compose.full.yml down --remove-orphans
```

Do not add the volume-removal option to the `down` command unless you
intentionally want to erase local PostgreSQL and infrastructure data.

If Docker reports `no configuration file provided: not found`, the command was
run outside the directory containing the Compose file or without `-f`. Use the
full path pattern shown above.

PowerShell line continuation uses a backtick:

```powershell
docker compose `
  -f .\BE\docker-compose.full.yml `
  up -d --build
```

Command Prompt line continuation uses a caret:

```text
docker compose ^
  -f .\BE\docker-compose.full.yml ^
  up -d --build
```

## Helper scripts

The helper scripts locate the repository root themselves and use the same
Compose file as `make full-up`:

```powershell
.\scripts\windows\full-up.ps1
.\scripts\windows\full-down.ps1
.\scripts\windows\logs.ps1 -Follow
.\scripts\windows\logs.ps1 payment-service -Follow
```

For a double-clickable wrapper, run `full-up.bat` from the repository root.
The scripts preserve volumes and fail immediately when Docker Desktop is not
available.

## SePay testing

Use the ngrok instructions in [README.md](README.md#testing-sepay-payment-locally-with-ngrok). The tunnel must expose API Gateway port `3000`, not payment-service port `8088`.

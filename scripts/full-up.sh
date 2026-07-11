#!/usr/bin/env sh
set -eu

repository_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
compose_file="$repository_root/BE/docker-compose.full.yml"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker CLI was not found. Install Docker Desktop or Docker Engine first." >&2
  exit 1
fi

if ! docker version >/dev/null 2>&1; then
  echo "Docker daemon is not running. Start Docker and retry." >&2
  exit 1
fi

echo "Starting the full StyleMind stack. Existing volumes will be preserved."
exec docker compose -f "$compose_file" up -d --build

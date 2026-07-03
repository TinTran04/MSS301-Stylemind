#!/usr/bin/env python3

from pathlib import Path
import re
import sys


ROOT = Path(__file__).resolve().parents[2]
IGNORED_PARTS = {".git", ".codegraph", "node_modules", "target", "dist"}

UNVERSIONED_PATH = re.compile(r'["\']/(api|internal)/(?!v1(?:/|["\']))')
UNVERSIONED_GATEWAY_PATH = re.compile(r"Path=/(api|internal)/(?!v1(?:/|\*|$))")
FRONTEND_SERVICE_PORT = re.compile(r"http://localhost:808\d")


def source_files():
    roots = (
        ROOT / "BE",
        ROOT / "FE" / "src",
        ROOT / "FE" / ".env.example",
    )
    for root in roots:
        candidates = [root] if root.is_file() else root.rglob("*")
        for path in candidates:
            if not path.is_file() or any(part in IGNORED_PARTS for part in path.parts):
                continue
            if path.suffix in {".java", ".yml", ".yaml", ".js", ".jsx"} or path.name.startswith(".env"):
                yield path


def main():
    failures = []

    for path in source_files():
        text = path.read_text(encoding="utf-8")
        relative_path = path.relative_to(ROOT)
        for line_number, line in enumerate(text.splitlines(), start=1):
            if UNVERSIONED_PATH.search(line) or UNVERSIONED_GATEWAY_PATH.search(line):
                failures.append(f"{relative_path}:{line_number}: unversioned API path: {line.strip()}")

            if relative_path.parts[0] == "FE" and FRONTEND_SERVICE_PORT.search(line):
                failures.append(f"{relative_path}:{line_number}: frontend service port: {line.strip()}")

    api_client = (ROOT / "FE/src/services/apiClient.js").read_text(encoding="utf-8")
    endpoints = (ROOT / "FE/src/services/endpoints.js").read_text(encoding="utf-8")
    env_example = (ROOT / "FE/.env.example").read_text(encoding="utf-8")

    if "baseURL:" not in api_client or "VITE_API_BASE_URL" not in api_client:
        failures.append("FE/src/services/apiClient.js: Axios must own VITE_API_BASE_URL as baseURL")
    if "VITE_API_GATEWAY" in endpoints or "http://localhost" in endpoints:
        failures.append("FE/src/services/endpoints.js: endpoint paths must not construct gateway origins")
    if "/api/v1/" not in endpoints:
        failures.append("FE/src/services/endpoints.js: endpoint paths must use /api/v1")
    if "VITE_API_GATEWAY" in env_example:
        failures.append("FE/.env.example: VITE_API_GATEWAY must be removed")

    if failures:
        print("\n".join(failures))
        return 1

    print("API versioning contract verified")
    return 0


if __name__ == "__main__":
    sys.exit(main())

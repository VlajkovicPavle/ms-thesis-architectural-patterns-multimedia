#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BENCHMARK_BASE_URL:-http://localhost:8080}"
TIMEOUT_SECONDS="${BENCHMARK_WAIT_TIMEOUT_SECONDS:-120}"
DEADLINE=$((SECONDS + TIMEOUT_SECONDS))

until curl -fsS "$BASE_URL/api/actuator/health" >/dev/null; do
  if [ "$SECONDS" -ge "$DEADLINE" ]; then
    printf 'Timed out waiting for %s/api/actuator/health\n' "$BASE_URL" >&2
    exit 1
  fi
  sleep 2
done

printf 'media-monolith is healthy at %s\n' "$BASE_URL"

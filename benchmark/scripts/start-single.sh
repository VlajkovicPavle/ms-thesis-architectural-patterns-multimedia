#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
APP_DIR="$ROOT_DIR/backend/media-monolith"

if [ ! -f "$APP_DIR/.env" ]; then
  cp "$APP_DIR/.env.example" "$APP_DIR/.env"
fi

docker build -t media-base:21 "$ROOT_DIR/backend/media-base"
(cd "$APP_DIR" && docker compose up -d --build)

printf 'Started single-instance media-monolith stack.\n'

#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DB_PATH="${BENCHMARK_DB_PATH:-$ROOT_DIR/benchmark/results/benchmark.duckdb}"
SCHEMA_PATH="$ROOT_DIR/benchmark/results/schema.sql"

run_duckdb() {
  local db_path="$1"
  if command -v duckdb >/dev/null 2>&1; then
    duckdb "$db_path"
    return
  fi

  case "$db_path" in
    "$ROOT_DIR"/*)
      local relative_db_path="${db_path#"$ROOT_DIR/"}"
      docker run --rm -i -v "$ROOT_DIR:/work:z" -w /work duckdb/duckdb:latest duckdb "$relative_db_path"
      ;;
    *)
      printf 'duckdb is not installed and BENCHMARK_DB_PATH is outside the repo: %s\n' "$db_path" >&2
      exit 1
      ;;
  esac
}

mkdir -p "$(dirname "$DB_PATH")"
run_duckdb "$DB_PATH" < "$SCHEMA_PATH"

printf 'Initialized benchmark database: %s\n' "$DB_PATH"

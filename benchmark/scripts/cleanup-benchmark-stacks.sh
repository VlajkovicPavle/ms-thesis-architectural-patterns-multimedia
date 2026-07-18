#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TARGET_VARIANT="${1:-all}"

export BENCHMARK_PROMETHEUS_CONFIG_FILE="$ROOT_DIR/benchmark/monitoring/prometheus/prometheus.yml"
export BENCHMARK_PROMETHEUS_TARGET_FILE="$ROOT_DIR/benchmark/monitoring/prometheus/targets/app.json"
export BENCHMARK_GRAFANA_PROVISIONING_DIR="$ROOT_DIR/benchmark/monitoring/grafana/provisioning"
export BENCHMARK_GRAFANA_DASHBOARDS_DIR="$ROOT_DIR/benchmark/monitoring/grafana/dashboards"

cleanup_variant() {
  local variant="$1"
  local app_dir project
  local compose_arguments=()
  case "$variant" in
    monolith)
      app_dir="$ROOT_DIR/backend/media-monolith"
      project="media-monolith"
      ;;
    modular_monolith)
      app_dir="$ROOT_DIR/backend/media-modular"
      project="media-modular"
      ;;
    microservices)
      app_dir="$ROOT_DIR/backend/media-microservices"
      project="media-microservices"
      compose_arguments=(-f "$app_dir/docker-compose.yml" -f "$ROOT_DIR/benchmark/monitoring/compose.monitoring.yml")
      ;;
    *)
      printf 'Unsupported cleanup variant: %s\n' "$variant" >&2
      return 1
      ;;
  esac
  [ -d "$app_dir" ] || return 0
  (cd "$app_dir" && docker compose -p "$project" "${compose_arguments[@]}" down -v --remove-orphans)
}

if [ "$TARGET_VARIANT" = "all" ]; then
  cleanup_variant monolith
  cleanup_variant modular_monolith
  cleanup_variant microservices
else
  cleanup_variant "$TARGET_VARIANT"
fi

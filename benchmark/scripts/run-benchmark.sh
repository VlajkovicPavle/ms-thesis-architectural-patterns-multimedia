#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
VARIANT="${BENCHMARK_VARIANT:-monolith}"
TOPOLOGY="${BENCHMARK_TOPOLOGY:-single}"
SCENARIO="${BENCHMARK_SCENARIO:-SmokeSimulation}"
RESOURCE_PROFILE="${BENCHMARK_RESOURCE_PROFILE:-baseline}"
LOAD_USERS="${BENCHMARK_LOAD_USERS:-12}"
RAMP_SECONDS="${BENCHMARK_RAMP_SECONDS:-60}"
POLL_ATTEMPTS="${BENCHMARK_POLL_ATTEMPTS:-180}"
POLL_PAUSE_MILLIS="${BENCHMARK_POLL_PAUSE_MILLIS:-1000}"
DOWNLOAD_RENDITION="${BENCHMARK_DOWNLOAD_RENDITION:-false}"
APP_PORT="${BENCHMARK_APP_PORT:-${APP_PORT:-8080}}"
POSTGRES_PORT="${BENCHMARK_POSTGRES_PORT:-${POSTGRES_PORT:-5433}}"
PROMETHEUS_PORT="${BENCHMARK_PROMETHEUS_PORT:-${PROMETHEUS_PORT:-9090}}"
CADVISOR_PORT="${BENCHMARK_CADVISOR_PORT:-${CADVISOR_PORT:-8081}}"
GRAFANA_PORT="${BENCHMARK_GRAFANA_PORT:-${GRAFANA_PORT:-3000}}"
APP_CPUS="${BENCHMARK_APP_CPUS:-${APP_CPUS:-}}"
APP_MEMORY="${BENCHMARK_APP_MEMORY:-${APP_MEMORY:-}}"
RENDITION_POOL_SIZE="${BENCHMARK_RENDITION_POOL_SIZE:-${RENDITION_POOL_SIZE:-}}"
BASE_URL="${BENCHMARK_BASE_URL:-http://localhost:$APP_PORT}"
PROMETHEUS_URL="${BENCHMARK_PROMETHEUS_URL:-http://localhost:$PROMETHEUS_PORT}"
DB_PATH="${BENCHMARK_DB_PATH:-$ROOT_DIR/benchmark/results/benchmark.duckdb}"
VIDEO_PATH="${BENCHMARK_VIDEO_PATH:-$ROOT_DIR/benchmark/data/videos/smoke-720p-10s.mp4}"
RESOLUTIONS="${BENCHMARK_RENDITIONS:-SD_360}"
AUTO_STACK="${BENCHMARK_AUTO_STACK:-true}"
RUN_ID="${BENCHMARK_RUN_ID:-$(date -u +%Y%m%dT%H%M%SZ)-$RESOURCE_PROFILE-$VARIANT}"
RUN_DIR="$ROOT_DIR/benchmark/results/runs/$RUN_ID"
REPORT_PATH="benchmark/results/runs/$RUN_ID/gatling"
METADATA_PATH="$RUN_DIR/metadata.json"
PROMETHEUS_DIR="$RUN_DIR/prometheus"

case "$VARIANT" in
  monolith)
    APP_DIR="$ROOT_DIR/backend/media-monolith"
    VARIANT_SQL="monolith"
    ;;
  modular_monolith)
    APP_DIR="$ROOT_DIR/backend/media-modular"
    VARIANT_SQL="modular_monolith"
    ;;
  *)
    printf 'Unsupported BENCHMARK_VARIANT=%s. Expected monolith or modular_monolith.\n' "$VARIANT" >&2
    exit 1
    ;;
esac

if [ "$TOPOLOGY" != "single" ]; then
  printf 'Unsupported BENCHMARK_TOPOLOGY=%s. Current runner supports single only.\n' "$TOPOLOGY" >&2
  exit 1
fi

apply_resource_profile() {
  case "$RESOURCE_PROFILE" in
    baseline)
      APP_CPUS="${APP_CPUS:-2.0}"
      APP_MEMORY="${APP_MEMORY:-2g}"
      RENDITION_POOL_SIZE="${RENDITION_POOL_SIZE:-8}"
      ;;
    vertical-1)
      APP_CPUS="${APP_CPUS:-1.0}"
      APP_MEMORY="${APP_MEMORY:-1g}"
      RENDITION_POOL_SIZE="${RENDITION_POOL_SIZE:-1}"
      ;;
    vertical-2)
      APP_CPUS="${APP_CPUS:-2.0}"
      APP_MEMORY="${APP_MEMORY:-2g}"
      RENDITION_POOL_SIZE="${RENDITION_POOL_SIZE:-2}"
      ;;
    vertical-4)
      APP_CPUS="${APP_CPUS:-4.0}"
      APP_MEMORY="${APP_MEMORY:-4g}"
      RENDITION_POOL_SIZE="${RENDITION_POOL_SIZE:-4}"
      ;;
    vertical-8)
      APP_CPUS="${APP_CPUS:-8.0}"
      APP_MEMORY="${APP_MEMORY:-8g}"
      RENDITION_POOL_SIZE="${RENDITION_POOL_SIZE:-8}"
      ;;
    custom)
      if [ -z "$APP_CPUS" ] || [ -z "$APP_MEMORY" ] || [ -z "$RENDITION_POOL_SIZE" ]; then
        printf 'custom BENCHMARK_RESOURCE_PROFILE requires BENCHMARK_APP_CPUS, BENCHMARK_APP_MEMORY, and BENCHMARK_RENDITION_POOL_SIZE.\n' >&2
        exit 1
      fi
      ;;
    *)
      printf 'Unsupported BENCHMARK_RESOURCE_PROFILE=%s. Expected baseline, vertical-1, vertical-2, vertical-4, vertical-8, or custom.\n' "$RESOURCE_PROFILE" >&2
      exit 1
      ;;
  esac
}

apply_resource_profile

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

resolution_sql_array() {
  local resolutions_sql=""
  IFS=',' read -ra resolution_values <<< "$RESOLUTIONS"
  for resolution in "${resolution_values[@]}"; do
    resolution="$(printf '%s' "$resolution" | xargs)"
    if [ -n "$resolution" ]; then
      resolutions_sql="$resolutions_sql'$resolution'::video_resolution,"
    fi
  done
  printf '[%s]' "${resolutions_sql%,}"
}

write_metadata() {
  cat > "$METADATA_PATH" <<JSON
{
  "runId": "$RUN_ID",
  "variant": "$VARIANT",
  "topology": "$TOPOLOGY",
  "resourceProfile": "$RESOURCE_PROFILE",
  "appCpus": "$APP_CPUS",
  "appMemory": "$APP_MEMORY",
  "renditionPoolSize": "$RENDITION_POOL_SIZE",
  "scenario": "$SCENARIO",
  "baseUrl": "$BASE_URL",
  "prometheusUrl": "$PROMETHEUS_URL",
  "videoFile": "$VIDEO_PATH",
  "renditions": "$RESOLUTIONS",
  "loadUsers": "$LOAD_USERS",
  "rampSeconds": "$RAMP_SECONDS",
  "pollAttempts": "$POLL_ATTEMPTS",
  "pollPauseMillis": "$POLL_PAUSE_MILLIS",
  "downloadRendition": "$DOWNLOAD_RENDITION",
  "autoStack": "$AUTO_STACK",
  "ports": {
    "app": "$APP_PORT",
    "postgres": "$POSTGRES_PORT",
    "prometheus": "$PROMETHEUS_PORT",
    "cadvisor": "$CADVISOR_PORT",
    "grafana": "$GRAFANA_PORT"
  },
  "appDir": "${APP_DIR#"$ROOT_DIR"/}",
  "startedAtUtc": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}
JSON
}

export_prometheus_query() {
  local phase="$1"
  local file_name="$2"
  local query="$3"
  local output_path="$PROMETHEUS_DIR/$phase-$file_name.json"

  if ! curl -fsS --get --data-urlencode "query=$query" "$PROMETHEUS_URL/api/v1/query" > "$output_path"; then
    printf '{"status":"unavailable","query":"%s"}\n' "$query" > "$output_path"
  fi
}

export_prometheus_snapshot() {
  local phase="$1"
  export_prometheus_query "$phase" "up" 'up'
  export_prometheus_query "$phase" "http-count" 'sum(http_server_requests_seconds_count)'
  export_prometheus_query "$phase" "http-sum" 'sum(http_server_requests_seconds_sum)'
  export_prometheus_query "$phase" "pipeline-count" 'sum(rendition_pipeline_duration_seconds_count)'
  export_prometheus_query "$phase" "pipeline-sum" 'sum(rendition_pipeline_duration_seconds_sum)'
  export_prometheus_query "$phase" "queue-size" 'rendition_queue_size'
  export_prometheus_query "$phase" "active-jobs" 'rendition_active_jobs'
  export_prometheus_query "$phase" "container-cpu" 'sum(rate(container_cpu_usage_seconds_total[1m]))'
  export_prometheus_query "$phase" "container-memory" 'sum(container_memory_working_set_bytes)'
}

mkdir -p "$RUN_DIR" "$PROMETHEUS_DIR"

if [ ! -f "$VIDEO_PATH" ]; then
  "$ROOT_DIR/benchmark/scripts/generate-video.sh"
fi

"$ROOT_DIR/benchmark/scripts/init-db.sh" >/dev/null
write_metadata

if [ "$AUTO_STACK" = "true" ]; then
  if [ ! -f "$APP_DIR/.env" ]; then
    cp "$APP_DIR/.env.example" "$APP_DIR/.env"
  fi
  export APP_PORT POSTGRES_PORT PROMETHEUS_PORT CADVISOR_PORT GRAFANA_PORT
  export APP_CPUS APP_MEMORY RENDITION_POOL_SIZE
  (cd "$APP_DIR" && docker compose down -v --remove-orphans)
  docker build -t media-base:21 "$ROOT_DIR/backend/media-base"
  (cd "$APP_DIR" && docker compose up -d --build)
  export BENCHMARK_BASE_URL="$BASE_URL"
  "$ROOT_DIR/benchmark/scripts/wait-single.sh"
fi

RESOLUTIONS_SQL="$(resolution_sql_array)"

run_duckdb "$DB_PATH" <<SQL
insert into benchmark_runs (
  run_id,
  variant,
  topology,
  resource_profile,
  app_cpus,
  app_memory,
  rendition_pool_size,
  scenario,
  base_url,
  video_file,
  requested_resolutions,
  status,
  report_path,
  notes
)
values (
  '$RUN_ID',
  '$VARIANT_SQL'::architecture_variant,
  '$TOPOLOGY'::benchmark_topology,
  '$RESOURCE_PROFILE',
  '$APP_CPUS',
  '$APP_MEMORY',
  $RENDITION_POOL_SIZE,
  '$SCENARIO',
  '$BASE_URL',
  '$VIDEO_PATH',
  $RESOLUTIONS_SQL,
  'running'::benchmark_status,
  '$REPORT_PATH',
  'Phase 5 benchmark runner'
);
SQL

export_prometheus_snapshot "before"

set +e
(cd "$ROOT_DIR/benchmark/gatling" && ./mvnw gatling:test \
  -Dgatling.simulationClass="benchmark.$SCENARIO" \
  -Dgatling.resultsFolder="$RUN_DIR/gatling" \
  -DbaseUrl="$BASE_URL" \
  -DvideoFile="$VIDEO_PATH" \
  -Drenditions="$RESOLUTIONS" \
  -DloadUsers="$LOAD_USERS" \
  -DrampSeconds="$RAMP_SECONDS" \
  -DpollAttempts="$POLL_ATTEMPTS" \
  -DpollPauseMillis="$POLL_PAUSE_MILLIS" \
  -DdownloadRendition="$DOWNLOAD_RENDITION")
EXIT_CODE=$?
set -e

export_prometheus_snapshot "after"

STATUS="passed"
if [ "$EXIT_CODE" -ne 0 ]; then
  STATUS="failed"
fi

run_duckdb "$DB_PATH" <<SQL
update benchmark_runs
set status = '$STATUS'::benchmark_status,
    gatling_exit_code = $EXIT_CODE,
    finished_at = current_timestamp
where run_id = '$RUN_ID';
SQL

printf 'Benchmark run %s finished with status=%s exit_code=%s\n' "$RUN_ID" "$STATUS" "$EXIT_CODE"
printf 'Run artifacts: %s\n' "$RUN_DIR"
exit "$EXIT_CODE"

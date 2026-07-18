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
WARM_UP_SECONDS="${BENCHMARK_WARM_UP_SECONDS:-0}"
DRAIN_SECONDS="${BENCHMARK_DRAIN_SECONDS:-30}"
PROMETHEUS_STEP_SECONDS="${BENCHMARK_PROMETHEUS_STEP_SECONDS:-5}"
TARGET_WAIT_SECONDS="${BENCHMARK_TARGET_WAIT_SECONDS:-120}"
DOWNLOAD_RENDITION="${BENCHMARK_DOWNLOAD_RENDITION:-false}"
AUTO_STACK="${BENCHMARK_AUTO_STACK:-true}"
KEEP_STACK="${BENCHMARK_KEEP_STACK:-false}"
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
FIXTURE_MANIFEST="${BENCHMARK_FIXTURE_MANIFEST:-$ROOT_DIR/benchmark/fixtures/source-video.manifest.json}"
EXPECTED_FIXTURE_MANIFEST_SHA256="${BENCHMARK_FIXTURE_MANIFEST_SHA256:-}"
EXPECTED_FIXTURE_SHA256="${BENCHMARK_FIXTURE_SHA256:-}"
RESOLUTIONS="${BENCHMARK_RENDITIONS:-SD_360,HD_720}"
PROTOCOL_ID="${BENCHMARK_PROTOCOL_ID:-ad-hoc-pilot}"
PROTOCOL_SHA256="${BENCHMARK_PROTOCOL_SHA256:-}"
SCHEDULE_SHA256="${BENCHMARK_SCHEDULE_SHA256:-}"
SCHEDULE_IDENTITY="${BENCHMARK_SCHEDULE_IDENTITY:-}"
PLANNED_RUN_ID="${BENCHMARK_PLANNED_RUN_ID:-}"
REPETITION="${BENCHMARK_REPETITION:-}"
BLOCK_ID="${BENCHMARK_BLOCK_ID:-}"
BLOCK_POSITION="${BENCHMARK_BLOCK_POSITION:-}"
REPLACEMENT_FOR_RUN_ID="${BENCHMARK_REPLACEMENT_FOR_RUN_ID:-}"
RUN_ID="${BENCHMARK_RUN_ID:-$(date -u +%Y%m%dT%H%M%SZ)-$RESOURCE_PROFILE-$VARIANT}"
RUN_DIR="$ROOT_DIR/benchmark/results/runs/$RUN_ID"
REPORT_PATH="benchmark/results/runs/$RUN_ID/gatling"
METADATA_PATH="$RUN_DIR/metadata.json"
TIMESTAMPS_PATH="$RUN_DIR/timestamps.json"
GATLING_TIMING_PATH="$RUN_DIR/gatling-timestamps.json"
SMOKE_VALIDATION_PATH="$RUN_DIR/smoke-validation.json"
TARGET_READINESS_PATH="$RUN_DIR/prometheus-target-readiness.json"
RECONCILIATION_PATH="$RUN_DIR/drain-reconciliation.json"
PROMETHEUS_DIR="$RUN_DIR/prometheus"
METRIC_SUMMARY_PATH="$PROMETHEUS_DIR/metric-summary.json"
RUN_VALIDATION_PATH="$RUN_DIR/run-validation.json"
OUTCOME_PATH="$RUN_DIR/business-outcomes.jsonl"
PROMETHEUS_TARGET_FILE="$RUN_DIR/prometheus-targets.json"
STACK_STARTED=false
GATLING_PID=""
DB_ROW_INSERTED=false
RUN_FINALIZED=false
COMPOSE_ARGUMENTS=()

case "$VARIANT" in
  monolith)
    APP_DIR="$ROOT_DIR/backend/media-monolith"
    VARIANT_SQL="monolith"
    COMPOSE_PROJECT="${BENCHMARK_COMPOSE_PROJECT:-media-monolith}"
    SUT_SERVICES_CSV="app"
    INFRASTRUCTURE_SERVICES_CSV="postgres"
    PROMETHEUS_TARGETS_JSON='[{"targets":["app:8080"],"labels":{"variant":"monolith","service":"media-monolith","target_id":"monolith-media-monolith","metrics_path":"/api/actuator/prometheus"}}]'
    ;;
  modular_monolith)
    APP_DIR="$ROOT_DIR/backend/media-modular"
    VARIANT_SQL="modular_monolith"
    COMPOSE_PROJECT="${BENCHMARK_COMPOSE_PROJECT:-media-modular}"
    SUT_SERVICES_CSV="app"
    INFRASTRUCTURE_SERVICES_CSV="postgres"
    PROMETHEUS_TARGETS_JSON='[{"targets":["app:8080"],"labels":{"variant":"modular_monolith","service":"media-modular","target_id":"modular-monolith-media-modular","metrics_path":"/api/actuator/prometheus"}}]'
    ;;
  microservices)
    APP_DIR="$ROOT_DIR/backend/media-microservices"
    VARIANT_SQL="microservices"
    COMPOSE_PROJECT="${BENCHMARK_COMPOSE_PROJECT:-media-microservices}"
    SUT_SERVICES_CSV="gateway,media-service,transcoder-service,notification-service"
    INFRASTRUCTURE_SERVICES_CSV="postgres,rabbitmq"
    PROMETHEUS_TARGETS_JSON='[{"targets":["media-service:8080"],"labels":{"variant":"microservices","service":"media-service","target_id":"microservices-media-service","metrics_path":"/api/actuator/prometheus"}},{"targets":["transcoder-service:8080"],"labels":{"variant":"microservices","service":"transcoder-service","target_id":"microservices-transcoder-service","metrics_path":"/actuator/prometheus"}},{"targets":["notification-service:8080"],"labels":{"variant":"microservices","service":"notification-service","target_id":"microservices-notification-service","metrics_path":"/api/actuator/prometheus"}}]'
    ;;
  *)
    printf 'Unsupported BENCHMARK_VARIANT=%s.\n' "$VARIANT" >&2
    exit 1
    ;;
esac
PROMETHEUS_TARGETS_JSON="${BENCHMARK_PROMETHEUS_TARGETS_JSON:-$PROMETHEUS_TARGETS_JSON}"

if [ "$TOPOLOGY" != "single" ]; then
  printf 'Unsupported BENCHMARK_TOPOLOGY=%s.\n' "$TOPOLOGY" >&2
  exit 1
fi

apply_resource_profile() {
  case "$RESOURCE_PROFILE" in
    baseline) APP_CPUS="${APP_CPUS:-2.0}"; APP_MEMORY="${APP_MEMORY:-2g}"; RENDITION_POOL_SIZE="${RENDITION_POOL_SIZE:-8}" ;;
    vertical-1) APP_CPUS="${APP_CPUS:-1.0}"; APP_MEMORY="${APP_MEMORY:-1g}"; RENDITION_POOL_SIZE="${RENDITION_POOL_SIZE:-1}" ;;
    vertical-2) APP_CPUS="${APP_CPUS:-2.0}"; APP_MEMORY="${APP_MEMORY:-2g}"; RENDITION_POOL_SIZE="${RENDITION_POOL_SIZE:-2}" ;;
    vertical-4) APP_CPUS="${APP_CPUS:-4.0}"; APP_MEMORY="${APP_MEMORY:-4g}"; RENDITION_POOL_SIZE="${RENDITION_POOL_SIZE:-4}" ;;
    vertical-8) APP_CPUS="${APP_CPUS:-8.0}"; APP_MEMORY="${APP_MEMORY:-8g}"; RENDITION_POOL_SIZE="${RENDITION_POOL_SIZE:-8}" ;;
    custom)
      if [ -z "$APP_CPUS" ] || [ -z "$APP_MEMORY" ] || [ -z "$RENDITION_POOL_SIZE" ]; then
        printf 'custom profile requires BENCHMARK_APP_CPUS, BENCHMARK_APP_MEMORY, and BENCHMARK_RENDITION_POOL_SIZE.\n' >&2
        exit 1
      fi
      ;;
    *) printf 'Unsupported BENCHMARK_RESOURCE_PROFILE=%s.\n' "$RESOURCE_PROFILE" >&2; exit 1 ;;
  esac
}

set_microservice_allocations() {
  if [ "$RESOURCE_PROFILE" = "custom" ]; then
    : "${GATEWAY_CPUS:?custom microservices profile requires GATEWAY_CPUS}"
    : "${GATEWAY_MEMORY:?custom microservices profile requires GATEWAY_MEMORY}"
    : "${MEDIA_SERVICE_CPUS:?custom microservices profile requires MEDIA_SERVICE_CPUS}"
    : "${MEDIA_SERVICE_MEMORY:?custom microservices profile requires MEDIA_SERVICE_MEMORY}"
    : "${TRANSCODER_SERVICE_CPUS:?custom microservices profile requires TRANSCODER_SERVICE_CPUS}"
    : "${TRANSCODER_SERVICE_MEMORY:?custom microservices profile requires TRANSCODER_SERVICE_MEMORY}"
    : "${NOTIFICATION_SERVICE_CPUS:?custom microservices profile requires NOTIFICATION_SERVICE_CPUS}"
    : "${NOTIFICATION_SERVICE_MEMORY:?custom microservices profile requires NOTIFICATION_SERVICE_MEMORY}"
  else
    case "$APP_CPUS:$APP_MEMORY" in
      1.0:1g) GATEWAY_CPUS=.125 GATEWAY_MEMORY=128m; MEDIA_SERVICE_CPUS=.25 MEDIA_SERVICE_MEMORY=256m; TRANSCODER_SERVICE_CPUS=.5 TRANSCODER_SERVICE_MEMORY=512m; NOTIFICATION_SERVICE_CPUS=.125 NOTIFICATION_SERVICE_MEMORY=128m ;;
      2.0:2g) GATEWAY_CPUS=.25 GATEWAY_MEMORY=256m; MEDIA_SERVICE_CPUS=.5 MEDIA_SERVICE_MEMORY=512m; TRANSCODER_SERVICE_CPUS=1.0 TRANSCODER_SERVICE_MEMORY=1g; NOTIFICATION_SERVICE_CPUS=.25 NOTIFICATION_SERVICE_MEMORY=256m ;;
      4.0:4g) GATEWAY_CPUS=.5 GATEWAY_MEMORY=512m; MEDIA_SERVICE_CPUS=1.0 MEDIA_SERVICE_MEMORY=1g; TRANSCODER_SERVICE_CPUS=2.0 TRANSCODER_SERVICE_MEMORY=2g; NOTIFICATION_SERVICE_CPUS=.5 NOTIFICATION_SERVICE_MEMORY=512m ;;
      8.0:8g) GATEWAY_CPUS=1.0 GATEWAY_MEMORY=1g; MEDIA_SERVICE_CPUS=2.0 MEDIA_SERVICE_MEMORY=2g; TRANSCODER_SERVICE_CPUS=4.0 TRANSCODER_SERVICE_MEMORY=4g; NOTIFICATION_SERVICE_CPUS=1.0 NOTIFICATION_SERVICE_MEMORY=1g ;;
      *) printf 'Microservices allocations require a standard total or custom profile.\n' >&2; exit 1 ;;
    esac
  fi
  RESOURCE_ALLOCATIONS_JSON="[{\"service\":\"gateway\",\"cpus\":\"$GATEWAY_CPUS\",\"memory\":\"$GATEWAY_MEMORY\"},{\"service\":\"media-service\",\"cpus\":\"$MEDIA_SERVICE_CPUS\",\"memory\":\"$MEDIA_SERVICE_MEMORY\"},{\"service\":\"transcoder-service\",\"cpus\":\"$TRANSCODER_SERVICE_CPUS\",\"memory\":\"$TRANSCODER_SERVICE_MEMORY\"},{\"service\":\"notification-service\",\"cpus\":\"$NOTIFICATION_SERVICE_CPUS\",\"memory\":\"$NOTIFICATION_SERVICE_MEMORY\"}]"
  export GATEWAY_CPUS GATEWAY_MEMORY MEDIA_SERVICE_CPUS MEDIA_SERVICE_MEMORY
  export TRANSCODER_SERVICE_CPUS TRANSCODER_SERVICE_MEMORY NOTIFICATION_SERVICE_CPUS NOTIFICATION_SERVICE_MEMORY
}

apply_resource_profile
if [ "$VARIANT" = "microservices" ]; then
  set_microservice_allocations
else
  RESOURCE_ALLOCATIONS_JSON="[{\"service\":\"app\",\"cpus\":\"$APP_CPUS\",\"memory\":\"$APP_MEMORY\"}]"
fi
RESOURCE_ALLOCATIONS_JSON="$(python3 "$ROOT_DIR/benchmark/scripts/validate-resource-allocations.py" "$APP_CPUS" "$APP_MEMORY" "$RESOURCE_ALLOCATIONS_JSON")"

run_duckdb() {
  local db_path="$1"
  if command -v duckdb >/dev/null 2>&1; then
    duckdb "$db_path"
  elif [[ "$db_path" == "$ROOT_DIR"/* ]]; then
    docker run --rm -i -v "$ROOT_DIR:/work:z" -w /work duckdb/duckdb:latest duckdb "${db_path#"$ROOT_DIR/"}"
  else
    printf 'duckdb is unavailable and BENCHMARK_DB_PATH is outside the repository.\n' >&2
    return 1
  fi
}

sql_array() {
  local csv="$1" type="$2" values=""
  IFS=',' read -ra items <<< "$csv"
  for item in "${items[@]}"; do
    item="$(printf '%s' "$item" | xargs)"
    [ -n "$item" ] && values="$values'$item'::$type,"
  done
  printf '[%s]' "${values%,}"
}

cleanup_current_stack() {
  if [ "$AUTO_STACK" != "true" ] || [ "$STACK_STARTED" != "true" ] || [ "$KEEP_STACK" = "true" ]; then
    return
  fi
  set +e
  (cd "$APP_DIR" && docker compose -p "$COMPOSE_PROJECT" "${COMPOSE_ARGUMENTS[@]}" down -v --remove-orphans)
  local cleanup_exit=$?
  set -e
  if [ "$cleanup_exit" -ne 0 ]; then
    printf 'Warning: failed to clean benchmark stack project=%s\n' "$COMPOSE_PROJECT" >&2
  fi
}

on_exit() {
  local exit_code=$?
  trap - EXIT
  if [ -n "$GATLING_PID" ] && kill -0 "$GATLING_PID" 2>/dev/null; then
    kill -TERM "$GATLING_PID" 2>/dev/null || true
    wait "$GATLING_PID" 2>/dev/null || true
  fi
  if [ "$exit_code" -ne 0 ] && [ "$DB_ROW_INSERTED" = "true" ] && [ "$RUN_FINALIZED" != "true" ]; then
    set +e
    run_duckdb "$DB_PATH" <<SQL
update benchmark_runs
set status = 'failed'::benchmark_status,
    technical_valid = false,
    technical_reason = 'runner_aborted_exit_$exit_code',
    finished_at = current_timestamp
where run_id = '$RUN_ID';
SQL
    set -e
  fi
  cleanup_current_stack
  exit "$exit_code"
}
trap on_exit EXIT

export_prometheus_range() {
  local file_name="$1"
  local query="$2"
  local output_path="$PROMETHEUS_DIR/$file_name.json"
  python3 -c 'import json,sys; print(json.dumps({"name":sys.argv[1],"query":sys.argv[2],"start":sys.argv[3],"end":sys.argv[4],"stepSeconds":int(sys.argv[5])}, separators=(",", ":")))' \
    "$file_name" "$query" "$MEASUREMENT_STARTED_AT" "$DRAIN_ENDED_AT" "$PROMETHEUS_STEP_SECONDS" >> "$PROMETHEUS_DIR/query-manifest.jsonl"
  if ! curl -fsS --get --data-urlencode "query=$query" --data-urlencode "start=$MEASUREMENT_STARTED_AT" \
    --data-urlencode "end=$DRAIN_ENDED_AT" --data-urlencode "step=$PROMETHEUS_STEP_SECONDS" \
    "$PROMETHEUS_URL/api/v1/query_range" > "$output_path"; then
    printf '{"status":"unavailable"}\n' > "$output_path"
  fi
}

export_prometheus_series() {
  local sut_regex="${SUT_SERVICES_CSV//,/|}" infrastructure_regex="${INFRASTRUCTURE_SERVICES_CSV//,/|}"
  local project_label="container_label_com_docker_compose_project" service_label="container_label_com_docker_compose_service"
  local project_matcher="$project_label=\"$COMPOSE_PROJECT\""
  : > "$PROMETHEUS_DIR/query-manifest.jsonl"
  export_prometheus_range "app-up" 'up{job="app"}'
  export_prometheus_range "business-http-count" 'http_server_requests_seconds_count{job="app"}'
  export_prometheus_range "business-http-sum" 'http_server_requests_seconds_sum{job="app"}'
  export_prometheus_range "business-pipeline-count" 'rendition_pipeline_duration_seconds_count{job="app"}'
  export_prometheus_range "business-pipeline-sum" 'rendition_pipeline_duration_seconds_sum{job="app"}'
  export_prometheus_range "business-pipeline-bucket" 'rendition_pipeline_duration_seconds_bucket{job="app"}'
  export_prometheus_range "business-queue-size" 'rendition_queue_size{job="app"}'
  export_prometheus_range "business-active-jobs" 'rendition_active_jobs{job="app"}'
  export_prometheus_range "sut-container-cpu" "sum by ($project_label,$service_label) (rate(container_cpu_usage_seconds_total{$project_matcher,$service_label=~\"$sut_regex\"}[30s]))"
  export_prometheus_range "sut-container-working-memory" "sum by ($project_label,$service_label) (container_memory_working_set_bytes{$project_matcher,$service_label=~\"$sut_regex\"})"
  export_prometheus_range "sut-container-network-receive" "sum by ($project_label,$service_label) (rate(container_network_receive_bytes_total{$project_matcher,$service_label=~\"$sut_regex\"}[30s]))"
  export_prometheus_range "sut-container-network-transmit" "sum by ($project_label,$service_label) (rate(container_network_transmit_bytes_total{$project_matcher,$service_label=~\"$sut_regex\"}[30s]))"
  export_prometheus_range "infrastructure-container-cpu" "sum by ($project_label,$service_label) (rate(container_cpu_usage_seconds_total{$project_matcher,$service_label=~\"$infrastructure_regex\"}[30s]))"
  export_prometheus_range "infrastructure-container-working-memory" "sum by ($project_label,$service_label) (container_memory_working_set_bytes{$project_matcher,$service_label=~\"$infrastructure_regex\"})"
  export_prometheus_range "infrastructure-container-network-receive" "sum by ($project_label,$service_label) (rate(container_network_receive_bytes_total{$project_matcher,$service_label=~\"$infrastructure_regex\"}[30s]))"
  export_prometheus_range "infrastructure-container-network-transmit" "sum by ($project_label,$service_label) (rate(container_network_transmit_bytes_total{$project_matcher,$service_label=~\"$infrastructure_regex\"}[30s]))"
}

mkdir -p "$RUN_DIR" "$PROMETHEUS_DIR"
printf '%s\n' "$PROMETHEUS_TARGETS_JSON" > "$PROMETHEUS_TARGET_FILE"
PROMETHEUS_TARGET_COUNT="$(python3 -c 'import json,sys; print(sum(len(group.get("targets", [])) for group in json.load(open(sys.argv[1]))))' "$PROMETHEUS_TARGET_FILE")"

ACTUAL_FIXTURE_MANIFEST_SHA256="$(sha256sum "$FIXTURE_MANIFEST" | cut -d' ' -f1)"
if [ -n "$EXPECTED_FIXTURE_MANIFEST_SHA256" ] && [ "$ACTUAL_FIXTURE_MANIFEST_SHA256" != "$EXPECTED_FIXTURE_MANIFEST_SHA256" ]; then
  printf 'Fixture manifest SHA-256 differs from the protocol binding.\n' >&2
  exit 1
fi
BENCHMARK_FIXTURE_MANIFEST="$FIXTURE_MANIFEST" python3 "$ROOT_DIR/benchmark/scripts/fixture.py" verify
VIDEO_PATH="$(python3 -c 'import json,sys; from pathlib import Path; d=json.load(open(sys.argv[1])); print((Path(sys.argv[2]) / d["file"]).resolve())' "$FIXTURE_MANIFEST" "$ROOT_DIR")"
FIXTURE_SHA256="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["sha256"])' "$FIXTURE_MANIFEST")"
if [ -n "$EXPECTED_FIXTURE_SHA256" ] && [ "$FIXTURE_SHA256" != "$EXPECTED_FIXTURE_SHA256" ]; then
  printf 'Fixture SHA-256 differs from the protocol binding.\n' >&2
  exit 1
fi

"$ROOT_DIR/benchmark/scripts/init-db.sh" >/dev/null
cat > "$METADATA_PATH" <<JSON
{
  "schemaVersion": 3,
  "runId": "$RUN_ID",
  "plannedRunId": "$PLANNED_RUN_ID",
  "repetition": ${REPETITION:-null},
  "blockId": "$BLOCK_ID",
  "blockPosition": ${BLOCK_POSITION:-null},
  "scheduleIdentity": "$SCHEDULE_IDENTITY",
  "replacementForRunId": "$REPLACEMENT_FOR_RUN_ID",
  "protocolId": "$PROTOCOL_ID",
  "protocolSha256": "$PROTOCOL_SHA256",
  "scheduleSha256": "$SCHEDULE_SHA256",
  "variant": "$VARIANT",
  "topology": "$TOPOLOGY",
  "resourceProfile": "$RESOURCE_PROFILE",
  "resourceTotal": {"cpus": "$APP_CPUS", "memory": "$APP_MEMORY"},
  "resourceAllocations": $RESOURCE_ALLOCATIONS_JSON,
  "sutServices": "$SUT_SERVICES_CSV",
  "infrastructureServices": "$INFRASTRUCTURE_SERVICES_CSV",
  "composeProject": "$COMPOSE_PROJECT",
  "scenario": "$SCENARIO",
  "baseUrl": "$BASE_URL",
  "prometheusUrl": "$PROMETHEUS_URL",
  "prometheusStepSeconds": $PROMETHEUS_STEP_SECONDS,
  "fixtureManifest": "${FIXTURE_MANIFEST#"$ROOT_DIR"/}",
  "fixtureManifestSha256": "$ACTUAL_FIXTURE_MANIFEST_SHA256",
  "fixtureSha256": "$FIXTURE_SHA256",
  "videoFile": "$VIDEO_PATH",
  "renditions": "$RESOLUTIONS",
  "loadUsers": $LOAD_USERS,
  "rampSeconds": $RAMP_SECONDS,
  "pollAttempts": $POLL_ATTEMPTS,
  "pollPauseMillis": $POLL_PAUSE_MILLIS,
  "warmUpSeconds": $WARM_UP_SECONDS,
  "drainSeconds": $DRAIN_SECONDS,
  "renditionPoolSize": $RENDITION_POOL_SIZE,
  "downloadRendition": $DOWNLOAD_RENDITION,
  "autoStack": $AUTO_STACK,
  "keepStack": $KEEP_STACK,
  "appDir": "${APP_DIR#"$ROOT_DIR"/}",
  "startedAtUtc": "$(date -u +%Y-%m-%dT%H:%M:%S.%NZ)"
}
JSON

RESOLUTIONS_SQL="$(sql_array "$RESOLUTIONS" video_resolution)"
SUT_SERVICES_SQL="$(sql_array "$SUT_SERVICES_CSV" varchar)"
INFRASTRUCTURE_SERVICES_SQL="$(sql_array "$INFRASTRUCTURE_SERVICES_CSV" varchar)"
run_duckdb "$DB_PATH" <<SQL
insert into benchmark_runs (
  run_id, planned_run_id, repetition, block_id, block_position, schedule_identity,
  replacement_for_run_id, variant, topology, resource_profile, app_cpus, app_memory,
  rendition_pool_size, scenario, base_url, video_file, fixture_sha256,
  fixture_manifest_sha256, requested_resolutions, status, report_path, outcome_path,
  protocol_id, protocol_sha256, schedule_sha256, compose_project, sut_services,
  infrastructure_services, resource_allocations_json, notes
)
values (
  '$RUN_ID', nullif('$PLANNED_RUN_ID',''), ${REPETITION:-NULL}, nullif('$BLOCK_ID',''),
  ${BLOCK_POSITION:-NULL}, nullif('$SCHEDULE_IDENTITY',''), nullif('$REPLACEMENT_FOR_RUN_ID',''),
  '$VARIANT_SQL'::architecture_variant, '$TOPOLOGY'::benchmark_topology, '$RESOURCE_PROFILE',
  '$APP_CPUS', '$APP_MEMORY', $RENDITION_POOL_SIZE, '$SCENARIO', '$BASE_URL', '$VIDEO_PATH',
  '$FIXTURE_SHA256', '$ACTUAL_FIXTURE_MANIFEST_SHA256', $RESOLUTIONS_SQL,
  'running'::benchmark_status, '$REPORT_PATH', '${OUTCOME_PATH#"$ROOT_DIR"/}', '$PROTOCOL_ID',
  '$PROTOCOL_SHA256', '$SCHEDULE_SHA256', '$COMPOSE_PROJECT', $SUT_SERVICES_SQL,
  $INFRASTRUCTURE_SERVICES_SQL, '$RESOURCE_ALLOCATIONS_JSON'::json,
  'Three-variant non-official pilot benchmark runner'
);
SQL
DB_ROW_INSERTED=true

export APP_PORT POSTGRES_PORT PROMETHEUS_PORT CADVISOR_PORT GRAFANA_PORT APP_CPUS APP_MEMORY RENDITION_POOL_SIZE
export GATEWAY_PORT="$APP_PORT" COMPOSE_PROJECT_NAME="$COMPOSE_PROJECT"
export BENCHMARK_PROMETHEUS_TARGET_FILE="$PROMETHEUS_TARGET_FILE"
export BENCHMARK_PROMETHEUS_CONFIG_FILE="$ROOT_DIR/benchmark/monitoring/prometheus/prometheus.yml"
export BENCHMARK_GRAFANA_PROVISIONING_DIR="$ROOT_DIR/benchmark/monitoring/grafana/provisioning"
export BENCHMARK_GRAFANA_DASHBOARDS_DIR="$ROOT_DIR/benchmark/monitoring/grafana/dashboards"
if [ "$VARIANT" = "microservices" ]; then
  COMPOSE_ARGUMENTS=(-f "$APP_DIR/docker-compose.yml" -f "$ROOT_DIR/benchmark/monitoring/compose.monitoring.yml")
fi

READINESS_EXIT=0
GATLING_EXIT_CODE=125
RECONCILIATION_EXIT=0
if [ "$AUTO_STACK" = "true" ]; then
  [ -d "$APP_DIR" ] || { printf 'Variant directory is missing: %s\n' "$APP_DIR" >&2; exit 1; }
  [ -f "$APP_DIR/.env" ] || cp "$APP_DIR/.env.example" "$APP_DIR/.env"
  "$ROOT_DIR/benchmark/scripts/cleanup-benchmark-stacks.sh" all
  STACK_STARTED=true
  docker build -t media-base:21 "$ROOT_DIR/backend/media-base"
  (cd "$APP_DIR" && docker compose -p "$COMPOSE_PROJECT" "${COMPOSE_ARGUMENTS[@]}" up -d --build)
  export BENCHMARK_BASE_URL="$BASE_URL"
  "$ROOT_DIR/benchmark/scripts/wait-single.sh"
fi

set +e
python3 "$ROOT_DIR/benchmark/scripts/wait-prometheus-targets.py" \
  --url "$PROMETHEUS_URL" --target-file "$PROMETHEUS_TARGET_FILE" \
  --output "$TARGET_READINESS_PATH" --timeout-seconds "$TARGET_WAIT_SECONDS"
READINESS_EXIT=$?
set -e

MEASUREMENT_STARTED_AT=""
MEASUREMENT_ENDED_AT=""
DRAIN_STARTED_AT="$(date -u +%Y-%m-%dT%H:%M:%S.%NZ)"
DRAIN_ENDED_AT="$DRAIN_STARTED_AT"
if [ "$READINESS_EXIT" -eq 0 ]; then
  [ "$WARM_UP_SECONDS" -le 0 ] || sleep "$WARM_UP_SECONDS"
  (cd "$ROOT_DIR/benchmark/gatling" && ./mvnw gatling:test \
    -Dgatling.simulationClass="benchmark.$SCENARIO" \
    -Dgatling.resultsFolder="$RUN_DIR/gatling" \
    -DbaseUrl="$BASE_URL" -DvideoFile="$VIDEO_PATH" -Drenditions="$RESOLUTIONS" \
    -DloadUsers="$LOAD_USERS" -DrampSeconds="$RAMP_SECONDS" \
    -DpollAttempts="$POLL_ATTEMPTS" -DpollPauseMillis="$POLL_PAUSE_MILLIS" \
    -DdownloadRendition="$DOWNLOAD_RENDITION" -DrunId="$RUN_ID" \
    -DoutcomeFile="$OUTCOME_PATH" -DtimingFile="$GATLING_TIMING_PATH" \
    -DsmokeValidationFile="$SMOKE_VALIDATION_PATH") &
  GATLING_PID=$!
  while [ ! -f "$GATLING_TIMING_PATH" ] && kill -0 "$GATLING_PID" 2>/dev/null; do
    sleep 0.1
  done
  if [ -f "$GATLING_TIMING_PATH" ]; then
    MEASUREMENT_STARTED_AT="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["scenarioInjectionStartedAtUtc"])' "$GATLING_TIMING_PATH")"
    MEASUREMENT_ENDED_AT="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["scenarioEndedAtUtc"])' "$GATLING_TIMING_PATH")"
    DRAIN_STARTED_AT="$MEASUREMENT_ENDED_AT"
    DRAIN_SECONDS_REMAINING="$(python3 -c 'import sys; from datetime import datetime,timezone; ended=datetime.fromisoformat(sys.argv[1].replace("Z","+00:00")); print(max(float(sys.argv[2])-(datetime.now(timezone.utc)-ended).total_seconds(),0))' "$MEASUREMENT_ENDED_AT" "$DRAIN_SECONDS")"
    if [ "$SCENARIO" = "LoadStressSimulation" ] && [ -f "$OUTCOME_PATH" ]; then
      set +e
      python3 "$ROOT_DIR/benchmark/scripts/reconcile-business-outcomes.py" \
        --outcomes "$OUTCOME_PATH" --base-url "$BASE_URL" --drain-seconds "$DRAIN_SECONDS_REMAINING" \
        --configured-drain-seconds "$DRAIN_SECONDS" \
        --poll-millis "$POLL_PAUSE_MILLIS" --output "$RECONCILIATION_PATH"
      RECONCILIATION_EXIT=$?
      set -e
    else
      python3 -c 'import sys,time; time.sleep(float(sys.argv[1]))' "$DRAIN_SECONDS_REMAINING"
    fi
    DRAIN_ENDED_AT="$(date -u +%Y-%m-%dT%H:%M:%S.%NZ)"
  fi
  set +e
  wait "$GATLING_PID"
  GATLING_EXIT_CODE=$?
  set -e
  GATLING_PID=""
fi

MEASUREMENT_STARTED_JSON=null
MEASUREMENT_ENDED_JSON=null
if [ -n "$MEASUREMENT_STARTED_AT" ]; then MEASUREMENT_STARTED_JSON="\"$MEASUREMENT_STARTED_AT\""; fi
if [ -n "$MEASUREMENT_ENDED_AT" ]; then MEASUREMENT_ENDED_JSON="\"$MEASUREMENT_ENDED_AT\""; fi

cat > "$TIMESTAMPS_PATH" <<JSON
{
  "scenarioInjectionStartedAtUtc": $MEASUREMENT_STARTED_JSON,
  "scenarioEndedAtUtc": $MEASUREMENT_ENDED_JSON,
  "measurementStartedAtUtc": $MEASUREMENT_STARTED_JSON,
  "measurementEndedAtUtc": $MEASUREMENT_ENDED_JSON,
  "drainStartedAtUtc": "$DRAIN_STARTED_AT",
  "drainEndedAtUtc": "$DRAIN_ENDED_AT",
  "prometheusRangeStartedAtUtc": $MEASUREMENT_STARTED_JSON,
  "prometheusRangeEndedAtUtc": "$DRAIN_ENDED_AT",
  "prometheusStepSeconds": $PROMETHEUS_STEP_SECONDS
}
JSON
python3 -c 'import json,sys; p=sys.argv[1]; d=json.load(open(p)); open(p,"w").write(json.dumps(d,indent=2)+"\n")' "$TIMESTAMPS_PATH"

METRICS_EXIT=1
if [ -n "$MEASUREMENT_STARTED_AT" ] && [ -n "$MEASUREMENT_ENDED_AT" ]; then
  export_prometheus_series
  set +e
  python3 "$ROOT_DIR/benchmark/scripts/summarize-prometheus.py" \
    --prometheus-dir "$PROMETHEUS_DIR" --measurement-start "$MEASUREMENT_STARTED_AT" \
    --measurement-end "$MEASUREMENT_ENDED_AT" --step-seconds "$PROMETHEUS_STEP_SECONDS" \
    --sut-services "$SUT_SERVICES_CSV" --expected-target-count "$PROMETHEUS_TARGET_COUNT" \
    --output "$METRIC_SUMMARY_PATH"
  METRICS_EXIT=$?
  set -e
else
  printf '{"schemaVersion":1,"requiredMetricsAvailable":false,"reason":"measurement timing unavailable"}\n' > "$METRIC_SUMMARY_PATH"
fi

EXTRA_VALIDATION_ARGUMENTS=()
[ "$RECONCILIATION_EXIT" -eq 0 ] || EXTRA_VALIDATION_ARGUMENTS+=(--extra-reason "drain_reconciliation_failed")
[ "$METRICS_EXIT" -eq 0 ] || EXTRA_VALIDATION_ARGUMENTS+=(--extra-reason "metric_summary_incomplete")
set +e
python3 "$ROOT_DIR/benchmark/scripts/validate-run.py" \
  --run-id "$RUN_ID" --scenario "$SCENARIO" --gatling-exit-code "$GATLING_EXIT_CODE" \
  --timing "$GATLING_TIMING_PATH" --readiness "$TARGET_READINESS_PATH" \
  --metrics "$METRIC_SUMMARY_PATH" --outcomes "$OUTCOME_PATH" --load-users "$LOAD_USERS" \
  --resolutions "$RESOLUTIONS" --smoke-validation "$SMOKE_VALIDATION_PATH" \
  "${EXTRA_VALIDATION_ARGUMENTS[@]}" --output "$RUN_VALIDATION_PATH"
VALIDATION_EXIT=$?
set -e

python3 "$ROOT_DIR/benchmark/scripts/finalize-run-metadata.py" \
  --metadata "$METADATA_PATH" --timestamps "$TIMESTAMPS_PATH" \
  --validation "$RUN_VALIDATION_PATH" --gatling-exit-code "$GATLING_EXIT_CODE"
TECHNICAL_VALID="$(python3 -c 'import json,sys; print(str(json.load(open(sys.argv[1]))["technicalValid"]).lower())' "$RUN_VALIDATION_PATH")"
TECHNICAL_REASON="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["technicalReason"])' "$RUN_VALIDATION_PATH")"
TECHNICAL_REASON_SQL="$(python3 -c 'import sys; print(sys.argv[1].replace("'"'"'", "'"'"''"'"'"))' "$TECHNICAL_REASON")"
METRIC_AVAILABILITY_JSON="$(python3 -c 'import json,sys; print(json.dumps(json.load(open(sys.argv[1]))["metricAvailability"],separators=(",",":")))' "$RUN_VALIDATION_PATH")"

STATUS="passed"
[ "$TECHNICAL_VALID" = "true" ] || STATUS="failed"
MEASUREMENT_START_SQL="NULL"; MEASUREMENT_END_SQL="NULL"
[ -z "$MEASUREMENT_STARTED_AT" ] || MEASUREMENT_START_SQL="'$MEASUREMENT_STARTED_AT'::timestamp"
[ -z "$MEASUREMENT_ENDED_AT" ] || MEASUREMENT_END_SQL="'$MEASUREMENT_ENDED_AT'::timestamp"
run_duckdb "$DB_PATH" <<SQL
update benchmark_runs
set status = '$STATUS'::benchmark_status,
    gatling_exit_code = $GATLING_EXIT_CODE,
    technical_valid = $TECHNICAL_VALID,
    technical_reason = '$TECHNICAL_REASON_SQL',
    metric_availability_json = '$METRIC_AVAILABILITY_JSON'::json,
    measurement_started_at = $MEASUREMENT_START_SQL,
    measurement_ended_at = $MEASUREMENT_END_SQL,
    drain_started_at = '$DRAIN_STARTED_AT'::timestamp,
    drain_ended_at = '$DRAIN_ENDED_AT'::timestamp,
    finished_at = current_timestamp
where run_id = '$RUN_ID';
SQL
RUN_FINALIZED=true

printf 'Benchmark run %s finished status=%s technical_valid=%s gatling_exit=%s\n' \
  "$RUN_ID" "$STATUS" "$TECHNICAL_VALID" "$GATLING_EXIT_CODE"
printf 'Run artifacts: %s\n' "$RUN_DIR"
exit "$VALIDATION_EXIT"

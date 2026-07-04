#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DB_PATH="${BENCHMARK_DB_PATH:-$ROOT_DIR/benchmark/results/benchmark.duckdb}"
BASE_URL="${BENCHMARK_BASE_URL:-http://localhost:8080}"
VIDEO_PATH="${BENCHMARK_VIDEO_PATH:-$ROOT_DIR/benchmark/data/videos/smoke-720p-10s.mp4}"
RESOLUTIONS="${BENCHMARK_RENDITIONS:-SD_360}"
RUN_ID="${BENCHMARK_RUN_ID:-$(date -u +%Y%m%dT%H%M%SZ)-smoke}"
RUN_DIR="$ROOT_DIR/benchmark/results/runs/$RUN_ID"
RESOLUTIONS_SQL=""
IFS=',' read -ra RESOLUTION_VALUES <<< "$RESOLUTIONS"
for resolution in "${RESOLUTION_VALUES[@]}"; do
  resolution="$(printf '%s' "$resolution" | xargs)"
  if [ -n "$resolution" ]; then
    RESOLUTIONS_SQL="$RESOLUTIONS_SQL'$resolution'::video_resolution,"
  fi
done
RESOLUTIONS_SQL="[${RESOLUTIONS_SQL%,}]"

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

mkdir -p "$RUN_DIR"

if [ ! -f "$VIDEO_PATH" ]; then
  "$ROOT_DIR/benchmark/scripts/generate-video.sh"
fi

"$ROOT_DIR/benchmark/scripts/init-db.sh" >/dev/null

run_duckdb "$DB_PATH" <<SQL
insert into benchmark_runs (
  run_id,
  variant,
  topology,
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
  'monolith'::architecture_variant,
  'single'::benchmark_topology,
  'SmokeSimulation',
  '$BASE_URL',
  '$VIDEO_PATH',
  $RESOLUTIONS_SQL,
  'running'::benchmark_status,
  'benchmark/results/runs/$RUN_ID/gatling',
  'Phase 1 smoke harness'
);
SQL

set +e
(cd "$ROOT_DIR/benchmark/gatling" && ./mvnw gatling:test \
  -Dgatling.simulationClass=benchmark.SmokeSimulation \
  -Dgatling.resultsFolder="$RUN_DIR/gatling" \
  -DbaseUrl="$BASE_URL" \
  -DvideoFile="$VIDEO_PATH" \
  -Drenditions="$RESOLUTIONS")
EXIT_CODE=$?
set -e

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

printf 'Smoke run %s finished with status=%s exit_code=%s\n' "$RUN_ID" "$STATUS" "$EXIT_CODE"
exit "$EXIT_CODE"

#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PROTOCOL_PATH="${BENCHMARK_PROTOCOL_PATH:-$ROOT_DIR/benchmark/protocol/pilot-protocol.json}"
SCHEDULE_PATH="${BENCHMARK_SCHEDULE_PATH:-$ROOT_DIR/benchmark/protocol/pilot-schedule.json}"
VARIANT_FILTER=",${BENCHMARK_MATRIX_VARIANTS:-},"
PROFILE_FILTER=",${BENCHMARK_MATRIX_PROFILES:-},"

python3 "$ROOT_DIR/benchmark/scripts/materialize-schedule.py" verify "$PROTOCOL_PATH" "$SCHEDULE_PATH"

while IFS=$'\t' read -r key value; do
  export "$key=$value"
done < <(python3 "$ROOT_DIR/benchmark/scripts/materialize-schedule.py" settings "$PROTOCOL_PATH" "$SCHEDULE_PATH")

while IFS=$'\t' read -r ordinal planned_run_id variant profile repetition block_id position schedule_identity; do
  if [ "$VARIANT_FILTER" != ",," ] && [[ "$VARIANT_FILTER" != *",$variant,"* ]]; then
    continue
  fi
  if [ "$PROFILE_FILTER" != ",," ] && [[ "$PROFILE_FILTER" != *",$profile,"* ]]; then
    continue
  fi

  export BENCHMARK_VARIANT="$variant"
  export BENCHMARK_RESOURCE_PROFILE="$profile"
  export BENCHMARK_PLANNED_RUN_ID="$planned_run_id"
  export BENCHMARK_REPETITION="$repetition"
  export BENCHMARK_BLOCK_ID="$block_id"
  export BENCHMARK_BLOCK_POSITION="$position"
  export BENCHMARK_SCHEDULE_IDENTITY="$schedule_identity"
  export BENCHMARK_RUN_ID="$(date -u +%Y%m%dT%H%M%SZ)-$planned_run_id"

  printf 'Starting pilot row %s: block=%s position=%s variant=%s profile=%s repetition=%s\n' \
    "$ordinal" "$block_id" "$position" "$variant" "$profile" "$repetition"
  "$ROOT_DIR/benchmark/scripts/run-benchmark.sh"
done < <(python3 "$ROOT_DIR/benchmark/scripts/materialize-schedule.py" rows "$PROTOCOL_PATH" "$SCHEDULE_PATH")

printf 'Pilot matrix finished. Summarize with benchmark/scripts/summarize-results.py\n'

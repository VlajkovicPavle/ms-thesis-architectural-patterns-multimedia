#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SCENARIO="${BENCHMARK_SCENARIO:-LoadStressSimulation}"
REPETITIONS="${BENCHMARK_REPETITIONS:-3}"
VARIANTS_CSV="${BENCHMARK_MATRIX_VARIANTS:-monolith,modular_monolith}"
PROFILES_CSV="${BENCHMARK_MATRIX_PROFILES:-baseline,vertical-1,vertical-2,vertical-4}"

IFS=',' read -ra VARIANTS <<< "$VARIANTS_CSV"
IFS=',' read -ra PROFILES <<< "$PROFILES_CSV"

for variant in "${VARIANTS[@]}"; do
  variant="$(printf '%s' "$variant" | xargs)"
  [ -n "$variant" ] || continue

  for profile in "${PROFILES[@]}"; do
    profile="$(printf '%s' "$profile" | xargs)"
    [ -n "$profile" ] || continue

    for repetition in $(seq 1 "$REPETITIONS"); do
      export BENCHMARK_VARIANT="$variant"
      export BENCHMARK_RESOURCE_PROFILE="$profile"
      export BENCHMARK_SCENARIO="$SCENARIO"
      export BENCHMARK_RUN_ID="$(date -u +%Y%m%dT%H%M%SZ)-phase5-${profile}-${variant}-r${repetition}"

      printf 'Starting %s: variant=%s profile=%s repetition=%s/%s\n' \
        "$BENCHMARK_RUN_ID" "$variant" "$profile" "$repetition" "$REPETITIONS"
      "$ROOT_DIR/benchmark/scripts/run-benchmark.sh"
    done
  done
done

printf 'Experiment matrix finished. Summarize with benchmark/scripts/summarize-results.py\n'

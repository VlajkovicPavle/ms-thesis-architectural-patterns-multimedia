#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

export BENCHMARK_SCENARIO=SmokeSimulation
export BENCHMARK_LOAD_USERS=1
export BENCHMARK_RAMP_SECONDS=0
export BENCHMARK_AUTO_STACK="${BENCHMARK_AUTO_STACK:-false}"

exec "$ROOT_DIR/benchmark/scripts/run-benchmark.sh"

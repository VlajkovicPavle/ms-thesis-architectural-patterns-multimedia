# Benchmark Harness

Phase 1 provides a single-instance smoke harness for the media monolith. Phase 2 adds a baseline runner that records run metadata, Gatling reports, Prometheus snapshots, and DuckDB run rows under a stable result layout. Phase 3 adds repeatable vertical resource profiles. Phase 5 packages the thesis experiment matrix, a real load/stress scenario, and result summaries. Phase 4 horizontal scaling is deferred/optional because the single-instance and vertical profiles are enough for the near-term thesis comparison.

## Prerequisites

- Docker with Compose v2.
- `ffmpeg` for deterministic test video generation.
- `duckdb` for benchmark metadata storage.
- Java 21 and Maven wrapper dependencies for the Gatling module.

## Smoke Flow

From the repository root:

```sh
benchmark/scripts/init-db.sh
benchmark/scripts/generate-video.sh
benchmark/scripts/reset-single.sh
benchmark/scripts/start-single.sh
benchmark/scripts/wait-single.sh
benchmark/scripts/run-gatling-smoke.sh
```

The default benchmark target is `http://localhost:8080`. The Gatling scenario uses `/api/v1/...` paths so the same base URL can later point at a load balancer without changing scenarios.

## Baseline Flow

The Phase 2 runner starts a clean single-instance stack, waits for health, runs the Gatling scenario, snapshots Prometheus before and after the run, and updates DuckDB:

```sh
benchmark/scripts/run-benchmark.sh
```

Useful overrides:

- `BENCHMARK_VARIANT=monolith|modular_monolith` selects `backend/media-monolith` or `backend/media-modular`.
- `BENCHMARK_BASE_URL=http://localhost:8080` points Gatling and health checks at the app or load balancer.
- `BENCHMARK_PROMETHEUS_URL=http://localhost:9090` controls Prometheus export.
- `BENCHMARK_RENDITIONS=SD_360,HD_720` controls requested renditions.
- `BENCHMARK_RESOURCE_PROFILE=baseline|vertical-1|vertical-2|vertical-4|vertical-8|custom` controls app CPU/memory limits and `rendition.pool-size`.
- `BENCHMARK_SCENARIO=SmokeSimulation|LoadStressSimulation` selects the Gatling simulation.
- `BENCHMARK_LOAD_USERS=12`, `BENCHMARK_RAMP_SECONDS=60`, `BENCHMARK_POLL_ATTEMPTS=180`, and `BENCHMARK_POLL_PAUSE_MILLIS=1000` tune `LoadStressSimulation`.
- `BENCHMARK_DOWNLOAD_RENDITION=true` adds one finished-rendition download per virtual user. Leave it `false` for the official write-heavy matrix unless read/download behavior is being sampled separately.
- `BENCHMARK_AUTO_STACK=false` skips Docker reset/start when an isolated stack is already running.
- `BENCHMARK_APP_PORT`, `BENCHMARK_POSTGRES_PORT`, `BENCHMARK_PROMETHEUS_PORT`, `BENCHMARK_CADVISOR_PORT`, and `BENCHMARK_GRAFANA_PORT` override host ports for isolated local runs.

Vertical profiles map app limits and pool size as follows:

| Profile | App CPUs | App Memory | Rendition Pool |
| --- | ---: | ---: | ---: |
| `baseline` | `2.0` | `2g` | `8` |
| `vertical-1` | `1.0` | `1g` | `1` |
| `vertical-2` | `2.0` | `2g` | `2` |
| `vertical-4` | `4.0` | `4g` | `4` |
| `vertical-8` | `8.0` | `8g` | `8` |

For `custom`, set `BENCHMARK_APP_CPUS`, `BENCHMARK_APP_MEMORY`, and `BENCHMARK_RENDITION_POOL_SIZE` explicitly.

## Phase 5 Experiment Matrix

The official near-term matrix compares the monolith and modular monolith on the same single-instance topology and vertical profiles:

| Dimension | Values |
| --- | --- |
| Variants | `monolith`, `modular_monolith` |
| Topology | `single` |
| Resource profiles | `baseline`, `vertical-1`, `vertical-2`, `vertical-4` |
| Scenario | `LoadStressSimulation` |
| Repetitions | `3` |
| Default requested renditions | `SD_360,HD_720` |

This yields 24 runs: 2 variants x 4 profiles x 3 repetitions. `vertical-8` remains available for exploratory runs but is not part of the official minimal matrix.

Run the full matrix from the repository root:

```sh
benchmark/scripts/run-experiment-matrix.sh
```

Useful matrix overrides:

- `BENCHMARK_REPETITIONS=1` for a quick trial.
- `BENCHMARK_MATRIX_VARIANTS=monolith` or `BENCHMARK_MATRIX_VARIANTS=modular_monolith` to resume only one variant.
- `BENCHMARK_MATRIX_PROFILES=vertical-2,vertical-4` to resume selected profiles.
- `BENCHMARK_LOAD_USERS`, `BENCHMARK_RAMP_SECONDS`, `BENCHMARK_RENDITIONS`, and `BENCHMARK_DOWNLOAD_RENDITION` flow through to every run.

`LoadStressSimulation` performs one full upload -> rendition request -> poll-until-finished flow per virtual user. Polling stops for a user after all requested renditions finish, avoiding the fixed over-polling used by the smoke scenario.

Example isolated monolith run while another variant owns the default ports:

```sh
BENCHMARK_APP_PORT=18080 \
BENCHMARK_POSTGRES_PORT=15433 \
BENCHMARK_PROMETHEUS_PORT=19090 \
BENCHMARK_CADVISOR_PORT=18081 \
BENCHMARK_GRAFANA_PORT=13000 \
BENCHMARK_RESOURCE_PROFILE=vertical-1 \
benchmark/scripts/run-benchmark.sh
```

## Outputs

- DuckDB database: `benchmark/results/benchmark.duckdb` with one enum-backed `benchmark_runs` table.
- Gatling reports: `benchmark/results/runs/<run-id>/gatling`
- Run metadata: `benchmark/results/runs/<run-id>/metadata.json`
- Prometheus snapshots: `benchmark/results/runs/<run-id>/prometheus/*.json`
- Generated smoke video: `benchmark/data/videos/smoke-720p-10s.mp4`

Summarize completed runs after any smoke, single run, or matrix run:

```sh
benchmark/scripts/summarize-results.py
```

Summary outputs:

- CSV: `benchmark/results/summary/benchmark-runs.csv`
- Markdown: `benchmark/results/summary/benchmark-summary.md`

The summarizer joins `benchmark_runs` with Gatling `global_stats.json` when available and exports request count, failures, mean response time, p95, p99, max response time, and mean requests/second. Missing Gatling stats are left blank so failed or interrupted runs remain visible in the table.

Live `.duckdb`, WAL, generated video, and run report artifacts are ignored by Git.

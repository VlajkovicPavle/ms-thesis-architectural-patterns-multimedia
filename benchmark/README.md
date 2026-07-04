# Benchmark Harness

Phase 1 provides a single-instance smoke harness for the media monolith. Phase 2 adds a baseline runner that records run metadata, Gatling reports, Prometheus snapshots, and DuckDB run rows under a stable result layout. Phase 3 adds repeatable vertical resource profiles.

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

Live `.duckdb`, WAL, generated video, and run report artifacts are ignored by Git.

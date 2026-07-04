# Benchmark Harness

Phase 1 provides a single-instance smoke harness for the media monolith.

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

## Outputs

- DuckDB database: `benchmark/results/benchmark.duckdb` with one enum-backed `benchmark_runs` table.
- Gatling reports: `benchmark/results/runs/<run-id>/gatling`
- Generated smoke video: `benchmark/data/videos/smoke-720p-10s.mp4`

Live `.duckdb`, WAL, generated video, and run report artifacts are ignored by Git.

# Benchmark Harness

This harness compares `monolith`, `modular_monolith`, and `microservices` through one public gateway URL. Transport statistics, five-way business outcomes, harness validity, SUT resources, and infrastructure resources are independent result dimensions.

The committed protocol and schedule remain non-official pilot artifacts. Unresolved settings are listed in `benchmark/protocol/pilot-protocol.json`.

## Prerequisites

- Docker with Compose v2.
- Java 21.
- Python 3.11 or newer.
- `curl`.
- DuckDB, or Docker for the DuckDB fallback.

## Fixture Binding

The measured source is `1280x800`, allowing valid `SD_360` and `HD_720` downscales. `benchmark/fixtures/source-video.recipe.json` pins FFmpeg by container digest. The binary remains ignored.

```sh
benchmark/scripts/generate-video.sh
python3 benchmark/scripts/fixture.py verify
```

Generation is never implicit. The protocol binds both the raw SHA-256 of `source-video.manifest.json` and the fixture SHA-256 recorded inside it. Schedule materialization and runner settings verify both values, so changing fixture provenance requires an intentional protocol and schedule rematerialization.

## Pilot Schedule

The schedule seed is committed in `pilot-protocol.json`. Each profile/repetition block contains every architecture exactly once. Block order is deterministically shuffled, and architecture position uses a balanced rotation. Every row records `blockId`, `position`, `repetition`, `plannedRunId`, and the schedule identity.

```sh
python3 benchmark/scripts/materialize-schedule.py materialize
python3 benchmark/scripts/materialize-schedule.py verify
benchmark/scripts/run-experiment-matrix.sh
```

Verification rejects hash drift, fixture drift, missing Cartesian coverage, duplicate variants within a block, invalid positions, unbalanced positions, or identity drift. `BENCHMARK_MATRIX_VARIANTS` and `BENCHMARK_MATRIX_PROFILES` only filter already-materialized rows for resume work.

## Run Lifecycle

```sh
BENCHMARK_VARIANT=microservices \
BENCHMARK_RESOURCE_PROFILE=vertical-2 \
benchmark/scripts/run-benchmark.sh
```

For auto-managed runs, the runner removes stale benchmark projects before startup and installs an exit trap that runs `docker compose down -v --remove-orphans` after success, validation failure, interruption, or runner failure. This prevents matrix transitions from retaining the previous variant. Set `BENCHMARK_KEEP_STACK=true` only for explicit debugging; it intentionally disables final cleanup. `BENCHMARK_AUTO_STACK=false` never cleans an externally managed stack.

Use `BENCHMARK_REPLACEMENT_FOR_RUN_ID=<invalid-run-id>` when a row replaces an invalid attempt. The replacement link is stored with schedule block metadata.

## Timing And Drain

Gatling emits `gatling-timestamps.json` from its `before()` and `after()` lifecycle hooks. These timestamps delimit scenario injection through the last scenario completion and exclude Maven startup, dependency work, and report generation. They are authoritative measurement boundaries in DuckDB and summaries.

Maven runs in the background. As soon as Gatling emits the end timestamp, the runner starts drain reconciliation while report generation may continue. Drain boundaries remain separate in `timestamps.json`.

During the configured drain period, `reconcile-business-outcomes.py` polls final video rendition status for unresolved records. At drain end it atomically rewrites the exhaustive JSONL so a late terminal result becomes `FINISHED` or `ERROR`; technically observable nonterminal work becomes `NO_TERMINAL_STATUS`; inaccessible final status becomes `TECHNICAL_STATUS_LOST`.

## Business And Technical Results

`LoadStressSimulation` emits one record for every expected user ID `1..N` and every exact planned resolution. The allowed outcomes are:

- `FINISHED`
- `ERROR`
- `PRE_IDENTIFIER_FAILURE`
- `NO_TERMINAL_STATUS`
- `TECHNICAL_STATUS_LOST`

HTTP checks remain Gatling transport KOs. Business `ERROR` and other business classifications do not invalidate the harness. Load simulations have no zero-KO assertion. Run-level `technical_valid` is determined only from harness artifacts: Gatling lifecycle completion, exact outcome keys, exact Prometheus target readiness, required metric availability, smoke identity checks, and reconciliation integrity.

`SmokeSimulation` records the exact returned resolution identities and count in `smoke-validation.json`; a smoke run is technically valid only when they exactly equal the requested set and all are terminal.

Confirmed business throughput is final `FINISHED` outcomes divided by the authoritative Gatling measurement duration. Transport request rate remains separately named.

## Monitoring

Prometheus uses file service discovery. Every target has canonical `variant`, `service`, and unique `target_id` labels; application-provided conflicting labels cannot replace them. Before injection, the runner waits for the active-target API to contain exactly the expected targets with health `up`, then archives the result in `prometheus-target-readiness.json`.

`BENCHMARK_PROMETHEUS_TARGETS_JSON` can provide another valid one-or-many file-SD array, but all canonical labels remain required.

Prometheus exports retain per-service labels and use `query_range` from authoritative measurement start through drain end. `query-manifest.jsonl` archives each exact query and boundary. SUT and infrastructure queries remain separate and memory uses `container_memory_working_set_bytes`.

`prometheus/metric-summary.json` aligns SUT services by sample timestamp over measurement boundaries and records:

- aggregate SUT CPU mean and p95 cores.
- aggregate SUT working-memory mean and maximum bytes.
- expected and available aligned sample counts.
- per-service sample counts and all raw query availability.

Incomplete app-up, SUT CPU, or SUT working-memory samples make `technical_valid=false` without deleting or rewriting business outcomes beyond normal drain reconciliation.

Grafana dashboards remain generic across variant, service, Compose project, and Compose service.

## Resource Boundary

| Variant | SUT services | Separate infrastructure |
| --- | --- | --- |
| `monolith` | `app` | `postgres` |
| `modular_monolith` | `app` | `postgres` |
| `microservices` | `gateway`, `media-service`, `transcoder-service`, `notification-service` | `postgres`, `rabbitmq` |

Existing variants assign the whole profile to `app`. Microservices divide the same total across four SUT containers and validate the CPU and memory sums. `RENDITION_POOL_SIZE` controls transcoder concurrency only.

## Outputs

- DuckDB: `benchmark/results/benchmark.duckdb`
- Metadata and validation: `metadata.json`, `run-validation.json`
- Timing: `gatling-timestamps.json`, `timestamps.json`, `drain-reconciliation.json`
- Outcomes: `business-outcomes.jsonl`
- Transport report: `gatling/`
- Monitoring: `prometheus/*.json`, `prometheus/metric-summary.json`

```sh
benchmark/scripts/summarize-results.py
```

CSV and Markdown summaries include schedule/block identity, replacement linkage, technical validity/reason, exact outcome completeness, all five outcomes, business throughput, transport KOs/latency, and aligned SUT CPU/memory statistics.

## Compose Compatibility

The microservices runner expects services named `gateway`, `media-service`, `transcoder-service`, `notification-service`, `postgres`, and `rabbitmq`. It maps `APP_PORT` to `GATEWAY_PORT` and layers `benchmark/monitoring/compose.monitoring.yml` into the same Compose project. Media and notification metrics use `/api/actuator/prometheus`; transcoder uses `/actuator/prometheus`.

The pilot remains non-official until repetition count, warm-up, drain, load, and ramp settings are frozen after pilot review.

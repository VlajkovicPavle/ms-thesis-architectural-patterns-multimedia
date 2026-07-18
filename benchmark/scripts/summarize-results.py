#!/usr/bin/env python3
import csv
import json
import shutil
import subprocess
import sys
from collections import Counter
from datetime import datetime
from pathlib import Path


ROOT_DIR = Path(__file__).resolve().parents[2]
DEFAULT_DB_PATH = ROOT_DIR / "benchmark/results/benchmark.duckdb"
DEFAULT_OUTPUT_DIR = ROOT_DIR / "benchmark/results/summary"
RUNS_DIR = ROOT_DIR / "benchmark/results/runs"
BUSINESS_OUTCOMES = [
    "FINISHED",
    "ERROR",
    "PRE_IDENTIFIER_FAILURE",
    "NO_TERMINAL_STATUS",
    "TECHNICAL_STATUS_LOST",
]

RUN_COLUMNS = [
    "run_id",
    "planned_run_id",
    "repetition",
    "block_id",
    "block_position",
    "schedule_identity",
    "replacement_for_run_id",
    "variant",
    "topology",
    "resource_profile",
    "app_cpus",
    "app_memory",
    "rendition_pool_size",
    "scenario",
    "base_url",
    "video_file",
    "fixture_sha256",
    "fixture_manifest_sha256",
    "requested_resolutions",
    "status",
    "gatling_exit_code",
    "technical_valid",
    "technical_reason",
    "started_at",
    "finished_at",
    "measurement_started_at",
    "measurement_ended_at",
    "drain_started_at",
    "drain_ended_at",
    "protocol_id",
    "protocol_sha256",
    "schedule_sha256",
    "compose_project",
    "sut_services",
    "infrastructure_services",
    "resource_allocations_json",
    "metric_availability_json",
    "report_path",
    "outcome_path",
    "notes",
]

SUMMARY_COLUMNS = [
    "run_id",
    "planned_run_id",
    "variant",
    "resource_profile",
    "repetition",
    "block_id",
    "block_position",
    "schedule_identity",
    "replacement_for_run_id",
    "scenario",
    "status",
    "gatling_exit_code",
    "technical_valid",
    "technical_reason",
    "protocol_id",
    "fixture_sha256",
    "fixture_manifest_sha256",
    "app_cpus",
    "app_memory",
    "rendition_pool_size",
    "requested_resolutions",
    "planned_business_outcomes",
    "recorded_business_outcomes",
    "outcome_completeness",
    "finished_outcomes",
    "error_outcomes",
    "pre_identifier_failure_outcomes",
    "no_terminal_status_outcomes",
    "technical_status_lost_outcomes",
    "measurement_duration_seconds",
    "confirmed_business_throughput_per_second",
    "transport_requests_total",
    "transport_requests_ok",
    "transport_requests_ko",
    "transport_mean_response_ms",
    "transport_p95_response_ms",
    "transport_p99_response_ms",
    "transport_max_response_ms",
    "transport_mean_requests_per_second",
    "sut_cpu_mean_cores",
    "sut_cpu_p95_cores",
    "sut_cpu_expected_samples",
    "sut_cpu_available_samples",
    "sut_memory_mean_bytes",
    "sut_memory_max_bytes",
    "sut_memory_expected_samples",
    "sut_memory_available_samples",
    "required_metrics_available",
    "metric_availability_json",
    "report_path",
    "outcome_path",
]


def main() -> int:
    db_path = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else DEFAULT_DB_PATH
    output_dir = Path(sys.argv[2]).resolve() if len(sys.argv) > 2 else DEFAULT_OUTPUT_DIR
    output_dir.mkdir(parents=True, exist_ok=True)
    summary_rows = [summarize(row) for row in load_run_rows(db_path)]
    write_csv(output_dir / "benchmark-runs.csv", summary_rows, SUMMARY_COLUMNS)
    write_markdown(output_dir / "benchmark-summary.md", summary_rows)
    print(f"Wrote {output_dir / 'benchmark-runs.csv'}")
    print(f"Wrote {output_dir / 'benchmark-summary.md'}")
    return 0


def load_run_rows(db_path: Path) -> list[dict[str, str]]:
    query = """
select
  run_id, planned_run_id, repetition, block_id, block_position, schedule_identity,
  replacement_for_run_id, variant, topology, resource_profile, app_cpus, app_memory,
  rendition_pool_size, scenario, base_url, video_file, fixture_sha256, fixture_manifest_sha256,
  array_to_string(requested_resolutions, ',') as requested_resolutions,
  status, gatling_exit_code, technical_valid, technical_reason, started_at, finished_at, measurement_started_at,
  measurement_ended_at, drain_started_at, drain_ended_at, protocol_id,
  protocol_sha256, schedule_sha256, compose_project,
  array_to_string(sut_services, ',') as sut_services,
  array_to_string(infrastructure_services, ',') as infrastructure_services,
  resource_allocations_json, metric_availability_json, report_path, outcome_path, notes
from benchmark_runs
order by started_at, run_id;
""".strip()
    output = run_duckdb(db_path, query)
    return list(csv.DictReader(output.splitlines(), fieldnames=RUN_COLUMNS))


def run_duckdb(db_path: Path, query: str) -> str:
    if shutil.which("duckdb"):
        command = ["duckdb", "-csv", "-noheader", str(db_path), query]
    elif shutil.which("docker") and db_path.is_relative_to(ROOT_DIR):
        command = [
            "docker",
            "run",
            "--rm",
            "-v",
            f"{ROOT_DIR}:/work:z",
            "-w",
            "/work",
            "duckdb/duckdb:latest",
            "duckdb",
            "-csv",
            "-noheader",
            str(db_path.relative_to(ROOT_DIR)),
            query,
        ]
    else:
        raise SystemExit("duckdb is not installed and Docker fallback is unavailable")
    return subprocess.run(command, check=True, text=True, capture_output=True).stdout


def summarize(row: dict[str, str]) -> dict[str, str]:
    gatling = load_global_stats(row["run_id"])
    metadata = load_metadata(row["run_id"])
    outcomes = load_outcomes(row)
    outcome_counts = Counter(record.get("outcome") for record in outcomes)
    resolution_count = len([value for value in row["requested_resolutions"].split(",") if value])
    is_load_scenario = row["scenario"] == "LoadStressSimulation"
    planned_count = int(metadata.get("loadUsers", 0)) * resolution_count if is_load_scenario else 0
    recorded_count = len(outcomes)
    expected_keys = {
        (user_id, resolution)
        for user_id in range(1, int(metadata.get("loadUsers", 0)) + 1)
        for resolution in row["requested_resolutions"].split(",")
        if resolution
    }
    actual_keys = [(record.get("userId"), record.get("resolution")) for record in outcomes]
    outcomes_exact = is_load_scenario and (
        bool(expected_keys)
        and len(actual_keys) == len(expected_keys)
        and len(set(actual_keys)) == len(actual_keys)
        and set(actual_keys) == expected_keys
    )
    duration = measurement_duration(row)
    finished_count = outcome_counts["FINISHED"]
    throughput = finished_count / duration if duration and duration > 0 else None
    metric_availability = load_metric_availability(row)
    cpu = metric_availability.get("sutCpu", {})
    memory = metric_availability.get("sutWorkingMemory", {})
    result = {
        **row,
        "planned_business_outcomes": str(planned_count) if planned_count else "",
        "recorded_business_outcomes": str(recorded_count),
        "outcome_completeness": (
            "complete" if outcomes_exact else "incomplete" if is_load_scenario else "not_applicable"
        ),
        "measurement_duration_seconds": format_number(duration),
        "confirmed_business_throughput_per_second": format_number(throughput),
        "transport_requests_total": value(gatling, "numberOfRequests", "total"),
        "transport_requests_ok": value(gatling, "numberOfRequests", "ok"),
        "transport_requests_ko": value(gatling, "numberOfRequests", "ko"),
        "transport_mean_response_ms": value(gatling, "meanResponseTime", "total"),
        "transport_p95_response_ms": value(gatling, "percentiles3", "total"),
        "transport_p99_response_ms": value(gatling, "percentiles4", "total"),
        "transport_max_response_ms": value(gatling, "maxResponseTime", "total"),
        "transport_mean_requests_per_second": value(
            gatling, "meanNumberOfRequestsPerSecond", "total"
        ),
        "sut_cpu_mean_cores": format_number(cpu.get("meanCores")),
        "sut_cpu_p95_cores": format_number(cpu.get("p95Cores")),
        "sut_cpu_expected_samples": string_value(cpu.get("expectedSampleCount")),
        "sut_cpu_available_samples": string_value(cpu.get("availableSampleCount")),
        "sut_memory_mean_bytes": format_number(memory.get("meanBytes")),
        "sut_memory_max_bytes": format_number(memory.get("maxBytes")),
        "sut_memory_expected_samples": string_value(memory.get("expectedSampleCount")),
        "sut_memory_available_samples": string_value(memory.get("availableSampleCount")),
        "required_metrics_available": string_value(
            metric_availability.get("requiredMetricsAvailable")
        ),
    }
    for outcome in BUSINESS_OUTCOMES:
        column = outcome.lower() + "_outcomes"
        result[column] = str(outcome_counts[outcome])
    return result


def load_metadata(run_id: str) -> dict:
    path = RUNS_DIR / run_id / "metadata.json"
    if not path.is_file():
        return {}
    with path.open(encoding="utf-8") as input_file:
        return json.load(input_file)


def load_outcomes(row: dict[str, str]) -> list[dict]:
    configured_path = row.get("outcome_path", "")
    path = ROOT_DIR / configured_path if configured_path else RUNS_DIR / row["run_id"] / "business-outcomes.jsonl"
    if not path.is_file():
        return []
    records = []
    with path.open(encoding="utf-8") as input_file:
        for line in input_file:
            if line.strip():
                records.append(json.loads(line))
    return records


def load_global_stats(run_id: str) -> dict:
    stats_files = sorted((RUNS_DIR / run_id / "gatling").glob("*/js/global_stats.json"))
    if not stats_files:
        return {}
    with stats_files[-1].open(encoding="utf-8") as stats_file:
        return json.load(stats_file)


def load_metric_availability(row: dict[str, str]) -> dict:
    path = RUNS_DIR / row["run_id"] / "prometheus" / "metric-summary.json"
    if path.is_file():
        with path.open(encoding="utf-8") as input_file:
            return json.load(input_file)
    raw_value = row.get("metric_availability_json", "")
    if not raw_value:
        return {}
    try:
        return json.loads(raw_value)
    except json.JSONDecodeError:
        return {}


def measurement_duration(row: dict[str, str]) -> float | None:
    try:
        started = datetime.fromisoformat(row["measurement_started_at"].replace("Z", "+00:00"))
        ended = datetime.fromisoformat(row["measurement_ended_at"].replace("Z", "+00:00"))
    except (ValueError, AttributeError):
        return None
    return (ended - started).total_seconds()


def value(stats: dict, key: str, child_key: str) -> str:
    child = stats.get(key, {})
    result = child.get(child_key, "") if isinstance(child, dict) else ""
    return str(result)


def format_number(value: float | None) -> str:
    return "" if value is None else f"{value:.6f}"


def string_value(value: object) -> str:
    if value is None:
        return ""
    if isinstance(value, bool):
        return str(value).lower()
    return str(value)


def write_csv(path: Path, rows: list[dict[str, str]], columns: list[str]) -> None:
    with path.open("w", newline="", encoding="utf-8") as output_file:
        writer = csv.DictWriter(output_file, fieldnames=columns, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


def write_markdown(path: Path, rows: list[dict[str, str]]) -> None:
    columns = [
        "variant",
        "resource_profile",
        "status",
        "technical_valid",
        "technical_reason",
        "block_id",
        "block_position",
        "outcome_completeness",
        "finished_outcomes",
        "error_outcomes",
        "technical_status_lost_outcomes",
        "confirmed_business_throughput_per_second",
        "transport_requests_ko",
        "transport_p95_response_ms",
        "sut_cpu_mean_cores",
        "sut_memory_mean_bytes",
        "run_id",
    ]
    with path.open("w", encoding="utf-8") as output_file:
        output_file.write("# Benchmark Summary\n\n")
        output_file.write("| " + " | ".join(columns) + " |\n")
        output_file.write("| " + " | ".join(["---"] * len(columns)) + " |\n")
        for row in rows:
            output_file.write(
                "| "
                + " | ".join(markdown_cell(row.get(column, "")) for column in columns)
                + " |\n"
            )


def markdown_cell(value: str) -> str:
    return str(value).replace("|", "\\|")


if __name__ == "__main__":
    raise SystemExit(main())

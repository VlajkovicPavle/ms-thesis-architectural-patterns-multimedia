#!/usr/bin/env python3
import csv
import json
import shutil
import subprocess
import sys
from pathlib import Path


ROOT_DIR = Path(__file__).resolve().parents[2]
DEFAULT_DB_PATH = ROOT_DIR / "benchmark/results/benchmark.duckdb"
DEFAULT_OUTPUT_DIR = ROOT_DIR / "benchmark/results/summary"
RUNS_DIR = ROOT_DIR / "benchmark/results/runs"


RUN_COLUMNS = [
    "run_id",
    "variant",
    "topology",
    "resource_profile",
    "app_cpus",
    "app_memory",
    "rendition_pool_size",
    "scenario",
    "base_url",
    "video_file",
    "requested_resolutions",
    "status",
    "gatling_exit_code",
    "started_at",
    "finished_at",
    "report_path",
    "notes",
]


SUMMARY_COLUMNS = [
    "run_id",
    "variant",
    "resource_profile",
    "scenario",
    "status",
    "gatling_exit_code",
    "app_cpus",
    "app_memory",
    "rendition_pool_size",
    "requested_resolutions",
    "requests_total",
    "requests_ok",
    "requests_ko",
    "mean_response_ms",
    "p95_response_ms",
    "p99_response_ms",
    "max_response_ms",
    "mean_requests_per_second",
    "report_path",
]


def main() -> int:
    db_path = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else DEFAULT_DB_PATH
    output_dir = Path(sys.argv[2]).resolve() if len(sys.argv) > 2 else DEFAULT_OUTPUT_DIR
    output_dir.mkdir(parents=True, exist_ok=True)

    rows = load_run_rows(db_path)
    summary_rows = [merge_gatling_stats(row) for row in rows]
    write_csv(output_dir / "benchmark-runs.csv", summary_rows, SUMMARY_COLUMNS)
    write_markdown(output_dir / "benchmark-summary.md", summary_rows)

    print(f"Wrote {output_dir / 'benchmark-runs.csv'}")
    print(f"Wrote {output_dir / 'benchmark-summary.md'}")
    return 0


def load_run_rows(db_path: Path) -> list[dict[str, str]]:
    query = """
select
  run_id,
  variant,
  topology,
  resource_profile,
  app_cpus,
  app_memory,
  rendition_pool_size,
  scenario,
  base_url,
  video_file,
  array_to_string(requested_resolutions, ',') as requested_resolutions,
  status,
  gatling_exit_code,
  started_at,
  finished_at,
  report_path,
  notes
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

    completed = subprocess.run(command, check=True, text=True, capture_output=True)
    return completed.stdout


def merge_gatling_stats(row: dict[str, str]) -> dict[str, str]:
    stats = load_global_stats(row["run_id"])
    return {
        **row,
        "requests_total": value(stats, "numberOfRequests", "total"),
        "requests_ok": value(stats, "numberOfRequests", "ok"),
        "requests_ko": value(stats, "numberOfRequests", "ko"),
        "mean_response_ms": value(stats, "meanResponseTime", "total"),
        "p95_response_ms": value(stats, "percentiles3", "total"),
        "p99_response_ms": value(stats, "percentiles4", "total"),
        "max_response_ms": value(stats, "maxResponseTime", "total"),
        "mean_requests_per_second": value(stats, "meanNumberOfRequestsPerSecond", "total"),
    }


def load_global_stats(run_id: str) -> dict:
    gatling_dir = RUNS_DIR / run_id / "gatling"
    stats_files = sorted(gatling_dir.glob("*/js/global_stats.json"))
    if not stats_files:
      return {}
    with stats_files[-1].open() as stats_file:
        return json.load(stats_file)


def value(stats: dict, key: str, child_key: str) -> str:
    child = stats.get(key, {})
    result = child.get(child_key, "") if isinstance(child, dict) else ""
    return str(result)


def write_csv(path: Path, rows: list[dict[str, str]], columns: list[str]) -> None:
    with path.open("w", newline="") as output_file:
        writer = csv.DictWriter(output_file, fieldnames=columns, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


def write_markdown(path: Path, rows: list[dict[str, str]]) -> None:
    columns = [
        "variant",
        "resource_profile",
        "scenario",
        "status",
        "requests_ok",
        "requests_ko",
        "mean_response_ms",
        "p95_response_ms",
        "p99_response_ms",
        "mean_requests_per_second",
        "run_id",
    ]
    with path.open("w") as output_file:
        output_file.write("# Benchmark Summary\n\n")
        output_file.write("| " + " | ".join(columns) + " |\n")
        output_file.write("| " + " | ".join(["---"] * len(columns)) + " |\n")
        for row in rows:
            output_file.write("| " + " | ".join(markdown_cell(row.get(column, "")) for column in columns) + " |\n")


def markdown_cell(value: str) -> str:
    return str(value).replace("|", "\\|")


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
import argparse
import json
import math
from datetime import datetime
from pathlib import Path


MILLISECONDS_PER_SECOND = 1000
REQUIRED_BUSINESS_QUERIES = (
    "business-http-count",
    "business-http-sum",
    "business-pipeline-count",
    "business-pipeline-sum",
    "business-pipeline-bucket",
    "business-queue-size",
    "business-active-jobs",
)


def epoch(timestamp: str) -> float:
    parsed = datetime.fromisoformat(timestamp.replace("Z", "+00:00")).timestamp()
    return math.floor(parsed * MILLISECONDS_PER_SECOND) / MILLISECONDS_PER_SECOND


def load_matrix(path: Path) -> tuple[dict, list[dict]]:
    if not path.is_file():
        return {"status": "missing"}, []
    payload = json.loads(path.read_text(encoding="utf-8"))
    if payload.get("status") != "success" or payload.get("data", {}).get("resultType") != "matrix":
        return payload, []
    return payload, payload["data"].get("result", [])


def percentile(values: list[float], percentile_value: float) -> float | None:
    if not values:
        return None
    ordered = sorted(values)
    position = (len(ordered) - 1) * percentile_value
    lower = math.floor(position)
    upper = math.ceil(position)
    if lower == upper:
        return ordered[lower]
    return ordered[lower] + (ordered[upper] - ordered[lower]) * (position - lower)


def aligned_aggregate(
    series: list[dict], services: list[str], started_at: float, ended_at: float, step: int
) -> dict:
    values_by_service: dict[str, dict[float, float]] = {service: {} for service in services}
    for item in series:
        service = item.get("metric", {}).get("container_label_com_docker_compose_service")
        if service not in values_by_service:
            continue
        for timestamp, raw_value in item.get("values", []):
            timestamp = float(timestamp)
            if started_at <= timestamp <= ended_at:
                values_by_service[service][timestamp] = float(raw_value)

    expected_count = math.floor((ended_at - started_at) / step) + 1
    complete_timestamps = sorted(
        set.intersection(
            *(set(values_by_service[service]) for service in services)
        )
        if services
        else set()
    )
    aggregates = [
        sum(values_by_service[service][timestamp] for service in services)
        for timestamp in complete_timestamps
    ]
    return {
        "expectedSampleCount": expected_count,
        "availableSampleCount": len(aggregates),
        "expectedServices": services,
        "availableServices": sorted(
            service for service, values in values_by_service.items() if values
        ),
        "samplesByService": {
            service: len(values_by_service[service]) for service in services
        },
        "values": aggregates,
    }


def query_availability(prometheus_dir: Path, started_at: float, ended_at: float) -> dict:
    availability = {}
    for path in sorted(prometheus_dir.glob("*.json")):
        payload, series = load_matrix(path)
        measurement_timestamps = {
            float(timestamp)
            for item in series
            for timestamp, _ in item.get("values", [])
            if started_at <= float(timestamp) <= ended_at
        }
        availability[path.stem] = {
            "available": bool(series),
            "status": payload.get("status", "unknown"),
            "seriesCount": len(series),
            "sampleCount": sum(len(item.get("values", [])) for item in series),
            "measurementSampleCount": len(measurement_timestamps),
        }
    return availability


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--prometheus-dir", type=Path, required=True)
    parser.add_argument("--measurement-start", required=True)
    parser.add_argument("--measurement-end", required=True)
    parser.add_argument("--step-seconds", type=int, required=True)
    parser.add_argument("--sut-services", required=True)
    parser.add_argument("--expected-target-count", type=int, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    services = [value.strip() for value in args.sut_services.split(",") if value.strip()]
    started_at = epoch(args.measurement_start)
    ended_at = epoch(args.measurement_end)
    expected_samples = math.floor((ended_at - started_at) / args.step_seconds) + 1

    _, cpu_series = load_matrix(args.prometheus_dir / "sut-container-cpu.json")
    _, memory_series = load_matrix(args.prometheus_dir / "sut-container-working-memory.json")
    _, up_series = load_matrix(args.prometheus_dir / "app-up.json")
    cpu = aligned_aggregate(cpu_series, services, started_at, ended_at, args.step_seconds)
    memory = aligned_aggregate(memory_series, services, started_at, ended_at, args.step_seconds)
    up_samples = [
        (float(timestamp), float(value))
        for item in up_series
        for timestamp, value in item.get("values", [])
        if started_at <= float(timestamp) <= ended_at and float(value) == 1
    ]
    expected_up_samples = expected_samples * args.expected_target_count
    available_up_samples = len(up_samples)

    cpu_values = cpu.pop("values")
    memory_values = memory.pop("values")
    queries = query_availability(args.prometheus_dir, started_at, ended_at)
    incomplete_business_queries = [
        name
        for name in REQUIRED_BUSINESS_QUERIES
        if queries.get(name, {}).get("measurementSampleCount") != expected_samples
    ]
    cpu.update(
        {
            "meanCores": sum(cpu_values) / len(cpu_values) if cpu_values else None,
            "p95Cores": percentile(cpu_values, 0.95),
        }
    )
    memory.update(
        {
            "meanBytes": sum(memory_values) / len(memory_values) if memory_values else None,
            "maxBytes": max(memory_values) if memory_values else None,
        }
    )
    required_available = (
        cpu["availableSampleCount"] == cpu["expectedSampleCount"]
        and memory["availableSampleCount"] == memory["expectedSampleCount"]
        and available_up_samples == expected_up_samples
        and not incomplete_business_queries
    )
    summary = {
        "schemaVersion": 1,
        "measurementStartedAtUtc": args.measurement_start,
        "measurementEndedAtUtc": args.measurement_end,
        "stepSeconds": args.step_seconds,
        "requiredMetricsAvailable": required_available,
        "appUp": {
            "expectedTargetCount": args.expected_target_count,
            "expectedSampleCount": expected_up_samples,
            "availableSampleCount": available_up_samples,
        },
        "sutCpu": cpu,
        "sutWorkingMemory": memory,
        "requiredBusinessMetrics": {
            "expectedQueries": list(REQUIRED_BUSINESS_QUERIES),
            "expectedSampleCount": expected_samples,
            "incompleteQueries": incomplete_business_queries,
        },
        "queries": queries,
    }
    args.output.write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    print(
        f"Prometheus summary required_available={str(required_available).lower()} "
        f"cpu={cpu['availableSampleCount']}/{cpu['expectedSampleCount']} "
        f"memory={memory['availableSampleCount']}/{memory['expectedSampleCount']}"
    )
    return 0 if required_available else 1


if __name__ == "__main__":
    raise SystemExit(main())

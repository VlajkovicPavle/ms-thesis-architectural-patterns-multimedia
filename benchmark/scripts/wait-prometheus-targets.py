#!/usr/bin/env python3
import argparse
import json
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


def expected_targets(path: Path) -> list[dict]:
    groups = json.loads(path.read_text(encoding="utf-8"))
    expected = []
    for group in groups:
        labels = group.get("labels", {})
        for required_label in ("variant", "service", "target_id"):
            if not labels.get(required_label):
                raise ValueError(f"File-SD target is missing canonical label {required_label}")
        for target in group.get("targets", []):
            expected.append(
                {
                    "instance": target,
                    "variant": labels["variant"],
                    "service": labels["service"],
                    "targetId": labels["target_id"],
                }
            )
    if not expected:
        raise ValueError("File-SD configuration contains no targets")
    identities = {target["targetId"] for target in expected}
    if len(identities) != len(expected):
        raise ValueError("File-SD target_id labels must be unique")
    return sorted(expected, key=lambda target: target["targetId"])


def active_targets(prometheus_url: str) -> list[dict]:
    with urllib.request.urlopen(
        f"{prometheus_url.rstrip('/')}/api/v1/targets?state=active", timeout=5
    ) as response:
        payload = json.load(response)
    if payload.get("status") != "success":
        raise ValueError("Prometheus targets API did not return success")
    observed = []
    for target in payload.get("data", {}).get("activeTargets", []):
        labels = target.get("labels", {})
        if labels.get("job") != "app":
            continue
        observed.append(
            {
                "instance": labels.get("instance"),
                "variant": labels.get("variant"),
                "service": labels.get("service"),
                "targetId": labels.get("target_id"),
                "health": target.get("health"),
                "lastError": target.get("lastError", ""),
                "scrapeUrl": target.get("scrapeUrl"),
            }
        )
    return sorted(observed, key=lambda target: str(target.get("targetId")))


def target_key(target: dict) -> tuple:
    return (
        target.get("instance"),
        target.get("variant"),
        target.get("service"),
        target.get("targetId"),
    )


def is_ready(expected: list[dict], observed: list[dict]) -> bool:
    expected_keys = {target_key(target) for target in expected}
    observed_keys = {target_key(target) for target in observed}
    return expected_keys == observed_keys and all(
        target.get("health") == "up" for target in observed
    )


def write_result(path: Path, result: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", required=True)
    parser.add_argument("--target-file", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--timeout-seconds", type=int, default=120)
    parser.add_argument("--poll-seconds", type=float, default=2.0)
    args = parser.parse_args()

    started_at = utc_now()
    attempts = 0
    observed = []
    error = ""
    try:
        expected = expected_targets(args.target_file)
    except (OSError, ValueError, json.JSONDecodeError) as exception:
        write_result(
            args.output,
            {
                "schemaVersion": 1,
                "ready": False,
                "startedAtUtc": started_at,
                "finishedAtUtc": utc_now(),
                "attempts": attempts,
                "expected": [],
                "observed": [],
                "reason": str(exception),
            },
        )
        return 1

    deadline = time.monotonic() + args.timeout_seconds
    while True:
        attempts += 1
        try:
            observed = active_targets(args.url)
            error = ""
        except (OSError, ValueError, urllib.error.URLError, json.JSONDecodeError) as exception:
            observed = []
            error = str(exception)
        if is_ready(expected, observed):
            ready = True
            break
        if time.monotonic() >= deadline:
            ready = False
            break
        time.sleep(args.poll_seconds)

    result = {
        "schemaVersion": 1,
        "ready": ready,
        "startedAtUtc": started_at,
        "finishedAtUtc": utc_now(),
        "attempts": attempts,
        "expected": expected,
        "observed": observed,
        "reason": "" if ready else error or "exact expected targets were not healthy before timeout",
    }
    write_result(args.output, result)
    print(
        f"Prometheus target readiness ready={str(ready).lower()} "
        f"expected={len(expected)} observed={len(observed)} attempts={attempts}"
    )
    return 0 if ready else 1


if __name__ == "__main__":
    raise SystemExit(main())

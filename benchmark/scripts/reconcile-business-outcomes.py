#!/usr/bin/env python3
import argparse
import json
import os
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path


TERMINAL = {"FINISHED", "ERROR"}
UNRESOLVED = {"NO_TERMINAL_STATUS", "TECHNICAL_STATUS_LOST"}


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


def load_jsonl(path: Path) -> list[dict]:
    with path.open(encoding="utf-8") as input_file:
        return [json.loads(line) for line in input_file if line.strip()]


def fetch_video_renditions(base_url: str, video_id: str) -> list[dict]:
    url = f"{base_url.rstrip('/')}/api/v1/rendition/video/{video_id}"
    with urllib.request.urlopen(url, timeout=10) as response:
        if response.status != 200:
            raise ValueError(f"Final status request returned HTTP {response.status}")
        payload = json.load(response)
    if not isinstance(payload, list):
        raise ValueError("Final status response is not a list")
    return payload


def apply_observations(records: list[dict], observations: dict[str, list[dict]]) -> int:
    resolved = 0
    for record in records:
        if record.get("outcome") not in UNRESOLVED or not record.get("videoId"):
            continue
        matching = next(
            (
                rendition
                for rendition in observations.get(record["videoId"], [])
                if rendition.get("resolution") == record.get("resolution")
            ),
            None,
        )
        if matching is None:
            continue
        record["renditionId"] = matching.get("id")
        record["lastObservedStatus"] = matching.get("status")
        record["observedAtUtc"] = utc_now()
        record["reconciledDuringDrain"] = True
        if matching.get("status") in TERMINAL:
            record["outcome"] = matching["status"]
            resolved += 1
        else:
            record["outcome"] = "NO_TERMINAL_STATUS"
    return resolved


def finalize_unresolved(records: list[dict], successful_videos: set[str]) -> None:
    finalized_at = utc_now()
    for record in records:
        if record.get("outcome") in UNRESOLVED:
            if record.get("videoId") in successful_videos:
                record["outcome"] = "NO_TERMINAL_STATUS"
            else:
                record["outcome"] = "TECHNICAL_STATUS_LOST"
        record["finalizedAtDrainEndUtc"] = finalized_at
        record.setdefault("reconciledDuringDrain", False)


def write_jsonl_atomic(path: Path, records: list[dict]) -> None:
    temporary_path = path.with_suffix(path.suffix + ".tmp")
    with temporary_path.open("w", encoding="utf-8") as output_file:
        for record in records:
            output_file.write(json.dumps(record, separators=(",", ":")) + "\n")
    os.replace(temporary_path, path)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--outcomes", type=Path, required=True)
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--drain-seconds", type=float, required=True)
    parser.add_argument("--configured-drain-seconds", type=float)
    parser.add_argument("--poll-millis", type=int, default=1000)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    records = load_jsonl(args.outcomes)
    started_at = utc_now()
    deadline = time.monotonic() + max(args.drain_seconds, 0)
    attempts = 0
    errors = []
    final_successful_videos: set[str] = set()

    while True:
        unresolved_videos = {
            record.get("videoId")
            for record in records
            if record.get("outcome") in UNRESOLVED and record.get("videoId")
        }
        observations = {}
        if unresolved_videos:
            attempts += 1
            final_successful_videos = set()
            for video_id in sorted(unresolved_videos):
                try:
                    observations[video_id] = fetch_video_renditions(args.base_url, video_id)
                    final_successful_videos.add(video_id)
                except (OSError, ValueError, urllib.error.URLError, json.JSONDecodeError) as exception:
                    errors.append({"videoId": video_id, "error": str(exception), "atUtc": utc_now()})
            apply_observations(records, observations)
        remaining = time.monotonic() < deadline
        if not remaining:
            break
        time.sleep(min(args.poll_millis / 1000, max(deadline - time.monotonic(), 0)))

    finalize_unresolved(records, final_successful_videos)
    write_jsonl_atomic(args.outcomes, records)
    result = {
        "schemaVersion": 1,
        "startedAtUtc": started_at,
        "endedAtUtc": utc_now(),
        "configuredDrainSeconds": (
            args.configured_drain_seconds
            if args.configured_drain_seconds is not None
            else args.drain_seconds
        ),
        "remainingDrainSecondsAtReconciliationStart": args.drain_seconds,
        "pollAttempts": attempts,
        "finalSuccessfulVideoIds": sorted(final_successful_videos),
        "errors": errors,
        "outcomeCounts": {
            outcome: sum(record.get("outcome") == outcome for record in records)
            for outcome in [
                "FINISHED",
                "ERROR",
                "PRE_IDENTIFIER_FAILURE",
                "NO_TERMINAL_STATUS",
                "TECHNICAL_STATUS_LOST",
            ]
        },
    }
    args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(f"Reconciled {len(records)} outcomes through drain end")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
import argparse
import json
from datetime import datetime, timezone
from pathlib import Path


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--metadata", type=Path, required=True)
    parser.add_argument("--timestamps", type=Path, required=True)
    parser.add_argument("--validation", type=Path, required=True)
    parser.add_argument("--gatling-exit-code", type=int, required=True)
    args = parser.parse_args()

    metadata = load(args.metadata)
    timestamps = load(args.timestamps)
    validation = load(args.validation)
    metadata.update(
        {
            "timestamps": timestamps,
            "technicalValid": validation["technicalValid"],
            "technicalReason": validation["technicalReason"],
            "targetReadiness": validation["targetReadiness"],
            "metricAvailability": validation["metricAvailability"],
            "gatlingExitCode": args.gatling_exit_code,
            "finishedAtUtc": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
        }
    )
    args.metadata.write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

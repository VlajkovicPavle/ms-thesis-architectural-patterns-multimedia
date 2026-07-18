#!/usr/bin/env python3
import json
import sys
from pathlib import Path


OUTCOMES = {
    "FINISHED",
    "ERROR",
    "PRE_IDENTIFIER_FAILURE",
    "NO_TERMINAL_STATUS",
    "TECHNICAL_STATUS_LOST",
}


def validate(path: Path, load_users: int, resolutions: list[str]) -> list[str]:
    errors = []
    if not resolutions or len(set(resolutions)) != len(resolutions):
        return ["Expected resolutions must be non-empty and unique"]
    if not path.is_file():
        return [f"Business outcome file is missing: {path}"]

    records = []
    with path.open(encoding="utf-8") as input_file:
        for line_number, line in enumerate(input_file, start=1):
            try:
                records.append(json.loads(line))
            except json.JSONDecodeError as exception:
                errors.append(f"Invalid JSONL at line {line_number}: {exception}")
    expected_keys = {
        (user_id, resolution)
        for user_id in range(1, load_users + 1)
        for resolution in resolutions
    }
    actual_keys = []
    for record in records:
        if record.get("outcome") not in OUTCOMES:
            errors.append(f"Invalid business outcome: {record.get('outcome')}")
        actual_keys.append((record.get("userId"), record.get("resolution")))

    actual_key_set = set(actual_keys)
    duplicates = sorted(key for key in actual_key_set if actual_keys.count(key) > 1)
    missing = sorted(expected_keys - actual_key_set)
    unexpected = sorted(actual_key_set - expected_keys, key=str)
    if duplicates:
        errors.append(f"Duplicate user/resolution keys: {duplicates}")
    if missing:
        errors.append(f"Missing user/resolution keys: {missing}")
    if unexpected:
        errors.append(f"Unexpected user/resolution keys: {unexpected}")
    if len(records) != len(expected_keys):
        errors.append(f"Expected {len(expected_keys)} records, found {len(records)}")
    return errors


def main() -> int:
    if len(sys.argv) != 4:
        raise SystemExit(
            "Usage: validate-business-outcomes.py OUTCOME_PATH LOAD_USERS RESOLUTIONS_CSV"
        )
    path = Path(sys.argv[1])
    load_users = int(sys.argv[2])
    resolutions = [value.strip() for value in sys.argv[3].split(",") if value.strip()]
    errors = validate(path, load_users, resolutions)
    if errors:
        raise SystemExit("; ".join(errors))
    print(f"Validated exact {load_users}x{len(resolutions)} business outcome keys")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

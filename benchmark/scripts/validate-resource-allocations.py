#!/usr/bin/env python3
import json
import re
import sys
from decimal import Decimal


MEMORY_UNITS = {
    "b": Decimal(1),
    "k": Decimal(1024),
    "kb": Decimal(1024),
    "m": Decimal(1024**2),
    "mb": Decimal(1024**2),
    "g": Decimal(1024**3),
    "gb": Decimal(1024**3),
}


def memory_bytes(value: str) -> Decimal:
    match = re.fullmatch(r"([0-9]+(?:\.[0-9]+)?)([a-zA-Z]+)", value.strip())
    if not match or match.group(2).lower() not in MEMORY_UNITS:
        raise ValueError(f"Unsupported memory value: {value}")
    return Decimal(match.group(1)) * MEMORY_UNITS[match.group(2).lower()]


def main() -> int:
    if len(sys.argv) != 4:
        raise SystemExit(
            "Usage: validate-resource-allocations.py TOTAL_CPUS TOTAL_MEMORY ALLOCATIONS_JSON"
        )
    expected_cpu = Decimal(sys.argv[1])
    expected_memory = memory_bytes(sys.argv[2])
    allocations = json.loads(sys.argv[3])
    actual_cpu = sum(Decimal(str(item["cpus"])) for item in allocations)
    actual_memory = sum(memory_bytes(item["memory"]) for item in allocations)
    if actual_cpu != expected_cpu:
        raise SystemExit(f"SUT CPU allocation sum is {actual_cpu}, expected {expected_cpu}")
    if actual_memory != expected_memory:
        raise SystemExit(
            f"SUT memory allocation sum is {actual_memory}, expected {expected_memory}"
        )
    print(json.dumps(allocations, separators=(",", ":")))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

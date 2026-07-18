#!/usr/bin/env python3
import hashlib
import json
import random
import sys
from collections import Counter, defaultdict
from pathlib import Path


ROOT_DIR = Path(__file__).resolve().parents[2]
DEFAULT_PROTOCOL = ROOT_DIR / "benchmark/protocol/pilot-protocol.json"
DEFAULT_SCHEDULE = ROOT_DIR / "benchmark/protocol/pilot-schedule.json"
DEFAULT_DIGEST = ROOT_DIR / "benchmark/protocol/pilot-schedule.sha256"


def canonical_json(value: object) -> bytes:
    return (json.dumps(value, indent=2, sort_keys=True) + "\n").encode()


def identity_json(value: object) -> bytes:
    return json.dumps(value, separators=(",", ":"), sort_keys=True).encode()


def sha256_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def file_sha256(path: Path) -> str:
    return sha256_bytes(path.read_bytes())


def load(path: Path) -> dict:
    with path.open(encoding="utf-8") as input_file:
        return json.load(input_file)


def fixture_binding(protocol: dict) -> dict:
    manifest_path = (ROOT_DIR / protocol["fixtureManifest"]).resolve()
    if not manifest_path.is_file():
        raise ValueError(f"Fixture manifest is missing: {manifest_path}")
    manifest = load(manifest_path)
    actual_manifest_sha = file_sha256(manifest_path)
    if protocol.get("fixtureManifestSha256") != actual_manifest_sha:
        raise ValueError("Protocol fixture manifest SHA-256 does not match the file")
    if protocol.get("fixtureSha256") != manifest.get("sha256"):
        raise ValueError("Protocol fixture SHA-256 does not match the manifest")
    if not manifest.get("materialized"):
        raise ValueError("Protocol fixture manifest is not materialized")
    return {
        "fixtureManifest": protocol["fixtureManifest"],
        "fixtureManifestSha256": actual_manifest_sha,
        "fixtureSha256": manifest["sha256"],
    }


def build_run_specs(protocol: dict) -> list[dict]:
    variants = list(protocol["variants"])
    blocks = [
        {"profile": profile, "repetition": repetition}
        for profile in protocol["resourceProfiles"]
        for repetition in range(1, int(protocol["repetitions"]) + 1)
    ]
    randomizer = random.Random(int(protocol["scheduleSeed"]))
    randomizer.shuffle(blocks)
    base_variant_order = variants.copy()
    randomizer.shuffle(base_variant_order)

    runs = []
    ordinal = 0
    for block_index, block in enumerate(blocks):
        block_id = f"{block['profile']}-r{block['repetition']}"
        rotation = block_index % len(base_variant_order)
        variant_order = base_variant_order[rotation:] + base_variant_order[:rotation]
        for position, variant in enumerate(variant_order, start=1):
            ordinal += 1
            runs.append(
                {
                    "ordinal": ordinal,
                    "plannedRunId": (
                        f"{protocol['protocolId']}-{block_id}-p{position}-{variant}"
                    ),
                    "variant": variant,
                    "resourceProfile": block["profile"],
                    "repetition": block["repetition"],
                    "blockId": block_id,
                    "position": position,
                }
            )
    return runs


def schedule_identity(protocol_sha: str, seed: int, fixture: dict, runs: list[dict]) -> str:
    identity_payload = {
        "protocolSha256": protocol_sha,
        "scheduleSeed": seed,
        **fixture,
        "runs": runs,
    }
    return sha256_bytes(identity_json(identity_payload))


def build_schedule(protocol_path: Path) -> dict:
    protocol = load(protocol_path)
    if protocol.get("official") or protocol.get("status") != "pilot":
        raise ValueError("Schedule materialization requires an explicitly non-official pilot protocol")
    fixture = fixture_binding(protocol)
    protocol_sha = file_sha256(protocol_path)
    runs = build_run_specs(protocol)
    identity = schedule_identity(protocol_sha, int(protocol["scheduleSeed"]), fixture, runs)
    runs_with_identity = [{**run, "scheduleIdentity": identity} for run in runs]
    return {
        "schemaVersion": 2,
        "scheduleIdentity": identity,
        "scheduleSeed": int(protocol["scheduleSeed"]),
        "protocolId": protocol["protocolId"],
        "protocolStatus": protocol["status"],
        "official": False,
        "protocolPath": str(protocol_path.relative_to(ROOT_DIR)),
        "protocolSha256": protocol_sha,
        **fixture,
        "runCount": len(runs_with_identity),
        "runs": runs_with_identity,
    }


def verify_coverage(protocol: dict, schedule: dict) -> None:
    variants = set(protocol["variants"])
    expected = {
        (profile, repetition, variant)
        for profile in protocol["resourceProfiles"]
        for repetition in range(1, int(protocol["repetitions"]) + 1)
        for variant in variants
    }
    actual = {
        (run["resourceProfile"], int(run["repetition"]), run["variant"])
        for run in schedule["runs"]
    }
    if actual != expected or len(schedule["runs"]) != len(expected):
        raise ValueError("Schedule does not cover each profile/repetition/variant exactly once")

    blocks: dict[str, list[dict]] = defaultdict(list)
    for run in schedule["runs"]:
        blocks[run["blockId"]].append(run)
    for block_id, block_runs in blocks.items():
        if {run["variant"] for run in block_runs} != variants:
            raise ValueError(f"Block {block_id} does not contain every variant exactly once")
        if {int(run["position"]) for run in block_runs} != set(range(1, len(variants) + 1)):
            raise ValueError(f"Block {block_id} has invalid positions")

    position_counts: dict[str, Counter] = defaultdict(Counter)
    for run in schedule["runs"]:
        position_counts[run["variant"]][int(run["position"])] += 1
    for variant, counts in position_counts.items():
        all_counts = [counts[position] for position in range(1, len(variants) + 1)]
        if max(all_counts) - min(all_counts) > 1:
            raise ValueError(f"Variant {variant} is not balanced across block positions")


def materialize(protocol_path: Path, schedule_path: Path, digest_path: Path) -> None:
    try:
        schedule = build_schedule(protocol_path)
        verify_coverage(load(protocol_path), schedule)
    except ValueError as exception:
        raise SystemExit(str(exception)) from exception
    schedule_path.write_bytes(canonical_json(schedule))
    digest_path.write_text(f"{file_sha256(schedule_path)}  {schedule_path.name}\n", encoding="utf-8")
    print(
        f"Materialized {schedule['runCount']} pilot runs in {schedule_path} "
        f"identity={schedule['scheduleIdentity']}"
    )


def verify(protocol_path: Path, schedule_path: Path, digest_path: Path) -> None:
    try:
        protocol = load(protocol_path)
        schedule = load(schedule_path)
        expected_schedule_sha = digest_path.read_text(encoding="utf-8").split()[0]
        actual_schedule_sha = file_sha256(schedule_path)
        if actual_schedule_sha != expected_schedule_sha:
            raise ValueError(
                f"Schedule SHA-256 mismatch: expected {expected_schedule_sha}, got {actual_schedule_sha}"
            )
        rebuilt = build_schedule(protocol_path)
        if schedule != rebuilt:
            raise ValueError("Materialized schedule differs from deterministic protocol expansion")
        verify_coverage(protocol, schedule)
        if schedule.get("official") or schedule.get("protocolStatus") != "pilot":
            raise ValueError("Pilot schedule must not claim official status")
    except (KeyError, ValueError) as exception:
        raise SystemExit(str(exception)) from exception
    print(
        f"Verified pilot schedule sha256={actual_schedule_sha} "
        f"identity={schedule['scheduleIdentity']} coverage={schedule['runCount']}"
    )


def rows(schedule_path: Path) -> None:
    schedule = load(schedule_path)
    for run in schedule["runs"]:
        print(
            "\t".join(
                [
                    str(run["ordinal"]),
                    run["plannedRunId"],
                    run["variant"],
                    run["resourceProfile"],
                    str(run["repetition"]),
                    run["blockId"],
                    str(run["position"]),
                    run["scheduleIdentity"],
                ]
            )
        )


def settings(protocol_path: Path, schedule_path: Path) -> None:
    protocol = load(protocol_path)
    schedule = load(schedule_path)
    values = {
        "BENCHMARK_PROTOCOL_ID": protocol["protocolId"],
        "BENCHMARK_PROTOCOL_SHA256": file_sha256(protocol_path),
        "BENCHMARK_SCHEDULE_SHA256": file_sha256(schedule_path),
        "BENCHMARK_SCHEDULE_IDENTITY": schedule["scheduleIdentity"],
        "BENCHMARK_FIXTURE_MANIFEST": str((ROOT_DIR / protocol["fixtureManifest"]).resolve()),
        "BENCHMARK_FIXTURE_MANIFEST_SHA256": protocol["fixtureManifestSha256"],
        "BENCHMARK_FIXTURE_SHA256": protocol["fixtureSha256"],
        "BENCHMARK_SCENARIO": protocol["scenario"],
        "BENCHMARK_RENDITIONS": ",".join(protocol["renditions"]),
        "BENCHMARK_LOAD_USERS": protocol["loadUsers"],
        "BENCHMARK_RAMP_SECONDS": protocol["rampSeconds"],
        "BENCHMARK_POLL_ATTEMPTS": protocol["pollAttempts"],
        "BENCHMARK_POLL_PAUSE_MILLIS": protocol["pollPauseMillis"],
        "BENCHMARK_WARM_UP_SECONDS": protocol["warmUpSeconds"],
        "BENCHMARK_DRAIN_SECONDS": protocol["drainSeconds"],
        "BENCHMARK_PROMETHEUS_STEP_SECONDS": protocol["prometheusStepSeconds"],
        "BENCHMARK_DOWNLOAD_RENDITION": str(protocol["downloadRendition"]).lower(),
    }
    for key, value in values.items():
        print(f"{key}\t{value}")


def main() -> int:
    action = sys.argv[1] if len(sys.argv) > 1 else "materialize"
    protocol_path = Path(sys.argv[2]).resolve() if len(sys.argv) > 2 else DEFAULT_PROTOCOL
    schedule_path = Path(sys.argv[3]).resolve() if len(sys.argv) > 3 else DEFAULT_SCHEDULE
    digest_path = schedule_path.with_suffix(".sha256") if len(sys.argv) > 3 else DEFAULT_DIGEST
    if action == "materialize":
        materialize(protocol_path, schedule_path, digest_path)
    elif action == "verify":
        verify(protocol_path, schedule_path, digest_path)
    elif action == "rows":
        rows(schedule_path)
    elif action == "settings":
        settings(protocol_path, schedule_path)
    else:
        raise SystemExit(
            "Usage: materialize-schedule.py [materialize|verify|rows|settings] [protocol] [schedule]"
        )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

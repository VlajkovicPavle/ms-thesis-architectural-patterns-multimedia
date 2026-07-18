#!/usr/bin/env python3
import argparse
import importlib.util
import json
from datetime import datetime
from pathlib import Path


def load_json(path: Path) -> dict:
    if not path.is_file():
        return {}
    return json.loads(path.read_text(encoding="utf-8"))


def load_outcome_validator():
    path = Path(__file__).with_name("validate-business-outcomes.py")
    spec = importlib.util.spec_from_file_location("validate_business_outcomes", path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module.validate


def valid_timing(timing: dict, run_id: str) -> bool:
    try:
        started = datetime.fromisoformat(
            timing["scenarioInjectionStartedAtUtc"].replace("Z", "+00:00")
        )
        ended = datetime.fromisoformat(timing["scenarioEndedAtUtc"].replace("Z", "+00:00"))
    except (KeyError, ValueError, AttributeError):
        return False
    return timing.get("runId") == run_id and ended >= started


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--run-id", required=True)
    parser.add_argument("--scenario", required=True)
    parser.add_argument("--gatling-exit-code", type=int, required=True)
    parser.add_argument("--timing", type=Path, required=True)
    parser.add_argument("--readiness", type=Path, required=True)
    parser.add_argument("--metrics", type=Path, required=True)
    parser.add_argument("--outcomes", type=Path)
    parser.add_argument("--load-users", type=int, default=0)
    parser.add_argument("--resolutions", default="")
    parser.add_argument("--smoke-validation", type=Path)
    parser.add_argument("--extra-reason", action="append", default=[])
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    reasons = [reason for reason in args.extra_reason if reason]
    timing = load_json(args.timing)
    readiness = load_json(args.readiness)
    metrics = load_json(args.metrics)
    if args.gatling_exit_code != 0:
        reasons.append(f"gatling_harness_exit_{args.gatling_exit_code}")
    if not valid_timing(timing, args.run_id):
        reasons.append("missing_or_invalid_gatling_timing")
    if readiness.get("ready") is not True:
        reasons.append("prometheus_targets_not_ready")
    if metrics.get("requiredMetricsAvailable") is not True:
        reasons.append("required_prometheus_metrics_incomplete")

    if args.scenario == "LoadStressSimulation":
        resolutions = [value.strip() for value in args.resolutions.split(",") if value.strip()]
        outcome_errors = load_outcome_validator()(args.outcomes, args.load_users, resolutions)
        reasons.extend(f"business_outcome_artifact:{error}" for error in outcome_errors)
    elif args.scenario == "SmokeSimulation":
        smoke = load_json(args.smoke_validation)
        if smoke.get("exactResolutionIdentitiesAndCount") is not True:
            reasons.append("smoke_resolution_identities_or_count_mismatch")
        if smoke.get("allTerminal") is not True:
            reasons.append("smoke_renditions_not_terminal")

    deduplicated_reasons = list(dict.fromkeys(reasons))
    result = {
        "schemaVersion": 1,
        "runId": args.run_id,
        "technicalValid": not deduplicated_reasons,
        "technicalReason": "; ".join(deduplicated_reasons),
        "reasons": deduplicated_reasons,
        "targetReadiness": readiness,
        "metricAvailability": metrics,
    }
    args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(
        f"Run technical_valid={str(result['technicalValid']).lower()} "
        f"reason={result['technicalReason'] or 'none'}"
    )
    return 0 if result["technicalValid"] else 1


if __name__ == "__main__":
    raise SystemExit(main())

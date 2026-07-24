#!/usr/bin/env python3
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path


SCRIPT_DIR = Path(__file__).resolve().parent
ROOT_DIR = SCRIPT_DIR.parents[1]


def load_script(name: str):
    path = SCRIPT_DIR / name
    spec = importlib.util.spec_from_file_location(name.replace("-", "_"), path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


class ScheduleTests(unittest.TestCase):
    def test_materialized_schedule_is_deterministic_and_complete(self):
        schedule_module = load_script("materialize-schedule.py")
        protocol_path = ROOT_DIR / "benchmark/protocol/pilot-protocol.json"
        schedule_path = ROOT_DIR / "benchmark/protocol/pilot-schedule.json"
        expected = json.loads(schedule_path.read_text(encoding="utf-8"))
        rebuilt = schedule_module.build_schedule(protocol_path)
        self.assertEqual(expected, rebuilt)
        schedule_module.verify_coverage(schedule_module.load(protocol_path), rebuilt)


class OutcomeTests(unittest.TestCase):
    def test_validator_requires_exact_user_resolution_keys(self):
        outcome_module = load_script("validate-business-outcomes.py")
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "outcomes.jsonl"
            records = [
                {"userId": 1, "resolution": "SD_360", "outcome": "FINISHED"},
                {"userId": 1, "resolution": "HD_720", "outcome": "ERROR"},
                {"userId": 2, "resolution": "SD_360", "outcome": "NO_TERMINAL_STATUS"},
                {
                    "userId": 2,
                    "resolution": "HD_720",
                    "outcome": "TECHNICAL_STATUS_LOST",
                },
            ]
            path.write_text(
                "".join(json.dumps(record) + "\n" for record in records), encoding="utf-8"
            )
            self.assertEqual([], outcome_module.validate(path, 2, ["SD_360", "HD_720"]))
            records[-1]["userId"] = 3
            path.write_text(
                "".join(json.dumps(record) + "\n" for record in records), encoding="utf-8"
            )
            self.assertTrue(outcome_module.validate(path, 2, ["SD_360", "HD_720"]))

    def test_drain_observation_rewrites_terminal_outcome(self):
        reconciliation = load_script("reconcile-business-outcomes.py")
        records = [
            {
                "videoId": "video-1",
                "resolution": "HD_720",
                "outcome": "NO_TERMINAL_STATUS",
            }
        ]
        resolved = reconciliation.apply_observations(
            records,
            {"video-1": [{"id": "rendition-1", "resolution": "HD_720", "status": "FINISHED"}]},
        )
        self.assertEqual(1, resolved)
        self.assertEqual("FINISHED", records[0]["outcome"])


class MonitoringTests(unittest.TestCase):
    def test_query_availability_requires_measurement_timestamp_coverage(self):
        metrics = load_script("summarize-prometheus.py")
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "business-active-jobs.json"
            path.write_text(
                json.dumps(
                    {
                        "status": "success",
                        "data": {
                            "resultType": "matrix",
                            "result": [
                                {"metric": {}, "values": [[5, "0"], [10, "1"], [20, "0"]]},
                                {"metric": {}, "values": [[10, "2"], [15, "1"]]},
                            ],
                        },
                    }
                ),
                encoding="utf-8",
            )

            availability = metrics.query_availability(Path(directory), 10, 15)

            self.assertEqual(2, availability["business-active-jobs"]["measurementSampleCount"])

    def test_aligned_aggregate_accepts_prometheus_millisecond_boundary(self):
        metrics = load_script("summarize-prometheus.py")
        started_at = metrics.epoch("2026-07-18T13:53:58.314301282Z")
        ended_at = metrics.epoch("2026-07-18T13:54:24.130146785Z")
        timestamps = [started_at + offset for offset in range(0, 26, 5)]
        series = [
            {
                "metric": {"container_label_com_docker_compose_service": service},
                "values": [[timestamp, "1"] for timestamp in timestamps],
            }
            for service in ("a", "b")
        ]

        aggregate = metrics.aligned_aggregate(series, ["a", "b"], started_at, ended_at, 5)

        self.assertEqual(6, aggregate["expectedSampleCount"])
        self.assertEqual(6, aggregate["availableSampleCount"])

    def test_aligned_aggregate_requires_all_services(self):
        metrics = load_script("summarize-prometheus.py")
        series = [
            {
                "metric": {"container_label_com_docker_compose_service": "a"},
                "values": [[10, "1"], [15, "2"]],
            },
            {
                "metric": {"container_label_com_docker_compose_service": "b"},
                "values": [[10, "3"]],
            },
        ]
        aggregate = metrics.aligned_aggregate(series, ["a", "b"], 10, 15, 5)
        self.assertEqual(2, aggregate["expectedSampleCount"])
        self.assertEqual(1, aggregate["availableSampleCount"])
        self.assertEqual([4.0], aggregate["values"])

    def test_readiness_requires_exact_canonical_targets(self):
        readiness = load_script("wait-prometheus-targets.py")
        expected = [
            {"instance": "app:8080", "variant": "monolith", "service": "media", "targetId": "one"}
        ]
        observed = [{**expected[0], "health": "up"}]
        self.assertTrue(readiness.is_ready(expected, observed))
        self.assertFalse(readiness.is_ready(expected, observed + [{**observed[0], "targetId": "extra"}]))


if __name__ == "__main__":
    unittest.main()

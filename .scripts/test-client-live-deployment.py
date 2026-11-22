#!/usr/bin/env python3
"""Check deployment evidence without a cluster or a Docker daemon."""
import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("collector", Path(__file__).with_name("client-live-deployment.py"))
collector = importlib.util.module_from_spec(spec)
spec.loader.exec_module(collector)

COMMIT = "a" * 40
DIGEST = "sha256:" + "1" * 64
IMAGE = "registry/data:sha-" + COMMIT
ROLLED_OUT = '7 7 2 2 2 2\n{"matchLabels":{"app":"data"}}\nsidecar data'
POD = f"Running\tdata\ttrue\t{IMAGE}\tregistry/data@{DIGEST}"
CONTAINER = f"False|running|healthy|{IMAGE}|{DIGEST}"


class EvidenceTest(unittest.TestCase):
    def test_kubernetes_reports_the_running_image_on_every_replica(self):
        with patch.object(collector, "output", side_effect=[ROLLED_OUT, POD + "\n" + POD]) as command:
            self.assertEqual(collector.kubernetes_image("test", "data-service", "data", COMMIT), DIGEST)
            self.assertIn("app=data", command.call_args.args)

    def test_kubernetes_rejects_partial_rollouts_and_unready_replicas(self):
        for status, rows in ((ROLLED_OUT.replace("7 7", "7 6"), POD), (ROLLED_OUT, POD),
                             (ROLLED_OUT, POD + "\n" + POD.replace("true", "false"))):
            with self.subTest(status=status, rows=rows), patch.object(collector, "output", side_effect=[status, rows]):
                with self.assertRaises(ValueError):
                    collector.kubernetes_image("test", "data-service", "data", COMMIT)

    def test_kubernetes_selects_the_specified_container(self):
        status = ROLLED_OUT.replace("sidecar data", "sidecar api")
        rows = "\n".join([POD.replace("\tdata\t", "\tapi\t"), POD.replace("\tdata\t", "\tsidecar\t")] * 2)
        with patch.object(collector, "output", side_effect=[status, rows]):
            self.assertEqual(collector.kubernetes_image("test", "data-service", "data/api", COMMIT), DIGEST)

    def test_selectors_honor_all_requirements(self):
        spec = {"matchLabels": {"app": "db"}, "matchExpressions": [
            {"key": "site", "operator": "In", "values": ["a", "b"]},
            {"key": "color", "operator": "NotIn", "values": ["old"]},
            {"key": "owner", "operator": "Exists"}, {"key": "retired", "operator": "DoesNotExist"}]}
        self.assertEqual(collector.selector(spec), "app=db,site in (a,b),color notin (old),owner,!retired")

    def test_compose_ignores_oneoff_commands_and_checks_container_health(self):
        rows = CONTAINER + "\n" + CONTAINER.replace("False|running|healthy", "True|exited|")
        with patch.object(collector, "output", side_effect=["c1\nc2", rows]) as command:
            self.assertEqual(collector.compose_image("project", "data-service", "dbrepo-data-service", COMMIT), DIGEST)
            self.assertIn("--all", command.call_args_list[0].args)
        for row in (CONTAINER.replace("healthy", "starting"), CONTAINER.replace("running", "exited")):
            with self.subTest(row=row), patch.object(collector, "output", side_effect=["c1", row]):
                with self.assertRaises(ValueError):
                    collector.compose_image("project", "data-service", "dbrepo-data-service", COMMIT)

    def test_rejects_mixed_digests_missing_digests_and_contradictory_commit_tags(self):
        for records in ([], [(IMAGE, "latest")], [(IMAGE, DIGEST), (IMAGE, "sha256:" + "2" * 64)],
                        [("image:sha-" + "b" * 40, DIGEST)]):
            with self.subTest(records=records), self.assertRaises(ValueError):
                collector.one_digest(records, "data-service", COMMIT)


if __name__ == "__main__":
    unittest.main()

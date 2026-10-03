"""Offline consistency checks for fresh-install and existing-broker definitions."""

import json
from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]
BROKER = ROOT / "dbrepo-broker-service"


class ConsumerRetentionTest(unittest.TestCase):
    def setUp(self):
        self.retention = json.loads((BROKER / "consumer-retention.json").read_text())
        self.compose = json.loads((BROKER / "definitions.json").read_text())
        template = (ROOT / "helm/dbrepo/templates/broker-secret.yaml").read_text()
        self.helm = json.loads(template.split("  load_definition.json: |\n", 1)[1].rsplit("{{- end }}", 1)[0])

    def test_fresh_install_matches_repeatable_upgrade(self):
        for definitions in (self.compose, self.helm):
            for kind, entries in self.retention.items():
                for entry in entries:
                    with self.subTest(kind=kind, entry=entry):
                        self.assertIn(entry, definitions[kind])

    def test_retention_has_no_expiry_or_automatic_replay(self):
        queue, = self.retention["queues"]
        self.assertTrue(queue["durable"])
        self.assertFalse(queue["auto_delete"])
        self.assertEqual({"x-queue-type": "quorum", "x-delivery-limit": -1}, queue["arguments"])
        exchange, = self.retention["exchanges"]
        self.assertTrue(exchange["durable"])
        self.assertEqual("direct", exchange["type"])
        binding, = self.retention["bindings"]
        self.assertEqual(exchange["name"], binding["source"])
        self.assertEqual(queue["name"], binding["destination"])
        policy, = self.retention["policies"]
        self.assertEqual("^dbrepo$", policy["pattern"])
        self.assertEqual("queues", policy["apply-to"])
        self.assertEqual({
            "dead-letter-exchange": exchange["name"],
            "dead-letter-routing-key": binding["routing_key"],
            "dead-letter-strategy": "at-least-once",
            "overflow": "reject-publish",
            "delivery-limit": 5,
        }, policy["definition"])

    def test_source_queue_stays_quorum_and_feature_flag_is_enabled(self):
        for definitions in (self.compose, self.helm):
            source = next(queue for queue in definitions["queues"] if queue["name"] == "dbrepo")
            self.assertEqual({"x-queue-type": "quorum"}, source["arguments"])
        for path in (BROKER / "advanced.config", ROOT / "helm/dbrepo/templates/broker-secret.yaml"):
            self.assertIn("[quorum_queue, stream_queue]", path.read_text())


if __name__ == "__main__":
    unittest.main()

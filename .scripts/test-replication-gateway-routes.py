#!/usr/bin/env python3
"""Check first-match regex routing in both shipped Nginx configurations."""

from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[1]
DATABASE = "11111111-1111-4111-8111-111111111111"
TABLE = "22222222-2222-4222-8222-222222222222"
BASE = f"/api/v1/database/{DATABASE}"


class ReplicationGatewayRoutesTest(unittest.TestCase):
    def test_table_and_outbox_routes(self):
        for filename in ("dbrepo-gateway-service/dbrepo.conf", "helm/dbrepo/files/dbrepo.conf"):
            config = (ROOT / filename).read_text()
            locations = re.findall(r'location ~ "([^"]+)"\s*\{(.*?)\n\s*}', config, re.S)
            cases = {f"{BASE}/table/{TABLE}/{suffix}": "data-service"
                     for suffix in ("timestamps", "timestamps/tuple-key", "data", "data/replicate", "history", "statistic")}
            cases[f"{BASE}/replication/outbox"] = "data-service"
            cases[f"{BASE}/replication/journal?after=0&limit=100"] = "data-service"
            cases[f"{BASE}/table/{TABLE}"] = "metadata-service"
            for path, service in cases.items():
                with self.subTest(config=filename, path=path):
                    block = next((body for pattern, body in locations if re.search(pattern, path)), None)
                    self.assertIsNotNone(block, path)
                    self.assertRegex(block, rf"proxy_pass\s+http://{service}(?::8080)?;")
                    self.assertNotIn("limit_except", block)


if __name__ == "__main__":
    unittest.main()

import json
import unittest
from io import BytesIO
from unittest.mock import patch

from backfill_datacite_oai import lookup


class BackfillTest(unittest.TestCase):
    doi = "10.1234/example"
    landing = "https://repository.example.org/pid/00000000-0000-0000-0000-000000000001"

    def resolve(self, **changes):
        attributes = {"doi": self.doi, "state": "findable", "url": self.landing, **changes}
        response = {"data": {
            "type": "dois", "attributes": attributes,
            "relationships": {"client": {"data": {"type": "clients", "id": "example.test"}}},
        }}
        with patch("backfill_datacite_oai.urlopen", return_value=BytesIO(json.dumps(response).encode())):
            return lookup("https://api.datacite.org", self.doi, {self.landing})

    def test_confirmed_registration(self):
        self.assertEqual("EXAMPLE.TEST", self.resolve())

    def test_skip_unrelated_or_unpublished_registration(self):
        for changes in ({"doi": "10.1234/other"}, {"url": self.landing + "/different"},
                        {"url": None}, {"state": "draft"}):
            with self.subTest(changes=changes):
                self.assertIsNone(self.resolve(**changes))


if __name__ == "__main__":
    unittest.main()

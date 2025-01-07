import unittest
import uuid

import pytest


class UserUnitTest(unittest.TestCase):

    @pytest.fixture(autouse=True)
    def prepare_fixture(self, rest_client, database):
        self.rest_client = rest_client
        self.database = database

    @pytest.mark.usefixtures("rest_client")
    def test_create_database_succeeds(self):
        name = str(uuid.uuid4()).replace("-", "")[0:10]
        # test
        response = self.rest_client.create_database(name=name, container_id=1,
                                                    is_public=True, is_schema_public=True)
        self.assertEqual(True, response.is_public)
        self.assertEqual(True, response.is_schema_public)
        self.assertEqual(None, response.description)

    @pytest.mark.usefixtures("rest_client", "database")
    def test_update_database_visibility_succeeds(self):
        # test
        response = self.rest_client.update_database_visibility(database_id=self.database.id, is_public=False,
                                                               is_schema_public=False)
        self.assertEqual(False, response.is_public)
        self.assertEqual(False, response.is_schema_public)


if __name__ == "__main__":
    unittest.main()

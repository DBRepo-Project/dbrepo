import unittest

from app import save_metadata_user
from conftest import execute_single_result


class AppUnitTest(unittest.TestCase):

    def test_save_succeeds(self):
        # test
        save_metadata_user("7a0b4b7f-77cd-4f28-a665-2da443024621", "65408e0f-2990-42cd-9533-a667c85b46b3", "foobar",
                           "s3cr3tp455w0rd")
        user_id, keycloak_id = execute_single_result("SELECT id, keycloak_id FROM mdb_users LIMIT 1")
        self.assertEqual("7a0b4b7f-77cd-4f28-a665-2da443024621", user_id)
        self.assertEqual("65408e0f-2990-42cd-9533-a667c85b46b3", keycloak_id)

import unittest
import uuid

import pytest

from conftest import TestKeyValue
from dbrepo.RestClient import RestClient


class UserUnitTest(unittest.TestCase):

    @pytest.fixture(autouse=True)
    def prepare_fixture(self, rest_client):
        self.rest_client = rest_client

    def test_get_users_succeeds(self):
        # test
        response = RestClient().get_users()

    def test_create_user_succeeds(self):
        username = str(uuid.uuid4()).replace("-", "")[0:10]
        password = str(uuid.uuid4()).replace("-", "")[0:10]
        # test
        response = RestClient().create_user(username=f'{username}', password=f'{password}',
                                            email=f'{username}@example.com')
        self.assertEqual(username, response.username)

    @pytest.mark.usefixtures("rest_client")
    def test_update_user_succeeds(self):
        # test
        response = self.rest_client.update_user(user_id=TestKeyValue.user_id, theme='dark', language='de',
                                                firstname='Foo', lastname='Bar', affiliation='TU Wien',
                                                orcid='https://orcid.org/0000-0003-4216-302X')
        self.assertEqual('dark', response.attributes.theme)
        self.assertEqual('Foo', response.given_name)
        self.assertEqual('Bar', response.family_name)
        self.assertEqual('TU Wien', response.attributes.affiliation)
        self.assertEqual('https://orcid.org/0000-0003-4216-302X', response.attributes.orcid)


if __name__ == "__main__":
    unittest.main()

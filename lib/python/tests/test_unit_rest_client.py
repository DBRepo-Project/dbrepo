import os
import unittest

import requests_mock

from dbrepo.RestClient import RestClient


class RestClientUnitTest(unittest.TestCase):

    def test_constructor_succeeds(self):
        # test
        os.environ['REST_API_SECURE'] = 'True'
        response = RestClient()
        self.assertTrue(response.secure)

    def test_constructor_token_succeeds(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/user', json=[])
            # test
            client = RestClient(password='bar')
            client.get_users()
            self.assertEqual('bar', client.password)

    def test_whoami_anonymous_succeeds(self):
        # test
        response = RestClient().whoami()
        self.assertIsNone(response)

    def test_whoami_succeeds(self):
        # test
        response = RestClient(username="foobar").whoami()
        self.assertEqual("foobar", response)


if __name__ == "__main__":
    unittest.main()

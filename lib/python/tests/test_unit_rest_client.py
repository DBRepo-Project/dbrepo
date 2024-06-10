import os
from unittest import TestCase, mock, main

from dbrepo.RestClient import RestClient


class DatabaseUnitTest(TestCase):

    def test_constructor_succeeds(self):
        # test
        client = RestClient()
        self.assertEqual("http://gateway-service", client.endpoint)
        self.assertIsNone(client.username)
        self.assertIsNone(client.password)
        self.assertTrue(client.secure)

    @mock.patch.dict(os.environ, {
        "REST_API_ENDPOINT": "https://test.dbrepo.tuwien.ac.at",
        "REST_API_USERNAME": "foo",
        "REST_API_PASSWORD": "bar",
        "REST_API_SECURE": "false",
    })
    def test_constructor_environment_succeeds(self):
        # test
        client = RestClient()
        self.assertEqual("https://test.dbrepo.tuwien.ac.at", client.endpoint)
        self.assertEqual("foo", client.username)
        self.assertEqual("bar", client.password)
        self.assertFalse(client.secure)

    def test_constructor_credentials_succeeds(self):
        # test
        client = RestClient(username='admin', password='pass')
        self.assertEqual("http://gateway-service", client.endpoint)
        self.assertEqual('admin', client.username)
        self.assertEqual('pass', client.password)
        self.assertTrue(client.secure)


if __name__ == "__main__":
    main()

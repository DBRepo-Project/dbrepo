import os
import unittest

from dbrepo.RestClient import RestClient


class RestClientUnitTest(unittest.TestCase):

    def test_constructor_succeeds(self):
        # test
        os.environ['REST_API_SECURE'] = 'True'
        response = RestClient()
        self.assertTrue(response.secure)

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

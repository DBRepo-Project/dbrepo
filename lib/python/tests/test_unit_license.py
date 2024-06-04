import unittest

import requests_mock

from dbrepo.RestClient import RestClient

from dbrepo.api.dto import License


class DatabaseUnitTest(unittest.TestCase):

    def test_get_licenses_empty_succeeds(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/license', json=[])
            # test
            response = RestClient().get_licenses()
            self.assertEqual([], response)

    def test_get_licenses_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [License(identifier='CC-BY-4.0', uri='https://creativecommons.org/licenses/by/4.0/',
                           description='The Creative Commons Attribution license allows re-distribution and re-use of a licensed work on the condition that the creator is appropriately credited.')]
            # mock
            mock.get('/api/database/license', json=[exp[0].model_dump()])
            # test
            response = RestClient().get_licenses()
            self.assertEqual(exp, response)


if __name__ == "__main__":
    unittest.main()

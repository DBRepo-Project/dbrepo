import unittest
import requests_mock
import dataclasses

from dbrepo.RestClient import RestClient

from dbrepo.api.dto import KeyAnalysis


class AnalyseUnitTest(unittest.TestCase):

    def test_analyse_keys_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = KeyAnalysis(keys={'id': 0, 'firstname': 1, 'lastname': 2})
            # mock
            mock.get('/api/analyse/keys', json=exp.model_dump(), status_code=202)
            # test
            response = RestClient().analyse_keys(file_path='f705a7bd0cb2d5e37ab2b425036810a2', separator=',',
                                                 upload=False)
            self.assertEqual(exp, response)


if __name__ == "__main__":
    unittest.main()

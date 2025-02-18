import os
import re
import unittest

import requests_mock
from pandas import DataFrame

from dbrepo.RestClient import RestClient
from dbrepo.api.dto import KeyAnalysis, DatatypeAnalysis, ColumnType
from dbrepo.api.exceptions import NotExistsError, MalformedError, ResponseCodeError


class AnalyseUnitTest(unittest.TestCase):

    def test_analyse_keys_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = KeyAnalysis(keys={'id': 0, 'firstname': 1, 'lastname': 2})
            # mock
            mock.register_uri('POST', os.environ.get('REST_UPLOAD_ENDPOINT'), real_http=True)
            mock.register_uri('GET', '/api/analyse/keys', json=exp.model_dump(), status_code=202)
            # test
            response = RestClient().analyse_keys(dataframe=DataFrame())
            self.assertEqual(exp, response)

    def test_analyse_keys_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.register_uri('POST', os.environ.get('REST_UPLOAD_ENDPOINT'), real_http=True)
            mock.register_uri('GET', '/api/analyse/keys', status_code=400)
            # test
            try:
                RestClient().analyse_keys(dataframe=DataFrame())
            except MalformedError:
                pass

    def test_analyse_keys_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.register_uri('POST', os.environ.get('REST_UPLOAD_ENDPOINT'), real_http=True)
            mock.register_uri('GET', '/api/analyse/keys', status_code=404)
            # test
            try:
                RestClient().analyse_keys(dataframe=DataFrame())
            except NotExistsError:
                pass

    def test_analyse_keys_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.register_uri('POST', os.environ.get('REST_UPLOAD_ENDPOINT'), real_http=True)
            mock.register_uri('GET', '/api/analyse/keys', status_code=200)
            # test
            try:
                RestClient().analyse_keys(dataframe=DataFrame())
            except ResponseCodeError:
                pass

    def test_analyse_datatypes_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = DatatypeAnalysis(separator=',',
                                   columns={'id': ColumnType.SERIAL})
            # mock
            mock.register_uri('POST', os.environ.get('REST_UPLOAD_ENDPOINT'), real_http=True)
            mock.register_uri('GET', re.compile('/api/analyse/datatypes.*'), json=exp.model_dump(), status_code=202)
            # test
            response = RestClient().analyse_datatypes(dataframe=DataFrame())
            self.assertEqual(exp, response)

    def test_analyse_datatypes_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.register_uri('POST', os.environ.get('REST_UPLOAD_ENDPOINT'), real_http=True)
            mock.register_uri('GET', re.compile('/api/analyse/datatypes.*'), status_code=400)
            # test
            try:
                RestClient().analyse_datatypes(dataframe=DataFrame())
            except MalformedError:
                pass

    def test_analyse_datatypes_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.register_uri('POST', os.environ.get('REST_UPLOAD_ENDPOINT'), real_http=True)
            mock.register_uri('GET', re.compile('/api/analyse/datatypes.*'), status_code=404)
            # test
            try:
                RestClient().analyse_datatypes(dataframe=DataFrame())
            except NotExistsError:
                pass

    def test_analyse_datatypes_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.register_uri('POST', os.environ.get('REST_UPLOAD_ENDPOINT'), real_http=True)
            mock.register_uri('GET', re.compile('/api/analyse/datatypes.*'), status_code=200)
            # test
            try:
                RestClient().analyse_datatypes(dataframe=DataFrame())
            except ResponseCodeError:
                pass


if __name__ == "__main__":
    unittest.main()

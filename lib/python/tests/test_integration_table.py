import os
import unittest

import requests_mock
from pandas import DataFrame

from dbrepo.RestClient import RestClient
from dbrepo.api.exceptions import ForbiddenError, MalformedError, NotExistsError, ServiceError, ResponseCodeError


class TableUnitTest(unittest.TestCase):

    def test_import_table_data_succeeds(self):
        with requests_mock.Mocker() as mock:
            mock.register_uri('POST', os.environ.get('REST_UPLOAD_ENDPOINT'), real_http=True)
            # mock
            mock.post('/api/database/1/table/9/data/import', status_code=202)
            # test
            RestClient(username="a", password="b").import_table_data(database_id=1, table_id=9,
                                                                     dataframe=DataFrame())

    def test_import_table_data_400_fails(self):
        with requests_mock.Mocker() as mock:
            mock.register_uri('POST', os.environ.get('REST_UPLOAD_ENDPOINT'), real_http=True)
            # mock
            mock.post('/api/database/1/table/9/data/import', status_code=400)
            try:
                RestClient(username="a", password="b").import_table_data(database_id=1, table_id=9,
                                                                         dataframe=DataFrame())
            except MalformedError:
                pass

    def test_import_table_data_403_fails(self):
        with requests_mock.Mocker() as mock:
            mock.register_uri('POST', os.environ.get('REST_UPLOAD_ENDPOINT'), real_http=True)
            # mock
            mock.post('/api/database/1/table/9/data/import', status_code=403)
            # test
            try:
                RestClient(username="a", password="b").import_table_data(database_id=1, table_id=9,
                                                                         dataframe=DataFrame())
            except ForbiddenError:
                pass

    def test_import_table_data_404_fails(self):
        with requests_mock.Mocker() as mock:
            mock.register_uri('POST', os.environ.get('REST_UPLOAD_ENDPOINT'), real_http=True)
            # mock
            mock.post('/api/database/1/table/9/data/import', status_code=404)
            # test
            try:
                RestClient(username="a", password="b").import_table_data(database_id=1, table_id=9,
                                                                         dataframe=DataFrame())
            except NotExistsError:
                pass

    def test_import_table_data_503_fails(self):
        with requests_mock.Mocker() as mock:
            mock.register_uri('POST', os.environ.get('REST_UPLOAD_ENDPOINT'), real_http=True)
            # mock
            mock.post('/api/database/1/table/9/data/import', status_code=503)
            # test
            try:
                RestClient(username="a", password="b").import_table_data(database_id=1, table_id=9,
                                                                         dataframe=DataFrame())
            except ServiceError:
                pass

    def test_import_table_data_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            mock.register_uri('POST', os.environ.get('REST_UPLOAD_ENDPOINT'), real_http=True)
            # mock
            mock.post('/api/database/1/table/9/data/import', status_code=200)
            # test
            try:
                RestClient(username="a", password="b").import_table_data(database_id=1, table_id=9,
                                                                         dataframe=DataFrame())
            except ResponseCodeError:
                pass


if __name__ == "__main__":
    unittest.main()

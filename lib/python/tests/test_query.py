import unittest
from json import dumps
from typing import Any

import requests_mock
import datetime

from dbrepo.RestClient import RestClient
from pandas import DataFrame

from dbrepo.api.dto import Result, Query, User, UserAttributes, QueryType
from dbrepo.api.exceptions import MalformedError, NotExistsError, ForbiddenError, QueryStoreError, \
    MetadataConsistencyError, AuthenticationError


class QueryTest(unittest.TestCase):

    def test_execute_query_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = Result(result=[{'id': 1, 'username': 'foo'}, {'id': 2, 'username': 'bar'}],
                         headers=[{'id': 0, 'username': 1}],
                         id=None)
            # mock
            mock.post('/api/database/1/query', json=exp.model_dump(), status_code=202)
            # test
            client = RestClient(username="a", password="b")
            response = client.execute_query(database_id=1, page=0, size=10,
                                            query="SELECT id, username FROM some_table WHERE id IN (1,2)")
            self.assertEqual(exp, response)

    def test_execute_query_malformed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/1/query', status_code=400)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.execute_query(database_id=1,
                                                query="SELECT id, username FROM some_table WHERE id IN (1,2)")
            except MalformedError:
                pass

    def test_execute_query_not_allowed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/1/query', status_code=403)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.execute_query(database_id=1,
                                                query="SELECT id, username FROM some_table WHERE id IN (1,2)")
            except ForbiddenError:
                pass

    def test_execute_query_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/1/query', status_code=404)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.execute_query(database_id=1,
                                                query="SELECT id, username FROM some_table WHERE id IN (1,2)")
            except NotExistsError:
                pass

    def test_execute_query_not_valid_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/1/query', status_code=409)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.execute_query(database_id=1,
                                                query="SELECT id, username FROM some_table WHERE id IN (1,2)")
            except QueryStoreError:
                pass

    def test_execute_query_not_expected_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/1/query', status_code=417)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.execute_query(database_id=1,
                                                query="SELECT id, username FROM some_table WHERE id IN (1,2)")
            except MetadataConsistencyError:
                pass

    def test_execute_query_not_auth_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/1/query', status_code=417)
            # test
            try:
                response = RestClient().execute_query(database_id=1,
                                                      query="SELECT id, username FROM some_table WHERE id IN (1,2)")
            except AuthenticationError:
                pass

    def test_find_query_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = Query(id=6,
                        creator=User(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise',
                                     attributes=UserAttributes(theme='light')),
                        execution=datetime.datetime(2024, 1, 1, 0, 0, 0, 0, datetime.timezone.utc),
                        created=datetime.datetime(2024, 1, 1, 0, 0, 0, 0, datetime.timezone.utc),
                        last_modified=datetime.datetime(2024, 1, 1, 0, 0, 0, 0, datetime.timezone.utc),
                        query='SELECT id, username FROM some_table WHERE id IN (1,2)',
                        query_normalized='SELECT id, username FROM some_table WHERE id IN (1,2)',
                        type=QueryType.QUERY,
                        database_id=1,
                        query_hash='da5ff66c4a57683171e2ffcec25298ee684680d1e03633cd286f9067d6924ad8',
                        result_hash='464740ba612225913bb15b26f13377707949b55e65288e89c3f8b4c6469aecb4',
                        is_persisted=False,
                        result_number=None,
                        identifiers=[])
            # mock
            mock.get('/api/database/1/query/6', json=exp.model_dump())
            # test
            response = RestClient().get_query(database_id=1, query_id=6)
            self.assertEqual(exp, response)

    def test_find_query_not_allowed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/1/query/6', status_code=403)
            # test
            try:
                response = RestClient().get_query(database_id=1, query_id=6)
            except ForbiddenError:
                pass

    def test_find_query_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/1/query/6', status_code=404)
            # test
            try:
                response = RestClient().get_query(database_id=1, query_id=6)
            except NotExistsError:
                pass

    def test_find_query_not_valid_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/1/query/6', status_code=501)
            # test
            try:
                response = RestClient().get_query(database_id=1, query_id=6)
            except QueryStoreError:
                pass

    def test_find_query_not_expected_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/1/query/6', status_code=417)
            # test
            try:
                response = RestClient().get_query(database_id=1, query_id=6)
            except MetadataConsistencyError:
                pass

    def test_get_queries_empty_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = []
            # mock
            mock.get('/api/database/1/query', json=[])
            # test
            response = RestClient().get_queries(database_id=1)
            self.assertEqual(exp, response)

    def test_get_queries_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [Query(id=6,
                         creator=User(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise',
                                      attributes=UserAttributes(theme='light')),
                         execution=datetime.datetime(2024, 1, 1, 0, 0, 0, 0, datetime.timezone.utc),
                         created=datetime.datetime(2024, 1, 1, 0, 0, 0, 0, datetime.timezone.utc),
                         last_modified=datetime.datetime(2024, 1, 1, 0, 0, 0, 0, datetime.timezone.utc),
                         query='SELECT id, username FROM some_table WHERE id IN (1,2)',
                         query_normalized='SELECT id, username FROM some_table WHERE id IN (1,2)',
                         type=QueryType.QUERY,
                         database_id=1,
                         query_hash='da5ff66c4a57683171e2ffcec25298ee684680d1e03633cd286f9067d6924ad8',
                         result_hash='464740ba612225913bb15b26f13377707949b55e65288e89c3f8b4c6469aecb4',
                         is_persisted=False,
                         result_number=None,
                         identifiers=[])]
            # mock
            mock.get('/api/database/1/query', json=[exp[0].model_dump()])
            # test
            response = RestClient().get_queries(database_id=1)
            self.assertEqual(exp, response)

    def test_get_queries_not_allowed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/1/query', status_code=403)
            # test
            try:
                response = RestClient().get_queries(database_id=1)
            except ForbiddenError:
                pass

    def test_get_queries_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/1/query', status_code=404)
            # test
            try:
                response = RestClient().get_queries(database_id=1)
            except NotExistsError:
                pass

    def test_get_queries_not_valid_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/1/query', status_code=501)
            # test
            try:
                response = RestClient().get_queries(database_id=1)
            except QueryStoreError:
                pass

    def test_get_queries_malformed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/1/query', status_code=423)
            # test
            try:
                response = RestClient().get_queries(database_id=1)
            except MalformedError:
                pass

    def test_get_query_data_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = Result(result=[{'id': 1, 'username': 'foo'}, {'id': 2, 'username': 'bar'}],
                         headers=[{'id': 0, 'username': 1}],
                         id=6)
            # mock
            mock.get('/api/database/1/query/6/data', json=exp.model_dump())
            # test
            response = RestClient().get_query_data(database_id=1, query_id=6)
            self.assertEqual(exp, response)

    def test_get_query_data_dataframe_succeeds(self):
        with requests_mock.Mocker() as mock:
            res = Result(result=[{'id': 1, 'username': 'foo'}, {'id': 2, 'username': 'bar'}],
                         headers=[{'id': 0, 'username': 1}],
                         id=6)
            exp = DataFrame.from_records(res.model_dump()['result'])
            # mock
            mock.get('/api/database/1/query/6/data', json=res.model_dump())
            # test
            response = RestClient().get_query_data(database_id=1, query_id=6, df=True)
            self.assertEqual(exp.shape, response.shape)
            self.assertTrue(DataFrame.equals(exp, response))

    def test_get_query_data_not_allowed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/1/query/6/data', status_code=403)
            # test
            try:
                response = RestClient().get_query_data(database_id=1, query_id=6)
            except ForbiddenError:
                pass

    def test_get_query_data_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/1/query/6/data', status_code=404)
            # test
            try:
                response = RestClient().get_query_data(database_id=1, query_id=6)
            except NotExistsError:
                pass

    def test_get_query_data_not_valid_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/1/query/6/data', status_code=409)
            # test
            try:
                response = RestClient().get_query_data(database_id=1, query_id=6)
            except QueryStoreError:
                pass

    def test_get_query_data_not_consistent_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/1/query/6/data', status_code=417)
            # test
            try:
                response = RestClient().get_query_data(database_id=1, query_id=6)
            except MetadataConsistencyError:
                pass

    def test_get_query_data_count_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = 2
            # mock
            mock.head('/api/database/1/query/6/data', headers={'X-Count': str(exp)})
            # test
            response = RestClient().get_query_data_count(database_id=1, query_id=6)
            self.assertEqual(exp, response)

    def test_get_query_data_count_not_allowed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.head('/api/database/1/query/6/data', status_code=403)
            # test
            try:
                response = RestClient().get_query_data_count(database_id=1, query_id=6)
            except ForbiddenError:
                pass

    def test_get_query_data_count_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.head('/api/database/1/query/6/data', status_code=404)
            # test
            try:
                response = RestClient().get_query_data_count(database_id=1, query_id=6)
            except NotExistsError:
                pass

    def test_get_query_data_count_not_valid_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.head('/api/database/1/query/6/data', status_code=409)
            # test
            try:
                response = RestClient().get_query_data_count(database_id=1, query_id=6)
            except QueryStoreError:
                pass

    def test_get_query_data_count_not_consistent_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.head('/api/database/1/query/6/data', status_code=417)
            # test
            try:
                response = RestClient().get_query_data_count(database_id=1, query_id=6)
            except MetadataConsistencyError:
                pass


if __name__ == "__main__":
    unittest.main()

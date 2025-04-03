import datetime
import json
import unittest

import requests_mock
from pandas import DataFrame

from dbrepo.RestClient import RestClient
from dbrepo.api.dto import Table, Column, Constraints, ColumnType, ConceptBrief, UnitBrief, \
    TableStatistics, ColumnStatistic, PrimaryKey, ColumnBrief, TableBrief, UserBrief, History, HistoryEventType
from dbrepo.api.exceptions import MalformedError, ForbiddenError, NotExistsError, NameExistsError, \
    AuthenticationError, ExternalSystemError, ServiceError, ServiceConnectionError, ResponseCodeError


class TableUnitTest(unittest.TestCase):

    def test_create_table_succeeds(self):
        exp = TableBrief(id="029d773f-f98b-40c0-ab22-b8b1635d4fbc",
                         database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                         name="Test",
                         description="Test Table",
                         internal_name="test",
                         owned_by='8638c043-5145-4be8-a3e4-4b79991b0a16',
                         is_versioned=True,
                         is_public=True,
                         is_schema_public=True)
        with requests_mock.Mocker() as mock:
            dataframe = DataFrame.from_records([{'id': 1, 'name': 'foobar'}], index=['id'])
            # mock
            mock.post('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table', json=exp.model_dump(),
                      status_code=201)
            # test
            client = RestClient(username="a", password="b")
            response = client.create_table(database_id="6bd39359-b154-456d-b9c2-caa516a45732", name="Test",
                                           description="Test Table", dataframe=dataframe, with_data=False,
                                           is_public=True, is_schema_public=True)
            self.assertEqual(exp, response)

    def test_create_table_index_missing_fails(self):
        with requests_mock.Mocker() as mock:
            dataframe = DataFrame.from_records([{'id': 1, 'name': 'foobar'}])
            # test
            client = RestClient(username="a", password="b")
            try:
                client.create_table(database_id="6bd39359-b154-456d-b9c2-caa516a45732", name="Test",
                                    description="Test Table", dataframe=dataframe, with_data=False,
                                    is_public=True, is_schema_public=True)
            except MalformedError:
                pass

    def test_create_table_400_fails(self):
        with requests_mock.Mocker() as mock:
            dataframe = DataFrame.from_records([{'id': 1, 'name': 'foobar'}], index=['id'])
            # mock
            mock.post('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table', status_code=400)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.create_table(database_id="6bd39359-b154-456d-b9c2-caa516a45732", name="Test",
                                               description="Test Table", dataframe=dataframe,
                                               is_public=True, is_schema_public=True)
            except MalformedError:
                pass

    def test_create_table_403_fails(self):
        with requests_mock.Mocker() as mock:
            dataframe = DataFrame.from_records([{'id': 1, 'name': 'foobar'}], index=['id'])
            # mock
            mock.post('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table', status_code=403)
            # test
            try:
                RestClient(username="a", password="b").create_table(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                                    name="Test",
                                                                    description="Test Table", dataframe=dataframe,
                                                                    is_public=True, is_schema_public=True)
            except ForbiddenError:
                pass

    def test_create_table_404_fails(self):
        with requests_mock.Mocker() as mock:
            dataframe = DataFrame.from_records([{'id': 1, 'name': 'foobar'}], index=['id'])
            # mock
            mock.post('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table', status_code=404)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.create_table(database_id="6bd39359-b154-456d-b9c2-caa516a45732", name="Test",
                                               description="Test Table", dataframe=dataframe,
                                               is_public=True, is_schema_public=True)
            except NotExistsError:
                pass

    def test_create_table_409_fails(self):
        with requests_mock.Mocker() as mock:
            dataframe = DataFrame.from_records([{'id': 1, 'name': 'foobar'}], index=['id'])
            # mock
            mock.post('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table', status_code=409)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.create_table(database_id="6bd39359-b154-456d-b9c2-caa516a45732", name="Test",
                                               description="Test Table", dataframe=dataframe,
                                               is_public=True, is_schema_public=True)
            except NameExistsError:
                pass

    def test_create_table_502_fails(self):
        with requests_mock.Mocker() as mock:
            dataframe = DataFrame.from_records([{'id': 1, 'name': 'foobar'}], index=['id'])
            # mock
            mock.post('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table', status_code=502)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.create_table(database_id="6bd39359-b154-456d-b9c2-caa516a45732", name="Test",
                                               description="Test Table", dataframe=dataframe,
                                               is_public=True, is_schema_public=True)
            except ServiceConnectionError:
                pass

    def test_create_table_503_fails(self):
        with requests_mock.Mocker() as mock:
            dataframe = DataFrame.from_records([{'id': 1, 'name': 'foobar'}], index=['id'])
            # mock
            mock.post('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table', status_code=503)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.create_table(database_id="6bd39359-b154-456d-b9c2-caa516a45732", name="Test",
                                               description="Test Table", dataframe=dataframe,
                                               is_public=True, is_schema_public=True)
            except ServiceError:
                pass

    def test_create_table_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            dataframe = DataFrame.from_records([{'id': 1, 'name': 'foobar'}], index=['id'])
            # mock
            mock.post('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table', status_code=200)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.create_table(database_id="6bd39359-b154-456d-b9c2-caa516a45732", name="Test",
                                               description="Test Table", dataframe=dataframe,
                                               is_public=True, is_schema_public=True)
            except ResponseCodeError:
                pass

    def test_create_table_anonymous_fails(self):
        with requests_mock.Mocker() as mock:
            dataframe = DataFrame.from_records([{'id': 1, 'name': 'foobar'}], index=['id'])
            # mock
            mock.post('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table', status_code=409)
            # test
            try:
                response = RestClient().create_table(database_id="6bd39359-b154-456d-b9c2-caa516a45732", name="Test",
                                                     description="Test Table", dataframe=dataframe,
                                                     is_public=True, is_schema_public=True)
            except AuthenticationError:
                pass

    def test_get_tables_empty_succeeds(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table', json=[])
            # test
            response = RestClient().get_tables(database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            self.assertEqual([], response)

    def test_get_tables_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [TableBrief(id="029d773f-f98b-40c0-ab22-b8b1635d4fbc",
                              name="Test",
                              description="Test Table",
                              database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                              internal_name="test",
                              is_public=True,
                              is_schema_public=True,
                              owned_by='8638c043-5145-4be8-a3e4-4b79991b0a16',
                              is_versioned=True)]
            # mock
            mock.get('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table', json=[exp[0].model_dump()])
            # test
            response = RestClient().get_tables(database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            self.assertEqual(exp, response)

    def test_get_tables_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table', status_code=403)
            # test
            try:
                RestClient().get_tables(database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            except ForbiddenError:
                pass

    def test_get_tables_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table', status_code=404)
            # test
            try:
                RestClient().get_tables(database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            except NotExistsError:
                pass

    def test_get_tables_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table', status_code=202)
            # test
            try:
                RestClient().get_tables(database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            except ResponseCodeError:
                pass

    def test_get_table_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = Table(id="029d773f-f98b-40c0-ab22-b8b1635d4fbc",
                        name="Test",
                        description="Test Table",
                        database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                        internal_name="test",
                        owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'),
                        is_versioned=True,
                        queue_name='test',
                        routing_key='dbrepo.test_database_1234.test',
                        is_public=True,
                        is_schema_public=True,
                        constraints=Constraints(uniques=[],
                                                foreign_keys=[],
                                                checks=[],
                                                primary_key=[PrimaryKey(id="1516310f-ecb5-4614-abe2-3b96114e1484",
                                                                        table=TableBrief(
                                                                            id="029d773f-f98b-40c0-ab22-b8b1635d4fbc",
                                                                            database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                                            name='Other',
                                                                            internal_name='other',
                                                                            description=None,
                                                                            is_versioned=True,
                                                                            is_public=True,
                                                                            is_schema_public=True,
                                                                            owned_by='8638c043-5145-4be8-a3e4-4b79991b0a16'),
                                                                        column=ColumnBrief(
                                                                            id="31a533b6-8ddf-43d6-ac6a-b9da597cb976",
                                                                            table_id="",
                                                                            database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                                            name='id',
                                                                            alias=None,
                                                                            internal_name='id',
                                                                            type=ColumnType.BIGINT))]),
                        columns=[Column(id="31a533b6-8ddf-43d6-ac6a-b9da597cb976",
                                        name="ID",
                                        ord=0,
                                        database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                        table_id="029d773f-f98b-40c0-ab22-b8b1635d4fbc",
                                        internal_name="id",
                                        type=ColumnType.BIGINT,
                                        is_null_allowed=False)])
            # mock
            mock.get('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692',
                     json=exp.model_dump())
            # test
            response = RestClient().get_table(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                              table_id="b3230b86-4743-498d-9015-3fad58049692")
            self.assertEqual(exp, response)

    def test_get_table_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692',
                     status_code=403)
            # test
            try:
                response = RestClient().get_table(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                  table_id="b3230b86-4743-498d-9015-3fad58049692")
            except ForbiddenError:
                pass

    def test_get_table_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692',
                     status_code=404)
            # test
            try:
                response = RestClient().get_table(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                  table_id="b3230b86-4743-498d-9015-3fad58049692")
            except NotExistsError:
                pass

    def test_get_table_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692',
                     status_code=202)
            # test
            try:
                response = RestClient().get_table(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                  table_id="b3230b86-4743-498d-9015-3fad58049692")
            except ResponseCodeError:
                pass

    def test_delete_table_succeeds(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692',
                        status_code=202)
            # test
            client = RestClient(username="a", password="b")
            client.delete_table(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                table_id="b3230b86-4743-498d-9015-3fad58049692")

    def test_delete_table_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692',
                        status_code=400)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.delete_table(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                    table_id="b3230b86-4743-498d-9015-3fad58049692")
            except MalformedError:
                pass

    def test_delete_table_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692',
                        status_code=403)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.delete_table(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                    table_id="b3230b86-4743-498d-9015-3fad58049692")
            except ForbiddenError:
                pass

    def test_delete_table_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692',
                        status_code=404)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.delete_table(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                    table_id="b3230b86-4743-498d-9015-3fad58049692")
            except NotExistsError:
                pass

    def test_delete_table_502_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692',
                        status_code=502)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.delete_table(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                    table_id="b3230b86-4743-498d-9015-3fad58049692")
            except ServiceConnectionError:
                pass

    def test_delete_table_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692',
                        status_code=503)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.delete_table(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                    table_id="b3230b86-4743-498d-9015-3fad58049692")
            except ServiceError:
                pass

    def test_delete_table_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692',
                        status_code=200)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.delete_table(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                    table_id="b3230b86-4743-498d-9015-3fad58049692")
            except ResponseCodeError:
                pass

    def test_delete_table_anonymous_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692',
                        status_code=404)
            # test
            try:
                RestClient().delete_table(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                          table_id="b3230b86-4743-498d-9015-3fad58049692")
            except AuthenticationError:
                pass

    def test_get_table_history_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [History(event=HistoryEventType.INSERT,
                           total=2,
                           timestamp=datetime.datetime(2024, 1, 1, 0, 0, 0, 0, datetime.timezone.utc))]
            # mock
            mock.get(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/history?size=100',
                json=[exp[0].model_dump()])
            # test
            response = RestClient().get_table_history(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                      table_id="b3230b86-4743-498d-9015-3fad58049692")
            self.assertEqual(exp, response)

    def test_get_table_history_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/history?size=100',
                status_code=400)
            # test
            try:
                RestClient().get_table_history(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                               table_id="b3230b86-4743-498d-9015-3fad58049692")
            except MalformedError:
                pass

    def test_get_table_history_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/history?size=100',
                status_code=403)
            # test
            try:
                RestClient().get_table_history(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                               table_id="b3230b86-4743-498d-9015-3fad58049692")
            except ForbiddenError:
                pass

    def test_get_table_history_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/history?size=100',
                status_code=404)
            # test
            try:
                RestClient().get_table_history(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                               table_id="b3230b86-4743-498d-9015-3fad58049692")
            except NotExistsError:
                pass

    def test_get_table_history_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/history?size=100',
                status_code=503)
            # test
            try:
                RestClient().get_table_history(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                               table_id="b3230b86-4743-498d-9015-3fad58049692")
            except ServiceError:
                pass

    def test_get_table_history_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/history?size=100',
                status_code=202)
            # test
            try:
                RestClient().get_table_history(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                               table_id="b3230b86-4743-498d-9015-3fad58049692")
            except ResponseCodeError:
                pass

    def test_get_table_data_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [{'id': 1, 'username': 'foo'}, {'id': 2, 'username': 'bar'}]
            df = DataFrame.from_records(json.dumps(exp))
            # mock
            mock.get(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                json=json.dumps(exp))
            # test
            response = RestClient().get_table_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                   table_id="b3230b86-4743-498d-9015-3fad58049692",
                                                   timestamp=datetime.datetime(2024, 1, 1, 0, 0, 0, 0,
                                                                               datetime.timezone.utc))
            self.assertTrue(DataFrame.equals(df, response))

    def test_get_table_data_dataframe_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [{'id': 1, 'username': 'foo'}, {'id': 2, 'username': 'bar'}]
            df = DataFrame.from_records(json.dumps(exp))
            # mock
            mock.get(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                json=json.dumps(exp))
            # test
            response = RestClient().get_table_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                   table_id="b3230b86-4743-498d-9015-3fad58049692")
            self.assertEqual(df.shape, response.shape)
            self.assertTrue(DataFrame.equals(df, response))

    def test_get_table_data_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=400)
            # test
            try:
                response = RestClient().get_table_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                       table_id="b3230b86-4743-498d-9015-3fad58049692")
            except MalformedError:
                pass

    def test_get_table_data_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=403)
            # test
            try:
                response = RestClient().get_table_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                       table_id="b3230b86-4743-498d-9015-3fad58049692")
            except ForbiddenError:
                pass

    def test_get_table_data_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=404)
            # test
            try:
                response = RestClient().get_table_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                       table_id="b3230b86-4743-498d-9015-3fad58049692")
            except NotExistsError:
                pass

    def test_get_table_data_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=503)
            # test
            try:
                response = RestClient().get_table_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                       table_id="b3230b86-4743-498d-9015-3fad58049692")
            except ServiceError:
                pass

    def test_get_table_data_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=202)
            # test
            try:
                response = RestClient().get_table_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                       table_id="b3230b86-4743-498d-9015-3fad58049692")
            except ResponseCodeError:
                pass

    def test_get_table_data_count_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = 2
            # mock
            mock.head(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                headers={'X-Count': str(exp)})
            # test
            response = RestClient().get_table_data_count(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                         table_id="b3230b86-4743-498d-9015-3fad58049692",
                                                         timestamp=datetime.datetime(2024, 1, 1, 0, 0, 0, 0))
            self.assertEqual(exp, response)

    def test_get_table_data_count_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.head(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=400)
            # test
            try:
                response = RestClient().get_table_data_count(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                             table_id="b3230b86-4743-498d-9015-3fad58049692")
            except MalformedError:
                pass

    def test_get_table_data_count_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.head(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=403)
            # test
            try:
                response = RestClient().get_table_data_count(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                             table_id="b3230b86-4743-498d-9015-3fad58049692")
            except ForbiddenError:
                pass

    def test_get_table_data_count_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.head(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=404)
            # test
            try:
                response = RestClient().get_table_data_count(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                             table_id="b3230b86-4743-498d-9015-3fad58049692")
            except NotExistsError:
                pass

    def test_get_table_data_count_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.head(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=503)
            # test
            try:
                response = RestClient().get_table_data_count(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                             table_id="b3230b86-4743-498d-9015-3fad58049692")
            except ServiceError:
                pass

    def test_get_table_data_count_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.head(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=202)
            # test
            try:
                response = RestClient().get_table_data_count(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                             table_id="b3230b86-4743-498d-9015-3fad58049692")
            except ResponseCodeError:
                pass

    def test_get_table_data_count_not_countable_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.head(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=409)
            # test
            try:
                response = RestClient().get_table_data_count(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                             table_id="b3230b86-4743-498d-9015-3fad58049692")
            except ExternalSystemError:
                pass

    def test_create_table_data_succeeds(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=201)
            # test
            client = RestClient(username="a", password="b")
            client.create_table_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                     table_id="b3230b86-4743-498d-9015-3fad58049692",
                                     data={'name': 'Josiah', 'age': 45, 'gender': 'male'})

    def test_create_table_data_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=400)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.create_table_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                         table_id="b3230b86-4743-498d-9015-3fad58049692",
                                         data={'name': 'Josiah', 'age': 45, 'gender': 'male'})
            except MalformedError:
                pass

    def test_create_table_data_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=403)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.create_table_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                         table_id="b3230b86-4743-498d-9015-3fad58049692",
                                         data={'name': 'Josiah', 'age': 45, 'gender': 'male'})
            except ForbiddenError:
                pass

    def test_create_table_data_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=404)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.create_table_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                         table_id="b3230b86-4743-498d-9015-3fad58049692",
                                         data={'name': 'Josiah', 'age': 45, 'gender': 'male'})
            except NotExistsError:
                pass

    def test_create_table_data_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=503)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.create_table_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                         table_id="b3230b86-4743-498d-9015-3fad58049692",
                                         data={'name': 'Josiah', 'age': 45, 'gender': 'male'})
            except ServiceError:
                pass

    def test_create_table_data_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=200)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.create_table_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                         table_id="b3230b86-4743-498d-9015-3fad58049692",
                                         data={'name': 'Josiah', 'age': 45, 'gender': 'male'})
            except ResponseCodeError:
                pass

    def test_create_table_data_anonymous_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=503)
            # test
            try:
                RestClient().create_table_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                               table_id="b3230b86-4743-498d-9015-3fad58049692",
                                               data={'name': 'Josiah', 'age': 45, 'gender': 'male'})
            except AuthenticationError:
                pass

    def test_update_table_data_succeeds(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=202)
            # test
            client = RestClient(username="a", password="b")
            client.update_table_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                     table_id="b3230b86-4743-498d-9015-3fad58049692",
                                     data={'name': 'Josiah', 'age': 45, 'gender': 'male'},
                                     keys={'id': 1})

    def test_update_table_data_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=400)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.update_table_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                         table_id="b3230b86-4743-498d-9015-3fad58049692",
                                         data={'name': 'Josiah', 'age': 45, 'gender': 'male'},
                                         keys={'id': 1})
            except MalformedError:
                pass

    def test_update_table_data_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=403)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.update_table_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                         table_id="b3230b86-4743-498d-9015-3fad58049692",
                                         data={'name': 'Josiah', 'age': 45, 'gender': 'male'},
                                         keys={'id': 1})
            except ForbiddenError:
                pass

    def test_update_table_data_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=404)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.update_table_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                         table_id="b3230b86-4743-498d-9015-3fad58049692",
                                         data={'name': 'Josiah', 'age': 45, 'gender': 'male'},
                                         keys={'id': 1})
            except NotExistsError:
                pass

    def test_update_table_data_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=503)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.update_table_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                         table_id="b3230b86-4743-498d-9015-3fad58049692",
                                         data={'name': 'Josiah', 'age': 45, 'gender': 'male'},
                                         keys={'id': 1})
            except ServiceError:
                pass

    def test_update_table_data_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=200)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.update_table_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                         table_id="b3230b86-4743-498d-9015-3fad58049692",
                                         data={'name': 'Josiah', 'age': 45, 'gender': 'male'},
                                         keys={'id': 1})
            except ResponseCodeError:
                pass

    def test_delete_table_data_succeeds(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=202)
            # test
            client = RestClient(username="a", password="b")
            client.delete_table_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                     table_id="b3230b86-4743-498d-9015-3fad58049692", keys={'id': 1})

    def test_delete_table_data_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=400)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.delete_table_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                         table_id="b3230b86-4743-498d-9015-3fad58049692", keys={'id': 1})
            except MalformedError:
                pass

    def test_delete_table_data_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=403)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.delete_table_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                         table_id="b3230b86-4743-498d-9015-3fad58049692", keys={'id': 1})
            except ForbiddenError:
                pass

    def test_delete_table_data_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=404)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.delete_table_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                         table_id="b3230b86-4743-498d-9015-3fad58049692", keys={'id': 1})
            except NotExistsError:
                pass

    def test_delete_table_data_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=503)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.delete_table_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                         table_id="b3230b86-4743-498d-9015-3fad58049692", keys={'id': 1})
            except ServiceError:
                pass

    def test_delete_table_data_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=200)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.delete_table_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                         table_id="b3230b86-4743-498d-9015-3fad58049692", keys={'id': 1})
            except ResponseCodeError:
                pass

    def test_delete_table_data_anonymous_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/data',
                status_code=404)
            # test
            try:
                RestClient().delete_table_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                               table_id="b3230b86-4743-498d-9015-3fad58049692",
                                               keys={'id': 1})
            except AuthenticationError:
                pass

    def test_update_table_column_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = Column(id="2f30858d-b47b-4068-9028-fb0524ddf6cb",
                         ord=0,
                         name="ID",
                         database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                         table_id="b3230b86-4743-498d-9015-3fad58049692",
                         internal_name="id",
                         type=ColumnType.BIGINT,
                         concept=ConceptBrief(id="029d773f-f98b-40c0-ab22-b8b1635d4fbc",
                                              uri="http://dbpedia.org/page/Category:Precipitation",
                                              name="Precipitation"),
                         unit=UnitBrief(id="029d773f-f98b-40c0-ab22-b8b1635d4fbc",
                                        uri="http://www.wikidata.org/entity/Q119856947",
                                        name="liters per square meter"),
                         is_null_allowed=False)
            # mock
            mock.put(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/column/758da021-f809-4d87-a866-9d4664a039dc',
                json=exp.model_dump(),
                status_code=202)
            # test
            client = RestClient(username="a", password="b")
            response = client.update_table_column(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                  table_id="b3230b86-4743-498d-9015-3fad58049692",
                                                  column_id="758da021-f809-4d87-a866-9d4664a039dc",
                                                  unit_uri="http://www.wikidata.org/entity/Q119856947",
                                                  concept_uri="http://dbpedia.org/page/Category:Precipitation")
            self.assertEqual(exp, response)

    def test_update_table_column_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/column/758da021-f809-4d87-a866-9d4664a039dc',
                status_code=400)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.update_table_column(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                           table_id="b3230b86-4743-498d-9015-3fad58049692",
                                           column_id="758da021-f809-4d87-a866-9d4664a039dc",
                                           unit_uri="http://www.wikidata.org/entity/Q119856947",
                                           concept_uri="http://dbpedia.org/page/Category:Precipitation")
            except MalformedError:
                pass

    def test_update_table_column_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/column/758da021-f809-4d87-a866-9d4664a039dc',
                status_code=403)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.update_table_column(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                           table_id="b3230b86-4743-498d-9015-3fad58049692",
                                           column_id="758da021-f809-4d87-a866-9d4664a039dc",
                                           unit_uri="http://www.wikidata.org/entity/Q119856947",
                                           concept_uri="http://dbpedia.org/page/Category:Precipitation")
            except ForbiddenError:
                pass

    def test_update_table_column_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/column/758da021-f809-4d87-a866-9d4664a039dc',
                status_code=404)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.update_table_column(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                           table_id="b3230b86-4743-498d-9015-3fad58049692",
                                           column_id="758da021-f809-4d87-a866-9d4664a039dc",
                                           unit_uri="http://www.wikidata.org/entity/Q119856947",
                                           concept_uri="http://dbpedia.org/page/Category:Precipitation")
            except NotExistsError:
                pass

    def test_update_table_column_502_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/column/758da021-f809-4d87-a866-9d4664a039dc',
                status_code=502)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.update_table_column(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                           table_id="b3230b86-4743-498d-9015-3fad58049692",
                                           column_id="758da021-f809-4d87-a866-9d4664a039dc",
                                           unit_uri="http://www.wikidata.org/entity/Q119856947",
                                           concept_uri="http://dbpedia.org/page/Category:Precipitation")
            except ServiceConnectionError:
                pass

    def test_update_table_column_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/column/758da021-f809-4d87-a866-9d4664a039dc',
                status_code=503)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.update_table_column(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                           table_id="b3230b86-4743-498d-9015-3fad58049692",
                                           column_id="758da021-f809-4d87-a866-9d4664a039dc",
                                           unit_uri="http://www.wikidata.org/entity/Q119856947",
                                           concept_uri="http://dbpedia.org/page/Category:Precipitation")
            except ServiceError:
                pass

    def test_update_table_column_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/column/758da021-f809-4d87-a866-9d4664a039dc',
                status_code=200)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.update_table_column(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                           table_id="b3230b86-4743-498d-9015-3fad58049692",
                                           column_id="758da021-f809-4d87-a866-9d4664a039dc",
                                           unit_uri="http://www.wikidata.org/entity/Q119856947",
                                           concept_uri="http://dbpedia.org/page/Category:Precipitation")
            except ResponseCodeError:
                pass

    def test_update_table_column_anonymous_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/column/758da021-f809-4d87-a866-9d4664a039dc',
                status_code=404)
            # test
            try:
                RestClient().update_table_column(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                 table_id="b3230b86-4743-498d-9015-3fad58049692",
                                                 column_id="758da021-f809-4d87-a866-9d4664a039dc",
                                                 unit_uri="http://www.wikidata.org/entity/Q119856947",
                                                 concept_uri="http://dbpedia.org/page/Category:Precipitation")
            except AuthenticationError:
                pass

    def test_analyse_table_statistics_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = TableStatistics(
                total_columns=1,
                total_rows=1000,
                columns={
                    "id": ColumnStatistic(name="id", val_min=1.0, val_max=9.0, mean=5.0, median=5.0, std_dev=2.73)})
            # mock
            mock.get(
                '/api/analyse/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/statistics',
                json=exp.model_dump(), status_code=202)
            # test
            response = RestClient().analyse_table_statistics(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                             table_id="b3230b86-4743-498d-9015-3fad58049692")
            self.assertEqual(exp, response)

    def test_analyse_table_statistics_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(
                '/api/analyse/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/statistics',
                status_code=400)
            # test
            try:
                RestClient().analyse_table_statistics(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                      table_id="b3230b86-4743-498d-9015-3fad58049692")
            except MalformedError:
                pass

    def test_analyse_table_statistics_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(
                '/api/analyse/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/statistics',
                status_code=404)
            # test
            try:
                RestClient().analyse_table_statistics(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                      table_id="b3230b86-4743-498d-9015-3fad58049692")
            except NotExistsError:
                pass

    def test_analyse_table_statistics_502_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(
                '/api/analyse/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/statistics',
                status_code=502)
            # test
            try:
                RestClient().analyse_table_statistics(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                      table_id="b3230b86-4743-498d-9015-3fad58049692")
            except ServiceConnectionError:
                pass

    def test_analyse_table_statistics_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(
                '/api/analyse/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/statistics',
                status_code=503)
            # test
            try:
                RestClient().analyse_table_statistics(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                      table_id="b3230b86-4743-498d-9015-3fad58049692")
            except ServiceError:
                pass

    def test_analyse_table_statistics_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(
                '/api/analyse/database/6bd39359-b154-456d-b9c2-caa516a45732/table/b3230b86-4743-498d-9015-3fad58049692/statistics',
                status_code=200)
            # test
            try:
                RestClient().analyse_table_statistics(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                      table_id="b3230b86-4743-498d-9015-3fad58049692")
            except ResponseCodeError:
                pass


if __name__ == "__main__":
    unittest.main()

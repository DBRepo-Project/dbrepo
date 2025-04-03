import datetime
import json
import unittest

import requests_mock
from pandas import DataFrame

from dbrepo.RestClient import RestClient
from dbrepo.api.dto import Query, QueryType, UserBrief, QueryDefinition, FilterDefinition, FilterType, Database, \
    ContainerBrief, ImageBrief, Image, Table, Constraints, PrimaryKey, TableBrief, ColumnBrief, ColumnType, \
    Column, Operator
from dbrepo.api.exceptions import MalformedError, NotExistsError, ForbiddenError, QueryStoreError, FormatNotAvailable, \
    ServiceError, ResponseCodeError, AuthenticationError


class QueryUnitTest(unittest.TestCase):
    database = Database(
        id="6bd39359-b154-456d-b9c2-caa516a45732",
        name='test',
        owner=UserBrief(id='abdbf897-e599-4e5a-a3f0-7529884ea011', username='mweise'),
        contact=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'),
        exchange_name='dbrepo',
        internal_name='test_abcd',
        is_public=True,
        is_schema_public=True,
        is_dashboard_enabled=True,
        tables=[Table(id="029d773f-f98b-40c0-ab22-b8b1635d4fbc",
                      name="Some Table",
                      description="Test Table",
                      database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                      internal_name="some_table",
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
                                                                          table_id="029d773f-f98b-40c0-ab22-b8b1635d4fbc",
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
                                      is_null_allowed=False),
                               Column(id="85de93a8-834c-4cf4-9d34-f80ebd97e606",
                                      name="Username",
                                      ord=1,
                                      database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                      table_id="029d773f-f98b-40c0-ab22-b8b1635d4fbc",
                                      internal_name="username",
                                      type=ColumnType.VARCHAR,
                                      is_null_allowed=False)
                               ])],
        container=ContainerBrief(id="44d811a8-4019-46ba-bd57-ea10a2eb0c74",
                                 name='MariaDB Galera 11.1.3',
                                 internal_name='mariadb',
                                 image=ImageBrief(id="b104648b-54d2-4d72-9834-8e0e6d428b39",
                                                  name='mariadb',
                                                  version='11.2.2',
                                                  default=True)))
    image = Image(id="b104648b-54d2-4d72-9834-8e0e6d428b39",
                  name='mariadb',
                  version='11.2.2',
                  default=True,
                  data_types=[],
                  operators=[Operator(id="6a96bd99-be3d-4d56-8c38-b14bdfead634",
                                      display_name="IN",
                                      value="IN",
                                      documentation="https://mariadb.com/kb/en/in/")])

    def test_create_subset_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [{'id': 1, 'username': 'foo'}, {'id': 2, 'username': 'bar'}]
            df = DataFrame.from_records(exp)
            # mock
            mock.get(f'/api/image/{self.image.id}', json=self.image.model_dump(),
                     status_code=200)
            mock.get(f'/api/database/{self.database.id}', json=self.database.model_dump(),
                     status_code=200)
            mock.post(f'/api/database/{self.database.id}/subset', json=exp,
                      headers={'X-Id': '85bc1217-29ab-4c09-9f98-8c019238a9c8'}, status_code=201)
            # test
            client = RestClient(username="a", password="b")
            response = client.create_subset(database_id="6bd39359-b154-456d-b9c2-caa516a45732", page=0, size=10,
                                            timestamp=datetime.datetime(2024, 1, 1, 0, 0, 0, 0, datetime.timezone.utc),
                                            query=QueryDefinition(table="some_table",
                                                                  columns=["id", "username"],
                                                                  filter=[FilterDefinition(type=FilterType.WHERE,
                                                                                           column="id",
                                                                                           operator="IN",
                                                                                           value="(1,2)")]))
            self.assertTrue(DataFrame.equals(df, response))

    def test_create_subset_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(f'/api/image/{self.image.id}', json=self.image.model_dump(),
                     status_code=200)
            mock.get(f'/api/database/{self.database.id}', json=self.database.model_dump(),
                     status_code=200)
            mock.post(f'/api/database/{self.database.id}/subset', status_code=400)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.create_subset(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                     query=QueryDefinition(table="some_table",
                                                           columns=["id", "username"],
                                                           filter=[FilterDefinition(type=FilterType.WHERE,
                                                                                    column="id",
                                                                                    operator="IN",
                                                                                    value="(1,2)")]))
            except MalformedError:
                pass

    def test_create_subset_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(f'/api/image/{self.image.id}', json=self.image.model_dump(),
                     status_code=200)
            mock.get(f'/api/database/{self.database.id}', json=self.database.model_dump(),
                     status_code=200)
            mock.post(f'/api/database/{self.database.id}/subset', status_code=403)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.create_subset(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                     query=QueryDefinition(table="some_table",
                                                           columns=["id", "username"],
                                                           filter=[FilterDefinition(type=FilterType.WHERE,
                                                                                    column="id",
                                                                                    operator="IN",
                                                                                    value="(1,2)")]))
            except ForbiddenError:
                pass

    def test_create_subset_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(f'/api/image/{self.image.id}', json=self.image.model_dump(),
                     status_code=200)
            mock.get(f'/api/database/{self.database.id}', json=self.database.model_dump(),
                     status_code=200)
            mock.post(f'/api/database/{self.database.id}/subset', status_code=404)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.create_subset(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                     query=QueryDefinition(table="some_table",
                                                           columns=["id", "username"],
                                                           filter=[FilterDefinition(type=FilterType.WHERE,
                                                                                    column="id",
                                                                                    operator="IN",
                                                                                    value="(1,2)")]))
            except NotExistsError:
                pass

    def test_create_subset_417_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(f'/api/image/{self.image.id}', json=self.image.model_dump(),
                     status_code=200)
            mock.get(f'/api/database/{self.database.id}', json=self.database.model_dump(),
                     status_code=200)
            mock.post(f'/api/database/{self.database.id}/subset', status_code=417)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.create_subset(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                     query=QueryDefinition(table="some_table",
                                                           columns=["id", "username"],
                                                           filter=[FilterDefinition(type=FilterType.WHERE,
                                                                                    column="id",
                                                                                    operator="IN",
                                                                                    value="(1,2)")]))
            except QueryStoreError:
                pass

    def test_create_subset_501_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(f'/api/image/{self.image.id}', json=self.image.model_dump(),
                     status_code=200)
            mock.get(f'/api/database/{self.database.id}', json=self.database.model_dump(),
                     status_code=200)
            mock.post(f'/api/database/{self.database.id}/subset', status_code=501)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.create_subset(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                     query=QueryDefinition(table="some_table",
                                                           columns=["id", "username"],
                                                           filter=[FilterDefinition(type=FilterType.WHERE,
                                                                                    column="id",
                                                                                    operator="IN",
                                                                                    value="(1,2)")]))
            except FormatNotAvailable:
                pass

    def test_create_subset_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(f'/api/image/{self.image.id}', json=self.image.model_dump(),
                     status_code=200)
            mock.get(f'/api/database/{self.database.id}', json=self.database.model_dump(),
                     status_code=200)
            mock.post(f'/api/database/{self.database.id}/subset', status_code=503)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.create_subset(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                     query=QueryDefinition(table="some_table",
                                                           columns=["id", "username"],
                                                           filter=[FilterDefinition(type=FilterType.WHERE,
                                                                                    column="id",
                                                                                    operator="IN",
                                                                                    value="(1,2)")]))
            except ServiceError:
                pass

    def test_create_subset_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(f'/api/image/{self.image.id}', json=self.image.model_dump(),
                     status_code=200)
            mock.get(f'/api/database/{self.database.id}', json=self.database.model_dump(),
                     status_code=200)
            mock.post(f'/api/database/{self.database.id}/subset', status_code=200)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.create_subset(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                     query=QueryDefinition(table="some_table",
                                                           columns=["id", "username"],
                                                           filter=[FilterDefinition(type=FilterType.WHERE,
                                                                                    column="id",
                                                                                    operator="IN",
                                                                                    value="(1,2)")]))
            except ResponseCodeError:
                pass

    def test_create_subset_anonymous_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [{'id': 1, 'username': 'foo'}, {'id': 2, 'username': 'bar'}]
            df = DataFrame.from_records(exp)
            # mock
            mock.get(f'/api/image/{self.image.id}', json=self.image.model_dump(),
                     status_code=200)
            mock.get(f'/api/database/{self.database.id}', json=self.database.model_dump(),
                     status_code=200)
            mock.post(f'/api/database/{self.database.id}/subset', json=exp,
                      headers={'X-Id': '85bc1217-29ab-4c09-9f98-8c019238a9c8'}, status_code=201)
            # test

            client = RestClient()
            response = client.create_subset(database_id="6bd39359-b154-456d-b9c2-caa516a45732", page=0, size=10,
                                            query=QueryDefinition(table="some_table",
                                                                  columns=["id", "username"],
                                                                  filter=[FilterDefinition(type=FilterType.WHERE,
                                                                                           column="id",
                                                                                           operator="IN",
                                                                                           value="(1,2)")]))
            self.assertTrue(DataFrame.equals(df, response))

    def test_get_subset_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = Query(id="e1df2bb8-1f12-494a-ade5-2c4aecdab939",
                        owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'),
                        execution=datetime.datetime(2024, 1, 1, 0, 0, 0, 0, datetime.timezone.utc),
                        query='SELECT id, username FROM some_table WHERE id IN (1,2)',
                        query_normalized='SELECT id, username FROM some_table WHERE id IN (1,2)',
                        type=QueryType.QUERY,
                        database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                        query_hash='da5ff66c4a57683171e2ffcec25298ee684680d1e03633cd286f9067d6924ad8',
                        result_hash='464740ba612225913bb15b26f13377707949b55e65288e89c3f8b4c6469aecb4',
                        is_persisted=False,
                        result_number=None,
                        identifiers=[])
            # mock
            mock.get('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/subset/e1df2bb8-1f12-494a-ade5-2c4aecdab939',
                     json=exp.model_dump())
            # test
            response = RestClient().get_subset(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                               subset_id="e1df2bb8-1f12-494a-ade5-2c4aecdab939")
            self.assertEqual(exp, response)

    def test_get_subset_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/subset/e1df2bb8-1f12-494a-ade5-2c4aecdab939',
                     status_code=403)
            # test
            try:
                RestClient().get_subset(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                        subset_id="e1df2bb8-1f12-494a-ade5-2c4aecdab939")
            except ForbiddenError:
                pass

    def test_get_subset_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/subset/e1df2bb8-1f12-494a-ade5-2c4aecdab939',
                     status_code=404)
            # test
            try:
                RestClient().get_subset(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                        subset_id="e1df2bb8-1f12-494a-ade5-2c4aecdab939")
            except NotExistsError:
                pass

    def test_get_subset_406_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/subset/e1df2bb8-1f12-494a-ade5-2c4aecdab939',
                     status_code=406)
            # test
            try:
                RestClient().get_subset(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                        subset_id="e1df2bb8-1f12-494a-ade5-2c4aecdab939")
            except FormatNotAvailable:
                pass

    def test_get_subset_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/subset/e1df2bb8-1f12-494a-ade5-2c4aecdab939',
                     status_code=503)
            # test
            try:
                RestClient().get_subset(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                        subset_id="e1df2bb8-1f12-494a-ade5-2c4aecdab939")
            except ServiceError:
                pass

    def test_get_subset_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/subset/e1df2bb8-1f12-494a-ade5-2c4aecdab939',
                     status_code=202)
            # test
            try:
                RestClient().get_subset(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                        subset_id="e1df2bb8-1f12-494a-ade5-2c4aecdab939")
            except ResponseCodeError:
                pass

    def test_get_queries_empty_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = []
            # mock
            mock.get(f'/api/database/{self.database.id}/subset', json=[])
            # test
            response = RestClient().get_queries(database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            self.assertEqual(exp, response)

    def test_update_subset_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = Query(id="e1df2bb8-1f12-494a-ade5-2c4aecdab939",
                        owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'),
                        execution=datetime.datetime(2024, 1, 1, 0, 0, 0, 0, datetime.timezone.utc),
                        query='SELECT id, username FROM some_table WHERE id IN (1,2)',
                        query_normalized='SELECT id, username FROM some_table WHERE id IN (1,2)',
                        type=QueryType.QUERY,
                        database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                        query_hash='da5ff66c4a57683171e2ffcec25298ee684680d1e03633cd286f9067d6924ad8',
                        result_hash='464740ba612225913bb15b26f13377707949b55e65288e89c3f8b4c6469aecb4',
                        is_persisted=True,
                        result_number=None,
                        identifiers=[])
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/subset/e1df2bb8-1f12-494a-ade5-2c4aecdab939',
                     json=exp.model_dump(),
                     status_code=202)
            # test
            response = RestClient(username='foo', password='bar').update_subset(
                database_id="6bd39359-b154-456d-b9c2-caa516a45732", subset_id="e1df2bb8-1f12-494a-ade5-2c4aecdab939",
                persist=True)
            self.assertEqual(exp, response)

    def test_update_subset_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/subset/e1df2bb8-1f12-494a-ade5-2c4aecdab939',
                     status_code=400)
            # test
            try:
                RestClient(username='foo', password='bar').update_subset(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    subset_id="e1df2bb8-1f12-494a-ade5-2c4aecdab939", persist=True)
            except MalformedError:
                pass

    def test_update_subset_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/subset/e1df2bb8-1f12-494a-ade5-2c4aecdab939',
                     status_code=403)
            # test
            try:
                RestClient(username='foo', password='bar').update_subset(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    subset_id="e1df2bb8-1f12-494a-ade5-2c4aecdab939", persist=True)
            except ForbiddenError:
                pass

    def test_update_subset_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/subset/e1df2bb8-1f12-494a-ade5-2c4aecdab939',
                     status_code=404)
            # test
            try:
                RestClient(username='foo', password='bar').update_subset(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    subset_id="e1df2bb8-1f12-494a-ade5-2c4aecdab939", persist=True)
            except NotExistsError:
                pass

    def test_update_subset_417_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/subset/e1df2bb8-1f12-494a-ade5-2c4aecdab939',
                     status_code=417)
            # test
            try:
                RestClient(username='foo', password='bar').update_subset(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    subset_id="e1df2bb8-1f12-494a-ade5-2c4aecdab939", persist=True)
            except QueryStoreError:
                pass

    def test_update_subset_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/subset/e1df2bb8-1f12-494a-ade5-2c4aecdab939',
                     status_code=503)
            # test
            try:
                RestClient(username='foo', password='bar').update_subset(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    subset_id="e1df2bb8-1f12-494a-ade5-2c4aecdab939", persist=True)
            except ServiceError:
                pass

    def test_update_subset_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/subset/e1df2bb8-1f12-494a-ade5-2c4aecdab939',
                     status_code=200)
            # test
            try:
                RestClient(username='foo', password='bar').update_subset(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    subset_id="e1df2bb8-1f12-494a-ade5-2c4aecdab939",
                    persist=True)
            except ResponseCodeError:
                pass

    def test_update_subset_anonymous_fails(self):
        # test
        try:
            RestClient().update_subset(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                       subset_id="e1df2bb8-1f12-494a-ade5-2c4aecdab939",
                                       persist=True)
        except AuthenticationError:
            pass

    def test_get_queries_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [Query(id="e1df2bb8-1f12-494a-ade5-2c4aecdab939",
                         owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'),
                         execution=datetime.datetime(2024, 1, 1, 0, 0, 0, 0, datetime.timezone.utc),
                         query='SELECT id, username FROM some_table WHERE id IN (1,2)',
                         query_normalized='SELECT id, username FROM some_table WHERE id IN (1,2)',
                         type=QueryType.QUERY,
                         database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                         query_hash='da5ff66c4a57683171e2ffcec25298ee684680d1e03633cd286f9067d6924ad8',
                         result_hash='464740ba612225913bb15b26f13377707949b55e65288e89c3f8b4c6469aecb4',
                         is_persisted=False,
                         result_number=None,
                         identifiers=[])]
            # mock
            mock.get(f'/api/database/{self.database.id}/subset', json=[exp[0].model_dump()])
            # test
            response = RestClient().get_queries(database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            self.assertEqual(exp, response)

    def test_get_queries_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(f'/api/database/{self.database.id}/subset', status_code=403)
            # test
            try:
                RestClient().get_queries(database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            except ForbiddenError:
                pass

    def test_get_queries_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(f'/api/database/{self.database.id}/subset', status_code=404)
            # test
            try:
                RestClient().get_queries(database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            except NotExistsError:
                pass

    def test_get_queries_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(f'/api/database/{self.database.id}/subset', status_code=503)
            # test
            try:
                RestClient().get_queries(database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            except ServiceError:
                pass

    def test_get_queries_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(f'/api/database/{self.database.id}/subset', status_code=202)
            # test
            try:
                RestClient().get_queries(database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            except ResponseCodeError:
                pass

    def test_get_subset_data_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [{'id': 1, 'username': 'foo'}, {'id': 2, 'username': 'bar'}]
            df = DataFrame.from_records(json.dumps(exp))
            # mock
            mock.get(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/subset/e1df2bb8-1f12-494a-ade5-2c4aecdab939/data',
                json=json.dumps(exp))
            # test
            response = RestClient().get_subset_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                    subset_id="e1df2bb8-1f12-494a-ade5-2c4aecdab939")
            self.assertTrue(DataFrame.equals(df, response))

    def test_get_subset_data_dataframe_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [{'id': 1, 'username': 'foo'}, {'id': 2, 'username': 'bar'}]
            df = DataFrame.from_records(json.dumps(exp))
            # mock
            mock.get(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/subset/e1df2bb8-1f12-494a-ade5-2c4aecdab939/data',
                json=json.dumps(exp))
            # test
            response = RestClient().get_subset_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                    subset_id="e1df2bb8-1f12-494a-ade5-2c4aecdab939")
            self.assertEqual(df.shape, response.shape)
            self.assertTrue(DataFrame.equals(df, response))

    def test_get_subset_data_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/subset/e1df2bb8-1f12-494a-ade5-2c4aecdab939/data',
                status_code=400)
            # test
            try:
                RestClient().get_subset_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                             subset_id="e1df2bb8-1f12-494a-ade5-2c4aecdab939")
            except MalformedError:
                pass

    def test_get_subset_data_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/subset/e1df2bb8-1f12-494a-ade5-2c4aecdab939/data',
                status_code=403)
            # test
            try:
                RestClient().get_subset_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                             subset_id="e1df2bb8-1f12-494a-ade5-2c4aecdab939")
            except ForbiddenError:
                pass

    def test_get_subset_data_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/subset/e1df2bb8-1f12-494a-ade5-2c4aecdab939/data',
                status_code=404)
            # test
            try:
                RestClient().get_subset_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                             subset_id="e1df2bb8-1f12-494a-ade5-2c4aecdab939")
            except NotExistsError:
                pass

    def test_get_subset_data_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/subset/e1df2bb8-1f12-494a-ade5-2c4aecdab939/data',
                status_code=503)
            # test
            try:
                RestClient().get_subset_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                             subset_id="e1df2bb8-1f12-494a-ade5-2c4aecdab939")
            except ServiceError:
                pass

    def test_get_subset_data_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/subset/e1df2bb8-1f12-494a-ade5-2c4aecdab939/data',
                status_code=202)
            # test
            try:
                RestClient().get_subset_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                             subset_id="e1df2bb8-1f12-494a-ade5-2c4aecdab939")
            except ResponseCodeError:
                pass

    def test_get_subset_data_count_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = 2
            # mock
            mock.head(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/subset/e1df2bb8-1f12-494a-ade5-2c4aecdab939/data',
                headers={'X-Count': str(exp)})
            # test
            response = RestClient().get_subset_data_count(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                          subset_id="e1df2bb8-1f12-494a-ade5-2c4aecdab939")
            self.assertEqual(exp, response)

    def test_get_subset_data_count_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.head(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/subset/e1df2bb8-1f12-494a-ade5-2c4aecdab939/data',
                status_code=400)
            # test
            try:
                RestClient().get_subset_data_count(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                   subset_id="e1df2bb8-1f12-494a-ade5-2c4aecdab939")
            except MalformedError:
                pass

    def test_get_subset_data_count_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.head(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/subset/e1df2bb8-1f12-494a-ade5-2c4aecdab939/data',
                status_code=403)
            # test
            try:
                RestClient().get_subset_data_count(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                   subset_id="e1df2bb8-1f12-494a-ade5-2c4aecdab939")
            except ForbiddenError:
                pass

    def test_get_subset_data_count_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.head(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/subset/e1df2bb8-1f12-494a-ade5-2c4aecdab939/data',
                status_code=404)
            # test
            try:
                RestClient().get_subset_data_count(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                   subset_id="e1df2bb8-1f12-494a-ade5-2c4aecdab939")
            except NotExistsError:
                pass

    def test_get_subset_data_count_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.head(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/subset/e1df2bb8-1f12-494a-ade5-2c4aecdab939/data',
                status_code=503)
            # test
            try:
                RestClient().get_subset_data_count(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                   subset_id="e1df2bb8-1f12-494a-ade5-2c4aecdab939")
            except ServiceError:
                pass

    def test_get_subset_data_count_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.head(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/subset/e1df2bb8-1f12-494a-ade5-2c4aecdab939/data',
                status_code=202)
            # test
            try:
                RestClient().get_subset_data_count(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                   subset_id="e1df2bb8-1f12-494a-ade5-2c4aecdab939")
            except ResponseCodeError:
                pass


if __name__ == "__main__":
    unittest.main()

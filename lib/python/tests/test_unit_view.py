import json
import unittest

import requests_mock
from pandas import DataFrame

from dbrepo.RestClient import RestClient
from dbrepo.api.dto import View, ViewColumn, ColumnType, UserBrief, ViewBrief, QueryDefinition, FilterDefinition, \
    FilterType, Database, Table, Constraints, PrimaryKey, TableBrief, ColumnBrief, Column, ContainerBrief, ImageBrief, \
    Image, Operator
from dbrepo.api.exceptions import ForbiddenError, NotExistsError, MalformedError, AuthenticationError, \
    ResponseCodeError, ExternalSystemError, ServiceError, ServiceConnectionError


class ViewUnitTest(unittest.TestCase):
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

    def test_get_views_empty_succeeds(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(f'/api/database/{self.database.id}/view', json=[])
            # test
            response = RestClient().get_views(database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            self.assertEqual([], response)

    def test_get_views_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [ViewBrief(id="1b3449d2-780e-4683-9af0-8733e608a4aa",
                             name="Data",
                             internal_name="data",
                             database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                             initial_view=False,
                             query="SELECT id FROM mytable WHERE deg > 0",
                             query_hash="94c74728b11a690e51d64719868824735f0817b7",
                             owned_by='8638c043-5145-4be8-a3e4-4b79991b0a16',
                             is_public=True,
                             is_schema_public=True)]
            # mock
            mock.get(f'/api/database/{self.database.id}/view', json=[exp[0].model_dump()])
            # test
            response = RestClient().get_views(database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            self.assertEqual(exp, response)

    def test_get_views_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(f'/api/database/{self.database.id}/view', status_code=404)
            # test
            try:
                response = RestClient().get_views(database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            except NotExistsError:
                pass

    def test_get_views_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(f'/api/database/{self.database.id}/view', status_code=202)
            # test
            try:
                response = RestClient().get_views(database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            except ResponseCodeError:
                pass

    def test_get_view_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = View(id="1b3449d2-780e-4683-9af0-8733e608a4aa",
                       name="Data",
                       internal_name="data",
                       database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                       initial_view=False,
                       query="SELECT id FROM mytable WHERE deg > 0",
                       query_hash="94c74728b11a690e51d64719868824735f0817b7",
                       owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'),
                       is_public=True,
                       is_schema_public=True,
                       columns=[ViewColumn(id="1b3449d2-780e-4683-9af0-8733e608a4aa",
                                           ord=0,
                                           name="id",
                                           internal_name="id",
                                           database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                           type=ColumnType.BIGINT,
                                           is_null_allowed=False)],
                       identifiers=[])
            # mock
            mock.get('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1b3449d2-780e-4683-9af0-8733e608a4aa',
                     json=exp.model_dump())
            # test
            response = RestClient().get_view(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                             view_id="1b3449d2-780e-4683-9af0-8733e608a4aa")
            self.assertEqual(exp, response)

    def test_get_view_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1b3449d2-780e-4683-9af0-8733e608a4aa',
                     status_code=403)
            # test
            try:
                response = RestClient().get_view(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                 view_id="1b3449d2-780e-4683-9af0-8733e608a4aa")
            except ForbiddenError:
                pass

    def test_get_view_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1b3449d2-780e-4683-9af0-8733e608a4aa',
                     status_code=404)
            # test
            try:
                response = RestClient().get_view(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                 view_id="1b3449d2-780e-4683-9af0-8733e608a4aa")
            except NotExistsError:
                pass

    def test_get_view_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1b3449d2-780e-4683-9af0-8733e608a4aa',
                     status_code=202)
            # test
            try:
                response = RestClient().get_view(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                 view_id="1b3449d2-780e-4683-9af0-8733e608a4aa")
            except ResponseCodeError:
                pass

    def test_update_view_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = ViewBrief(id="1b3449d2-780e-4683-9af0-8733e608a4aa",
                            name="Data",
                            internal_name="data",
                            database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                            initial_view=False,
                            query="SELECT id FROM mytable WHERE deg > 0",
                            query_hash="94c74728b11a690e51d64719868824735f0817b7",
                            owned_by='8638c043-5145-4be8-a3e4-4b79991b0a16',
                            is_public=False,
                            is_schema_public=False)
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1', json=exp.model_dump(),
                     status_code=202)
            # test
            response = RestClient(username='foo', password='bar').update_view(
                database_id="6bd39359-b154-456d-b9c2-caa516a45732", view_id=1,
                is_public=False, is_schema_public=False)
            self.assertEqual(exp, response)

    def test_update_view_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1', status_code=403)
            # test
            try:
                RestClient(username='foo', password='bar').update_view(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732", view_id=1, is_public=False,
                    is_schema_public=False)
            except ForbiddenError:
                pass

    def test_update_view_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1', status_code=404)
            # test
            try:
                RestClient(username='foo', password='bar').update_view(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732", view_id=1, is_public=False,
                    is_schema_public=False)
            except NotExistsError:
                pass

    def test_update_view_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1', status_code=200)
            # test
            try:
                RestClient(username='foo', password='bar').update_view(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732", view_id=1, is_public=False,
                    is_schema_public=False)
            except ResponseCodeError:
                pass

    def test_update_view_anonymous_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1', status_code=403)
            # test
            try:
                RestClient().update_view(database_id="6bd39359-b154-456d-b9c2-caa516a45732", view_id=1, is_public=False,
                                         is_schema_public=False)
            except AuthenticationError:
                pass

    def test_create_view_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = ViewBrief(id="1b3449d2-780e-4683-9af0-8733e608a4aa",
                       name="Data",
                       internal_name="data",
                       database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                       initial_view=False,
                       query="SELECT id FROM some_table WHERE id IN (1,2)",
                       query_hash="94c74728b11a690e51d64719868824735f0817b7",
                       owned_by='8638c043-5145-4be8-a3e4-4b79991b0a16',
                       is_public=True,
                       is_schema_public=True)
            # mock
            mock.get(f'/api/image/{self.image.id}', json=self.image.model_dump(),
                     status_code=200)
            mock.get(f'/api/database/{self.database.id}', json=self.database.model_dump(),
                     status_code=200)
            mock.post(f'/api/database/{self.database.id}/view', json=exp.model_dump(), status_code=201)
            # test
            client = RestClient(username="a", password="b")
            response = client.create_view(database_id="6bd39359-b154-456d-b9c2-caa516a45732", name="Data",
                                          is_public=True, is_schema_public=True,
                                          query=QueryDefinition(table="some_table",
                                                                columns=["id"],
                                                                filter=[FilterDefinition(type=FilterType.WHERE,
                                                                                         column="id",
                                                                                         operator="IN",
                                                                                         value="(1,2)")]))
            self.assertEqual(exp, response)

    def test_create_view_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(f'/api/image/{self.image.id}', json=self.image.model_dump(),
                     status_code=200)
            mock.get(f'/api/database/{self.database.id}', json=self.database.model_dump(),
                     status_code=200)
            mock.post(f'/api/database/{self.database.id}/view', status_code=400)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.create_view(database_id="6bd39359-b154-456d-b9c2-caa516a45732", name="Data",
                                   is_public=True, is_schema_public=True,
                                   query=QueryDefinition(table="some_table",
                                                         columns=["id"],
                                                         filter=[FilterDefinition(type=FilterType.WHERE,
                                                                                  column="id",
                                                                                  operator="IN",
                                                                                  value="(1,2)")]))
            except MalformedError:
                pass

    def test_create_view_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(f'/api/image/{self.image.id}', json=self.image.model_dump(),
                     status_code=200)
            mock.get(f'/api/database/{self.database.id}', json=self.database.model_dump(),
                     status_code=200)
            mock.post(f'/api/database/{self.database.id}/view', status_code=403)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.create_view(database_id="6bd39359-b154-456d-b9c2-caa516a45732", name="Data",
                                   is_public=True, is_schema_public=True,
                                   query=QueryDefinition(table="some_table",
                                                         columns=["id"],
                                                         filter=[FilterDefinition(type=FilterType.WHERE,
                                                                                  column="id",
                                                                                  operator="IN",
                                                                                  value="(1,2)")]))
            except ForbiddenError:
                pass

    def test_create_view_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(f'/api/image/{self.image.id}', json=self.image.model_dump(),
                     status_code=200)
            mock.get(f'/api/database/{self.database.id}', json=self.database.model_dump(),
                     status_code=200)
            mock.post(f'/api/database/{self.database.id}/view', status_code=404)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.create_view(database_id="6bd39359-b154-456d-b9c2-caa516a45732", name="Data",
                                   is_public=True, is_schema_public=True,
                                   query=QueryDefinition(table="some_table",
                                                         columns=["id"],
                                                         filter=[FilterDefinition(type=FilterType.WHERE,
                                                                                  column="id",
                                                                                  operator="IN",
                                                                                  value="(1,2)")]))
            except NotExistsError:
                pass

    def test_create_view_423_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(f'/api/image/{self.image.id}', json=self.image.model_dump(),
                     status_code=200)
            mock.get(f'/api/database/{self.database.id}', json=self.database.model_dump(),
                     status_code=200)
            mock.post(f'/api/database/{self.database.id}/view', status_code=423)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.create_view(database_id="6bd39359-b154-456d-b9c2-caa516a45732", name="Data",
                                   is_public=True, is_schema_public=True,
                                   query=QueryDefinition(table="some_table",
                                                         columns=["id"],
                                                         filter=[FilterDefinition(type=FilterType.WHERE,
                                                                                  column="id",
                                                                                  operator="IN",
                                                                                  value="(1,2)")]))
            except ExternalSystemError:
                pass

    def test_create_view_502_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(f'/api/image/{self.image.id}', json=self.image.model_dump(),
                     status_code=200)
            mock.get(f'/api/database/{self.database.id}', json=self.database.model_dump(),
                     status_code=200)
            mock.post(f'/api/database/{self.database.id}/view', status_code=502)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.create_view(database_id="6bd39359-b154-456d-b9c2-caa516a45732", name="Data",
                                   is_public=True, is_schema_public=True,
                                   query=QueryDefinition(table="some_table",
                                                         columns=["id"],
                                                         filter=[FilterDefinition(type=FilterType.WHERE,
                                                                                  column="id",
                                                                                  operator="IN",
                                                                                  value="(1,2)")]))
            except ServiceConnectionError:
                pass

    def test_create_view_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(f'/api/image/{self.image.id}', json=self.image.model_dump(),
                     status_code=200)
            mock.get(f'/api/database/{self.database.id}', json=self.database.model_dump(),
                     status_code=200)
            mock.post(f'/api/database/{self.database.id}/view', status_code=503)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.create_view(database_id="6bd39359-b154-456d-b9c2-caa516a45732", name="Data",
                                   is_public=True, is_schema_public=True,
                                   query=QueryDefinition(table="some_table",
                                                         columns=["id"],
                                                         filter=[FilterDefinition(type=FilterType.WHERE,
                                                                                  column="id",
                                                                                  operator="IN",
                                                                                  value="(1,2)")]))
            except ServiceError:
                pass

    def test_create_view_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(f'/api/image/{self.image.id}', json=self.image.model_dump(),
                     status_code=200)
            mock.get(f'/api/database/{self.database.id}', json=self.database.model_dump(),
                     status_code=200)
            mock.post(f'/api/database/{self.database.id}/view', status_code=200)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.create_view(database_id="6bd39359-b154-456d-b9c2-caa516a45732", name="Data",
                                   is_public=True, is_schema_public=True,
                                   query=QueryDefinition(table="some_table",
                                                         columns=["id"],
                                                         filter=[FilterDefinition(type=FilterType.WHERE,
                                                                                  column="id",
                                                                                  operator="IN",
                                                                                  value="(1,2)")]))
            except ResponseCodeError:
                pass

    def test_create_view_anonymous_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(f'/api/image/{self.image.id}', json=self.image.model_dump(),
                     status_code=200)
            mock.get(f'/api/database/{self.database.id}', json=self.database.model_dump(),
                     status_code=200)
            mock.post(f'/api/database/{self.database.id}/view', status_code=404)
            # test
            try:
                RestClient().create_view(database_id="6bd39359-b154-456d-b9c2-caa516a45732", name="Data",
                                         is_public=True, is_schema_public=True,
                                         query=QueryDefinition(table="some_table",
                                                               columns=["id"],
                                                               filter=[
                                                                   FilterDefinition(type=FilterType.WHERE,
                                                                                    column="id",
                                                                                    operator="IN",
                                                                                    value="(1,2)")]))
            except AuthenticationError:
                pass

    def test_delete_view_succeeds(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1b3449d2-780e-4683-9af0-8733e608a4aa',
                        status_code=202)
            # test
            client = RestClient(username="a", password="b")
            client.delete_view(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                               view_id="1b3449d2-780e-4683-9af0-8733e608a4aa")

    def test_delete_view_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1b3449d2-780e-4683-9af0-8733e608a4aa',
                        status_code=400)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.delete_view(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                   view_id="1b3449d2-780e-4683-9af0-8733e608a4aa")
            except MalformedError:
                pass

    def test_delete_view_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1b3449d2-780e-4683-9af0-8733e608a4aa',
                        status_code=403)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.delete_view(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                   view_id="1b3449d2-780e-4683-9af0-8733e608a4aa")
            except ForbiddenError:
                pass

    def test_delete_view_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1b3449d2-780e-4683-9af0-8733e608a4aa',
                        status_code=404)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.delete_view(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                   view_id="1b3449d2-780e-4683-9af0-8733e608a4aa")
            except NotExistsError:
                pass

    def test_delete_view_423_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1b3449d2-780e-4683-9af0-8733e608a4aa',
                        status_code=423)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.delete_view(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                   view_id="1b3449d2-780e-4683-9af0-8733e608a4aa")
            except ExternalSystemError:
                pass

    def test_delete_view_502_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1b3449d2-780e-4683-9af0-8733e608a4aa',
                        status_code=502)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.delete_view(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                   view_id="1b3449d2-780e-4683-9af0-8733e608a4aa")
            except ServiceConnectionError:
                pass

    def test_delete_view_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1b3449d2-780e-4683-9af0-8733e608a4aa',
                        status_code=503)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.delete_view(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                   view_id="1b3449d2-780e-4683-9af0-8733e608a4aa")
            except ServiceError:
                pass

    def test_delete_view_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1b3449d2-780e-4683-9af0-8733e608a4aa',
                        status_code=200)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.delete_view(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                   view_id="1b3449d2-780e-4683-9af0-8733e608a4aa")
            except ResponseCodeError:
                pass

    def test_delete_view_anonymous_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1b3449d2-780e-4683-9af0-8733e608a4aa',
                        status_code=403)
            # test
            try:
                RestClient().delete_view(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                         view_id="1b3449d2-780e-4683-9af0-8733e608a4aa")
            except AuthenticationError:
                pass

    def test_get_view_data_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [{'id': 1, 'username': 'foo'}, {'id': 2, 'username': 'bar'}]
            df = DataFrame.from_records(json.dumps(exp))
            # mock
            mock.get(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1b3449d2-780e-4683-9af0-8733e608a4aa/data',
                json=json.dumps(exp))
            # test
            response = RestClient().get_view_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                  view_id="1b3449d2-780e-4683-9af0-8733e608a4aa")
            self.assertTrue(DataFrame.equals(df, response))

    def test_get_view_data_dataframe_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [{'id': 1, 'username': 'foo'}, {'id': 2, 'username': 'bar'}]
            df = DataFrame.from_records(json.dumps(exp))
            # mock
            mock.get(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1b3449d2-780e-4683-9af0-8733e608a4aa/data',
                json=json.dumps(exp))
            # test
            response: DataFrame = RestClient().get_view_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                             view_id="1b3449d2-780e-4683-9af0-8733e608a4aa")
            self.assertEqual(df.shape, response.shape)
            self.assertTrue(DataFrame.equals(df, response))

    def test_get_view_data_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1b3449d2-780e-4683-9af0-8733e608a4aa/data',
                status_code=400)
            # test
            try:
                response = RestClient().get_view_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                      view_id="1b3449d2-780e-4683-9af0-8733e608a4aa")
            except MalformedError:
                pass

    def test_get_view_data_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1b3449d2-780e-4683-9af0-8733e608a4aa/data',
                status_code=403)
            # test
            try:
                response = RestClient().get_view_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                      view_id="1b3449d2-780e-4683-9af0-8733e608a4aa")
            except ForbiddenError:
                pass

    def test_get_view_data_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1b3449d2-780e-4683-9af0-8733e608a4aa/data',
                status_code=404)
            # test
            try:
                response = RestClient().get_view_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                      view_id="1b3449d2-780e-4683-9af0-8733e608a4aa")
            except NotExistsError:
                pass

    def test_get_view_data_409_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1b3449d2-780e-4683-9af0-8733e608a4aa/data',
                status_code=409)
            # test
            try:
                response = RestClient().get_view_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                      view_id="1b3449d2-780e-4683-9af0-8733e608a4aa")
            except ExternalSystemError:
                pass

    def test_get_view_data_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1b3449d2-780e-4683-9af0-8733e608a4aa/data',
                status_code=503)
            # test
            try:
                response = RestClient().get_view_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                      view_id="1b3449d2-780e-4683-9af0-8733e608a4aa")
            except ServiceError:
                pass

    def test_get_view_data_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1b3449d2-780e-4683-9af0-8733e608a4aa/data',
                status_code=202)
            # test
            try:
                response = RestClient().get_view_data(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                      view_id="1b3449d2-780e-4683-9af0-8733e608a4aa")
            except ResponseCodeError:
                pass

    def test_get_view_data_count_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = 844737
            # mock
            mock.head(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1b3449d2-780e-4683-9af0-8733e608a4aa/data',
                headers={'X-Count': str(exp)})
            # test
            response = RestClient().get_view_data_count(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                        view_id="1b3449d2-780e-4683-9af0-8733e608a4aa")
            self.assertEqual(exp, response)

    def test_get_view_data_count_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.head(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1b3449d2-780e-4683-9af0-8733e608a4aa/data',
                status_code=400)
            # test
            try:
                response = RestClient().get_view_data_count(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                            view_id="1b3449d2-780e-4683-9af0-8733e608a4aa")
            except MalformedError:
                pass

    def test_get_view_data_count_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.head(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1b3449d2-780e-4683-9af0-8733e608a4aa/data',
                status_code=403)
            # test
            try:
                response = RestClient().get_view_data_count(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                            view_id="1b3449d2-780e-4683-9af0-8733e608a4aa")
            except ForbiddenError:
                pass

    def test_get_view_data_count_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.head(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1b3449d2-780e-4683-9af0-8733e608a4aa/data',
                status_code=404)
            # test
            try:
                response = RestClient().get_view_data_count(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                            view_id="1b3449d2-780e-4683-9af0-8733e608a4aa")
            except NotExistsError:
                pass

    def test_get_view_data_count_409_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.head(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1b3449d2-780e-4683-9af0-8733e608a4aa/data',
                status_code=409)
            # test
            try:
                response = RestClient().get_view_data_count(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                            view_id="1b3449d2-780e-4683-9af0-8733e608a4aa")
            except ExternalSystemError:
                pass

    def test_get_view_data_count_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.head(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1b3449d2-780e-4683-9af0-8733e608a4aa/data',
                status_code=503)
            # test
            try:
                response = RestClient().get_view_data_count(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                            view_id="1b3449d2-780e-4683-9af0-8733e608a4aa")
            except ServiceError:
                pass

    def test_get_view_data_count_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.head(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/1b3449d2-780e-4683-9af0-8733e608a4aa/data',
                status_code=202)
            # test
            try:
                response = RestClient().get_view_data_count(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                            view_id="1b3449d2-780e-4683-9af0-8733e608a4aa")
            except ResponseCodeError:
                pass


if __name__ == "__main__":
    unittest.main()

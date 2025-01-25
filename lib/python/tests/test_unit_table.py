import json
import unittest

import requests_mock
from pandas import DataFrame

from dbrepo.RestClient import RestClient
from dbrepo.api.dto import Table, CreateTableConstraints, Column, Constraints, ColumnType, ConceptBrief, UnitBrief, \
    TableStatistics, ColumnStatistic, PrimaryKey, ColumnBrief, TableBrief, UserBrief
from dbrepo.api.exceptions import MalformedError, ForbiddenError, NotExistsError, NameExistsError, \
    AuthenticationError, ExternalSystemError


class TableUnitTest(unittest.TestCase):

    def test_create_table_succeeds(self):
        exp = TableBrief(id=2,
                         database_id=1,
                         name="Test",
                         description="Test Table",
                         internal_name="test",
                         owned_by='8638c043-5145-4be8-a3e4-4b79991b0a16',
                         is_versioned=True,
                         is_public=True,
                         is_schema_public=True)
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/1/table', json=exp.model_dump(), status_code=201)
            # test
            client = RestClient(username="a", password="b")
            response = client.create_table(database_id=1, name="Test", description="Test Table", columns=[],
                                           is_public=True, is_schema_public=True,
                                           constraints=CreateTableConstraints())
            self.assertEqual(exp, response)

    def test_create_table_malformed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/1/table', status_code=400)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.create_table(database_id=1, name="Test", description="Test Table", columns=[],
                                               is_public=True, is_schema_public=True,
                                               constraints=CreateTableConstraints())
            except MalformedError:
                pass

    def test_create_table_not_allowed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/1/table', status_code=403)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.create_table(database_id=1, name="Test", description="Test Table", columns=[],
                                               is_public=True, is_schema_public=True,
                                               constraints=CreateTableConstraints())
            except ForbiddenError:
                pass

    def test_create_table_not_allowed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/1/table', status_code=404)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.create_table(database_id=1, name="Test", description="Test Table", columns=[],
                                               is_public=True, is_schema_public=True,
                                               constraints=CreateTableConstraints())
            except NotExistsError:
                pass

    def test_create_table_name_exists_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/1/table', status_code=409)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.create_table(database_id=1, name="Test", description="Test Table", columns=[],
                                               is_public=True, is_schema_public=True,
                                               constraints=CreateTableConstraints())
            except NameExistsError:
                pass

    def test_create_table_not_auth_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/1/table', status_code=409)
            # test
            try:
                response = RestClient().create_table(database_id=1, name="Test", description="Test Table", columns=[],
                                                     is_public=True, is_schema_public=True,
                                                     constraints=CreateTableConstraints())
            except AuthenticationError:
                pass

    def test_get_tables_empty_succeeds(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/1/table', json=[])
            # test
            response = RestClient().get_tables(database_id=1)
            self.assertEqual([], response)

    def test_get_tables_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [TableBrief(id=2,
                              name="Test",
                              description="Test Table",
                              database_id=1,
                              internal_name="test",
                              is_public=True,
                              is_schema_public=True,
                              owned_by='8638c043-5145-4be8-a3e4-4b79991b0a16',
                              is_versioned=True)]
            # mock
            mock.get('/api/database/1/table', json=[exp[0].model_dump()])
            # test
            response = RestClient().get_tables(database_id=1)
            self.assertEqual(exp, response)

    def test_get_table_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = Table(id=2,
                        name="Test",
                        description="Test Table",
                        database_id=1,
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
                                                primary_key=[PrimaryKey(id=1,
                                                                        table=TableBrief(id=2, database_id=1,
                                                                                         name='Other',
                                                                                         internal_name='other',
                                                                                         description=None,
                                                                                         is_versioned=True,
                                                                                         is_public=True,
                                                                                         is_schema_public=True,
                                                                                         owned_by='8638c043-5145-4be8-a3e4-4b79991b0a16'),
                                                                        column=ColumnBrief(id=1, table_id=2,
                                                                                           database_id=1,
                                                                                           name='id',
                                                                                           alias=None,
                                                                                           internal_name='id',
                                                                                           type=ColumnType.BIGINT))]),
                        columns=[Column(id=1,
                                        name="ID",
                                        ord=0,
                                        database_id=1,
                                        table_id=2,
                                        internal_name="id",
                                        auto_generated=True,
                                        is_primary_key=True,
                                        type=ColumnType.BIGINT,
                                        is_public=True,
                                        is_null_allowed=False)])
            # mock
            mock.get('/api/database/1/table/2', json=exp.model_dump())
            # test
            response = RestClient().get_table(database_id=1, table_id=2)
            self.assertEqual(exp, response)

    def test_get_table_not_allowed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/1/table/2', status_code=403)
            # test
            try:
                response = RestClient().get_table(database_id=1, table_id=2)
            except ForbiddenError:
                pass

    def test_get_table_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/1/table/2', status_code=404)
            # test
            try:
                response = RestClient().get_table(database_id=1, table_id=2)
            except NotExistsError:
                pass

    def test_delete_table_succeeds(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/1/table/2', status_code=202)
            # test
            client = RestClient(username="a", password="b")
            client.delete_table(database_id=1, table_id=2)

    def test_delete_table_not_allowed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/1/table/2', status_code=403)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.delete_table(database_id=1, table_id=2)
            except ForbiddenError:
                pass

    def test_delete_table_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/1/table/2', status_code=404)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.delete_table(database_id=1, table_id=2)
            except NotExistsError:
                pass

    def test_delete_table_not_auth_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/1/table/2', status_code=404)
            # test
            try:
                RestClient().delete_table(database_id=1, table_id=2)
            except AuthenticationError:
                pass

    def test_get_table_data_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [{'id': 1, 'username': 'foo'}, {'id': 2, 'username': 'bar'}]
            df = DataFrame.from_records(json.dumps(exp))
            # mock
            mock.get('/api/database/1/table/9/data', json=json.dumps(exp))
            # test
            response = RestClient().get_table_data(database_id=1, table_id=9)
            self.assertTrue(DataFrame.equals(df, response))

    def test_get_table_data_dataframe_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [{'id': 1, 'username': 'foo'}, {'id': 2, 'username': 'bar'}]
            df = DataFrame.from_records(json.dumps(exp))
            # mock
            mock.get('/api/database/1/table/9/data', json=json.dumps(exp))
            # test
            response = RestClient().get_table_data(database_id=1, table_id=9)
            self.assertEqual(df.shape, response.shape)
            self.assertTrue(DataFrame.equals(df, response))

    def test_get_table_data_malformed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/1/table/9/data', status_code=400)
            # test
            try:
                response = RestClient().get_table_data(database_id=1, table_id=9)
            except MalformedError:
                pass

    def test_get_table_data_not_allowed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/1/table/9/data', status_code=403)
            # test
            try:
                response = RestClient().get_table_data(database_id=1, table_id=9)
            except ForbiddenError:
                pass

    def test_get_table_data_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/1/table/9/data', status_code=404)
            # test
            try:
                response = RestClient().get_table_data(database_id=1, table_id=9)
            except NotExistsError:
                pass

    def test_get_table_data_count_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = 2
            # mock
            mock.head('/api/database/1/table/9/data', headers={'X-Count': str(exp)})
            # test
            response = RestClient().get_table_data_count(database_id=1, table_id=9)
            self.assertEqual(exp, response)

    def test_get_table_data_count_malformed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.head('/api/database/1/table/9/data', status_code=400)
            # test
            try:
                response = RestClient().get_table_data_count(database_id=1, table_id=9)
            except MalformedError:
                pass

    def test_get_table_data_count_not_allowed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.head('/api/database/1/table/9/data', status_code=403)
            # test
            try:
                response = RestClient().get_table_data_count(database_id=1, table_id=9)
            except ForbiddenError:
                pass

    def test_get_table_data_count_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.head('/api/database/1/table/9/data', status_code=404)
            # test
            try:
                response = RestClient().get_table_data_count(database_id=1, table_id=9)
            except NotExistsError:
                pass

    def test_get_table_data_count_not_countable_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.head('/api/database/1/table/9/data', status_code=409)
            # test
            try:
                response = RestClient().get_table_data_count(database_id=1, table_id=9)
            except ExternalSystemError:
                pass

    def test_create_table_data_succeeds(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/1/table/9/data', status_code=201)
            # test
            client = RestClient(username="a", password="b")
            client.create_table_data(database_id=1, table_id=9,
                                     data={'name': 'Josiah', 'age': 45, 'gender': 'male'})

    def test_create_table_data_malformed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/1/table/9/data', status_code=400)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.create_table_data(database_id=1, table_id=9,
                                         data={'name': 'Josiah', 'age': 45, 'gender': 'male'})
            except MalformedError:
                pass

    def test_create_table_data_not_allowed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/1/table/9/data', status_code=403)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.create_table_data(database_id=1, table_id=9,
                                         data={'name': 'Josiah', 'age': 45, 'gender': 'male'})
            except ForbiddenError:
                pass

    def test_create_table_data_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/1/table/9/data', status_code=404)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.create_table_data(database_id=1, table_id=9,
                                         data={'name': 'Josiah', 'age': 45, 'gender': 'male'})
            except NotExistsError:
                pass

    def test_update_table_data_succeeds(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/1/table/9/data', status_code=202)
            # test
            client = RestClient(username="a", password="b")
            client.update_table_data(database_id=1, table_id=9,
                                     data={'name': 'Josiah', 'age': 45, 'gender': 'male'},
                                     keys={'id': 1})

    def test_update_table_data_malformed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/1/table/9/data', status_code=400)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.update_table_data(database_id=1, table_id=9,
                                         data={'name': 'Josiah', 'age': 45, 'gender': 'male'},
                                         keys={'id': 1})
            except MalformedError:
                pass

    def test_update_table_data_not_allowed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/1/table/9/data', status_code=403)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.update_table_data(database_id=1, table_id=9,
                                         data={'name': 'Josiah', 'age': 45, 'gender': 'male'},
                                         keys={'id': 1})
            except ForbiddenError:
                pass

    def test_update_table_data_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/1/table/9/data', status_code=404)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.update_table_data(database_id=1, table_id=9,
                                         data={'name': 'Josiah', 'age': 45, 'gender': 'male'},
                                         keys={'id': 1})
            except NotExistsError:
                pass

    def test_delete_table_data_succeeds(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/1/table/9/data', status_code=202)
            # test
            client = RestClient(username="a", password="b")
            client.delete_table_data(database_id=1, table_id=9, keys={'id': 1})

    def test_delete_table_data_malformed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/1/table/9/data', status_code=400)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.delete_table_data(database_id=1, table_id=9, keys={'id': 1})
            except MalformedError:
                pass

    def test_delete_table_data_not_allowed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/1/table/9/data', status_code=403)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.delete_table_data(database_id=1, table_id=9, keys={'id': 1})
            except ForbiddenError:
                pass

    def test_delete_table_data_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/1/table/9/data', status_code=404)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.delete_table_data(database_id=1, table_id=9, keys={'id': 1})
            except NotExistsError:
                pass

    def test_delete_table_data_not_auth_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/1/table/9/data', status_code=404)
            # test
            try:
                RestClient().delete_table_data(database_id=1, table_id=9, keys={'id': 1})
            except AuthenticationError:
                pass

    def test_update_table_column_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = Column(id=1,
                         ord=0,
                         name="ID",
                         database_id=1,
                         table_id=2,
                         internal_name="id",
                         auto_generated=True,
                         is_primary_key=True,
                         type=ColumnType.BIGINT,
                         is_public=True,
                         concept=ConceptBrief(id=2,
                                              uri="http://dbpedia.org/page/Category:Precipitation",
                                              name="Precipitation"),
                         unit=UnitBrief(id=2,
                                        uri="http://www.wikidata.org/entity/Q119856947",
                                        name="liters per square meter"),
                         is_null_allowed=False)
            # mock
            mock.put('/api/database/1/table/2/column/1', json=exp.model_dump(), status_code=202)
            # test
            client = RestClient(username="a", password="b")
            response = client.update_table_column(database_id=1, table_id=2, column_id=1,
                                                  unit_uri="http://www.wikidata.org/entity/Q119856947",
                                                  concept_uri="http://dbpedia.org/page/Category:Precipitation")
            self.assertEqual(exp, response)

    def test_update_table_column_malformed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/1/table/2/column/1', status_code=400)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.update_table_column(database_id=1, table_id=2, column_id=1,
                                           unit_uri="http://www.wikidata.org/entity/Q119856947",
                                           concept_uri="http://dbpedia.org/page/Category:Precipitation")
            except MalformedError:
                pass

    def test_update_table_column_not_allowed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/1/table/2/column/1', status_code=403)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.update_table_column(database_id=1, table_id=2, column_id=1,
                                           unit_uri="http://www.wikidata.org/entity/Q119856947",
                                           concept_uri="http://dbpedia.org/page/Category:Precipitation")
            except ForbiddenError:
                pass

    def test_update_table_column_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/1/table/2/column/1', status_code=404)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.update_table_column(database_id=1, table_id=2, column_id=1,
                                           unit_uri="http://www.wikidata.org/entity/Q119856947",
                                           concept_uri="http://dbpedia.org/page/Category:Precipitation")
            except NotExistsError:
                pass

    def test_update_table_column_not_auth_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/1/table/2/column/1', status_code=404)
            # test
            try:
                RestClient().update_table_column(database_id=1, table_id=2, column_id=1,
                                                 unit_uri="http://www.wikidata.org/entity/Q119856947",
                                                 concept_uri="http://dbpedia.org/page/Category:Precipitation")
            except AuthenticationError:
                pass

    def test_analyse_table_statistics_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = TableStatistics(
                columns={"id": ColumnStatistic(val_min=1.0, val_max=9.0, mean=5.0, median=5.0, std_dev=2.73)})
            # mock
            mock.get('/api/analyse/database/1/table/2/statistics', json=exp.model_dump(), status_code=202)
            # test
            response = RestClient().analyse_table_statistics(database_id=1, table_id=2)
            self.assertEqual(exp, response)

    def test_analyse_table_statistics_malformed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/analyse/database/1/table/2/statistics', status_code=400)
            # test
            try:
                RestClient().analyse_table_statistics(database_id=1, table_id=2)
            except MalformedError:
                pass

    def test_analyse_table_statistics_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/analyse/database/1/table/2/statistics', status_code=404)
            # test
            try:
                RestClient().analyse_table_statistics(database_id=1, table_id=2)
            except NotExistsError:
                pass


if __name__ == "__main__":
    unittest.main()

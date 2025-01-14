import unittest

import opensearchpy
from dbrepo.api.dto import Database, Table, Column, ColumnType, Constraints, PrimaryKey, \
    TableMinimal, ColumnMinimal, ConceptBrief, UnitBrief, UserBrief, ContainerBrief, ImageBrief
from opensearchpy import NotFoundError

from app import app
from init.clients.opensearch_client import OpenSearchClient

req = Database(id=1,
               name="Test",
               internal_name="test_tuw1",
               owner=UserBrief(id="c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502", username="foo"),
               contact=UserBrief(id="c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502", username="foo"),
               exchange_name="dbrepo",
               is_public=True,
               is_schema_public=True,
               container=ContainerBrief(id=1,
                                        name="MariaDB",
                                        internal_name="mariadb",
                                        host="data-db",
                                        image=ImageBrief(id=1,
                                                         name="mariadb",
                                                         version="11.1.3",
                                                         jdbc_method="mariadb")),
               tables=[Table(id=1,
                             database_id=1,
                             name="Data",
                             internal_name="data",
                             owner=UserBrief(id="c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502", username="foo"),
                             constraints=Constraints(uniques=[], foreign_keys=[], checks=[], primary_key=[]),
                             is_versioned=False,
                             queue_name="dbrepo",
                             routing_key="dbrepo.1.1",
                             is_public=True,
                             is_schema_public=True,
                             columns=[Column(id=1,
                                             database_id=1,
                                             table_id=1,
                                             name="ID",
                                             ord=0,
                                             internal_name="id",
                                             type=ColumnType.BIGINT,
                                             is_null_allowed=False,
                                             size=20,
                                             d=0,
                                             concept=ConceptBrief(id=1, uri="http://www.wikidata.org/entity/Q2221906"),
                                             unit=UnitBrief(id=1,
                                                            uri="http://www.ontology-of-units-of-measure.org/resource/om-2/degreeCelsius"),
                                             val_min=0,
                                             val_max=10)]
                             )])


class OpenSearchClientTest(unittest.TestCase):

    def test_update_database_succeeds(self):
        with app.app_context():
            # mock
            OpenSearchClient().update_database(database_id=req.id, data=req)

            # test
            req.tables = [Table(id=1,
                                name="Test Table",
                                internal_name="test_table",
                                queue_name="dbrepo",
                                routing_key="dbrepo.test_tuw1.test_table",
                                is_public=True,
                                is_schema_public=True,
                                database_id=req.id,
                                constraints=Constraints(uniques=[], foreign_keys=[], checks=[],
                                                        primary_key=[PrimaryKey(id=1,
                                                                                table=TableMinimal(id=1,
                                                                                                   database_id=req.id),
                                                                                column=ColumnMinimal(id=1, table_id=1,
                                                                                                     database_id=req.id))]),
                                is_versioned=True,
                                owner=UserBrief(id="c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502", username="foo"),
                                columns=[Column(id=1,
                                                database_id=req.id,
                                                table_id=1,
                                                ord=0,
                                                name="ID",
                                                internal_name="id",
                                                type=ColumnType.BIGINT,
                                                is_null_allowed=False)])]
            database = OpenSearchClient().update_database(database_id=req.id, data=req)
            self.assertEqual(1, database.id)
            self.assertEqual("Test", database.name)
            self.assertEqual("test_tuw1", database.internal_name)
            self.assertEqual("c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502", database.owner.id)
            self.assertEqual("foo", database.owner.username)
            self.assertEqual("c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502", database.contact.id)
            self.assertEqual("foo", database.contact.username)
            self.assertEqual("dbrepo", database.exchange_name)
            self.assertEqual(True, database.is_public)
            self.assertEqual(1, database.container.id)
            # ...
            self.assertEqual(1, database.container.image.id)
            # ...
            self.assertEqual(1, len(database.tables))
            self.assertEqual(1, database.tables[0].id)
            self.assertEqual("Test Table", database.tables[0].name)
            self.assertEqual("test_table", database.tables[0].internal_name)
            self.assertEqual("dbrepo", database.tables[0].queue_name)
            self.assertEqual("dbrepo.test_tuw1.test_table", database.tables[0].routing_key)
            self.assertEqual(True, database.tables[0].is_public)
            self.assertEqual(1, database.tables[0].database_id)
            self.assertEqual(True, database.tables[0].is_versioned)
            self.assertEqual("c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502", database.tables[0].owner.id)
            self.assertEqual("foo", database.tables[0].owner.username)
            self.assertEqual(1, len(database.tables[0].columns))
            self.assertEqual(1, database.tables[0].columns[0].id)
            self.assertEqual("ID", database.tables[0].columns[0].name)
            self.assertEqual("id", database.tables[0].columns[0].internal_name)
            self.assertEqual(ColumnType.BIGINT, database.tables[0].columns[0].type)
            self.assertEqual(1, database.tables[0].columns[0].database_id)
            self.assertEqual(1, database.tables[0].columns[0].table_id)
            self.assertEqual(False, database.tables[0].columns[0].is_null_allowed)

    def test_update_database_create_succeeds(self):
        with app.app_context():
            # test
            database = OpenSearchClient().update_database(database_id=req.id, data=req)
            self.assertEqual(1, database.id)
            self.assertEqual("Test", database.name)
            self.assertEqual("test_tuw1", database.internal_name)
            self.assertEqual("c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502", database.owner.id)
            self.assertEqual("foo", database.owner.username)
            self.assertEqual("c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502", database.contact.id)
            self.assertEqual("foo", database.contact.username)
            self.assertEqual("dbrepo", database.exchange_name)
            self.assertEqual(True, database.is_public)
            self.assertEqual(1, database.container.id)
            # ...
            self.assertEqual(1, database.container.image.id)
            # ...
            self.assertEqual(1, len(database.tables))

    def test_update_database_malformed_fails(self):
        with app.app_context():
            app.config['OPENSEARCH_USERNAME'] = 'i_do_not_exist'

            # test
            try:
                database = OpenSearchClient().update_database(database_id=req.id, data=req)
            except opensearchpy.exceptions.TransportError:
                pass

    def test_delete_database_fails(self):
        with app.app_context():

            # test
            try:
                OpenSearchClient().delete_database(database_id=9999)
            except opensearchpy.exceptions.NotFoundError:
                pass

    def test_delete_database_succeeds(self):
        with app.app_context():
            # mock
            OpenSearchClient().update_database(database_id=req.id, data=req)

            # test
            OpenSearchClient().delete_database(database_id=req.id)

    def test_get_database_succeeds(self):
        with app.app_context():
            # mock
            OpenSearchClient().update_database(database_id=req.id, data=req)

            # test
            database = OpenSearchClient().get_database(database_id=req.id)
            self.assertEqual(req.id, database.id)

    def test_get_database_fails(self):
        with app.app_context():

            # mock
            OpenSearchClient().update_database(database_id=req.id, data=req)

            # test
            try:
                OpenSearchClient().get_database(database_id=req.id)
            except opensearchpy.exceptions.NotFoundError:
                pass

    def test_get_fields_for_index_database_succeeds(self):
        with app.app_context():
            # mock
            OpenSearchClient().update_database(database_id=req.id, data=req)

            # test
            response = OpenSearchClient().get_fields_for_index(field_type="database")
            self.assertTrue(len(response) > 0)

    def test_get_fields_for_index_user_succeeds(self):
        with app.app_context():
            # mock
            OpenSearchClient().update_database(database_id=req.id, data=req)

            # test
            response = OpenSearchClient().get_fields_for_index(field_type="user")
            self.assertTrue(len(response) > 0)

    def test_fuzzy_search_succeeds(self):
        with app.app_context():
            # mock
            OpenSearchClient().update_database(database_id=req.id, data=req)

            # test
            response = OpenSearchClient().fuzzy_search(search_term="test")
            self.assertTrue(len(response) > 0)

    def test_unit_independent_search_fails(self):
        with app.app_context():
            # mock
            OpenSearchClient().update_database(database_id=req.id, data=req)

            # test
            try:
                OpenSearchClient().unit_independent_search(0, 100, {
                    "unit.uri": "http://www.ontology-of-units-of-measure.org/resource/om-2/degreeCelsius"})
                self.fail()
            except NotFoundError:
                pass

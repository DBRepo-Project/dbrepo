import os
import unittest

import opensearchpy
from dbrepo.api.dto import Database, Table, Column, ColumnType, Constraints, PrimaryKey, \
    ConceptBrief, UnitBrief, UserBrief, ContainerBrief, ImageBrief, TableBrief, ColumnBrief
from opensearchpy import NotFoundError

from clients.opensearch_client import OpenSearchClient

req = Database(id="209acf92-5c9b-4633-ad99-113c86f6e948",
               name="Test",
               internal_name="test_tuw1",
               owner=UserBrief(id="c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502", username="foo"),
               contact=UserBrief(id="c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502", username="foo"),
               exchange_name="dbrepo",
               is_public=True,
               is_schema_public=True,
               is_dashboard_enabled=True,
               container=ContainerBrief(id="7efe8b27-6cdc-4387-80e3-92ee28f4a7c5",
                                        name="MariaDB",
                                        internal_name="mariadb",
                                        image=ImageBrief(id="f97791b4-baf4-4b18-8f7d-3084818e6549",
                                                         name="mariadb",
                                                         version="11.1.3",
                                                         default=True)),
               tables=[Table(id="f94a6164-cad4-4873-a9fd-3fe5313b2e95",
                             database_id="209acf92-5c9b-4633-ad99-113c86f6e948",
                             name="Data",
                             internal_name="data",
                             owner=UserBrief(id="c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502", username="foo"),
                             constraints=Constraints(uniques=[], foreign_keys=[], checks=[], primary_key=[]),
                             is_versioned=False,
                             queue_name="dbrepo",
                             routing_key="dbrepo.1.1",
                             is_public=True,
                             is_schema_public=True,
                             columns=[Column(id="7bef7e68-88f1-438e-9b94-0a77afd21471",
                                             database_id="209acf92-5c9b-4633-ad99-113c86f6e948",
                                             table_id="f94a6164-cad4-4873-a9fd-3fe5313b2e95",
                                             name="ID",
                                             ord=0,
                                             internal_name="id",
                                             type=ColumnType.BIGINT,
                                             is_null_allowed=False,
                                             size=20,
                                             d=0,
                                             concept=ConceptBrief(id="fb32ecf6-1f68-49b4-85ee-04e76263cbef",
                                                                  uri="http://www.wikidata.org/entity/Q2221906"),
                                             unit=UnitBrief(id="a67d735e-32ef-4917-b412-fe099c6757a1",
                                                            uri="http://www.ontology-of-units-of-measure.org/resource/om-2/degreeCelsius"),
                                             val_min=0,
                                             val_max=10)]
                             )])


class SearchServiceClientUnitTest(unittest.TestCase):

    def test_update_database_succeeds(self):
        req.tables = [Table(id="f94a6164-cad4-4873-a9fd-3fe5313b2e95",
                            name="Test Table",
                            internal_name="test_table",
                            queue_name="dbrepo",
                            routing_key="dbrepo.test_tuw1.test_table",
                            is_public=True,
                            is_schema_public=True,
                            database_id=req.id,
                            constraints=Constraints(uniques=[],
                                                    foreign_keys=[],
                                                    checks=[],
                                                    primary_key=[PrimaryKey(id="f0d4dfdf-d987-4c73-aa40-1038db79bb31",
                                                                            table=TableBrief(
                                                                                id="f94a6164-cad4-4873-a9fd-3fe5313b2e95",
                                                                                database_id=req.id,
                                                                                name="Test Table",
                                                                                internal_name="test_table",
                                                                                is_public=True,
                                                                                is_schema_public=True,
                                                                                is_versioned=True,
                                                                                owned_by="c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502"),
                                                                            column=ColumnBrief(
                                                                                id="90d81c6a-e69a-4413-97b8-fd2266a6e641",
                                                                                name="ID",
                                                                                database_id=req.id,
                                                                                table_id="90d81c6a-e69a-4413-97b8-fd2266a6e641",
                                                                                internal_name="id",
                                                                                type=ColumnType.BIGINT))]),
                            is_versioned=True,
                            owner=UserBrief(id="c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502", username="foo"),
                            columns=[Column(id="c63bde4a-61e4-42f1-ab64-350579c0691f",
                                            database_id=req.id,
                                            table_id="f94a6164-cad4-4873-a9fd-3fe5313b2e95",
                                            ord=0,
                                            name="ID",
                                            internal_name="id",
                                            type=ColumnType.BIGINT,
                                            is_null_allowed=False)])]
        # mock
        OpenSearchClient().update_database(database_id=req.id, data=req)

        # test
        database = OpenSearchClient().update_database(database_id=req.id, data=req)
        self.assertEqual("209acf92-5c9b-4633-ad99-113c86f6e948", database.id)
        self.assertEqual("Test", database.name)
        self.assertEqual("test_tuw1", database.internal_name)
        self.assertEqual("c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502", database.owner.id)
        self.assertEqual("foo", database.owner.username)
        self.assertEqual("c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502", database.contact.id)
        self.assertEqual("foo", database.contact.username)
        self.assertEqual("dbrepo", database.exchange_name)
        self.assertEqual(True, database.is_public)
        self.assertEqual("7efe8b27-6cdc-4387-80e3-92ee28f4a7c5", database.container.id)
        # ...
        self.assertEqual("f97791b4-baf4-4b18-8f7d-3084818e6549", database.container.image.id)
        # ...
        self.assertEqual(1, len(database.tables))
        self.assertEqual("f94a6164-cad4-4873-a9fd-3fe5313b2e95", database.tables[0].id)
        self.assertEqual("Test Table", database.tables[0].name)
        self.assertEqual("test_table", database.tables[0].internal_name)
        self.assertEqual("dbrepo", database.tables[0].queue_name)
        self.assertEqual("dbrepo.test_tuw1.test_table", database.tables[0].routing_key)
        self.assertEqual(True, database.tables[0].is_public)
        self.assertEqual("209acf92-5c9b-4633-ad99-113c86f6e948", database.tables[0].database_id)
        self.assertEqual(True, database.tables[0].is_versioned)
        self.assertEqual("c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502", database.tables[0].owner.id)
        self.assertEqual("foo", database.tables[0].owner.username)
        self.assertEqual(1, len(database.tables[0].columns))
        self.assertEqual("c63bde4a-61e4-42f1-ab64-350579c0691f", database.tables[0].columns[0].id)
        self.assertEqual("ID", database.tables[0].columns[0].name)
        self.assertEqual("id", database.tables[0].columns[0].internal_name)
        self.assertEqual(ColumnType.BIGINT, database.tables[0].columns[0].type)
        self.assertEqual("209acf92-5c9b-4633-ad99-113c86f6e948", database.tables[0].columns[0].database_id)
        self.assertEqual("f94a6164-cad4-4873-a9fd-3fe5313b2e95", database.tables[0].columns[0].table_id)
        self.assertEqual(False, database.tables[0].columns[0].is_null_allowed)

    def test_update_database_create_succeeds(self):
        # test
        database = OpenSearchClient().update_database(database_id=req.id, data=req)
        self.assertEqual("209acf92-5c9b-4633-ad99-113c86f6e948", database.id)
        self.assertEqual("Test", database.name)
        self.assertEqual("test_tuw1", database.internal_name)
        self.assertEqual("c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502", database.owner.id)
        self.assertEqual("foo", database.owner.username)
        self.assertEqual("c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502", database.contact.id)
        self.assertEqual("foo", database.contact.username)
        self.assertEqual("dbrepo", database.exchange_name)
        self.assertEqual(True, database.is_public)
        self.assertEqual("7efe8b27-6cdc-4387-80e3-92ee28f4a7c5", database.container.id)
        # ...
        self.assertEqual("f97791b4-baf4-4b18-8f7d-3084818e6549", database.container.image.id)
        # ...
        self.assertEqual(1, len(database.tables))

    def test_update_database_malformed_fails(self):
        os.environ['OPENSEARCH_USERNAME'] = 'i_do_not_exist'

        # test
        try:
            database = OpenSearchClient().update_database(database_id=req.id, data=req)
        except opensearchpy.exceptions.TransportError:
            pass

    def test_delete_database_fails(self):

        # test
        try:
            OpenSearchClient().delete_database(database_id="deadbeef-a5aa-49bb-87e7-6c6271731a1a")
        except opensearchpy.exceptions.NotFoundError:
            pass

    def test_delete_database_succeeds(self):
        # mock
        OpenSearchClient().update_database(database_id=req.id, data=req)

        # test
        OpenSearchClient().delete_database(database_id=req.id)

    def test_get_fields_for_index_database_succeeds(self):
        # mock
        OpenSearchClient().update_database(database_id=req.id, data=req)

        # test
        response = OpenSearchClient().get_fields_for_index(field_type="database")
        self.assertTrue(len(response) > 0)

    def test_get_fields_for_index_user_succeeds(self):
        # mock
        OpenSearchClient().update_database(database_id=req.id, data=req)

        # test
        response = OpenSearchClient().get_fields_for_index(field_type="user")
        self.assertTrue(len(response) > 0)

    def test_fuzzy_search_succeeds(self):
        # mock
        OpenSearchClient().update_database(database_id=req.id, data=req)

        # test
        OpenSearchClient().fuzzy_search(search_term="test_tuw")

    def test_unit_independent_search_fails(self):
        # mock
        OpenSearchClient().update_database(database_id=req.id, data=req)

        # test
        try:
            OpenSearchClient().unit_independent_search(0, 100, {
                "unit.uri": "http://www.ontology-of-units-of-measure.org/resource/om-2/degreeCelsius"})
            self.fail()
        except NotFoundError:
            pass

import datetime
import unittest

from app import app

from clients.opensearch_client import OpenSearchClient


class OpenSearchClientTest(unittest.TestCase):

    def test_index_exists_succeeds(self):
        with app.app_context():
            client = RestClient(endpoint=self.metadata_service_endpoint)
            # mock
            client.update_database(database_id=1, data=req)

            # test
            req.tables = [Table(id=1,
                                name="Test Table",
                                internal_name="test_table",
                                queue_name="dbrepo",
                                routing_key="dbrepo.test_tuw1.test_table",
                                is_public=True,
                                database_id=req.id,
                                constraints=Constraints(uniques=[], foreign_keys=[], checks=[],
                                                        primary_key=[PrimaryKey(id=1,
                                                                                table=TableMinimal(id=1, database_id=1),
                                                                                column=ColumnMinimal(id=1, table_id=1,
                                                                                                     database_id=1))]),
                                is_versioned=True,
                                created_by="c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502",
                                creator=User(id="c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502",
                                             username="foo",
                                             attributes=UserAttributes(theme="dark")),
                                owner=User(id="c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502",
                                           username="foo",
                                           attributes=UserAttributes(theme="dark")),
                                created=datetime.datetime(2024, 4, 25, 17, 44, tzinfo=datetime.timezone.utc),
                                columns=[Column(id=1,
                                                name="ID",
                                                internal_name="id",
                                                database_id=req.id,
                                                table_id=1,
                                                column_type=ColumnType.BIGINT,
                                                is_public=True,
                                                is_null_allowed=False)])]
            database = client.update_database(database_id=1, data=req)
            self.assertEqual(1, database.id)
            self.assertEqual("Test", database.name)
            self.assertEqual("test_tuw1", database.internal_name)
            self.assertEqual("c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502", database.creator.id)
            self.assertEqual("foo", database.creator.username)
            self.assertEqual("dark", database.creator.attributes.theme)
            self.assertEqual("c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502", database.owner.id)
            self.assertEqual("foo", database.owner.username)
            self.assertEqual("dark", database.owner.attributes.theme)
            self.assertEqual("c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502", database.contact.id)
            self.assertEqual("foo", database.contact.username)
            self.assertEqual("dark", database.contact.attributes.theme)
            self.assertEqual(datetime.datetime(2024, 3, 25, 16, tzinfo=datetime.timezone.utc), database.created)
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
            self.assertEqual("c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502", database.tables[0].created_by)
            self.assertEqual("c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502", database.tables[0].creator.id)
            self.assertEqual("foo", database.tables[0].creator.username)
            self.assertEqual("dark", database.tables[0].creator.attributes.theme)
            self.assertEqual("c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502", database.tables[0].owner.id)
            self.assertEqual("foo", database.tables[0].owner.username)
            self.assertEqual("dark", database.tables[0].owner.attributes.theme)
            self.assertEqual(datetime.datetime(2024, 4, 25, 17, 44, tzinfo=datetime.timezone.utc),
                             database.tables[0].created)
            self.assertEqual(1, len(database.tables[0].columns))
            self.assertEqual(1, database.tables[0].columns[0].id)
            self.assertEqual("ID", database.tables[0].columns[0].name)
            self.assertEqual("id", database.tables[0].columns[0].internal_name)
            self.assertEqual(ColumnType.BIGINT, database.tables[0].columns[0].column_type)
            self.assertEqual(1, database.tables[0].columns[0].database_id)
            self.assertEqual(1, database.tables[0].columns[0].table_id)
            self.assertEqual(True, database.tables[0].columns[0].is_public)
            self.assertEqual(False, database.tables[0].columns[0].is_null_allowed)

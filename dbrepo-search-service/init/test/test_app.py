import datetime
import unittest

from dbrepo.api.dto import Database, User, UserAttributes, Container, Image, Table, Constraints, Column, ColumnType, \
    Concept, Unit, DataType

from app import app
from clients.opensearch_client import OpenSearchClient

req = Database(id=1,
               name="Test",
               internal_name="test_tuw1",
               creator=User(id="c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502",
                            username="foo",
                            attributes=UserAttributes(theme="dark")),
               owner=User(id="c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502",
                          username="foo",
                          attributes=UserAttributes(theme="dark")),
               contact=User(id="c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502",
                            username="foo",
                            attributes=UserAttributes(theme="dark")),
               created=datetime.datetime(2024, 3, 25, 16, tzinfo=datetime.timezone.utc),
               exchange_name="dbrepo",
               is_public=True,
               container=Container(id=1,
                                   name="MariaDB",
                                   internal_name="mariadb",
                                   host="data-db",
                                   port="3306",
                                   created=datetime.datetime(2024, 3, 1, 10, tzinfo=datetime.timezone.utc),
                                   sidecar_host="data-db-sidecar",
                                   sidecar_port=3305,
                                   image=Image(id=1,
                                               registry="docker.io",
                                               name="mariadb",
                                               version="11.1.3",
                                               dialect="org.hibernate.dialect.MariaDBDialect",
                                               driver_class="org.mariadb.jdbc.Driver",
                                               jdbc_method="mariadb",
                                               default_port=3306,
                                               data_types=[
                                                   DataType(display_name="SERIAL", value="serial",
                                                            documentation="https://mariadb.com/kb/en/bigint/",
                                                            is_quoted=False, is_buildable=True)])),
               tables=[Table(id=1, database_id=1, name="Data", internal_name="data",
                             creator=User(id="c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502",
                                          username="foo",
                                          attributes=UserAttributes(theme="dark")),
                             owner=User(id="c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502",
                                        username="foo",
                                        attributes=UserAttributes(theme="dark")),
                             created=datetime.datetime(2024, 3, 1, 10, tzinfo=datetime.timezone.utc),
                             constraints=Constraints(uniques=[], foreign_keys=[], checks=[], primary_key=[]),
                             is_versioned=True,
                             created_by="c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502",
                             queue_name="dbrepo",
                             routing_key="dbrepo.1.1",
                             is_public=True,
                             columns=[Column(id=1, database_id=1, table_id=1, name="ID", internal_name="id",
                                             column_type=ColumnType.BIGINT, is_public=True, is_null_allowed=False,
                                             size=20, d=0,
                                             concept=Concept(id=1, uri="http://www.wikidata.org/entity/Q2221906",
                                                             created=datetime.datetime(2024, 3, 1, 10,
                                                                                       tzinfo=datetime.timezone.utc)),
                                             unit=Unit(id=1,
                                                       uri="http://www.ontology-of-units-of-measure.org/resource/om-2/degreeCelsius",
                                                       created=datetime.datetime(2024, 3, 1, 10,
                                                                                 tzinfo=datetime.timezone.utc)),
                                             val_min=0,
                                             val_max=10)]
                             )])


class OpenSearchClientTest(unittest.TestCase):

    def test_index_exists_succeeds(self):
        with app.app_context():
            client = OpenSearchClient()
            # mock
            client.update_database(database_id=1, data=req)

            # test
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
            self.assertEqual("Data", database.tables[0].name)
            self.assertEqual("data", database.tables[0].internal_name)
            self.assertEqual("dbrepo", database.tables[0].queue_name)
            self.assertEqual("dbrepo.1.1", database.tables[0].routing_key)
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
            self.assertEqual(datetime.datetime(2024, 3, 1, 10, tzinfo=datetime.timezone.utc),
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

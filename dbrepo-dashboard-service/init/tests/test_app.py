import unittest

import requests_mock
from dbrepo.api.dto import Database, Table, Constraints, Column, ColumnType, ConceptBrief, UnitBrief, \
    UserBrief, ContainerBrief, ImageBrief, DatabaseBrief
from dbrepo.api.exceptions import NotExistsError

from app import fetch_databases

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


class AppUnitTest(unittest.TestCase):

    def test_fetch_databases_succeeds(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database',
                     json=[DatabaseBrief(id='209acf92-5c9b-4633-ad99-113c86f6e948',
                                         name="Test",
                                         internal_name="test_tuw1",
                                         owner_id="c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502",
                                         is_public=True,
                                         is_schema_public=True,
                                         contact=UserBrief(id="c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502",
                                                           username="foo")).model_dump()])
            mock.get(f'/api/database/{req.id}', json=req.model_dump())
            # test
            response = fetch_databases()
            self.assertEqual(1, len(response))

    def test_fetch_databases_empty_succeeds(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database', json=[])
            # test
            response = fetch_databases()
            self.assertEqual(0, len(response))

    def test_fetch_databases_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database',
                     json=[DatabaseBrief(id='209acf92-5c9b-4633-ad99-113c86f6e948',
                                         name="Test",
                                         internal_name="test_tuw1",
                                         owner_id="c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502",
                                         is_public=True,
                                         is_schema_public=True,
                                         contact=UserBrief(id="c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502",
                                                           username="foo")).model_dump()])
            mock.get(f'/api/database/{req.id}', status_code=404)
            # test
            try:
                fetch_databases()
            except NotExistsError:
                pass
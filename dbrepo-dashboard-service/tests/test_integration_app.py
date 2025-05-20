import logging
import os
import time
import unittest

import jwt
from dbrepo.api.dto import Database, Table, Constraints, Column, ColumnType, ConceptBrief, UnitBrief, \
    UserBrief, ContainerBrief, ImageBrief
from dbrepo.core.client.dashboard import DashboardServiceClient

from app import app

req = Database(id="209acf92-5c9b-4633-ad99-113c86f6e948",
               name="Test",
               internal_name="test_tuw1",
               dashboard_uid="2432cf61e71dea",
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


class AppIntegrationTest(unittest.TestCase):

    def token(self, roles: [str], iat: int = int(time.time())):
        claims = {
            'iat': iat,
            'uid': 'c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502',
            'preferred_username': 'foo',
            'realm_access': {
                'roles': roles
            }
        }
        with open('./tests/rsa/rs256.key', 'rb') as fh:
            return jwt.JWT().encode(claims, jwt.jwk_from_pem(fh.read()), alg='RS256')

    def dashboard_client(self):
        return DashboardServiceClient(os.getenv('DASHBOARD_UI_ENDPOINT', 'http://localhost:3000'),
                                      os.getenv('SYSTEM_USERNAME', 'admin'), os.getenv('SYSTEM_PASSWORD', 'admin'))

    def test_create_dashboard_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.post('/api/dashboard',
                                        headers={'Authorization': f'Bearer {self.token(["system"])}'})
            self.assertEqual(415, response.status_code)

    def test_health_succeeds(self):
        with app.test_client() as test_client:
            # test
            response = test_client.get('/health')
            self.assertEqual(200, response.status_code)

    def test_create_dashboard_no_auth_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.post('/api/dashboard')
            self.assertEqual(401, response.status_code)

    def test_create_dashboard_no_body_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.post('/api/dashboard',
                                        headers={'Authorization': f'Bearer {self.token(["system"])}',
                                                 'Content-Type': 'application/json'})
            self.assertEqual(400, response.status_code)

    def test_create_dashboard_empty_body_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.post('/api/dashboard',
                                        headers={'Authorization': f'Bearer {self.token(["system"])}',
                                                 'Content-Type': 'application/json'},
                                        json={})
            self.assertEqual(400, response.status_code)

    def test_create_dashboard_malformed_body_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.post('/api/dashboard',
                                        headers={'Authorization': f'Bearer {self.token(["system"])}',
                                                 'Content-Type': 'application/json'},
                                        json=dict({'is_public': True}))
            self.assertEqual(400, response.status_code)

    def test_create_dashboard_succeeds(self):
        with app.test_client() as test_client:
            # test
            response = test_client.post('/api/dashboard',
                                        headers={'Authorization': f'Bearer {self.token(["system"])}',
                                                 'Content-Type': 'application/json'},
                                        json=dict({'is_public': True,
                                                   'is_schema_public': True,
                                                   'database_name': 'some_database',
                                                   'owner_username': 'foobar'}))
            self.assertEqual(201, response.status_code)

    def test_update_dashboard_no_auth_fails(self):
        with app.test_client() as test_client:
            headers = {'Authorization': f'Bearer {self.token(["system"])}', 'Content-Type': 'application/json'}
            json_payload = dict({'is_public': True,
                                 'is_schema_public': True,
                                 'database_name': 'some_database',
                                 'owner_username': 'foobar'})
            # mock
            response = test_client.post('/api/dashboard', headers=headers, json=json_payload)
            # test
            response = test_client.put(f"/api/dashboard/{response.json['uid']}")
            self.assertEqual(401, response.status_code)

    def test_update_dashboard_no_body_fails(self):
        with app.test_client() as test_client:
            headers = {'Authorization': f'Bearer {self.token(["system"])}', 'Content-Type': 'application/json'}
            json_payload = dict({'is_public': True,
                                 'is_schema_public': True,
                                 'database_name': 'some_database',
                                 'owner_username': 'foobar'})
            # mock
            response = test_client.post('/api/dashboard', headers=headers, json=json_payload)
            # test
            response = test_client.put(f"/api/dashboard/{response.json['uid']}",
                                       headers=headers)
            self.assertEqual(400, response.status_code)

    def test_update_dashboard_empty_body_fails(self):
        with app.test_client() as test_client:
            headers = {'Authorization': f'Bearer {self.token(["system"])}', 'Content-Type': 'application/json'}
            json_payload = dict({'is_public': True,
                                 'is_schema_public': True,
                                 'database_name': 'some_database',
                                 'owner_username': 'foobar'})
            # mock
            response = test_client.post('/api/dashboard', headers=headers, json=json_payload)
            # test
            response = test_client.put(f"/api/dashboard/{response.json['uid']}", headers=headers, json={})
            self.assertEqual(400, response.status_code)

    def test_update_dashboard_malformed_body_fails(self):
        with app.test_client() as test_client:
            headers = {'Authorization': f'Bearer {self.token(["system"])}', 'Content-Type': 'application/json'}
            json_payload = dict({'is_public': True,
                                 'is_schema_public': True,
                                 'database_name': 'some_database',
                                 'owner_username': 'foobar'})
            # mock
            response = test_client.post('/api/dashboard', headers=headers, json=json_payload)
            # test
            response = test_client.put(f"/api/dashboard/{response.json['uid']}", headers=headers,
                                       json=dict({'is_public': True}))
            self.assertEqual(400, response.status_code)

    def test_update_dashboard_succeeds(self):
        with app.test_client() as test_client:
            headers = {'Authorization': f'Bearer {self.token(["system"])}', 'Content-Type': 'application/json'}
            json_payload = dict({'is_public': True,
                                 'is_schema_public': True,
                                 'is_dashboard_enabled': True,
                                 'database_name': 'some_database',
                                 'owner_username': 'foobar'})
            # mock
            response = test_client.post('/api/dashboard', headers=headers, json=json_payload)
            req.dashboard_uid = response.json['uid']
            # test
            response = test_client.put(f"/api/dashboard/{req.dashboard_uid}", headers=headers,
                                       json=req.model_dump())
            self.assertEqual(202, response.status_code)
            dashboard = self.dashboard_client().find(req.dashboard_uid)['dashboard']
            self.assertIsNotNone(dashboard['title'])
            self.assertEqual(['managed'], dashboard['tags'])
            self.assertEqual(1, len(dashboard['links']))
            self.assertEqual('Database', dashboard['links'][0]['title'])
            self.assertEqual('link', dashboard['links'][0]['type'])
            self.assertEqual('info', dashboard['links'][0]['icon'])
            self.assertEqual(f'http://localhost/database/{req.id}', dashboard['links'][0]['url'])
            self.assertEqual(2, len(dashboard['panels']))
            panel0 = dashboard['panels'][0]
            self.assertEqual('row', panel0['type'])
            self.assertEqual('Generated Dashboard', panel0['title'])
            panel1 = dashboard['panels'][1]
            self.assertEqual('stat', panel1['type'])
            self.assertEqual('Datasources', panel1['title'])
            self.assertEqual('Auto-generated', panel1['description'])

    def test_update_dashboard_unmanaged_succeeds(self):
        req = Database(id="209acf92-5c9b-4633-ad99-113c86f6e948",
                       name="Test",
                       internal_name="test_tuw1",
                       dashboard_uid="2432cf61e71dea",
                       owner=UserBrief(id="c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502", username="foo"),
                       contact=UserBrief(id="c6b71ef5-2d2f-48b2-9d79-b8f23a3a0502", username="foo"),
                       exchange_name="dbrepo",
                       is_public=True,
                       is_schema_public=True,
                       is_dashboard_enabled=False,  # <<<
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
        with app.test_client() as test_client:
            headers = {'Authorization': f'Bearer {self.token(["system"])}', 'Content-Type': 'application/json'}
            json_payload = dict({'is_public': True,
                                 'is_schema_public': True,
                                 'is_dashboard_enabled': False,
                                 'database_name': 'some_database',
                                 'owner_username': 'foobar'})
            # mock
            response = test_client.post('/api/dashboard', headers=headers, json=json_payload)
            req.dashboard_uid = response.json['uid']
            # test
            response = test_client.put(f"/api/dashboard/{req.dashboard_uid}", headers=headers,
                                       json=req.model_dump())
            self.assertEqual(202, response.status_code)
            dashboard = self.dashboard_client().find(req.dashboard_uid)['dashboard']
            self.assertEqual([], dashboard['tags'])

    def test_update_dashboard_not_found_created_succeeds(self):
        with app.test_client() as test_client:
            headers = {'Authorization': f'Bearer {self.token(["system"])}', 'Content-Type': 'application/json'}
            # test
            response = test_client.put(f"/api/dashboard/{req.dashboard_uid}", headers=headers, json=req.model_dump())
            self.assertEqual(202, response.status_code)

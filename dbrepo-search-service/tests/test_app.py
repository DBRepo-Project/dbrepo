import json
import time
import unittest
from typing import List

import jwt
from dbrepo.api.dto import Database, Table, Constraints, Column, ColumnType, ConceptBrief, UnitBrief, \
    UserBrief, ContainerBrief, ImageBrief

from app import app

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


class JwtTest(unittest.TestCase):

    def token(self, roles: List[str], iat: int = int(time.time())):
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

    def test_update_database_media_type_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.put(f'/api/v1/search/database/{req.id}',
                                       headers={'Authorization': f'Bearer {self.token(["system"])}'})
            self.assertEqual(415, response.status_code)

    def test_health_succeeds(self):
        with app.test_client() as test_client:
            # test
            response = test_client.get('/health')
            self.assertEqual(200, response.status_code)

    def test_update_database_no_auth_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.put(f'/api/v1/search/database/{req.id}')
            self.assertEqual(401, response.status_code)

    def test_update_database_no_body_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.put(f'/api/v1/search/database/{req.id}',
                                       headers={'Authorization': f'Bearer {self.token(["system"])}',
                                                'Content-Type': 'application/json'})
            self.assertEqual(400, response.status_code)

    def test_update_database_empty_body_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.put(f'/api/v1/search/database/{req.id}',
                                       headers={'Authorization': f'Bearer {self.token(["system"])}',
                                                'Content-Type': 'application/json'},
                                       data={})
            self.assertEqual(400, response.status_code)

    def test_update_database_malformed_body_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.put(f'/api/v1/search/database/{req.id}',
                                       headers={'Authorization': f'Bearer {self.token(["system"])}',
                                                'Content-Type': 'application/json'},
                                       data=dict({"id": 1}))
            self.assertEqual(400, response.status_code)

    def test_update_database_succeeds(self):
        with app.test_client() as test_client:
            # test
            response = test_client.put(f'/api/v1/search/database/{req.id}',
                                       headers={'Authorization': f'Bearer {self.token(["system"])}',
                                                'Content-Type': 'application/json'},
                                       data=req.model_dump_json())
            self.assertEqual(202, response.status_code)

    def test_get_fields_succeeds(self):
        with app.test_client() as test_client:
            # test
            response = test_client.get('/api/v1/search/database/fields', headers={'Content-Type': 'application/json'})
            self.assertEqual(200, response.status_code)

    def test_get_fields_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.get('/api/v1/search/unknown/fields', headers={'Content-Type': 'application/json'})
            self.assertEqual(404, response.status_code)

    def test_delete_database_no_auth_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.delete('/api/v1/search/database/1')
            self.assertEqual(401, response.status_code)

    def test_delete_database_no_role_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.delete(f'/api/v1/search/database/{req.id}',
                                          headers={'Authorization': f'Bearer {self.token([])}'})
            self.assertEqual(403, response.status_code)

    def test_delete_database_succeeds(self):
        with app.test_client() as test_client:
            # mock
            test_client.put(f'/api/v1/search/database/{req.id}',
                            headers={'Authorization': f'Bearer {self.token(["system"])}',
                                     'Content-Type': 'application/json'},
                            data=req.model_dump_json())
            # test
            response = test_client.delete(f'/api/v1/search/database/{req.id}',
                                          headers={'Authorization': f'Bearer {self.token(["system"])}'})
            self.assertEqual(202, response.status_code)

    def test_delete_database_not_found_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.delete(f'/api/v1/search/database/{req.id}',
                                          headers={'Authorization': f'Bearer {self.token(["system"])}'})
            self.assertEqual(404, response.status_code)

    def test_get_fuzzy_search_succeeds(self):
        with app.test_client() as test_client:
            # test
            response = test_client.get('/api/v1/search?q=test')
            self.assertEqual(200, response.status_code)

    def test_get_fuzzy_search_no_query_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.get('/api/v1/search')
            self.assertEqual(400, response.status_code)

    def test_get_index_succeeds(self):
        with app.test_client() as test_client:
            # test
            response = test_client.get('/api/v1/search/table')
            self.assertEqual(200, response.status_code)

    def test_get_index_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.get('/api/v1/search/unknown')
            self.assertEqual(404, response.status_code)

    def test_post_general_search_media_type_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.post('/api/v1/search/database')
            self.assertEqual(415, response.status_code)

    def test_post_general_search_no_body_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.post('/api/v1/search/database', headers={'Content-Type': 'application/json'})
            self.assertEqual(400, response.status_code)

    def test_post_general_search_succeeds(self):
        with app.test_client() as test_client:
            # mock
            test_client.put(f'/api/v1/search/database/{req.id}',
                            headers={'Authorization': f'Bearer {self.token(["system"])}',
                                     'Content-Type': 'application/json'},
                            data=req.model_dump_json())
            # test
            response = test_client.post('/api/v1/search/database', headers={'Content-Type': 'application/json'},
                                        data=json.dumps({'id': '7b4942c3-6312-4f1d-bff9-cd24ea53ea05'}))
            self.assertEqual(200, response.status_code)

    def test_post_general_search_table_succeeds(self):
        with app.test_client() as test_client:
            # mock
            test_client.put(f'/api/v1/search/database/{req.id}',
                            headers={'Authorization': f'Bearer {self.token(["system"])}',
                                     'Content-Type': 'application/json'},
                            data=req.model_dump_json())
            # test
            response = test_client.post('/api/v1/search/table', headers={'Content-Type': 'application/json'},
                                        data=json.dumps({'id': '7b4942c3-6312-4f1d-bff9-cd24ea53ea05'}))
            self.assertEqual(200, response.status_code)

    def test_post_general_search_column_succeeds(self):
        with app.test_client() as test_client:
            # mock
            test_client.put(f'/api/v1/search/database/{req.id}',
                            headers={'Authorization': f'Bearer {self.token(["system"])}',
                                     'Content-Type': 'application/json'},
                            data=req.model_dump_json())
            # test
            response = test_client.post('/api/v1/search/column', headers={'Content-Type': 'application/json'},
                                        data=json.dumps({'id': '7b4942c3-6312-4f1d-bff9-cd24ea53ea05'}))
            self.assertEqual(200, response.status_code)

    def test_post_general_search_identifier_succeeds(self):
        with app.test_client() as test_client:
            # mock
            test_client.put(f'/api/v1/search/database/{req.id}',
                            headers={'Authorization': f'Bearer {self.token(["system"])}',
                                     'Content-Type': 'application/json'},
                            data=req.model_dump_json())
            # test
            response = test_client.post('/api/v1/search/identifier', headers={'Content-Type': 'application/json'},
                                        data=json.dumps({'id': '7b4942c3-6312-4f1d-bff9-cd24ea53ea05'}))
            self.assertEqual(200, response.status_code)

    def test_post_general_search_concept_succeeds(self):
        with app.test_client() as test_client:
            # mock
            test_client.put(f'/api/v1/search/database/{req.id}',
                            headers={'Authorization': f'Bearer {self.token(["system"])}',
                                     'Content-Type': 'application/json'},
                            data=req.model_dump_json())
            # test
            response = test_client.post('/api/v1/search/concept', headers={'Content-Type': 'application/json'},
                                        data=json.dumps({'id': '7b4942c3-6312-4f1d-bff9-cd24ea53ea05'}))
            self.assertEqual(200, response.status_code)

    def test_post_general_search_unit_succeeds(self):
        with app.test_client() as test_client:
            # mock
            test_client.put(f'/api/v1/search/database/{req.id}',
                            headers={'Authorization': f'Bearer {self.token(["system"])}',
                                     'Content-Type': 'application/json'},
                            data=req.model_dump_json())
            # test
            response = test_client.post('/api/v1/search/unit', headers={'Content-Type': 'application/json'},
                                        data=json.dumps({'id': '7b4942c3-6312-4f1d-bff9-cd24ea53ea05'}))
            self.assertEqual(200, response.status_code)

    def test_post_general_search_view_succeeds(self):
        with app.test_client() as test_client:
            # mock
            test_client.put(f'/api/v1/search/database/{req.id}',
                            headers={'Authorization': f'Bearer {self.token(["system"])}',
                                     'Content-Type': 'application/json'},
                            data=req.model_dump_json())
            # test
            response = test_client.post('/api/v1/search/view', headers={'Content-Type': 'application/json'},
                                        data=json.dumps({'id': '7b4942c3-6312-4f1d-bff9-cd24ea53ea05'}))
            self.assertEqual(200, response.status_code)

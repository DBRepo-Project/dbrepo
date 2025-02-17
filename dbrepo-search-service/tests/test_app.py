import json
import time
import unittest

import jwt
from dbrepo.api.dto import Database, Table, Constraints, Column, ColumnType, ConceptBrief, UnitBrief, \
    UserBrief, ContainerBrief, ImageBrief

from app import app

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
               tables=[Table(id=1, database_id=1, name="Data", internal_name="data",
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
                                             ord=0,
                                             name="ID",
                                             internal_name="id",
                                             type=ColumnType.BIGINT,
                                             is_public=True,
                                             is_null_allowed=False,
                                             size=20,
                                             d=0,
                                             concept=ConceptBrief(id=1,
                                                                  uri="http://www.wikidata.org/entity/Q2221906"),
                                             unit=UnitBrief(id=1,
                                                            uri="http://www.ontology-of-units-of-measure.org/resource/om-2/degreeCelsius"),
                                             val_min=0,
                                             val_max=10)]
                             )])


class JwtTest(unittest.TestCase):

    def token(self, roles: [str], iat: int = int(time.time())):
        claims = {
            'iat': iat,
            'realm_access': {
                'roles': roles
            }
        }
        with open('test/rsa/rs256.key', 'rb') as fh:
            return jwt.JWT().encode(claims, jwt.jwk_from_pem(fh.read()), alg='RS256')

    def test_update_database_media_type_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.put('/api/search/database/1',
                                       headers={'Authorization': f'Bearer {self.token(["update-search-index"])}'})
            self.assertEqual(415, response.status_code)

    def test_health_succeeds(self):
        with app.test_client() as test_client:
            # test
            response = test_client.get('/health')
            self.assertEqual(200, response.status_code)

    def test_update_database_no_auth_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.put('/api/search/database/1')
            self.assertEqual(401, response.status_code)

    def test_update_database_no_body_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.put('/api/search/database/1',
                                       headers={'Authorization': f'Bearer {self.token(["update-search-index"])}',
                                                'Content-Type': 'application/json'})
            self.assertEqual(400, response.status_code)

    def test_update_database_empty_body_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.put('/api/search/database/1',
                                       headers={'Authorization': f'Bearer {self.token(["update-search-index"])}',
                                                'Content-Type': 'application/json'},
                                       data={})
            self.assertEqual(400, response.status_code)

    def test_update_database_malformed_body_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.put('/api/search/database/1',
                                       headers={'Authorization': f'Bearer {self.token(["update-search-index"])}',
                                                'Content-Type': 'application/json'},
                                       data=dict({"id": 1}))
            self.assertEqual(400, response.status_code)

    def test_update_database_succeeds(self):
        with app.test_client() as test_client:
            # test
            response = test_client.put('/api/search/database/1',
                                       headers={'Authorization': f'Bearer {self.token(["update-search-index"])}',
                                                'Content-Type': 'application/json'},
                                       data=req.model_dump_json())
            self.assertEqual(202, response.status_code)

    def test_get_fields_succeeds(self):
        with app.test_client() as test_client:
            # test
            response = test_client.get('/api/search/database/fields', headers={'Content-Type': 'application/json'})
            self.assertEqual(200, response.status_code)

    def test_get_fields_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.get('/api/search/unknown/fields', headers={'Content-Type': 'application/json'})
            self.assertEqual(404, response.status_code)

    def test_delete_database_no_auth_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.delete('/api/search/database/1')
            self.assertEqual(401, response.status_code)

    def test_delete_database_no_role_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.delete('/api/search/database/1',
                                          headers={'Authorization': f'Bearer {self.token([])}'})
            self.assertEqual(403, response.status_code)

    def test_delete_database_succeeds(self):
        with app.test_client() as test_client:
            # mock
            test_client.put('/api/search/database/1',
                            headers={'Authorization': f'Bearer {self.token(["update-search-index"])}',
                                     'Content-Type': 'application/json'},
                            data=req.model_dump_json())
            # test
            response = test_client.delete('/api/search/database/1',
                                          headers={'Authorization': f'Bearer {self.token(["admin"])}'})
            self.assertEqual(202, response.status_code)

    def test_delete_database_not_found_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.delete('/api/search/database/1',
                                          headers={'Authorization': f'Bearer {self.token(["admin"])}'})
            self.assertEqual(404, response.status_code)

    def test_get_fuzzy_search_succeeds(self):
        with app.test_client() as test_client:
            # test
            response = test_client.get('/api/search?q=test')
            self.assertEqual(200, response.status_code)

    def test_get_fuzzy_search_no_query_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.get('/api/search')
            self.assertEqual(400, response.status_code)

    def test_get_index_succeeds(self):
        with app.test_client() as test_client:
            # test
            response = test_client.get('/api/search/table')
            self.assertEqual(200, response.status_code)

    def test_get_index_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.get('/api/search/unknown')
            self.assertEqual(404, response.status_code)

    def test_post_general_search_media_type_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.post('/api/search/database')
            self.assertEqual(415, response.status_code)

    def test_post_general_search_no_body_fails(self):
        with app.test_client() as test_client:
            # test
            response = test_client.post('/api/search/database', headers={'Content-Type': 'application/json'})
            self.assertEqual(400, response.status_code)

    def test_post_general_search_succeeds(self):
        with app.test_client() as test_client:
            # mock
            test_client.put('/api/search/database/1',
                            headers={'Authorization': f'Bearer {self.token(["update-search-index"])}',
                                     'Content-Type': 'application/json'},
                            data=req.model_dump_json())
            # test
            response = test_client.post('/api/search/database', headers={'Content-Type': 'application/json'},
                                        data=json.dumps({'id': 1}))
            self.assertEqual(200, response.status_code)

    def test_post_general_search_table_succeeds(self):
        with app.test_client() as test_client:
            # mock
            test_client.put('/api/search/database/1',
                            headers={'Authorization': f'Bearer {self.token(["update-search-index"])}',
                                     'Content-Type': 'application/json'},
                            data=req.model_dump_json())
            # test
            response = test_client.post('/api/search/table', headers={'Content-Type': 'application/json'},
                                        data=json.dumps({'id': 1}))
            self.assertEqual(200, response.status_code)

    def test_post_general_search_column_succeeds(self):
        with app.test_client() as test_client:
            # mock
            test_client.put('/api/search/database/1',
                            headers={'Authorization': f'Bearer {self.token(["update-search-index"])}',
                                     'Content-Type': 'application/json'},
                            data=req.model_dump_json())
            # test
            response = test_client.post('/api/search/column', headers={'Content-Type': 'application/json'},
                                        data=json.dumps({'id': 1}))
            self.assertEqual(200, response.status_code)

    def test_post_general_search_identifier_succeeds(self):
        with app.test_client() as test_client:
            # mock
            test_client.put('/api/search/database/1',
                            headers={'Authorization': f'Bearer {self.token(["update-search-index"])}',
                                     'Content-Type': 'application/json'},
                            data=req.model_dump_json())
            # test
            response = test_client.post('/api/search/identifier', headers={'Content-Type': 'application/json'},
                                        data=json.dumps({'id': 1}))
            self.assertEqual(200, response.status_code)

    def test_post_general_search_concept_succeeds(self):
        with app.test_client() as test_client:
            # mock
            test_client.put('/api/search/database/1',
                            headers={'Authorization': f'Bearer {self.token(["update-search-index"])}',
                                     'Content-Type': 'application/json'},
                            data=req.model_dump_json())
            # test
            response = test_client.post('/api/search/concept', headers={'Content-Type': 'application/json'},
                                        data=json.dumps({'id': 1}))
            self.assertEqual(200, response.status_code)

    def test_post_general_search_unit_succeeds(self):
        with app.test_client() as test_client:
            # mock
            test_client.put('/api/search/database/1',
                            headers={'Authorization': f'Bearer {self.token(["update-search-index"])}',
                                     'Content-Type': 'application/json'},
                            data=req.model_dump_json())
            # test
            response = test_client.post('/api/search/unit', headers={'Content-Type': 'application/json'},
                                        data=json.dumps({'id': 1}))
            self.assertEqual(200, response.status_code)

    def test_post_general_search_view_succeeds(self):
        with app.test_client() as test_client:
            # mock
            test_client.put('/api/search/database/1',
                            headers={'Authorization': f'Bearer {self.token(["update-search-index"])}',
                                     'Content-Type': 'application/json'},
                            data=req.model_dump_json())
            # test
            response = test_client.post('/api/search/view', headers={'Content-Type': 'application/json'},
                                        data=json.dumps({'id': 1}))
            self.assertEqual(200, response.status_code)

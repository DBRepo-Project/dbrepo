#!/usr/bin/env python3
"""Check optional Bearer headers and real Search routes without external services."""
import base64
import importlib.util
import logging
import os
from pathlib import Path
import sys
import tempfile
import time
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT/'lib/python'))

import jwt
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from dbrepo.core.client.auth import AuthServiceClient
from jwt.exceptions import JWTDecodeError


class AuthorizationHeaderTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.logs = tempfile.TemporaryDirectory()
        with patch.dict(os.environ, {'LOG_PATH': cls.logs.name, 'LOG_APPLICATION_LEVEL': 'error'}):
            spec = importlib.util.spec_from_file_location('search_header_test_app', ROOT/'dbrepo-search-service/app.py')
            cls.service = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(cls.service)
        private = rsa.generate_private_key(public_exponent=65537, key_size=2048)
        signing = jwt.jwk_from_pem(private.private_bytes(serialization.Encoding.PEM,
            serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))
        cls.public = dict(signing.to_dict(), kid='header-fixture', use='sig', alg='RS256')
        cls.token = jwt.JWT().encode({'uid': 'fixture-id', 'preferred_username': 'fixture-user',
            'realm_access': {'roles': ['read-database']}, 'exp': int(time.time())+300},
            signing, alg='RS256', optional_headers={'kid': 'header-fixture'})

    @classmethod
    def tearDownClass(cls):
        logging.shutdown()
        cls.logs.cleanup()

    def test_optional_header_parsing_and_signed_identity(self):
        auth = AuthServiceClient('http://auth.invalid', 'fixture', 'synthetic')
        for method, expected in [(auth.get_user_id, 'fixture-id'), (auth.get_username, 'fixture-user')]:
            self.assertEqual((None, None, None), method(None))
            with patch.object(auth, 'verify_jwt') as verify:
                for header in ['', ' ', 'Bearer', 'Bearer ', 'Bearer\t', 'Bearer one two', 'Basic synthetic']:
                    with self.subTest(method=method.__name__, header=header):
                        identity, error, status = method(header)
                        self.assertIsNone(identity)
                        self.assertEqual(401, status)
                        self.assertEqual('search.user.unauthorized', error['code'])
                verify.assert_not_called()
            with patch('dbrepo.core.client.auth._realm_keys', return_value=[self.public]):
                for scheme in ['Bearer ', 'bearer ', 'BEARER\t']:
                    self.assertEqual((expected,None,None), method(scheme+self.token))
            with patch.object(auth, 'verify_jwt', side_effect=JWTDecodeError('JWT Expired')):
                self.assertEqual(401, method('Bearer synthetic')[2])
            with patch.object(auth, 'verify_jwt', side_effect=JWTDecodeError('Invalid token')):
                self.assertEqual(403, method('Bearer synthetic')[2])

    def test_search_routes_reject_malformed_headers_before_search(self):
        service = self.service
        with service.app.test_client() as client, patch.object(service, 'search_client') as search:
            for method, path, payload in [('GET','/api/v1/search?q=fixture',None),
                    ('GET','/api/v1/search/database',None),('POST','/api/v1/search/database',{})]:
                for header in ['', ' ', 'Bearer', 'Bearer ', 'Bearer\t', 'Bearer one two']:
                    with self.subTest(method=method, header=header):
                        result = client.open(path,method=method,json=payload,headers={'Authorization': header})
                        self.assertEqual(401,result.status_code)
            search.assert_not_called()

    def test_search_keeps_anonymous_signed_and_internal_basic_paths(self):
        service = self.service
        with service.app.test_client() as client, patch.object(service, 'search_client') as search:
            search.return_value.fuzzy_search.return_value=[]
            self.assertEqual(200,client.get('/api/v1/search?q=fixture').status_code)
            self.assertIsNone(search.return_value.fuzzy_search.call_args.kwargs['username'])
            with patch('dbrepo.core.client.auth._realm_keys', return_value=[self.public]):
                self.assertEqual(200,client.get('/api/v1/search?q=fixture',headers={
                    'Authorization':'Bearer '+self.token}).status_code)
            self.assertEqual('fixture-user',search.return_value.fuzzy_search.call_args.kwargs['username'])
            with patch.dict(service.app.config, {'SYSTEM_USERNAME':'fixture-system','SYSTEM_PASSWORD':'synthetic'}):
                header='Basic '+base64.b64encode(b'fixture-system:synthetic').decode()
                self.assertEqual(202,client.delete('/api/v1/search/database/fixture',headers={'Authorization':header}).status_code)
                search.return_value.delete_database.assert_called_once_with('fixture')
                self.assertEqual(401,client.delete('/api/v1/search/database/fixture').status_code)


if __name__ == '__main__':
    unittest.main()

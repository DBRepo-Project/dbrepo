import json
import time
import unittest
from unittest.mock import patch
from pathlib import Path

import jwt
import requests_mock
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives import serialization
from dbrepo.core.client.auth import AuthServiceClient, _realm_keys


def signing_key():
    key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    return jwt.jwk_from_pem(key.private_bytes(serialization.Encoding.PEM,
                            serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))


class SigningKeyTest(unittest.TestCase):
    def setUp(self):
        _realm_keys.cache_clear()
        self.key = signing_key()
        self.client = AuthServiceClient('http://installation-a', 'client', 'secret')
        self.url = 'http://installation-a/realms/dbrepo/protocol/openid-connect/certs'

    def token(self, key):
        return jwt.JWT().encode({'uid': 'user-id', 'preferred_username': 'user',
                                 'realm_access': {'roles': ['system']}, 'exp': int(time.time()) + 60},
                                key, alg='RS256', optional_headers={'kid': 'signing-key'})

    def jwks(self, key):
        return {'keys': [dict(key.to_dict(), kid='signing-key', use='sig', alg='RS256')]}

    def test_production_realm_templates_contain_no_generated_keys(self):
        root = Path(__file__).resolve().parents[3]
        for folder in ['dbrepo-auth-service', 'helm/dbrepo/files']:
            for realm in ['dbrepo', 'master']:
                data = json.loads((root / folder / f'{realm}-realm.json').read_text())
                for provider in data['components']['org.keycloak.keys.KeyProvider']:
                    self.assertNotIn('id', provider)
                    self.assertFalse(set(provider['config']) & {'privateKey', 'certificate', 'secret', 'kid'})

    def test_installation_key_is_accepted_and_cached(self):
        with requests_mock.Mocker() as mock, patch("dbrepo.core.client.auth.time.monotonic", return_value=120):
            request = mock.get(self.url, json=self.jwks(self.key))
            for _ in range(2):
                self.assertEqual('user-id', self.client.verify_jwt(self.token(self.key)).id)
            self.assertEqual(1, request.call_count)

    def test_other_installation_key_is_rejected_even_with_matching_kid(self):
        with requests_mock.Mocker() as mock:
            mock.get(self.url, json=self.jwks(self.key))
            self.assertFalse(self.client.is_valid_token(self.token(signing_key())))

    def test_retired_export_key_is_rejected_even_if_keycloak_still_serves_it(self):
        fixture = Path(__file__).resolve().parents[3] / 'dbrepo-search-service/tests/rsa/rs256.key'
        old_key = jwt.jwk_from_pem(fixture.read_bytes())
        with requests_mock.Mocker() as mock:
            mock.get(self.url, json=self.jwks(old_key))
            self.assertFalse(self.client.is_valid_token(self.token(old_key)))

    def test_keycloak_failure_and_malformed_token_fail_closed(self):
        with requests_mock.Mocker() as mock:
            mock.get(self.url, status_code=503)
            self.assertFalse(self.client.is_valid_token(self.token(self.key)))
        for token in ['broken', 'e30.e30.signature', '!!!.x.y', 'W10.e30.signature']:
            self.assertFalse(self.client.is_valid_token(token))


if __name__ == '__main__':
    unittest.main()

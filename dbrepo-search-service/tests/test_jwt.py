import time
import unittest

import jwt
import requests_mock

from app import verify_token, app, verify_password, get_user_roles
from clients.keycloak_client import User


class JwtTest(unittest.TestCase):

    def response(self, roles: [str]) -> dict:
        return dict({
            'client_id': 'username',
            'realm_access': {
                'roles': roles
            }
        })

    def token(self, roles: [str], iat: int = int(time.time())) -> str:
        claims = {
            'iat': iat,
            'realm_access': {
                'roles': roles
            }
        }
        with open('test/rsa/rs256.key', 'rb') as fh:
            return jwt.JWT().encode(claims, jwt.jwk_from_pem(fh.read()), alg='RS256')

    def test_verify_token_no_token_fails(self):
        with app.app_context():
            # test
            user = verify_token(None)
            self.assertFalse(user)

    def test_verify_token_empty_token_fails(self):
        with app.app_context():
            # test
            user = verify_token('')
            self.assertFalse(user)

    def test_verify_token_malformed_token_fails(self):
        with app.app_context():
            # test
            user = verify_token('eyEYEY12345')
            self.assertFalse(user)

    def test_verify_token_succeeds(self):
        with app.app_context():
            with requests_mock.Mocker() as mock:
                # mock
                mock.post('http://auth-service:8080/api/auth/realms/dbrepo/protocol/openid-connect/token',
                          json=self.response([]))
                # test
                user = verify_token(self.token([]))
                self.assertEqual([], user.roles)

    def test_verify_password_no_username_fails(self):
        with app.app_context():
            # test
            user = verify_password(None, 'pass')
            self.assertFalse(user)

    def test_verify_password_empty_username_fails(self):
        with app.app_context():
            # test
            user = verify_password('', 'pass')
            self.assertFalse(user)

    def test_verify_password_no_password_fails(self):
        with app.app_context():
            # test
            user = verify_password('username', None)
            self.assertFalse(user)

    def test_verify_password_empty_password_fails(self):
        with app.app_context():
            # test
            user = verify_password('username', '')
            self.assertFalse(user)

    def test_verify_password_succeeds(self):
        with app.app_context():
            with requests_mock.Mocker() as mock:
                # mock
                mock.post('http://auth-service:8080/realms/dbrepo/protocol/openid-connect/token',
                          json=self.response([]))
                # test
                user = verify_password('username', 'password')
                self.assertIsNotNone(user)

    def test_get_user_roles_succeeds(self):
        with app.app_context():
            # test
            roles: [str] = get_user_roles(
                User(id='b98415d8-28bc-4472-84ff-3d09cc79aff6', username='username', roles=[]))
            self.assertEqual([], roles)

import time
import unittest

import jwt
import requests_mock

from clients.keycloak_client import KeycloakClient


class AuthServiceClientUnitTest(unittest.TestCase):

    def response(self, username) -> dict:
        return dict({
            "client_id": username,
            "access_token": "eyEY1234"
        })

    def token(self, username: str, roles: [str], iat: int = int(time.time())) -> str:
        claims = {
            'iat': iat,
            'client_id': username,
            'realm_access': {
                'roles': roles
            }
        }
        with open('tests/rsa/rs256.key', 'rb') as fh:
            return jwt.JWT().encode(claims, jwt.jwk_from_pem(fh.read()), alg='RS256')

    def test_obtain_user_token_succeeds(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('http://auth-service:8080/realms/dbrepo/protocol/openid-connect/token',
                      json=self.response("username"))
            # test
            token = KeycloakClient().obtain_user_token("username", "password")
            self.assertEqual("eyEY1234", token)

    def test_obtain_user_token_malformed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('http://auth-service:8080/realms/dbrepo/protocol/openid-connect/token',
                      json={"client_id": "username"})
            # test
            try:
                KeycloakClient().obtain_user_token("username", "password")
                self.fail()
            except AssertionError:
                pass

    def test_verify_jwt_succeeds(self):
        # test
        user = KeycloakClient().verify_jwt(self.token("username", []))
        self.assertEqual("username", user.username)

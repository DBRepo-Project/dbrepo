import os
import unittest

import requests_mock

from dbrepo.RestClient import RestClient
from dbrepo.api.dto import JwtAuth
from dbrepo.api.exceptions import MalformedError, ServiceConnectionError, ServiceError, ForbiddenError, \
    AuthenticationError, ResponseCodeError


class RestClientUnitTest(unittest.TestCase):

    def test_constructor_succeeds(self):
        with requests_mock.Mocker() as mock:
            # test
            os.environ['REST_API_SECURE'] = 'True'
            response = RestClient()
            self.assertTrue(response.secure)

    def test_get_jwt_auth_empty_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = JwtAuth(access_token='ey123',
                          refresh_token='ey456',
                          id_token='ey789',
                          expires_in=3600,
                          scope='scope',
                          token_type='Bearer',
                          not_before_policy=0,
                          session_state='session_state',
                          refresh_expires_in=7200)
            # mock
            mock.post('/api/user/token', json=exp.model_dump(), status_code=202)
            # test
            response = RestClient().get_jwt_auth()
            self.assertEqual(exp, response)

    def test_get_jwt_auth_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/user/token', status_code=400)
            # test
            try:
                response = RestClient().get_jwt_auth()
            except MalformedError:
                pass

    def test_get_jwt_auth_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/user/token', status_code=403)
            # test
            try:
                response = RestClient().get_jwt_auth()
            except ForbiddenError:
                pass

    def test_get_jwt_auth_428_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/user/token', status_code=428)
            # test
            try:
                response = RestClient().get_jwt_auth()
            except AuthenticationError:
                pass

    def test_get_jwt_auth_502_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/user/token', status_code=502)
            # test
            try:
                response = RestClient().get_jwt_auth()
            except ServiceConnectionError:
                pass

    def test_get_jwt_auth_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/user/token', status_code=503)
            # test
            try:
                response = RestClient().get_jwt_auth()
            except ServiceError:
                pass

    def test_get_jwt_auth_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/user/token', status_code=418)
            # test
            try:
                response = RestClient().get_jwt_auth()
            except ResponseCodeError:
                pass


if __name__ == "__main__":
    unittest.main()

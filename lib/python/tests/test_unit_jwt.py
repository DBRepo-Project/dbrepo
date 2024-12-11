import json
from unittest import TestCase, main

import requests_mock

from dbrepo.RestClient import RestClient
from dbrepo.api.dto import JwtAuth


class DatabaseUnitTest(TestCase):

    def test_get_jwt_auth_succeeds(self):
        exp = JwtAuth(access_token='eyABC',
                      refresh_token='ey123',
                      id_token='eyXYZ',
                      expires_in=3600,
                      refresh_expires_in=36000,
                      not_before_policy=0,
                      scope='openid',
                      session_state='4604e4b1-2163-42c3-806d-3be2e426c3a5',
                      token_type='Bearer')
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/user/token', json=exp.model_dump(), status_code=202)
            # test
            response = RestClient().get_jwt_auth(username='foo', password='bar')
            self.assertEqual(exp, response)

    def test_get_jwt_auth_empty_succeeds(self):
        exp = JwtAuth(access_token='eyABC',
                      refresh_token='ey123',
                      id_token='eyXYZ',
                      expires_in=3600,
                      refresh_expires_in=36000,
                      not_before_policy=0,
                      scope='openid',
                      session_state='4604e4b1-2163-42c3-806d-3be2e426c3a5',
                      token_type='Bearer')
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/user/token', json=exp.model_dump(), status_code=202)
            # test
            response = RestClient().get_jwt_auth()
            self.assertEqual(exp, response)

    def test_refresh_jwt_auth_succeeds(self):
        exp = JwtAuth(access_token='eyABC',
                      refresh_token='ey123',
                      id_token='eyXYZ',
                      expires_in=3600,
                      refresh_expires_in=36000,
                      not_before_policy=0,
                      scope='openid',
                      session_state='4604e4b1-2163-42c3-806d-3be2e426c3a5',
                      token_type='Bearer')
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/user/token', json=exp.model_dump(), status_code=202)
            # test
            response = RestClient().refresh_jwt_auth(refresh_token='ey123')
            self.assertEqual(exp, response)


if __name__ == "__main__":
    main()

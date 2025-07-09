import os
import unittest

import requests_mock

from app import get_auth_user


class AppUnitTest(unittest.TestCase):
    token_res = {
        "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJETU9zNU5UamEyTjZOQW4tX080QjdqeGdHNTBhbGY0bDRkWWRVWm14SUprIn0.eyJleHAiOjE3MzczODI1MjQsImlhdCI6MTczNzM4MjQ2NCwianRpIjoiNzUxZGY2ZDAtZWYzMy00MDkzLTgyYzgtNGU2MTRiMjc5NmMzIiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3JlYWxtcy9tYXN0ZXIiLCJzdWIiOiJkMzFjN2VlMS1iODI3LTQ5ZGMtYmZhYS04ZjljZmU4ZDY4NWQiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJhZG1pbi1jbGkiLCJzZXNzaW9uX3N0YXRlIjoiYWU2NGQyYmQtMzIyNS00ZTA1LTk5NDMtMmJiOTFmYjhmZTUyIiwic2NvcGUiOiJwcm9maWxlIGVtYWlsIiwic2lkIjoiYWU2NGQyYmQtMzIyNS00ZTA1LTk5NDMtMmJiOTFmYjhmZTUyIn0.hBFe36TbbHLbvYckWYwVfs1AHrenRu2sC-GPjoJFb_5vKrmJBibnHQ8B3ZS2Y2NdsmWgZUwCgu-t6rZ7boAq1K3AuLQgAUQjr0W63aVwTgg-RAnUX5QV2PZO39U9QPuB_EgfKyeqTlw-9wKLC1zWaqHc1JwaudlaoreKnRVhoy-Y6lhglyqgVWmvoEPRDjBx7Q4JQQEIuRwLuYT-c6TTtginMdwSMWZ3cTv9tXf23pUUUkugtAA0Z9cCQxuD9zeKG2YunWjDic1ZIIEwO0IxD-lP_yvtB2expolcyzgPe80Z8Psigtnoaoiti_ERqqP8DRcPVdsXEpBrQhlC8Wj86A",
        "expires_in": 60,
        "refresh_expires_in": 1800,
        "refresh_token": "eyJhbGciOiJIUzUxMiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJkZmY2Y2NjNS1iZDliLTQ2N2MtOGU5My0wZmU1MWQzNTNlYWYifQ.eyJleHAiOjE3MzczODQyNjQsImlhdCI6MTczNzM4MjQ2NCwianRpIjoiMGU3NGQ1YzctNTg3ZC00ZTc0LTljN2UtNjIzNDhjNjEzZmYyIiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3JlYWxtcy9tYXN0ZXIiLCJhdWQiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvcmVhbG1zL21hc3RlciIsInN1YiI6ImQzMWM3ZWUxLWI4MjctNDlkYy1iZmFhLThmOWNmZThkNjg1ZCIsInR5cCI6IlJlZnJlc2giLCJhenAiOiJhZG1pbi1jbGkiLCJzZXNzaW9uX3N0YXRlIjoiYWU2NGQyYmQtMzIyNS00ZTA1LTk5NDMtMmJiOTFmYjhmZTUyIiwic2NvcGUiOiJwcm9maWxlIGVtYWlsIiwic2lkIjoiYWU2NGQyYmQtMzIyNS00ZTA1LTk5NDMtMmJiOTFmYjhmZTUyIn0.iwmBDa5K5PKxxB8YT5VhA4gS_EVgaGa5gmZ-1ySAPX36jxPpaVGb905ApFZxIQMYMYjo3D1w0H2Uoij8eOWiPw",
        "token_type": "Bearer",
        "not-before-policy": 0,
        "session_state": "ae64d2bd-3225-4e05-9943-2bb91fb8fe52",
        "scope": "profile email"
    }

    def endpoint(self):
        return os.getenv('AUTH_SERVICE_ENDPOINT', 'http://localhost:8080')

    def test_fetch_token_bad_request_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post(f'{self.endpoint()}/realms/master/protocol/openid-connect/token', json=self.token_res,
                      status_code=400)
            # test
            try:
                get_auth_user('admin')
            except IOError:
                pass

    def test_fetch_token_unauthorized_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post(f'{self.endpoint()}/realms/master/protocol/openid-connect/token', json=self.token_res,
                      status_code=401)
            # test
            try:
                get_auth_user('admin')
            except IOError:
                pass

    def test_fetch_user_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post(f'{self.endpoint()}/realms/master/protocol/openid-connect/token', json=self.token_res,
                      status_code=200)
            mock.get(f'{self.endpoint()}/admin/realms/dbrepo/users/?username=admin', json=[], status_code=200)

            # test
            try:
                get_auth_user('admin')
            except FileNotFoundError:
                pass

    def test_fetch_user_too_much_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post(f'{self.endpoint()}/realms/master/protocol/openid-connect/token', json=self.token_res,
                      status_code=200)
            mock.get(f'{self.endpoint()}/admin/realms/dbrepo/users/?username=admin', json=[{}, {}], status_code=200)

            # test
            try:
                get_auth_user('admin')
            except FileNotFoundError:
                pass

    def test_fetch_user_not_ok_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post(f'{self.endpoint()}/realms/master/protocol/openid-connect/token', json=self.token_res,
                      status_code=200)
            mock.get(f'{self.endpoint()}/admin/realms/dbrepo/users/?username=admin', json=[{}], status_code=202)

            # test
            try:
                get_auth_user('admin')
            except FileNotFoundError:
                pass

    def test_fetch_user_no_attrs_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post(f'{self.endpoint()}/realms/master/protocol/openid-connect/token', json=self.token_res,
                      status_code=200)
            mock.get(f'{self.endpoint()}/admin/realms/dbrepo/users/?username=admin', json=[{
                "id": "5b516520-67cb-4aa0-86a6-d12f8b8f1a20"
            }], status_code=200)

            # test
            try:
                get_auth_user('admin')
            except ModuleNotFoundError:
                pass

    def test_fetch_user_empty_attrs_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post(f'{self.endpoint()}/realms/master/protocol/openid-connect/token', json=self.token_res,
                      status_code=200)
            mock.get(f'{self.endpoint()}/admin/realms/dbrepo/users/?username=admin', json=[{
                "id": "5b516520-67cb-4aa0-86a6-d12f8b8f1a20",
                "attributes": {}
            }], status_code=200)

            # test
            try:
                get_auth_user('admin')
            except ImportError:
                pass

    def test_fetch_user_malformed_attr_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post(f'{self.endpoint()}/realms/master/protocol/openid-connect/token', json=self.token_res,
                      status_code=200)
            mock.get(f'{self.endpoint()}/admin/realms/dbrepo/users/?username=admin', json=[{
                "id": "5b516520-67cb-4aa0-86a6-d12f8b8f1a20",
                "attributes": {
                    "LDAP_ID": []
                }
            }], status_code=200)

            # test
            try:
                get_auth_user('admin')
            except EnvironmentError:
                pass

    def test_fetch_succeeds(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post(f'{self.endpoint()}/realms/master/protocol/openid-connect/token', json=self.token_res,
                      status_code=200)
            mock.get(f'{self.endpoint()}/admin/realms/dbrepo/users/?username=admin', json=[{
                "id": "5b516520-67cb-4aa0-86a6-d12f8b8f1a20",
                "attributes": {
                    "LDAP_ID": ["7a0b4b7f-77cd-4f28-a665-2da443024621"]
                }
            }], status_code=200)

            # test
            ldap_user_id, user_id = get_auth_user('admin')
            self.assertEqual("7a0b4b7f-77cd-4f28-a665-2da443024621", ldap_user_id)
            self.assertEqual("5b516520-67cb-4aa0-86a6-d12f8b8f1a20", user_id)

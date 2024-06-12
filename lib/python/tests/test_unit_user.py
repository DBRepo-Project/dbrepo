import unittest

import requests_mock

from dbrepo.RestClient import RestClient
from dbrepo.api.dto import User, UserAttributes, UserBrief
from dbrepo.api.exceptions import ResponseCodeError, UsernameExistsError, EmailExistsError, NotExistsError, \
    ForbiddenError, AuthenticationError, MalformedError, ServiceError


class UserUnitTest(unittest.TestCase):

    def test_whoami_fails(self):
        username = RestClient().whoami()
        self.assertIsNone(username)

    def test_whoami_succeeds(self):
        client = RestClient(username="a", password="b")
        username = client.whoami()
        self.assertEqual("a", username)

    def test_get_users_empty_succeeds(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('http://gateway-service/api/user', json=[])
            # test
            response = RestClient().get_users()
            self.assertEqual([], response)

    def test_get_user_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [
                User(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise',
                     attributes=UserAttributes(theme='dark'))
            ]
            # mock
            mock.get('http://gateway-service/api/user', json=[exp[0].model_dump()])
            # test
            response = RestClient().get_users()
            self.assertEqual(exp, response)

    def test_get_user_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('http://gateway-service/api/user', status_code=404)
            # test
            try:
                response = RestClient().get_users()
            except ResponseCodeError as e:
                pass

    def test_create_user_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise')
            # mock
            mock.post('http://gateway-service/api/user', json=exp.model_dump(), status_code=201)
            # test
            response = RestClient().create_user(username='mweise', password='s3cr3t', email='mweise@example.com')
            self.assertEqual(exp, response)

    def test_create_user_bad_request_fails(self):
        with requests_mock.Mocker() as mock:
            exp = UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise')
            # mock
            mock.post('http://gateway-service/api/user', json=exp.model_dump(), status_code=400)
            # test
            try:
                response = RestClient().create_user(username='mweise', password='s3cr3t', email='mweise@example.com')
            except MalformedError as e:
                pass

    def test_create_user_username_exists_fails(self):
        with requests_mock.Mocker() as mock:
            exp = UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise')
            # mock
            mock.post('http://gateway-service/api/user', json=exp.model_dump(), status_code=409)
            # test
            try:
                response = RestClient().create_user(username='mweise', password='s3cr3t', email='mweise@example.com')
            except UsernameExistsError as e:
                pass

    def test_create_user_default_role_not_exists_fails(self):
        with requests_mock.Mocker() as mock:
            exp = UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise')
            # mock
            mock.post('http://gateway-service/api/user', json=exp.model_dump(), status_code=404)
            # test
            try:
                response = RestClient().create_user(username='mweise', password='s3cr3t', email='mweise@example.com')
            except NotExistsError as e:
                pass

    def test_create_user_emails_exists_fails(self):
        with requests_mock.Mocker() as mock:
            exp = UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise')
            # mock
            mock.post('http://gateway-service/api/user', json=exp.model_dump(), status_code=417)
            # test
            try:
                response = RestClient().create_user(username='mweise', password='s3cr3t', email='mweise@example.com')
            except EmailExistsError as e:
                pass

    def test_get_user_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = User(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise',
                       attributes=UserAttributes(theme='dark'))
            # mock
            mock.get('http://gateway-service/api/user/8638c043-5145-4be8-a3e4-4b79991b0a16',
                     json=exp.model_dump())
            # test
            response = RestClient().get_user(user_id='8638c043-5145-4be8-a3e4-4b79991b0a16')
            self.assertEqual(exp, response)

    def test_get_user_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('http://gateway-service/api/user/8638c043-5145-4be8-a3e4-4b79991b0a16', status_code=404)
            # test
            try:
                response = RestClient().get_user(user_id='8638c043-5145-4be8-a3e4-4b79991b0a16')
            except NotExistsError as e:
                pass

    def test_update_user_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = User(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise', given_name='Martin',
                       attributes=UserAttributes(theme='dark'))
            # mock
            mock.put('http://gateway-service/api/user/8638c043-5145-4be8-a3e4-4b79991b0a16', status_code=202,
                     json=exp.model_dump())
            # test
            client = RestClient(username="a", password="b")
            response = client.update_user(user_id='8638c043-5145-4be8-a3e4-4b79991b0a16', firstname='Martin',
                                          language='en', theme='light')
            self.assertEqual(exp, response)

    def test_update_user_not_allowed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('http://gateway-service/api/user/8638c043-5145-4be8-a3e4-4b79991b0a16', status_code=403)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.update_user(user_id='8638c043-5145-4be8-a3e4-4b79991b0a16', firstname='Martin',
                                              language='en', theme='light')
            except ForbiddenError as e:
                pass

    def test_update_user_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('http://gateway-service/api/user/8638c043-5145-4be8-a3e4-4b79991b0a16', status_code=404)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.update_user(user_id='8638c043-5145-4be8-a3e4-4b79991b0a16', firstname='Martin',
                                              language='en', theme='light')
            except NotExistsError as e:
                pass

    def test_update_user_not_auth_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('http://gateway-service/api/user/8638c043-5145-4be8-a3e4-4b79991b0a16', status_code=405)
            # test
            try:
                response = RestClient().update_user(user_id='8638c043-5145-4be8-a3e4-4b79991b0a16', firstname='Martin',
                                                    language='en', theme='light')
            except AuthenticationError as e:
                pass

    def test_update_user_password_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = User(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise', given_name='Martin',
                       attributes=UserAttributes(theme='dark'))
            # mock
            mock.put('http://gateway-service/api/user/8638c043-5145-4be8-a3e4-4b79991b0a16/password', status_code=202,
                     json=exp.model_dump())
            # test
            client = RestClient(username="a", password="b")
            response = client.update_user_password(user_id='8638c043-5145-4be8-a3e4-4b79991b0a16',
                                                   password='s3cr3t1n0rm4t10n')
            self.assertEqual(exp, response)

    def test_update_user_password_not_allowed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('http://gateway-service/api/user/8638c043-5145-4be8-a3e4-4b79991b0a16/password', status_code=403)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.update_user_password(user_id='8638c043-5145-4be8-a3e4-4b79991b0a16',
                                                       password='s3cr3t1n0rm4t10n')
            except ForbiddenError as e:
                pass

    def test_update_user_password_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('http://gateway-service/api/user/8638c043-5145-4be8-a3e4-4b79991b0a16/password', status_code=404)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.update_user_password(user_id='8638c043-5145-4be8-a3e4-4b79991b0a16',
                                                       password='s3cr3t1n0rm4t10n')
            except NotExistsError as e:
                pass

    def test_update_user_password_keycloak_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('http://gateway-service/api/user/8638c043-5145-4be8-a3e4-4b79991b0a16/password', status_code=503)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.update_user_password(user_id='8638c043-5145-4be8-a3e4-4b79991b0a16',
                                                       password='s3cr3t1n0rm4t10n')
            except ServiceError as e:
                pass

    def test_update_user_password_not_auth_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('http://gateway-service/api/user/8638c043-5145-4be8-a3e4-4b79991b0a16/password', status_code=503)
            # test
            try:
                response = RestClient().update_user_password(user_id='8638c043-5145-4be8-a3e4-4b79991b0a16',
                                                             password='s3cr3t1n0rm4t10n')
            except AuthenticationError as e:
                pass


if __name__ == "__main__":
    unittest.main()

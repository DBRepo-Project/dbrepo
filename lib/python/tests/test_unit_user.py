import unittest

import requests_mock

from dbrepo.RestClient import RestClient
from dbrepo.api.dto import UserAttributes, UserBrief, User, UnitBrief
from dbrepo.api.exceptions import ResponseCodeError, NotExistsError, \
    ForbiddenError, AuthenticationError, ServiceError, MalformedError


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
            mock.get('/api/user', json=[])
            # test
            response = RestClient().get_users()
            self.assertEqual([], response)

    def test_get_users_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise')]
            # mock
            mock.get('/api/user', json=[exp[0].model_dump()])
            # test
            response = RestClient().get_users()
            self.assertEqual(exp, response)

    def test_get_users_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/user', status_code=404)
            # test
            try:
                response = RestClient().get_users()
            except ResponseCodeError as e:
                pass

    def test_get_user_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = User(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise',
                       attributes=UserAttributes(theme='dark', language='en'))
            # mock
            mock.get('/api/user/8638c043-5145-4be8-a3e4-4b79991b0a16',
                     json=exp.model_dump())
            # test
            response = RestClient().get_user(user_id='8638c043-5145-4be8-a3e4-4b79991b0a16')
            self.assertEqual(exp, response)

    def test_get_user_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/user/8638c043-5145-4be8-a3e4-4b79991b0a16', status_code=404)
            # test
            try:
                response = RestClient().get_user(user_id='8638c043-5145-4be8-a3e4-4b79991b0a16')
            except NotExistsError as e:
                pass

    def test_update_user_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise', given_name='Martin')
            # mock
            mock.put('/api/user/8638c043-5145-4be8-a3e4-4b79991b0a16', status_code=202,
                     json=exp.model_dump())
            # test
            client = RestClient(username="a", password="b")
            response = client.update_user(user_id='8638c043-5145-4be8-a3e4-4b79991b0a16', firstname='Martin',
                                          language='en', theme='light')
            self.assertEqual(exp, response)

    def test_get_user_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(f'/api/user/f27921d4-b05f-4e21-a122-4953a6a779a2', status_code=403)
            # test
            try:
                RestClient().get_user('f27921d4-b05f-4e21-a122-4953a6a779a2')
            except ForbiddenError:
                pass

    def test_get_user_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(f'/api/user/f27921d4-b05f-4e21-a122-4953a6a779a2', status_code=404)
            # test
            try:
                RestClient().get_user('f27921d4-b05f-4e21-a122-4953a6a779a2')
            except NotExistsError:
                pass

    def test_get_user_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get(f'/api/user/f27921d4-b05f-4e21-a122-4953a6a779a2', status_code=409)
            # test
            try:
                RestClient().get_user('f27921d4-b05f-4e21-a122-4953a6a779a2')
            except ResponseCodeError:
                pass

    def test_update_user_anonymous_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/user/f27921d4-b05f-4e21-a122-4953a6a779a2', status_code=202)
            # test
            try:
                RestClient().update_user(user_id='f27921d4-b05f-4e21-a122-4953a6a779a2', theme='dark', language='en')
            except AuthenticationError:
                pass

    def test_update_user_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/user/f27921d4-b05f-4e21-a122-4953a6a779a2', status_code=400)
            # test
            try:
                RestClient(username='foo', password='bar').update_user(user_id='f27921d4-b05f-4e21-a122-4953a6a779a2',
                                                                       theme='dark', language='en')
            except MalformedError:
                pass

    def test_update_user_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/user/f27921d4-b05f-4e21-a122-4953a6a779a2', status_code=403)
            # test
            try:
                RestClient(username='foo', password='bar').update_user(user_id='f27921d4-b05f-4e21-a122-4953a6a779a2',
                                                                       theme='dark', language='en')
            except ForbiddenError:
                pass

    def test_update_user_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/user/f27921d4-b05f-4e21-a122-4953a6a779a2', status_code=404)
            # test
            try:
                RestClient(username='foo', password='bar').update_user(user_id='f27921d4-b05f-4e21-a122-4953a6a779a2',
                                                                       theme='dark', language='en')
            except NotExistsError:
                pass

    def test_update_user_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/user/f27921d4-b05f-4e21-a122-4953a6a779a2', status_code=200)
            # test
            try:
                RestClient(username='foo', password='bar').update_user(user_id='f27921d4-b05f-4e21-a122-4953a6a779a2',
                                                                       theme='dark', language='en')
            except ResponseCodeError:
                pass


if __name__ == "__main__":
    unittest.main()

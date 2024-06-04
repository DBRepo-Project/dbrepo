import logging
import unittest
import random
import string

from dbrepo.RestClient import RestClient


def rand(size=6, chars=string.ascii_uppercase + string.digits):
    return ''.join(random.choice(chars) for _ in range(size))


class UserComponentTest(unittest.TestCase):

    def test_create_user_find_whoami_login_basic_oidc(self):
        # params
        username = rand(size=8).lower()
        password = rand(size=8)
        email = rand(size=8) + "@example.com"
        print(f"creating user {username} with password {password} with email {email}")
        # create user
        client = RestClient(endpoint="http://localhost")
        response = client.create_user(username=username, password=password, email=email)
        self.assertEqual(username, response.username)
        self.assertIsNotNone(response.id)
        user_id = response.id
        # find user
        client = RestClient(endpoint="http://localhost", username=username, password=password)
        response = client.get_user(user_id=user_id)
        self.assertEqual(username, response.username)
        self.assertEqual(user_id, response.id)
        # whoami
        response = client.whoami()
        self.assertEqual(username, response)
        # login basic
        response = client.get_jwt_auth(username=username, password=password)
        self.assertIsNotNone(response.id_token)
        access_token = response.access_token
        self.assertIsNotNone(response.access_token)
        self.assertIsNotNone(response.refresh_token)
        self.assertEqual(0, response.not_before_policy)
        self.assertIsNotNone(response.expires_in)
        self.assertIsNotNone(response.refresh_expires_in)
        self.assertIsNotNone(response.session_state)
        self.assertIsNotNone(response.scope)
        # login oidc
        client = RestClient(endpoint="http://localhost", password=access_token)
        response = client.update_user(user_id=user_id, theme="light", language="en")


if __name__ == "__main__":
    unittest.main()

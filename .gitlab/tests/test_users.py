#!/bin/env python3
import uuid

from postgres import Postgres

from api_authentication.api.authentication_endpoint_api import AuthenticationEndpointApi
from api_authentication.api.user_endpoint_api import UserEndpointApi

authentication = AuthenticationEndpointApi()
user = UserEndpointApi()

token = ""  # keep


def create_user(username):
    response = user.register({
        "username": username,
        "password": username,
        "email": username + "@gmail.com"
    })
    print("created user with id %d" % response.id)
    return response


def update_password(user_id, password):
    response = user.update_password({
        "password": password
    }, user_id)
    print("updated password for user with id %d" % user_id)
    return response


def auth_user(username):
    response = authentication.authenticate_user1({
        "username": username,
        "password": username
    })
    print("authenticated user with id %d" % response.id)
    token = response.token
    user.api_client.default_headers = {"Authorization": "Bearer " + token}
    return response


def update_user(user_id):
    response = user.update({
        "firstname": "Josiah",
        "lastname": "Carberry",
        "affiliation": "Wesleyan University",
        "orcid": "0000-0002-1825-0097",
        "titles_after": "PhD"
    }, user_id)
    print("updated user with id %d" % user_id)
    return response


def update_theme(user_id):
    response = user.update_theme({
        "theme_dark": True
    }, user_id)
    print("updated theme user with id %d" % user_id)


def verify_user(user_id):
    db = Postgres("dbname=fda user=postgres password=postgres")
    token = db.one("SELECT ")


def test_users():
    #
    # create 1 user
    #
    username = str(uuid.uuid1()).replace('-', '')
    uid = create_user(username).id
    auth_user(username)
    update_password(uid, username)
    update_user(uid)
    #
    # create 1 user and modify information
    #
    username = str(uuid.uuid1()).replace('-', '')
    uid = create_user(username).id
    auth_user(username)
    update_user(uid)
    update_theme(uid)

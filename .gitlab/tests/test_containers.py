#!/bin/env python3

import time
import uuid

from api_authentication.api.authentication_endpoint_api import AuthenticationEndpointApi
from api_authentication.api.user_endpoint_api import UserEndpointApi
from api_container.api.container_endpoint_api import ContainerEndpointApi
from api_database.api.database_endpoint_api import DatabaseEndpointApi

authentication = AuthenticationEndpointApi()
user = UserEndpointApi()
container = ContainerEndpointApi()
database = DatabaseEndpointApi()

token = ""  # keep


def create_user(username):
    response = user.register({
        "username": username,
        "password": username,
        "email": username + "@gmail.com"
    })
    print("created user with id %d" % response.id)
    return response


def auth_user(username):
    response = authentication.authenticate_user1({
        "username": username,
        "password": username
    })
    print("authenticated user with id %d" % response.id)
    token = response.token
    container.api_client.default_headers = {"Authorization": "Bearer " + token}
    database.api_client.default_headers = {"Authorization": "Bearer " + token}
    return response


def create_container():
    response = container.create1({
        "name": "Pilot Factory Data",
        "repository": "mariadb",
        "tag": "10.5"
    })
    print("created container with id %d" % response.id)
    return response


def start_container(container_id):
    response = container.modify({
        "action": "start"
    }, container_id)
    print("... starting")
    time.sleep(5)
    print("started container with id %d" % response.id)
    return response


def create_database(container_id, is_public=True):
    response = database.create({
        "name": "Pilot Factory Data",
        "is_public": is_public
    }, container_id)
    print("created database with id %d" % response.id)
    return response


def find_database(container_id, database_id):
    response = database.find_by_id(container_id, database_id)
    print("found database with id %d" % response.id)
    return response


def update_database(container_id, database_id):
    response = database.update({
        "description": "This dataset includes daily values from 1983 to the current day, divided into annual files. This includes the maximum hourly average and the number of times the hourly average limit value for ozone was exceeded and the daily averages for sulfur dioxide (SO2), carbon monoxide (CO), nitrogen oxide (NOx), nitrogen monoxide (NO), nitrogen dioxide (NO2), particulate matter (PM10 and PM2.5). ) and particle number (PN), provided that they are of sufficient quality. The values of the completed day for the current year are updated every 30 minutes after midnight (UTC+1).",
        "publisher": "Technical University of Vienna",
        "license": {
            "identifier": "CC0-1.0",
            "uri": "https://creativecommons.org/publicdomain/zero/1.0/legalcode"
        },
        "language": "en",
        "publication_year": 2022
    }, container_id, database_id)
    print("updated database with id %d" % response.id)
    return response


def test_containers():
    username = str(uuid.uuid1()).replace("-", "")
    uid = create_user(username).id
    auth_user(username)
    # container 1
    cid = create_container().id
    start_container(cid)
    dbid = create_database(cid).id
    update_database(cid, dbid)

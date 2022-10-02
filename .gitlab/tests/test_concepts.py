#!/bin/env python3

import time
import uuid
import requests as rq

from api_authentication.api.authentication_endpoint_api import AuthenticationEndpointApi
from api_authentication.api.user_endpoint_api import UserEndpointApi
from api_container.api.container_endpoint_api import ContainerEndpointApi
from api_database.api.container_database_endpoint_api import ContainerDatabaseEndpointApi
from api_table.api.table_endpoint_api import TableEndpointApi
from api_units.api.default_api import DefaultApi

authentication = AuthenticationEndpointApi()
user = UserEndpointApi()
container = ContainerEndpointApi()
database = ContainerDatabaseEndpointApi()
table = TableEndpointApi()
unit = DefaultApi()

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
    table.api_client.default_headers = {"Authorization": "Bearer " + token}
    user.api_client.default_headers = {"Authorization": "Bearer " + token}
    return response


def create_container():
    response = container.create1({
        "name": "Airquality " + str(uuid.uuid1()),
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
        "name": "Airquality " + str(uuid.uuid1()),
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


def create_table(container_id, database_id, columns=None):
    if columns is None:
        columns = [{
            "name": "Date",
            "type": "date",
            "dfid": 1,
            "unique": False,
            "primary_key": False,
            "null_allowed": True,
        }, {
            "name": "Location",
            "type": "string",
            "unique": False,
            "primary_key": False,
            "null_allowed": True,
        }, {
            "name": "Parameter",
            "type": "string",
            "unique": False,
            "primary_key": False,
            "null_allowed": True,
        }, {
            "name": "Interval",
            "type": "string",
            "unique": False,
            "primary_key": False,
            "null_allowed": True,
        }, {
            "name": "Unit",
            "type": "string",
            "unique": False,
            "primary_key": False,
            "null_allowed": True,
        }, {
            "name": "Value",
            "type": "decimal",
            "unique": False,
            "primary_key": False,
            "null_allowed": True,
        }, {
            "name": "Status",
            "type": "string",
            "unique": False,
            "primary_key": False,
            "null_allowed": True,
        }]
    response = table.create({
        "name": "Airquality " + str(uuid.uuid1()),
        "description": "Airquality in Zürich, Switzerland",
        "columns": columns
    }, "Bearer " + token, container_id, database_id)
    print("created table with id %d" % response.id)
    return response


def find_table(container_id, database_id, table_id):
    response = table.find_by_id(container_id, database_id, table_id)
    print("found table with id %d" % response.id)
    return response


def find_concept(concept):
    response = rq.get("http://localhost:9095/api/units/uri/" + concept)
    print("found concept for name %s" % concept)
    return response.json()


def create_concept(name, uri):
    response = rq.post("http://localhost:9095/api/units/saveconcept", {
        "name": name,
        "uri": uri
    })
    print("created concept for name %s" % name)
    return response.json()


def assign_concept(database_id, table_id, column_id, uri):
    response = rq.post("http://localhost:9095/api/units/savecolumnsconcept", {
        "cdbid": database_id,
        "cid": column_id,
        "tid": table_id,
        "uri": uri
    })
    print("assigned concept to column with id %d" % column_id)
    return response.json()


def test_concepts():
    #
    # create 1 user and 1 container
    #
    username = str(uuid.uuid1()).replace('-', '')
    uid = create_user(username).id
    auth_user(username)
    # container 1
    cid = create_container().id
    start_container(cid)
    dbid = create_database(cid).id
    update_database(cid, dbid)
    tid = create_table(cid, dbid).id
    curi = find_concept("time")["URI"]
    create_concept("time", curi)
    assign_concept(dbid, tid, 2, curi)
    tname = find_table(cid, dbid, tid).internal_name

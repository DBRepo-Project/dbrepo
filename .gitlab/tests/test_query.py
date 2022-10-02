#!/bin/env python3

import time
import os
import shutil
import uuid

from api_authentication.api.authentication_endpoint_api import AuthenticationEndpointApi
from api_authentication.api.user_endpoint_api import UserEndpointApi
from api_container.api.container_endpoint_api import ContainerEndpointApi
from api_database.api.database_endpoint_api import DatabaseEndpointApi
from api_table.api.table_endpoint_api import TableEndpointApi
from api_query.api.table_data_endpoint_api import TableDataEndpointApi
from api_query.api.query_endpoint_api import QueryEndpointApi
from api_query.api.table_history_endpoint_api import TableHistoryEndpointApi
from api_identifier.api.identifier_endpoint_api import IdentifierEndpointApi
from api_identifier.api.persistence_endpoint_api import PersistenceEndpointApi
from api_query.api.view_endpoint_api import ViewEndpointApi
from api_query.rest import ApiException

authentication = AuthenticationEndpointApi()
user = UserEndpointApi()
container = ContainerEndpointApi()
database = DatabaseEndpointApi()
table = TableEndpointApi()
query = QueryEndpointApi()
history = TableHistoryEndpointApi()
data = TableDataEndpointApi()
identifier = IdentifierEndpointApi()
persistence = PersistenceEndpointApi()
view = ViewEndpointApi()

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
    container.api_client.default_headers = {"Authorization": "Bearer " + token}
    database.api_client.default_headers = {"Authorization": "Bearer " + token}
    table.api_client.default_headers = {"Authorization": "Bearer " + token}
    data.api_client.default_headers = {"Authorization": "Bearer " + token}
    query.api_client.default_headers = {"Authorization": "Bearer " + token}
    identifier.api_client.default_headers = {"Authorization": "Bearer " + token}
    user.api_client.default_headers = {"Authorization": "Bearer " + token}
    persistence.api_client.default_headers = {"Authorization": "Bearer " + token}
    history.api_client.default_headers = {"Authorization": "Bearer " + token}
    view.api_client.default_headers = {"Authorization": "Bearer " + token}
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


def fill_table(container_id, database_id, table_id):
    shutil.copyfile(os.getcwd() + "/tests/resources/ugz_ogd_air_h1_2021.csv", "/tmp/ugz_ogd_air_h1_2021.csv")
    response = data.import_csv({
        "location": "/tmp/ugz_ogd_air_h1_2021.csv",
        "separator": ",",
        "quote": "\"",
        "skip_lines": 1
    }, container_id, database_id, table_id)
    print("filled table with id %d" % table_id)
    return response


def create_query(container_id, database_id, statement, page=0, size=3):
    response = query.execute({
        "statement": statement
    }, container_id, database_id, page=page, size=size)
    print("executed query with id %d" % response.id)
    return response


def delete_tuple(container_id, database_id, table_id, keys):
    response = data.delete(keys, container_id, database_id, table_id)
    print("deleted tuples for table with id %d" % table_id)
    return response


def download_query_data(container_id, database_id, query_id):
    response = query.export1(container_id, database_id, query_id)
    print("downloaded query data for query with id %d" % query_id)
    return response


def list_views(container_id, database_id):
    response = view.find_all(container_id, database_id)
    print("list views for database with id %d" % database_id)
    return response


def create_view(container_id, database_id, table_name):
    response = view.create({
        "name": "Air Quality " + str(uuid.uuid1()),
        "query": "SELECT `date`, `parameter`, `value` FROM `" + table_name + "` WHERE `date` = '2021-10-02T14:00'",
        "is_public": True
    }, container_id, database_id)
    print("created view with id %d" % response.id)
    return response


def data_view(container_id, database_id, view_id):
    response = view.data(container_id, database_id, view_id)
    print("retrieved data for view with id %d" % response.id)
    return response


def test_identifiers():
    #
    # create 1 user and 2 containers (public, private)
    #
    username = str(uuid.uuid1()).replace("-", "")
    uid = create_user(username).id
    auth_user(username)
    # container 1
    cid = create_container().id
    start_container(cid)
    dbid = create_database(cid).id
    update_database(cid, dbid)
    tid = create_table(cid, dbid).id
    tname = find_table(cid, dbid, tid).internal_name
    fill_table(cid, dbid, tid)
    create_query(cid, dbid, "select `id` from `" + tname + "`")
    create_query(cid, dbid, "select `date` from `" + tname + "`")
    qid = create_query(cid, dbid, "select `date`, `location`, `status` from `" + tname + "`").id
    create_query(cid, dbid, "select `date`, `location`, `status` from `" + tname + "` order by `date` asc")
    create_query(cid, dbid, "select t.`date`, t.location, t.status from `" + tname + "` t group by t.`date` order by t.`date` asc")
    create_query(cid, dbid, "select `date`, `location`, `status` from `" + tname + "` group by `date`, `location` asc")
    download_query_data(cid, dbid, qid)
    # container 2 (=private)
    cid = create_container().id
    start_container(cid)
    dbid = create_database(cid, False).id
    update_database(cid, dbid)
    tid = create_table(cid, dbid).id
    tname = find_table(cid, dbid, tid).internal_name
    fill_table(cid, dbid, tid)
    qid = create_query(cid, dbid, "select `id` from `" + tname + "`").id
    qid = create_query(cid, dbid, "select `id` from `" + tname + "`").id
    vid = create_view(cid, dbid, tname).id
    data_view(cid, dbid, vid)
    list_views(cid, dbid)
    for i in range(5, 10):
        delete_tuple(cid, dbid, tid, {
            "keys": {
                "id": i
            }
        })
        time.sleep(1)
    delete_tuple(cid, dbid, tid, {
        "keys": {
            "location": "Schimmelstrasse"
        }
    })

#!/bin/env python3

import time
import os
import shutil
import uuid
import requests as rq
from postgres import Postgres

import api_query.rest
from api_broker.BrokerServiceClient import BrokerServiceClient
from api_authentication.api.authentication_endpoint_api import AuthenticationEndpointApi
from api_authentication.api.user_endpoint_api import UserEndpointApi
from api_container.api.container_endpoint_api import ContainerEndpointApi
from api_database.api.container_database_endpoint_api import ContainerDatabaseEndpointApi
from api_table.api.table_endpoint_api import TableEndpointApi
from api_query.api.table_data_endpoint_api import TableDataEndpointApi
from api_query.api.query_endpoint_api import QueryEndpointApi
from api_identifier.api.identifier_endpoint_api import IdentifierEndpointApi
from api_identifier.api.persistence_endpoint_api import PersistenceEndpointApi
from api_units.api.default_api import DefaultApi

authentication = AuthenticationEndpointApi()
user = UserEndpointApi()
container = ContainerEndpointApi()
database = ContainerDatabaseEndpointApi()
table = TableEndpointApi()
query = QueryEndpointApi()
data = TableDataEndpointApi()
identifier = IdentifierEndpointApi()
persistence = PersistenceEndpointApi()
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
    }, container_id, database_id)
    print("created table with id %d" % response.id)
    return response


def find_table(container_id, database_id, table_id):
    response = table.find_by_id(container_id, database_id, table_id)
    print("found table with id %d" % response.id)
    return response


def fill_table(container_id, database_id, table_id):
    shutil.copyfile(os.getcwd() + "/resources/ugz_ogd_air_h1_2021.csv", "/tmp/ugz_ogd_air_h1_2021.csv")
    response = data.import_csv({
        "location": "/tmp/ugz_ogd_air_h1_2021.csv",
        "separator": ",",
        "quote": "\"",
        "skip_lines": 1
    }, container_id, database_id, table_id)
    print("filled table with id %d" % table_id)
    return response


def create_query(container_id, database_id, statement, page=0, size=3):
    try:
        response = query.execute({
            "statement": statement
        }, container_id, database_id, page=page, size=size)
        print("executed query with id %d" % response.id)
        return response
    except api_query.rest.ApiException as e:
        print(e)


def create_identifier(container_id, database_id, query_id, visibility="everyone"):
    response = identifier.create({
        "qid": query_id,
        "title": "Airquality",
        "description": "Subset used for a scientific article",
        "visibility": visibility,
        "creators": [{
            "name": "Weise, Martin",
            "affiliation": "TU Wien",
            "orcid": "0000-0003-4216-302X"
        }, {
            "name": "Rauber, Andreas",
            "affiliation": "TU Wien",
            "orcid": "0000-0002-9272-6225"
        }],
        "publication_day": 2,
        "publication_month": 8,
        "publication_year": 2022,
        "related_identifiers": [{
            "value": "http://localhost:3000/container/" + str(container_id) + "/database/" + str(database_id),
            "type": "URL",
            "relation": "IsCitedBy"
        }]
    }, token, container_id, database_id)
    print("created identifier with id %d" % response.id)
    return response


def delete_tuple(container_id, database_id, table_id, keys):
    response = data.delete(keys, container_id, database_id, table_id)
    print("deleted tuples for table with id %d" % table_id)
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


def download_query_data(container_id, database_id, query_id):
    response = query.export1(container_id, database_id, query_id)
    print("downloaded query data for query with id %d" % query_id)
    return response


def download_identifier_metadata(container_id, database_id, identifier_id):
    response = identifier.export(container_id, database_id, identifier_id)
    print("downloaded identifier metadata for identifier with id %d" % identifier_id)
    return response


def send_tuple(exchange, routing_key, username, password, payload):
    broker = BrokerServiceClient(exchange=exchange, routing_key=routing_key, host="localhost", username=username,
                                 password=password)
    response = broker.send(payload)
    print("sent tuple to exchange with routing key %s" % routing_key)
    return response


if __name__ == '__main__':
    #
    # create 1 user and 3 containers (public, private, public)
    #
    uid = create_user("test1").id
    auth_user("test1")
    update_password(uid, "test1")
    update_user(uid)
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
    fill_table(cid, dbid, tid)
    create_query(cid, dbid, "select `id` from `" + tname + "`")
    create_query(cid, dbid, "select `date` from `" + tname + "`")
    qid = create_query(cid, dbid, "select `date`, `location`, `status` from `" + tname + "`").id
    create_query(cid, dbid, "select `foo` from `" + tname + "`")
    iid = create_identifier(cid, dbid, qid).id
    download_query_data(cid, dbid, qid)
    download_identifier_metadata(cid, dbid, iid)
    # container 2 (=private)
    cid = create_container().id
    start_container(cid)
    dbid = create_database(cid, False).id
    update_database(cid, dbid)
    tid = create_table(cid, dbid).id
    tname = find_table(cid, dbid, tid).internal_name
    fill_table(cid, dbid, tid)
    qid = create_query(cid, dbid, "select `id` from `" + tname + "`").id
    create_identifier(cid, dbid, qid, visibility="self")
    qid = create_query(cid, dbid, "select `id` from `" + tname + "`").id
    create_identifier(cid, dbid, qid)
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
    # container 3 with 4 tables
    cid = create_container().id
    start_container(cid)
    dbid = create_database(cid).id
    dbexchange = find_database(cid, dbid).exchange
    update_database(cid, dbid)
    create_table(cid, dbid, columns=[])
    create_table(cid, dbid, columns=[{
        "name": "primary",
        "type": "string",
        "unique": True,
        "primary_key": True,
        "null_allowed": False,
    }])
    tid = create_table(cid, dbid, columns=[{
        "name": "primary",
        "type": "number",
        "unique": True,
        "primary_key": True,
        "null_allowed": False,
    }]).id
    ttopic = find_table(cid, dbid, tid).topic
    send_tuple(dbexchange, ttopic, "test1", "test1", {"primary": 1})
    send_tuple(dbexchange, ttopic, "test1", "test1", {"primary": 2})
    send_tuple(dbexchange, ttopic, "test1", "test1", {"primary": 3})
    create_table(cid, dbid, columns=[{
        "name": "primary",
        "type": "date",
        "unique": True,
        "primary_key": True,
        "null_allowed": False,
    }])
    #
    # create 1 user and 1 container and issue queries to own and foreign database
    #
    uid = create_user("test2").id
    auth_user("test2")
    # container 4
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
    create_identifier(cid, dbid, qid)
    # container 1 (foreign container query)
    tname = find_table(1, 1, 1).internal_name
    qid = create_query(1, 1, "select `id` from `" + tname + "`").id
    create_identifier(1, 1, qid)
    #
    # create 1 user and modify information
    #
    uid = create_user("test3").id
    auth_user("test3")
    update_user(uid)
    update_theme(uid)
    print("FINISHED")

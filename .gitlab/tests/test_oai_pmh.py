#!/bin/env python3

import time
import os
import shutil
import uuid
import requests as rq
from pika.exceptions import ChannelClosedByBroker
from postgres import Postgres

from api_authentication.api.authentication_endpoint_api import AuthenticationEndpointApi
from api_authentication.api.user_endpoint_api import UserEndpointApi
from api_container.api.container_endpoint_api import ContainerEndpointApi
from api_database.api.container_database_endpoint_api import ContainerDatabaseEndpointApi
from api_query.rest import ApiException
from api_table.api.table_endpoint_api import TableEndpointApi
from api_query.api import TableDataEndpointApi
from api_query.api.query_endpoint_api import QueryEndpointApi
from api_identifier.api import IdentifierEndpointApi
from api_identifier.api.persistence_endpoint_api import PersistenceEndpointApi

authentication = AuthenticationEndpointApi()
user = UserEndpointApi()
container = ContainerEndpointApi()
database = ContainerDatabaseEndpointApi()
table = TableEndpointApi()
query = QueryEndpointApi()
data = TableDataEndpointApi()
identifier = IdentifierEndpointApi()
persistence = PersistenceEndpointApi()

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
    }, "Bearer " + token, container_id, database_id)
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
    except ApiException as e:
        print(e)


def create_identifier(container_id, database_id, query_id, visibility="everyone"):
    response = identifier.create({
        "qid": query_id,
        "title": "Airquality",
        "description": "Subset used for a scientific article",
        "publisher": "TU Wien",
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


def oai_identify():
    response = rq.get("http://localhost:9095/api/oai?verb=Identify")
    if "persistent" not in response.text:
        print("Invalid response %s" % response.text)
        raise Exception("Invalid response")
    print("identified repository")


def oai_list_identifiers():
    response = rq.get("http://localhost:9095/api/oai?verb=ListIdentifiers")
    if "pid/1" not in response.text or "pid/2" not in response.text or "pid/3" not in response.text \
            or "pid/4" not in response.text:
        print("Invalid response %s" % response.text)
        raise Exception("Invalid response")
    print("listed identifiers")


def oai_list_metadata_formats():
    response = rq.get("http://localhost:9095/api/oai?verb=ListMetadataFormats")
    if "oai_dc" not in response.text:
        print("Invalid response %s" % response.text)
        raise Exception("Invalid response")
    print("listed metadata formats")


def oai_get_record(record_id, expected):
    response = rq.get("http://localhost:9095/api/oai?verb=GetRecord&metadataPrefix=oai_dc&identifier=" + str(record_id))
    if expected not in response.text:
        print("Invalid response %s" % response.text)
        raise Exception("Invalid response")
    print("retrieved record with id %d" % record_id)


def test_oai_pmh():
    #
    # create 1 user and 1 container
    #
    username = str(uuid.uuid1()).replace('-', '')
    uid = create_user(username).id
    auth_user(username)
    update_password(uid, username)
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
    create_query(cid, dbid, "select `foo` from `" + tname + "`")
    iid = create_identifier(cid, dbid, qid).id
    #
    # OAI-PMH
    #
    oai_identify()
    oai_list_identifiers()
    oai_list_metadata_formats()
    oai_get_record(1, "dc:creator>Weise, Martin")
    oai_get_record(1, "dc:creator>Rauber, Andreas")
    oai_get_record(6, "code=\"idDoesNotExist\"")

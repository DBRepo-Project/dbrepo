import math
import os
import logging
from ast import literal_eval
from typing import List, Any

import requests
from dbrepo.api.dto import Database, ApiError
from flasgger import LazyJSONEncoder, Swagger, swag_from
from flask import Flask, request
from flask_cors import CORS
from flask_httpauth import HTTPTokenAuth, HTTPBasicAuth, MultiAuth
from opensearchpy import TransportError, NotFoundError
from prometheus_flask_exporter import PrometheusMetrics
from pydantic import ValidationError
from logging.config import dictConfig

from clients.keycloak_client import User, KeycloakClient
from clients.opensearch_client import OpenSearchClient

logging.addLevelName(level=logging.NOTSET, levelName='TRACE')
logging.basicConfig(level=logging.DEBUG)

# logging configuration
dictConfig({
    'version': 1,
    'formatters': {
        'default': {
            'format': '[%(asctime)s] %(levelname)s in %(module)s: %(message)s',
        },
        'simple': {
            'format': '[%(asctime)s] %(levelname)s: %(message)s',
        },
    },
    'handlers': {'wsgi': {
        'class': 'logging.StreamHandler',
        'stream': 'ext://flask.logging.wsgi_errors_stream',
        'formatter': 'simple'  # default
    }},
    'root': {
        'level': 'DEBUG',
        'handlers': ['wsgi']
    }
})

# create app object
app = Flask(__name__)

cors = CORS(app, resources={r"/api/*": {"origins": "*"}})

metrics = PrometheusMetrics(app)
metrics.info("app_info", "Application info", version="0.0.1")
app.config["SWAGGER"] = {"openapi": "3.0.1", "title": "Swagger UI", "uiversion": 3}

token_auth = HTTPTokenAuth(scheme='Bearer')
basic_auth = HTTPBasicAuth()
auth = MultiAuth(token_auth, basic_auth)

swagger_config = {
    "headers": [],
    "specs": [
        {
            "endpoint": "api-search",
            "route": "/api-search.json",
            "rule_filter": lambda rule: rule.endpoint.startswith('actuator') or rule.endpoint.startswith(
                'search') or rule.endpoint.startswith('database'),
            "model_filter": lambda tag: True,  # all in
        }
    ],
    "static_url_path": "/flasgger_static",
    "swagger_ui": True,
    "specs_route": "/swagger-ui/",
}

template = {
    "openapi": "3.0.0",
    "components": {
        "securitySchemes": {
            "bearerAuth": {
                "type": "http",
                "scheme": "bearer",
                "bearerFormat": "JWT",
                "in": "header"
            },
            "basicAuth": {
                "type": "http",
                "scheme": "basic",
                "in": "header"
            }
        },
    },
    "info": {
        "title": "Database Repository Search Service API",
        "description": "Service that searches the search database",
        "version": "__APPVERSION__",
        "contact": {
            "name": "Prof. Andreas Rauber",
            "email": "andreas.rauber@tuwien.ac.at"
        },
        "license": {
            "name": "Apache 2.0",
            "url": "https://www.apache.org/licenses/LICENSE-2.0"
        },
    },
    "externalDocs": {
        "description": "Sourcecode Documentation",
        "url": "https://www.ifs.tuwien.ac.at/infrastructures/dbrepo/__APPVERSION__/"
    },
    "servers": [
        {
            "url": "http://localhost:4000",
            "description": "Generated server url"
        },
        {
            "url": "https://test.dbrepo.tuwien.ac.at",
            "description": "Sandbox"
        }
    ]
}

swagger = Swagger(app, config=swagger_config, template=template)
app.config["GATEWAY_ENDPOINT"] = os.getenv("GATEWAY_ENDPOINT", "http://localhost")
app.config["JWT_ALGORITHM"] = "HS256"
app.config["JWT_PUBKEY"] = '-----BEGIN PUBLIC KEY-----\n' + os.getenv("JWT_PUBKEY",
                                                                      "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqqnHQ2BWWW9vDNLRCcxD++xZg/16oqMo/c1l+lcFEjjAIJjJp/HqrPYU/U9GvquGE6PbVFtTzW1KcKawOW+FJNOA3CGo8Q1TFEfz43B8rZpKsFbJKvQGVv1Z4HaKPvLUm7iMm8Hv91cLduuoWx6Q3DPe2vg13GKKEZe7UFghF+0T9u8EKzA/XqQ0OiICmsmYPbwvf9N3bCKsB/Y10EYmZRb8IhCoV9mmO5TxgWgiuNeCTtNCv2ePYqL/U0WvyGFW0reasIK8eg3KrAUj8DpyOgPOVBn3lBGf+3KFSYi+0bwZbJZWqbC/Xlk20Go1YfeJPRIt7ImxD27R/lNjgDO/MwIDAQAB") + '\n-----END PUBLIC KEY-----'
app.config["AUTH_SERVICE_ENDPOINT"] = os.getenv("AUTH_SERVICE_ENDPOINT", "http://localhost/api/auth")
app.config["AUTH_SERVICE_CLIENT"] = os.getenv("AUTH_SERVICE_CLIENT", "dbrepo-client")
app.config["AUTH_SERVICE_CLIENT_SECRET"] = os.getenv("AUTH_SERVICE_CLIENT_SECRET", "MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG")
app.config["ADMIN_USERNAME"] = os.getenv('ADMIN_USERNAME', 'admin')
app.config["ADMIN_PASSWORD"] = os.getenv('ADMIN_PASSWORD', 'admin')
app.config["OPENSEARCH_HOST"] = os.getenv('OPENSEARCH_HOST', 'localhost')
app.config["OPENSEARCH_PORT"] = os.getenv('OPENSEARCH_PORT', '9200')
app.config["OPENSEARCH_USERNAME"] = os.getenv('OPENSEARCH_USERNAME', 'admin')
app.config["OPENSEARCH_PASSWORD"] = os.getenv('OPENSEARCH_PASSWORD', 'admin')

app.json_encoder = LazyJSONEncoder

available_types = literal_eval(
    os.getenv("COLLECTION", "['database','table','column','identifier','unit','concept','user','view']"))


@token_auth.verify_token
def verify_token(token: str):
    if token is None or token == "":
        return False
    try:
        client = KeycloakClient()
        return client.verify_jwt(access_token=token)
    except AssertionError:
        return False


@basic_auth.verify_password
def verify_password(username: str, password: str) -> Any:
    if username is None or username == "" or password is None or password == "":
        return False
    if username == app.config["ADMIN_USERNAME"] and password == app.config["ADMIN_PASSWORD"]:
        return User(username=username, roles=["admin"])
    client = KeycloakClient()
    try:
        return client.verify_jwt(access_token=client.obtain_user_token(username=username, password=password))
    except AssertionError as error:
        logging.error(error)
        return False
    except requests.exceptions.ConnectionError as error:
        logging.error(f"Failed to connect to Authentication Service {error}")
        return False


@token_auth.get_user_roles
def get_user_roles(user: User) -> List[str]:
    return user.roles


@basic_auth.get_user_roles
def get_user_roles(user: User) -> List[str]:
    return user.roles


def general_filter(index, results):
    """
    Applies filtering to the result of opensearch queries.

    we only want to return specific entries of the result dict to the user, depending on the queried index.
    the keys for the entries per index that shouldn't be deleted are specified in the important_keys dict.

    :param index: the search index the query results are about
    :param results: the raw response of the query_index_by_term_opensearch function.
    :return:
    """
    important_keys = {
        "column": ["id", "name", "column_type"],
        "table": ["id", "name", "description"],
        "identifier": ["id", "type", "creator"],
        "user": ["id", "username"],
        "database": ["id", "name", "is_public", "details"],
        "concept": ["uri", "name"],
        "unit": [],
        "view": ["id", "name", "creator", " created"],
    }
    if index not in important_keys.keys():
        error_msg = "the keys to be returned to the user for your index aren't specified in the important Keys dict"
        raise KeyError(error_msg)
    for result in results:
        result_keys_copy = tuple(result.keys())
        for key in result_keys_copy:
            if key not in important_keys[index]:
                del result[key]
    logging.debug('general filter results: %s', results)
    return results


@app.route("/health", methods=["GET"], endpoint="actuator_health")
@swag_from("os-yml/health.yml")
def health():
    return dict({"status": "UP"}), 200


@app.route("/api/search/<string:index>", methods=["GET"], endpoint="search_get_index")
@metrics.gauge(name='dbrepo_search_index_list', description='Time needed to list search index')
@swag_from("os-yml/get_index.yml")
def get_index(index: str):
    """
    returns all entries in a specific index
    :param index: desired index
    :return: list of the results
    """
    logging.info(f'Searching for index: {index}')
    if index not in available_types:
        return ApiError(status='NOT_FOUND', message='Failed to find index',
                        code='search.index.missing').model_dump(), 404
    results = OpenSearchClient().query_index_by_term_opensearch("*", "contains")
    results = general_filter(index, results)

    results_per_page = min(request.args.get("results_per_page", 50, type=int), 500)
    max_pages = math.ceil(len(results) / results_per_page)
    page = min(request.args.get("page", 1, type=int), max_pages)
    results = results[(results_per_page * (page - 1)): (results_per_page * page)]
    return dict({"results": results}), 200


@app.route("/api/search/<string:type>/fields", methods=["GET"], endpoint="search_get_index_fields")
@metrics.gauge(name='dbrepo_search_type_list', description='Time needed to list search types')
@swag_from("os-yml/get_fields.yml")
def get_fields(type: str):
    """
    returns a list of attributes of the data for a specific index.
    :param type: The search type
    :return:
    """
    logging.info(f'Searching in index database for type: {type}')
    if type not in available_types:
        return ApiError(status='NOT_FOUND', message='Failed to find type',
                        code='search.type.missing').model_dump(), 404
    fields = OpenSearchClient().get_fields_for_index(type)
    logging.debug(f'get fields for type {type} resulted in {len(fields)} field(s)')
    return fields, 200


@app.route("/api/search", methods=["GET"], endpoint="search_fuzzy_search")
@metrics.gauge(name='dbrepo_search_fuzzy', description='Time needed to search fuzzy')
@swag_from("os-yml/get_fuzzy_search.yml")
def get_fuzzy_search():
    """
    Main endpoint for fuzzy searching.
    :return:
    """
    search_term: str = request.args.get('q')
    if search_term is None or len(search_term) == 0:
        return ApiError(status='BAD_REQUEST', message='Provide a search term with ?q=term',
                        code='search.fuzzy.invalid').model_dump(), 400
    logging.debug(f"search request query: {search_term}")
    results = OpenSearchClient().fuzzy_search(search_term)
    if "hits" in results and "hits" in results["hits"]:
        results = [hit["_source"] for hit in results["hits"]["hits"]]
    return dict({"results": results}), 200


@app.route("/api/search/<string:type>", methods=["POST"], endpoint="search_post_general_search")
@metrics.gauge(name='dbrepo_search_type', description='Time needed to search by type')
@swag_from("os-yml/post_general_search.yml")
def post_general_search(type):
    """
    Main endpoint for fuzzy searching.
    :return:
    """
    if request.content_type != "application/json":
        return ApiError(status='UNSUPPORTED_MEDIA_TYPE', message='Content type needs to be application/json',
                        code='search.general.media').model_dump(), 415
    req_body = request.json
    logging.info(f'Searching in index database for type: {type}')
    logging.debug(f"search request body: {req_body}")
    if type is not None and type not in available_types:
        return ApiError(status='NOT_FOUND', message=f'Type {type} is not in collection: {available_types}',
                        code='search.general.missing').model_dump(), 404
    t1 = request.args.get("t1")
    if not str(t1).isdigit():
        t1 = None
    t2 = request.args.get("t2")
    if not str(t2).isdigit():
        t2 = None
    if t1 is not None and t2 is not None and "unit.uri" in req_body and "concept.uri" in req_body:
        response = OpenSearchClient().unit_independent_search(t1, t2, req_body)
    else:
        response = OpenSearchClient().general_search(type, t1, t2, req_body)
    # filter by type
    if type == 'table':
        tmp = []
        for database in response:
            if database["tables"] is not None:
                for table in database["tables"]:
                    table["is_public"] = database["is_public"]
                    tmp.append(table)
        response = tmp
    if type == 'identifier':
        tmp = []
        for database in response:
            if database["identifiers"] is not None:
                for identifier in database['identifiers']:
                    tmp.append(identifier)
            if database["subsets"] is not None:
                for identifier in database['subsets']:
                    tmp.append(identifier)
            if database["tables"] is not None:
                for table in database['tables']:
                    if database["identifiers"] is not None:
                        for identifier in table['identifiers']:
                            tmp.append(identifier)
        for view in [x for xs in response for x in xs["views"]]:
            if 'identifier' in view:
                tmp.append(view['identifier'])
        response = tmp
    elif type == 'column':
        response = [x for xs in response for x in xs["tables"]]
        for table in response:
            for column in table["columns"]:
                column["table_id"] = table["id"]
                column["database_id"] = table["database_id"]
        response = [x for xs in response for x in xs["columns"]]
    elif type == 'concept':
        tmp = []
        tables = [x for xs in response for x in xs["tables"]]
        for column in [x for xs in tables for x in xs["columns"]]:
            if 'concept' in column and column["concept"] is not None:
                tmp.append(column["concept"])
        response = tmp
    elif type == 'unit':
        tmp = []
        tables = [x for xs in response for x in xs["tables"]]
        for column in [x for xs in tables for x in xs["columns"]]:
            if 'unit' in column and column["unit"] is not None:
                tmp.append(column["unit"])
        response = tmp
    elif type == 'view':
        response = [x for xs in response for x in xs["views"]]
    return dict({'results': response, 'type': type}), 200


@app.route("/api/search/database/<int:database_id>", methods=["PUT"], endpoint="database_put_database")
@metrics.gauge(name='dbrepo_search_update_database',
               description='Time needed to update a database in the search database')
@auth.login_required(role=['admin'])
@swag_from("os-yml/update_database.yml")
def update_database(database_id: int):
    logging.debug(f"updating database with id: {database_id}")
    try:
        payload: Database = Database.model_validate(request.json)
    except ValidationError as e:
        logging.error(f"Failed to validate: {e}")
        return ApiError(status='BAD_REQUEST', message=f'Malformed payload: {e}',
                        code='search.general.missing').model_dump(), 400
    try:
        database = OpenSearchClient().update_database(database_id, payload)
        logging.info(f"Updated database with id : {database_id}")
        return database.model_dump(), 202
    except NotFoundError:
        return ApiError(status='NOT_FOUND', message='Failed to find database',
                        code='search.database.missing').model_dump(), 404
    except TransportError:
        return ApiError(status='BAD_REQUEST', message='Failed to update database',
                        code='search.database.invalid').model_dump(), 400


@app.route("/api/search/database/<int:database_id>", methods=["DELETE"], endpoint="database_delete_database")
@metrics.gauge(name='dbrepo_search_delete_database',
               description='Time needed to delete a database in the search database')
@auth.login_required(role=['admin'])
@swag_from("os-yml/delete_database.yml")
def delete_database(database_id: int):
    try:
        OpenSearchClient().delete_database(database_id)
        return None, 202
    except NotFoundError:
        return ApiError(status='NOT_FOUND', message='Failed to find database',
                        code='search.database.missing').model_dump(), 404

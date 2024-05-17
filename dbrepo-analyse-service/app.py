import json
import logging
from typing import Any, List
import os

from json import dumps

import requests.exceptions
from dbrepo.api.dto import ApiError
from flasgger import LazyJSONEncoder, Swagger
from flask_httpauth import HTTPBasicAuth, MultiAuth, HTTPTokenAuth
from flasgger.utils import swag_from
from flask import Flask, Response, request
from flask_cors import CORS
from prometheus_flask_exporter import PrometheusMetrics

from botocore.exceptions import ClientError

from clients.keycloak_client import KeycloakClient, User
from determine_dt import determine_datatypes
from determine_pk import determine_pk
from determine_stats import determine_stats

logging.addLevelName(level=logging.NOTSET, levelName='TRACE')
logging.basicConfig(level=logging.DEBUG)

from logging.config import dictConfig

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

token_auth = HTTPTokenAuth(scheme='Bearer')
basic_auth = HTTPBasicAuth()
auth = MultiAuth(token_auth, basic_auth)

metrics = PrometheusMetrics(app)
metrics.info("app_info", "Application info", version="__APPVERSION__")
app.config["SWAGGER"] = {"openapi": "3.0.1", "title": "Swagger UI", "uiversion": 3}

swagger_config = {
    "headers": [],
    "specs": [
        {
            "endpoint": "api-analyse",
            "route": "/api-analyse.json",
            "rule_filter": lambda rule: rule.endpoint.startswith('actuator') or rule.endpoint.startswith('analyse'),
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
        "title": "Database Repository Analyse Service API",
        "description": "Service that analyses data structures",
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
            "url": "http://localhost:5000",
            "description": "Generated server url"
        },
        {
            "url": "https://test.dbrepo.tuwien.ac.at",
            "description": "Sandbox"
        }
    ]
}

swagger = Swagger(app, config=swagger_config, template=template)
app.config["GATEWAY_SERVICE_ENDPOINT"] = os.getenv("GATEWAY_SERVICE_ENDPOINT", "http://localhost")
app.config["JWT_ALGORITHM"] = "HS256"
app.config["JWT_PUBKEY"] = '-----BEGIN PUBLIC KEY-----\n' + os.getenv("JWT_PUBKEY",
                                                                      "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqqnHQ2BWWW9vDNLRCcxD++xZg/16oqMo/c1l+lcFEjjAIJjJp/HqrPYU/U9GvquGE6PbVFtTzW1KcKawOW+FJNOA3CGo8Q1TFEfz43B8rZpKsFbJKvQGVv1Z4HaKPvLUm7iMm8Hv91cLduuoWx6Q3DPe2vg13GKKEZe7UFghF+0T9u8EKzA/XqQ0OiICmsmYPbwvf9N3bCKsB/Y10EYmZRb8IhCoV9mmO5TxgWgiuNeCTtNCv2ePYqL/U0WvyGFW0reasIK8eg3KrAUj8DpyOgPOVBn3lBGf+3KFSYi+0bwZbJZWqbC/Xlk20Go1YfeJPRIt7ImxD27R/lNjgDO/MwIDAQAB") + '\n-----END PUBLIC KEY-----'
app.config["AUTH_SERVICE_ENDPOINT"] = os.getenv("AUTH_SERVICE_ENDPOINT", "http://localhost/api/auth")
app.config["AUTH_SERVICE_CLIENT"] = os.getenv("AUTH_SERVICE_CLIENT", "dbrepo")
app.config["AUTH_SERVICE_CLIENT_SECRET"] = os.getenv("AUTH_SERVICE_CLIENT_SECRET", "MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG")
app.config["ADMIN_USERNAME"] = os.getenv('ADMIN_USERNAME', 'admin')
app.config["ADMIN_PASSWORD"] = os.getenv('ADMIN_PASSWORD', 'admin')
app.config["S3_ENDPOINT"] = os.getenv('S3_ENDPOINT', 'http://localhost:9000')
app.config["S3_ACCESS_KEY_ID"] = os.getenv('S3_ACCESS_KEY_ID', 'seaweedfsadmin')
app.config["S3_SECRET_ACCESS_KEY"] = os.getenv('S3_SECRET_ACCESS_KEY', 'seaweedfsadmin')
app.config["S3_EXPORT_BUCKET"] = os.getenv('S3_EXPORT_BUCKET', 'dbrepo-download')
app.config["S3_IMPORT_BUCKET"] = os.getenv('S3_IMPORT_BUCKET', 'dbrepo-upload')

app.json_encoder = LazyJSONEncoder


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


@app.route("/health", methods=["GET"], endpoint="analyse_health")
@swag_from("as-yml/health.yml")
def get_health():
    res = dumps({"status": "UP", "message": "Application is up and running"})
    return Response(res, mimetype="application/json"), 200


@app.route("/api/analyse/datatypes", methods=["GET"], endpoint="analyse_analyse_datatypes")
@swag_from("as-yml/analyse_datatypes.yml")
def analyse_datatypes():
    filename: str = request.args.get('filename')
    separator: str = request.args.get('separator')
    enum: bool = request.args.get('enum', False)
    enum_tol: float = request.args.get('enum_tol')

    if filename is None or separator is None:
        return Response(
            json.dumps({'success': False, 'message': "Missing required query parameters 'filename' and 'separator'"}),
            mimetype="application/json"), 400

    try:
        res = determine_datatypes(filename, enum, enum_tol, separator)
        logging.debug("determine datatype resulted in datatypes %s", res)
        return Response(res, mimetype="application/json"), 202
    except OSError as e:
        logging.error(f"Failed to determine data types: {e}")
        return ApiError(status='BAD_REQUEST', message=str(e), code='analyse.csv.invalid'), 400
    except ClientError as e:
        logging.error(f"Failed to determine separator: {e}")
        return ApiError(status='NOT_FOUND', message='Failed to find csv', code='analyse.csv.missing'), 404


@app.route("/api/analyse/keys", methods=["GET"], endpoint="analyse_analyse_keys")
@swag_from("as-yml/analyse_keys.yml")
def analyse_keys():
    filename: str = request.args.get("filename")
    separator: str = request.args.get('separator')
    logging.debug(f"Analyse keys from filename '{filename}' with separator {separator}")
    if filename is None or separator is None:
        return ApiError(status='BAD_REQUEST', message="Missing required query parameters 'filename' and 'separator'",
                        code='analyse.csv.invalid'), 400
    try:
        res = {
            'keys': determine_pk(filename, separator)
        }
        logging.info(f"Determined list of primary keys: {res}")
        return Response(dumps(res), mimetype="application/json"), 202
    except OSError as e:
        logging.error(f"Failed to determine primary key: {e}")
        return ApiError(status='BAD_REQUEST', message=str(e), code='analyse.database.invalid'), 400


@app.route("/api/analyse/database/<database_id>/table/<table_id>/statistics", methods=["GET"],
           endpoint="analyse_analyse_table_stat")
@auth.login_required(role=['admin', 'export-query-data', 'export-table-data'])
@swag_from("as-yml/analyse_table_stat.yml")
def analyse_table_stat(database_id: int = None, table_id: int = None):
    if database_id is None:
        return ApiError(status='BAD_REQUEST', message="Missing path variable 'database_id'",
                        code='analyse.database.invalid'), 400
    if table_id is None:
        return ApiError(status='BAD_REQUEST', message="Missing path variable 'table_id'",
                        code='analyse.table.invalid'), 400

    try:
        table_stats = determine_stats(database_id=database_id, table_id=table_id)
        logging.info(f"Analysed table statistics")
        return table_stats.model_dump(), 202
    except OSError:
        return ApiError(status='NOT_FOUND', message='Database or table does not exist',
                        code='analyse.database.missing'), 404

import json
import os
import logging
import requests

from typing import Any, List
from flasgger import LazyJSONEncoder, Swagger
from flask import Flask, request, Response
from flask_httpauth import HTTPBasicAuth, MultiAuth, HTTPTokenAuth
from flasgger.utils import swag_from
from json import dumps

from clients.keycloak_client import KeycloakClient, User
from clients.s3_client import S3Client
from prometheus_flask_exporter import PrometheusMetrics

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
            "endpoint": "api-sidecar",
            "route": "/api-sidecar.json",
            "rule_filter": lambda rule: rule.endpoint.startswith('actuator') or rule.endpoint.startswith('sidecar'),
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
        "title": "Database Repository Data Database sidecar API",
        "description": "Sidecar that downloads the import .csv file",
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
            "url": "http://localhost:8080",
            "description": "Generated server url"
        },
        {
            "url": "https://test.dbrepo.tuwien.ac.at",
            "description": "Sandbox"
        }
    ]
}

swagger = Swagger(app, config=swagger_config, template=template)
app.config["JWT_ALGORITHM"] = "HS256"
app.config["JWT_PUBKEY"] = '-----BEGIN PUBLIC KEY-----\n' + os.getenv("JWT_PUBKEY",
                                                                      "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqqnHQ2BWWW9vDNLRCcxD++xZg/16oqMo/c1l+lcFEjjAIJjJp/HqrPYU/U9GvquGE6PbVFtTzW1KcKawOW+FJNOA3CGo8Q1TFEfz43B8rZpKsFbJKvQGVv1Z4HaKPvLUm7iMm8Hv91cLduuoWx6Q3DPe2vg13GKKEZe7UFghF+0T9u8EKzA/XqQ0OiICmsmYPbwvf9N3bCKsB/Y10EYmZRb8IhCoV9mmO5TxgWgiuNeCTtNCv2ePYqL/U0WvyGFW0reasIK8eg3KrAUj8DpyOgPOVBn3lBGf+3KFSYi+0bwZbJZWqbC/Xlk20Go1YfeJPRIt7ImxD27R/lNjgDO/MwIDAQAB") + '\n-----END PUBLIC KEY-----'
app.config["AUTH_SERVICE_ENDPOINT"] = os.getenv("AUTH_SERVICE_ENDPOINT", "http://localhost/api/auth")
app.config["AUTH_SERVICE_CLIENT"] = os.getenv("AUTH_SERVICE_CLIENT", "dbrepo-client")
app.config["AUTH_SERVICE_CLIENT_SECRET"] = os.getenv("AUTH_SERVICE_CLIENT_SECRET", "MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG")
app.config["ADMIN_USERNAME"] = os.getenv('ADMIN_USERNAME', 'admin')
app.config["ADMIN_PASSWORD"] = os.getenv('ADMIN_PASSWORD', 'admin')
app.config["S3_ACCESS_KEY_ID"] = os.getenv('S3_ACCESS_KEY_ID', 'seaweedfsadmin')
app.config["S3_ENDPOINT"] = os.getenv('S3_ENDPOINT', 'http://localhost:9000')
app.config["S3_EXPORT_BUCKET"] = os.getenv('S3_EXPORT_BUCKET', 'dbrepo-download')
app.config["S3_FILE_PATH"] = os.getenv('S3_FILE_PATH', '/tmp')
app.config["S3_SECRET_ACCESS_KEY"] = os.getenv('S3_SECRET_ACCESS_KEY', 'seaweedfsadmin')
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


@app.route("/health", methods=["GET"], endpoint="actuator_health")
def health():
    logging.debug("endpoint health, body=%s", request)
    res = dumps({"status": "UP", "message": "Application is up and running"})
    return Response(res, mimetype="application/json"), 200


@app.route("/sidecar/import/<string:filename>", methods=["POST"], endpoint="sidecar_import")
@metrics.gauge(name='dbrepo_sidecar_import_dataset', description='Time needed to import dataset from S3')
@auth.login_required(role=['admin', 'import-database-data'])
@swag_from("ds-yml/import.yml")
def import_csv(filename):
    auth.current_user()
    logging.debug('endpoint import csv, filename=%s, body=%s', filename, request)
    s3_client = S3Client()
    response = s3_client.download_file(filename, app.config["S3_FILE_PATH"], app.config['S3_IMPORT_BUCKET'])
    if response is False:
        return Response(), 400
    return Response(json.dumps(response)), 202


@app.route("/sidecar/export/<string:filename>", methods=["POST"], endpoint="sidecar_export")
@metrics.gauge(name='dbrepo_sidecar_export_dataset', description='Time needed to export dataset to S3')
@auth.login_required(role=['admin', 'export-query-data', 'export-table-data'])
@swag_from("ds-yml/export.yml")
def import_csv(filename):
    logging.debug('endpoint export csv, filename=%s, body=%s', filename, request)
    s3_client = S3Client()
    response = s3_client.upload_file(filename, app.config["S3_FILE_PATH"], app.config['S3_EXPORT_BUCKET'])
    if response is False:
        return Response(), 400
    return Response(), 202

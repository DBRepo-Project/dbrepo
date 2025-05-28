import logging
import os
from http import HTTPStatus
from json import dumps
from typing import List, Any

from dbrepo.api.dto import ApiError, Database, User
from dbrepo.core.api.exceptions import DashboardNotFound
from dbrepo.core.client.auth import AuthServiceClient
from dbrepo.core.client.dashboard import DashboardServiceClient
from flasgger import LazyJSONEncoder, Swagger, swag_from
from flask import Flask, request, Response
from flask_cors import CORS
from flask_httpauth import HTTPTokenAuth, HTTPBasicAuth, MultiAuth
from grafana_client.client import GrafanaClientError
from prometheus_flask_exporter import PrometheusMetrics
from pydantic import ValidationError

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
            'format': '[%(asctime)s] [%(levelname)s] %(message)s',
        },
        "ecs": {
            "()": "ecs_logging.StdlibFormatter"
        },
    },
    'handlers': {
        'wsgi': {
            'class': 'logging.StreamHandler',
            'stream': 'ext://flask.logging.wsgi_errors_stream',
            'formatter': 'simple'
        },
        'file': {
            'class': 'logging.handlers.TimedRotatingFileHandler',
            'formatter': 'ecs',
            'filename': '/var/log/app/service/dashboard/app.log',
            'when': 'm',
            'interval': 1,
            'backupCount': 5,
            'encoding': 'utf8'
        },
    },
    'root': {
        'level': 'DEBUG',
        'handlers': ['wsgi', 'file']
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
            "endpoint": "api-docs",
            "route": "/api-docs.json",
            "rule_filter": lambda rule: True,
            "model_filter": lambda tag: True,  # all in
        }
    ],
    "static_url_path": "/flasgger_static",
    "swagger_ui": True,
    "specs_route": "/swagger-ui/",
}

template = {
    "openapi": "3.0.0",
    "info": {
        "title": "Database Repository Dashboard Service API",
        "description": "Service that manages the dashboards",
        "version": "1.9.0",
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
        "url": "https://www.ifs.tuwien.ac.at/infrastructures/dbrepo/1.7/"
    },
    "servers": [
        {
            "url": "http://localhost",
            "description": "Generated server url"
        },
        {
            "url": "https://test.dbrepo.tuwien.ac.at",
            "description": "Sandbox"
        }
    ],
    "components": {
        "schemas": {
            "ApiError": {
                "properties": {
                    "message": {
                        "example": "Message",
                        "type": "string"
                    },
                    "status": {
                        "example": "BAD_REQUEST",
                        "type": "string"
                    },
                    "code": {
                        "example": "error.dashboard.create",
                        "type": "string"
                    }
                },
                "type": "object"
            },
        },
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
    }
}

swagger = Swagger(app, config=swagger_config, template=template)
app.config["AUTH_SERVICE_ENDPOINT"] = os.getenv("AUTH_SERVICE_ENDPOINT", "http://localhost:8080")
app.config["AUTH_SERVICE_CLIENT"] = os.getenv("AUTH_SERVICE_CLIENT", "dbrepo-client")
app.config["AUTH_SERVICE_CLIENT_SECRET"] = os.getenv("AUTH_SERVICE_CLIENT_SECRET", "MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG")
app.config["BASE_URL"] = os.getenv("BASE_URL", "http://localhost")
app.config["JSON_DATASOURCE_NAME"] = os.getenv('JSON_DATASOURCE_NAME', 'dbrepojson0')
app.config["JWT_ALGORITHM"] = "HS256"
app.config["JWT_PUBKEY"] = '-----BEGIN PUBLIC KEY-----\n' + os.getenv("JWT_PUBKEY",
                                                                      "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqqnHQ2BWWW9vDNLRCcxD++xZg/16oqMo/c1l+lcFEjjAIJjJp/HqrPYU/U9GvquGE6PbVFtTzW1KcKawOW+FJNOA3CGo8Q1TFEfz43B8rZpKsFbJKvQGVv1Z4HaKPvLUm7iMm8Hv91cLduuoWx6Q3DPe2vg13GKKEZe7UFghF+0T9u8EKzA/XqQ0OiICmsmYPbwvf9N3bCKsB/Y10EYmZRb8IhCoV9mmO5TxgWgiuNeCTtNCv2ePYqL/U0WvyGFW0reasIK8eg3KrAUj8DpyOgPOVBn3lBGf+3KFSYi+0bwZbJZWqbC/Xlk20Go1YfeJPRIt7ImxD27R/lNjgDO/MwIDAQAB") + '\n-----END PUBLIC KEY-----'
app.config["READONLY_USERNAME"] = os.getenv('READONLY_USERNAME', 'user')
app.config["READONLY_PASSWORD"] = os.getenv('READONLY_PASSWORD', 'user')

app.json_encoder = LazyJSONEncoder

headers = {'Content-Type': 'application/json'}


def dashboard_client():
    return DashboardServiceClient(endpoint=os.getenv('DASHBOARD_UI_ENDPOINT', 'http://localhost:3000'),
                                  username=os.getenv('SYSTEM_USERNAME', 'admin'),
                                  password=os.getenv('SYSTEM_PASSWORD', 'admin'))


def auth_client():
    return AuthServiceClient(app.config["AUTH_SERVICE_ENDPOINT"], app.config["AUTH_SERVICE_CLIENT"],
                             app.config["AUTH_SERVICE_CLIENT_SECRET"], app.config["JWT_PUBKEY"])


@token_auth.verify_token
def verify_token(token: str) -> bool | User:
    return auth_client().is_valid_token(token)


@basic_auth.verify_password
def verify_password(username: str, password: str) -> Any:
    return auth_client().is_valid_password(username, password)


@token_auth.get_user_roles
def get_user_roles(user: User) -> List[str]:
    return auth_client().get_user_roles(user)


@basic_auth.get_user_roles
def get_user_roles(user: User) -> List[str]:
    return auth_client().get_user_roles(user)


@app.route("/health", methods=["GET"], endpoint="actuator_health")
def health():
    return dict({"status": "UP"}), 200


@app.route("/api/dashboard", methods=["POST"], endpoint="create_dashboard")
@metrics.gauge(name='dbrepo_create_dashboard', description='Time needed to create dashboard')
@swag_from("/app/ds-yml/create_dashboard.yml")
@auth.login_required(role=['system'])
def create_dashboard():
    for parameter in [param for param in ['is_public', 'is_schema_public', 'owner_username', 'database_name'] if
                      param not in request.json]:
        return Response(ApiError(status='BAD_REQUEST', message=f'Missing required parameter: {parameter}',
                                 code="error.dashboard.malformed").model_dump_json(), 400, headers)

    is_public = bool(request.json['is_public'])
    is_schema_public = bool(request.json['is_schema_public'])
    owner_username = request.json['owner_username']
    logging.debug(f"endpoint create dashboard, is_public={is_public}, is_schema_public={is_schema_public}, "
                  f"owner_username={owner_username}")
    try:
        db = dashboard_client().create(request.json['database_name'])
        dashboard_client().update_anonymous_read_access(db['uid'], is_public, is_schema_public)
        return Response(dumps(db)), 201, headers
    except GrafanaClientError as e:
        logging.error(f"Failed to create dashboard: {e.response['message']}")
        dto = ApiError(status=HTTPStatus(e.status_code).phrase.upper(),
                       message=f"Failed to create dashboard: {e.response['message']}", code="error.dashboard.create")
        if e.status_code == 409 or e.status_code == 412:
            dto.code = "error.dashboard.exists"
            return Response(dto.model_dump_json(), 409, headers)
        return Response(dto.model_dump_json(), e.status_code, headers)


@app.route("/api/dashboard/<string:uid>", methods=["PUT"], endpoint="update_dashboard")
@metrics.gauge(name='dbrepo_update_dashboard', description='Time needed to update dashboard')
@swag_from("/app/ds-yml/update_dashboard.yml")
@auth.login_required(role=['system'])
def update_dashboard(uid: str):
    logging.debug(f'endpoint update dashboard, uid={uid}')
    try:
        database = Database.model_validate(request.json)
    except ValidationError as e:
        logging.error(f'Model malformed: {str(e).strip()}')
        return Response(ApiError(status='BAD_REQUEST', message='Invalid database format',
                                 code='error.database.malformed').model_dump_json(), 400, headers)
    try:
        dashboard_client().update(database)
    except DashboardNotFound:
        return Response(ApiError(status='NOT_FOUND', message=f"Failed to update dashboard: not found",
                                 code="error.dashboard.missing").model_dump_json(), 404, headers)
    dashboard_client().update_anonymous_read_access(uid, database.is_public, database.is_schema_public)
    return Response(), 202, headers


@app.route("/api/dashboard/<string:uid>/access/<string:username>", methods=["PUT"], endpoint="update_dashboard_access")
@metrics.gauge(name='dbrepo_update_dashboard_access', description='Time needed to update dashboard access')
@swag_from("/app/ds-yml/update_dashboard_access.yml")
@auth.login_required(role=['system'])
def update_dashboard(uid: str, username: str):
    logging.debug(f'endpoint update dashboard access, uid={uid}, username={username}')
    # not implemented
    return Response(), 202, headers

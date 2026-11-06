import logging
import math
import os
from dbrepo.api.dto import Database, ApiError
from dbrepo.api.exceptions import ServiceConnectionError
from dbrepo.core.client.auth import User, AuthServiceClient
from dbrepo.core.client.search import SearchServiceClient, semantic_value
from flasgger import LazyJSONEncoder, Swagger, swag_from
from flask import Flask, request, Response
from flask_cors import CORS
from flask_httpauth import HTTPTokenAuth, HTTPBasicAuth, MultiAuth
from json import dumps
from opensearchpy import NotFoundError
from prometheus_flask_exporter import PrometheusMetrics
from pydantic import ValidationError
from pydantic.deprecated.json import pydantic_encoder
from typing import List, Any

logging.addLevelName(level=logging.NOTSET, levelName='TRACE')
logging.basicConfig(level=logging.DEBUG)

from logging.config import dictConfig

LOG_PATH = os.getenv("LOG_PATH", "/var/log/app/service/search")

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
            'filename': f'{LOG_PATH}/app.log',
            'when': 'm',
            'interval': 1,
            'backupCount': 5,
            'encoding': 'utf8'
        },
    },
    'root': {
        'level': os.getenv("LOG_APPLICATION_LEVEL", "debug").upper(),
        'handlers': ['wsgi', 'file']
    }
})

# create app object
app = Flask(__name__)

cors = CORS(app, resources={r"/api/*": {"origins": "*"}},
            expose_headers=['X-Count', 'X-Total-Pages', 'X-Page', 'X-Page-Size'])

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
            "rule_filter": lambda rule: rule.endpoint.startswith('search'),
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
        "schemas": {
            "IndexDto": {
                "required": ["results", "type"],
                "properties": {
                    "results": {
                        "type": "array",
                        "items": {
                            "type": "object",
                        }
                    },
                    "type": {
                        "type": "string",
                        "description": "Same as the requested type",
                        "enum": ["database", "table", "view", "column", "user", "identifier", "concept", "unit"]
                    }
                }
            },
            "IndexFieldsDto": {
                "required": ["results"],
                "type": "object",
                "properties": {
                    "results": {
                        "type": "array",
                        "items": {
                            "$ref": "#/components/schemas/IndexFieldDto"
                        }
                    }
                }
            },
            "IndexFieldDto": {
                "required": ["attr_name", "attr_friendly_name", "type"],
                "type": "object",
                "properties": {
                    "attr_name": {
                        "type": "string",
                        "example": "name"
                    },
                    "attr_friendly_name": {
                        "type": "string",
                        "example": "Name"
                    },
                    "type": {
                        "type": "string",
                        "example": "string",
                        "description": "OpenSearch data types."
                    }
                }
            },
            "SearchRequestDto": {
                "required": ["search_term", "field_value_pairs"],
                "type": "object",
                "properties": {
                    "search_term": {
                        "type": "string"
                    },
                    "field_value_pairs": {
                        "type": "object"
                    }
                }
            },
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
    },
    "info": {
        "title": "Database Repository Search Service API",
        "description": "Service that searches the search database",
        "version": "1.13.3",
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
        "url": "https://www.ifs.tuwien.ac.at/infrastructures/dbrepo/1.13/"
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
app.config["METADATA_SERVICE_ENDPOINT"] = os.getenv("METADATA_SERVICE_ENDPOINT", "http://metadata-service:8080")
app.config["AUTH_SERVICE_ENDPOINT"] = os.getenv("AUTH_SERVICE_ENDPOINT", "http://localhost:8080")
app.config["AUTH_SERVICE_CLIENT"] = os.getenv("AUTH_SERVICE_CLIENT", "dbrepo-client")
app.config["AUTH_SERVICE_CLIENT_SECRET"] = os.getenv("AUTH_SERVICE_CLIENT_SECRET", "MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG")
app.config["OPENSEARCH_HOST"] = os.getenv('OPENSEARCH_HOST', 'search-db')
app.config["OPENSEARCH_PORT"] = os.getenv('OPENSEARCH_PORT', '9200')
app.config["OPENSEARCH_USERNAME"] = os.getenv('OPENSEARCH_USERNAME', 'admin')
app.config["OPENSEARCH_PASSWORD"] = os.getenv('OPENSEARCH_PASSWORD', 'admin')
app.config["SYSTEM_USERNAME"] = os.getenv('SYSTEM_USERNAME', 'admin')
app.config["SYSTEM_PASSWORD"] = os.getenv('SYSTEM_PASSWORD', 'admin')

app.json_encoder = LazyJSONEncoder

auth_client = AuthServiceClient(app.config["AUTH_SERVICE_ENDPOINT"], app.config["AUTH_SERVICE_CLIENT"],
                                app.config["AUTH_SERVICE_CLIENT_SECRET"])


@token_auth.verify_token
def verify_token(token: str) -> bool | User:
    return auth_client.is_valid_token(token)


@basic_auth.verify_password
def verify_password(username: str, password: str) -> Any:
    if username == app.config["SYSTEM_USERNAME"] and password == app.config["SYSTEM_PASSWORD"]:
        return User(id='internal', username=app.config["SYSTEM_USERNAME"], roles=["system"])
    return auth_client.is_valid_password(username, password)


@token_auth.get_user_roles
def get_user_roles(user: User) -> List[str]:
    return auth_client.get_user_roles(user)


@basic_auth.get_user_roles
def get_user_roles(user: User) -> List[str]:
    return auth_client.get_user_roles(user)


headers = {'Content-Type': 'application/json'}


@app.errorhandler(ServiceConnectionError)
def metadata_unavailable(error):
    logging.warning("Current search metadata is unavailable")
    return ApiError(status='SERVICE_UNAVAILABLE', message='Current search metadata is unavailable',
                    code='search.metadata.unavailable').model_dump(), 503, headers


def general_filter(index: str, results: list[Database]):
    """Project already authorized databases into the requested result type."""
    if index == 'database':
        return results
    if index == 'user':
        return [database.owner for database in results]
    tables = [table for database in results for table in database.tables or []]
    views = [view for database in results for view in database.views or []]
    if index == 'table':
        return tables
    if index == 'view':
        return views
    if index == 'identifier':
        return ([identifier for database in results
                 for identifier in (database.identifiers or []) + (database.subsets or [])]
                + [identifier for source in tables + views for identifier in source.identifiers or []])
    columns = [column for table in tables for column in table.columns]
    if index == 'column':
        return columns
    if index in ('concept', 'unit'):
        return [value for column in columns if (value := semantic_value(column, index)) is not None]
    raise KeyError(index)


def search_pagination(default_size=None):
    if default_size is None and 'page' not in request.args and 'results_per_page' not in request.args:
        return None
    page = int(request.args.get('page', '1'))
    size = int(request.args.get('results_per_page', str(default_size or 50)))
    if page < 1 or not 1 <= size <= 500:
        raise ValueError('Invalid search pagination')
    return page, size


def search_response(results, field_type, pagination):
    def sort_key(item):
        data = item.model_dump(mode='json') if hasattr(item, 'model_dump') else item
        if field_type in ('concept', 'unit'):
            identity = data.get('uri') or ''
        elif field_type == 'user':
            identity = data.get('id') or data.get('username') or ''
        else:
            identity = data.get('id') or ''
        return identity, dumps(data, sort_keys=True)

    results = sorted(results, key=sort_key)
    response_headers = dict(headers, **{'X-Count': str(len(results)),
        'Access-Control-Expose-Headers': 'X-Count, X-Total-Pages, X-Page, X-Page-Size'})
    if pagination is not None:
        page, size = pagination
        response_headers.update({'X-Total-Pages': str(math.ceil(len(results) / size)),
                                 'X-Page': str(page), 'X-Page-Size': str(size)})
        results = results[(page - 1) * size:page * size]
    return Response(dumps(results, default=pydantic_encoder)), 200, response_headers


def invalid_search_pagination():
    return ApiError(status='BAD_REQUEST', message='Page must be positive and results_per_page must be between 1 and 500',
                    code='search.pagination.invalid').model_dump(), 400, headers


def search_client():
    return SearchServiceClient(app.config["OPENSEARCH_HOST"], int(app.config["OPENSEARCH_PORT"]),
                               app.config["OPENSEARCH_USERNAME"], app.config["OPENSEARCH_PASSWORD"])


@app.route("/health", methods=["GET"], endpoint="actuator_health")
def health():
    return dict({"status": "UP"}), 200, headers


@app.route("/api/v1/search/<string:index>", methods=["GET"], endpoint="search_get_index")
@metrics.gauge(name='dbrepo_search_index_list', description='Time needed to list search index')
@swag_from("/app/os-yml/get_index.yml")
def get_index(index: str):
    """
    returns all entries in a specific index
    :param index: desired index
    :return: list of the results
    """
    logging.debug(f'endpoint get search type: {index}')
    try:
        pagination = search_pagination(default_size=50)
    except ValueError:
        return invalid_search_pagination()
    username, error, status = auth_client.get_username(request.headers.get('Authorization'))
    if error is not None and status is not None:
        return error, status, headers
    results = search_client().general_search(username=username, user_token=request.headers.get('Authorization'))
    try:
        results = general_filter(index, results)

        return search_response(results, index, pagination)
    except KeyError:
        return ApiError(status='NOT_FOUND', message=f'Failed to find get index: {index}',
                        code='search.index.missing').model_dump(), 404, headers


@app.route("/api/v1/search/<string:field_type>/fields", methods=["GET"], endpoint="search_get_index_fields")
@metrics.gauge(name='dbrepo_search_type_list', description='Time needed to list search types')
@swag_from("/app/os-yml/get_fields.yml")
def get_fields(field_type: str):
    """
    returns a list of attributes of the data for a specific index.
    :param field_type: The search type
    :return:
    """
    logging.debug(f'endpoint get search type fields: {field_type}')
    try:
        fields = search_client().get_fields_for_index(field_type)
        logging.debug(f'get fields for field_type {field_type} resulted in {len(fields)} field(s)')
        return Response(dumps(fields, default=pydantic_encoder)), 200, headers
    except NotFoundError:
        return ApiError(status='NOT_FOUND', message=f'Failed to find fields for search type {field_type}',
                        code='search.type.missing').model_dump(), 404, headers


@app.route("/api/v1/search", methods=["GET"], endpoint="search_fuzzy_search")
@metrics.gauge(name='dbrepo_search_fuzzy', description='Time needed to search fuzzy')
@swag_from("/app/os-yml/get_fuzzy_search.yml")
def get_fuzzy_search():
    """
    Main endpoint for fuzzy searching.
    :return:
    """
    search_term: str | None = request.args.get('q')
    logging.debug(f'endpoint get fuzzy search, q={search_term}')
    if search_term is None or len(search_term) == 0:
        return ApiError(status='BAD_REQUEST', message='Provide a search term with ?q=term',
                        code='search.fuzzy.invalid').model_dump(), 400
    logging.debug(f"search request query: {search_term}")
    try:
        pagination = search_pagination()
    except ValueError:
        return invalid_search_pagination()
    username, error, status = auth_client.get_username(request.headers.get('Authorization'))
    if error is not None and status is not None:
        return error, status, headers
    results = search_client().fuzzy_search(search_term=search_term,
                                           username=username,
                                           user_token=request.headers.get('Authorization'))
    return search_response(results, 'database', pagination)


@app.route("/api/v1/search/<string:field_type>", methods=["POST"], endpoint="search_post_general_search")
@metrics.gauge(name='dbrepo_search_type', description='Time needed to search by type')
@swag_from("/app/os-yml/post_general_search.yml")
def post_general_search(field_type):
    """
    Main endpoint for fuzzy searching.
    :return:
    """
    if request.content_type != "application/json":
        return ApiError(status='UNSUPPORTED_MEDIA_TYPE', message='Content type needs to be application/json',
                        code='search.general.media').model_dump(), 415
    try:
        pagination = search_pagination()
    except ValueError:
        return invalid_search_pagination()
    value_pairs = request.json
    logging.debug(f'endpoint get general search, field_type={field_type}, value_pairs={value_pairs}')
    if not isinstance(value_pairs, dict):
        return ApiError(status='BAD_REQUEST', message='Search fields must be an object',
                        code='search.general.invalid').model_dump(), 400, headers
    value_pairs = value_pairs.copy()
    t1 = request.args.get('t1', value_pairs.pop('t1', None))
    t2 = request.args.get('t2', value_pairs.pop('t2', None))
    for field in ('concept', 'unit'):
        for alias in (f'{field}.uri', f'tables.columns.{field}_uri'):
            if alias in value_pairs:
                value_pairs[f'tables.columns.{field}.uri'] = value_pairs.pop(alias)
    username, error, status = auth_client.get_username(request.headers.get('Authorization'))
    if error is not None and status is not None:
        return error, status
    if t1 is not None or t2 is not None:
        try:
            t1, t2 = float(t1), float(t2)
            if not math.isfinite(t1) or not math.isfinite(t2) or t1 > t2:
                raise ValueError('Invalid range')
            if not value_pairs.get('tables.columns.unit.uri') or not value_pairs.get('tables.columns.concept.uri'):
                raise ValueError('A range requires a concept and unit')
        except (TypeError, ValueError):
            return ApiError(status='BAD_REQUEST', message='Provide a finite ordered range, concept and unit',
                            code='search.general.range').model_dump(), 400, headers
        response = search_client().unit_independent_search(t1, t2, value_pairs, username,
                                                           request.headers.get('Authorization'))
    else:
        response = search_client().general_search(field_value_pairs=value_pairs,
                                                 username=username,
                                                 user_token=request.headers.get('Authorization'))
    try:
        response = general_filter(field_type, response)
    except KeyError:
        return ApiError(status='NOT_FOUND', message=f'Failed to find search type: {field_type}',
                        code='search.type.missing').model_dump(), 404, headers
    return search_response(response, field_type, pagination)


@app.route("/api/v1/search/database/<string:database_id>", methods=["PUT"], endpoint="search_save_database")
@metrics.gauge(name='dbrepo_search_save_database',
               description='Time needed to update a database in the search database')
@auth.login_required(role=['system'])
def save_database(database_id: str):
    logging.debug(f"save database with id: {database_id}")
    try:
        payload = Database.model_validate(request.json)
    except ValidationError as e:
        logging.error(f"Failed to validate: {str(e).strip()}")
        return ApiError(status='BAD_REQUEST', message=f'Malformed payload: {str(e).strip()}',
                        code='search.general.missing').model_dump(), 400
    search_client().save_database(database_id, payload)
    return Response(), 202, headers


@app.route("/api/v1/search/database/<string:database_id>", methods=["DELETE"], endpoint="database_delete_database")
@metrics.gauge(name='dbrepo_search_delete_database',
               description='Time needed to delete a database in the search database')
@auth.login_required(role=['system'])
def delete_database(database_id: str):
    logging.debug(f"delete database with id: {database_id}")
    try:
        search_client().delete_database(database_id)
        return Response(dumps({})), 202, headers
    except NotFoundError:
        return ApiError(status='NOT_FOUND', message='Failed to find database',
                        code='search.database.missing').model_dump(), 404

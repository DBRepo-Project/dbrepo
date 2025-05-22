import logging
import math
import os
from json import dumps
from typing import List, Any

from dbrepo.api.dto import Database, ApiError
from dbrepo.core.client.auth import User, AuthServiceClient
from dbrepo.core.client.search import SearchServiceClient, flatten
from flasgger import LazyJSONEncoder, Swagger, swag_from
from flask import Flask, request, Response
from flask_cors import CORS
from flask_httpauth import HTTPTokenAuth, HTTPBasicAuth, MultiAuth
from opensearchpy import NotFoundError
from prometheus_flask_exporter import PrometheusMetrics
from pydantic import ValidationError
from pydantic.deprecated.json import pydantic_encoder

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
            'filename': '/var/log/app/service/search/app.log',
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
app.config["JWT_ALGORITHM"] = "HS256"
app.config["JWT_PUBKEY"] = '-----BEGIN PUBLIC KEY-----\n' + os.getenv("JWT_PUBKEY",
                                                                      "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqqnHQ2BWWW9vDNLRCcxD++xZg/16oqMo/c1l+lcFEjjAIJjJp/HqrPYU/U9GvquGE6PbVFtTzW1KcKawOW+FJNOA3CGo8Q1TFEfz43B8rZpKsFbJKvQGVv1Z4HaKPvLUm7iMm8Hv91cLduuoWx6Q3DPe2vg13GKKEZe7UFghF+0T9u8EKzA/XqQ0OiICmsmYPbwvf9N3bCKsB/Y10EYmZRb8IhCoV9mmO5TxgWgiuNeCTtNCv2ePYqL/U0WvyGFW0reasIK8eg3KrAUj8DpyOgPOVBn3lBGf+3KFSYi+0bwZbJZWqbC/Xlk20Go1YfeJPRIt7ImxD27R/lNjgDO/MwIDAQAB") + '\n-----END PUBLIC KEY-----'
app.config["AUTH_SERVICE_ENDPOINT"] = os.getenv("AUTH_SERVICE_ENDPOINT", "http://localhost:8080")
app.config["AUTH_SERVICE_CLIENT"] = os.getenv("AUTH_SERVICE_CLIENT", "dbrepo-client")
app.config["AUTH_SERVICE_CLIENT_SECRET"] = os.getenv("AUTH_SERVICE_CLIENT_SECRET", "MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG")
app.config["OPENSEARCH_HOST"] = os.getenv('OPENSEARCH_HOST', 'search-db')
app.config["OPENSEARCH_PORT"] = os.getenv('OPENSEARCH_PORT', '9200')
app.config["OPENSEARCH_USERNAME"] = os.getenv('OPENSEARCH_USERNAME', 'admin')
app.config["OPENSEARCH_PASSWORD"] = os.getenv('OPENSEARCH_PASSWORD', 'admin')

app.json_encoder = LazyJSONEncoder

auth_client = AuthServiceClient(app.config["AUTH_SERVICE_ENDPOINT"], app.config["AUTH_SERVICE_CLIENT"],
                                app.config["AUTH_SERVICE_CLIENT_SECRET"], app.config["JWT_PUBKEY"])


@token_auth.verify_token
def verify_token(token: str) -> bool | User:
    return auth_client.is_valid_token(token)


@basic_auth.verify_password
def verify_password(username: str, password: str) -> Any:
    return auth_client.is_valid_password(username, password)


@token_auth.get_user_roles
def get_user_roles(user: User) -> List[str]:
    return auth_client.get_user_roles(user)


@basic_auth.get_user_roles
def get_user_roles(user: User) -> List[str]:
    return auth_client.get_user_roles(user)


headers = {'Content-Type': 'application/json'}


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
        "database": ["id", "name", "is_public", "is_schema_public", "details"],
        "concept": ["uri", "name"],
        "unit": [],
        "view": ["id", "name", "creator"],
    }
    if index not in important_keys.keys():
        raise KeyError(f"Failed to find index {index} in: {important_keys.keys()}")
    for result in results:
        result_keys_copy = tuple(result.keys())
        for key in result_keys_copy:
            if key not in important_keys[index]:
                del result[key]
    logging.debug('general filter results: %s', results)
    return results


def search_client():
    return SearchServiceClient(app.config["OPENSEARCH_HOST"], int(app.config["OPENSEARCH_PORT"]),
                               app.config["OPENSEARCH_USERNAME"], app.config["OPENSEARCH_PASSWORD"])


@app.route("/health", methods=["GET"], endpoint="actuator_health")
def health():
    return dict({"status": "UP"}), 200, headers


@app.route("/api/search/<string:index>", methods=["GET"], endpoint="search_get_index")
@metrics.gauge(name='dbrepo_search_index_list', description='Time needed to list search index')
@swag_from("/app/os-yml/get_index.yml")
def get_index(index: str):
    """
    returns all entries in a specific index
    :param index: desired index
    :return: list of the results
    """
    logging.debug(f'endpoint get search type: {index}')
    results = search_client().query_index_by_term_opensearch("*", "contains")
    try:
        results = general_filter(index, results)

        results_per_page = min(request.args.get("results_per_page", 50, type=int), 500)
        max_pages = math.ceil(len(results) / results_per_page)
        page = min(request.args.get("page", 1, type=int), max_pages)
        results = results[(results_per_page * (page - 1)): (results_per_page * page)]
        return Response(dumps(results, default=pydantic_encoder)), 200, headers
    except KeyError:
        return ApiError(status='NOT_FOUND', message=f'Failed to find get index: {index}',
                        code='search.index.missing').model_dump(), 404, headers


@app.route("/api/search/<string:field_type>/fields", methods=["GET"], endpoint="search_get_index_fields")
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


@app.route("/api/search", methods=["GET"], endpoint="search_fuzzy_search")
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
    user_id, error, status = auth_client.get_user_id(request.headers.get('Authorization'))
    if error is not None and status is not None:
        return error, status, headers
    results = search_client().fuzzy_search(search_term=search_term,
                                           user_id=user_id,
                                           user_token=request.headers.get('Authorization'))
    return Response(dumps(results, default=pydantic_encoder)), 200, headers


@app.route("/api/search/<string:field_type>", methods=["POST"], endpoint="search_post_general_search")
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
    value_pairs = request.json
    logging.debug(f'endpoint get general search, field_type={field_type}, value_pairs={value_pairs}')
    t1 = request.args.get("t1")
    if not str(t1).isdigit():
        t1 = None
    t2 = request.args.get("t2")
    if not str(t2).isdigit():
        t2 = None
    user_id, error, status = auth_client.get_user_id(request.headers.get('Authorization'))
    if error is not None and status is not None:
        return error, status
    if t1 is not None and t2 is not None and "unit.uri" in value_pairs and "concept.uri" in value_pairs:
        response = search_client().unit_independent_search(t1, t2, value_pairs, user_id)
    else:
        response = search_client().general_search(field_type=field_type,
                                                  field_value_pairs=value_pairs,
                                                  user_id=user_id,
                                                  user_token=request.headers.get('Authorization'))
    # filter by type
    tables = [table for table in flatten([database.tables for database in response]) if
              table.is_public or table.is_schema_public or (user_id is not None and table.owner.id == user_id)]
    views = [view for view in flatten([database.views for database in response]) if
             view.is_public or view.is_schema_public or (user_id is not None and view.owner.id == user_id)]
    if field_type == 'table':
        logging.debug(f'filtered to {len(tables)} tables')
        response = tables
    if field_type == 'identifier':
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
    elif field_type == 'column':
        response = flatten([table.columns for table in tables])
    elif field_type == 'concept':
        tmp = []
        tables = [x for xs in response for x in xs["tables"]]
        for column in [x for xs in tables for x in xs["columns"]]:
            if 'concept' in column and column["concept"] is not None:
                tmp.append(column["concept"])
        response = tmp
    elif field_type == 'unit':
        tmp = []
        tables = [x for xs in response for x in xs["tables"]]
        for column in [x for xs in tables for x in xs["columns"]]:
            if 'unit' in column and column["unit"] is not None:
                tmp.append(column["unit"])
        response = tmp
    elif field_type == 'view':
        response = views
    return Response(dumps(response, default=pydantic_encoder)), 200, headers


@app.route("/api/search/database/<string:database_id>", methods=["PUT"], endpoint="search_save_database")
@metrics.gauge(name='dbrepo_search_save_database',
               description='Time needed to update a database in the search database')
@auth.login_required(role=['update-search-index'])
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


@app.route("/api/search/database/<string:database_id>", methods=["DELETE"], endpoint="database_delete_database")
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

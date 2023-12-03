"""Search App Initialization."""

import os
import logging
from flasgger import LazyJSONEncoder, Swagger
from flask import Flask
from opensearchpy import OpenSearch
from config import Config
from prometheus_flask_exporter import PrometheusMetrics

log_level = os.getenv('LOG_LEVEL', 'INFO')

logging.basicConfig(level=logging.getLevelName(log_level))

from logging.config import dictConfig


def create_app(config_class=Config):
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
            'level': log_level,
            'handlers': ['wsgi']
        }
    })

    # create app object
    app = Flask(__name__)

    metrics = PrometheusMetrics(app)
    metrics.info("app_info", "Application info", version="0.0.1")
    app.config["SWAGGER"] = {"openapi": "3.0.1", "title": "Swagger UI", "uiversion": 3}

    swagger_config = {
        "headers": [],
        "specs": [
            {
                "endpoint": "api-search",
                "route": "/api-search.json",
                "rule_filter": lambda rule: rule.endpoint.startswith('actuator'),
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
            "title": "Database Repository Search Service API",
            "description": "Service that searches the search database",
            "version": "1.3.0",
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
            "url": "https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services"
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
    # https://flask-jwt-extended.readthedocs.io/en/stable/options/
    app.config["JWT_ALGORITHM"] = "HS256"
    app.config["JWT_DECODE_ISSUER"] = os.getenv("JWT_ISSUER")
    app.config["JWT_PUBLIC_KEY"] = os.getenv("JWT_PUBKEY")

    app.json_encoder = LazyJSONEncoder

    # load configuration
    app.config.from_object(config_class)
    logging.info('opensearch endpoint 1: %s:%d', app.config["SEARCH_HOST"], app.config["SEARCH_PORT"])

    app.opensearch_client = (
        OpenSearch(hosts=[{"host": app.config["SEARCH_HOST"], "port": app.config["SEARCH_PORT"]}],
                   http_compress=True,
                   http_auth=(app.config["SEARCH_USERNAME"], app.config["SEARCH_PASSWORD"]),
                   )
        if app.config["SEARCH_HOST"]
        else None
    )

    # register blueprints
    from app.api import api_bp

    app.register_blueprint(api_bp)

    return app

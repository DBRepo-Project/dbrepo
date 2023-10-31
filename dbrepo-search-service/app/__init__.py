"""Search App Initialization."""

import os
import logging
from flasgger import LazyJSONEncoder
from flask import Flask
from opensearchpy import OpenSearch
from config import Config
from prometheus_flask_exporter import PrometheusMetrics

logging.basicConfig(level=logging.DEBUG)

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
            'level': 'DEBUG',
            'handlers': ['wsgi']
        }
    })

    # create app object
    app = Flask(__name__)

    metrics = PrometheusMetrics(app)
    metrics.info("app_info", "Application info", version="0.0.1")
    app.config["SWAGGER"] = {"openapi": "3.0.0", "title": "Swagger UI", "uiversion": 3}
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

import json
import os
import logging

from flasgger import LazyJSONEncoder, Swagger
from flask import Flask, request, Response
from flasgger.utils import swag_from
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

metrics = PrometheusMetrics(app)
metrics.info("app_info", "Application info", version="0.0.1")
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
    "info": {
        "title": "Database Repository Data Database sidecar API",
        "description": "Sidecar that downloads the import .csv file",
        "version": "dbrepo-latest",
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
# https://flask-jwt-extended.readthedocs.io/en/stable/options/
app.config["JWT_ALGORITHM"] = "HS256"
app.config["JWT_DECODE_ISSUER"] = os.getenv("JWT_ISSUER")
app.config["JWT_PUBLIC_KEY"] = os.getenv("JWT_PUBKEY")

app.json_encoder = LazyJSONEncoder


@app.route("/health", methods=["GET"], endpoint="actuator_health")
@swag_from("ds-yml/health.yml")
def health():
    return Response(json.dumps({"status": "UP"}), mimetype="application/json"), 200


@app.route("/sidecar/import/<string:filename>", methods=["POST"], endpoint="sidecar_import")
@swag_from("ds-yml/import.yml")
def import_csv(filename):
    logging.debug('endpoint import csv, filename=%s, body=%s', filename, request)
    s3_client = S3Client()
    response = s3_client.download_file(filename)
    if response is False:
        return Response(), 400
    return Response(json.dumps(response)), 202


@app.route("/sidecar/export/<string:filename>", methods=["POST"], endpoint="sidecar_export")
@swag_from("ds-yml/export.yml")
def import_csv(filename):
    logging.debug('endpoint export csv, filename=%s, body=%s', filename, request)
    s3_client = S3Client()
    response = s3_client.upload_file(filename)
    if response is False:
        return Response(), 400
    return Response(), 202

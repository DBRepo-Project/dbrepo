import dataclasses
import json
import logging
from _csv import Error

from json import dumps
from logging.config import dictConfig

from flasgger import LazyJSONEncoder, Swagger
from flasgger.utils import swag_from
from flask import Flask, Response, request
from flask_cors import CORS
from flask_sqlalchemy import SQLAlchemy
from gevent.pywsgi import WSGIServer
from opensearchpy import OpenSearch
from prometheus_flask_exporter import PrometheusMetrics

from botocore.exceptions import ClientError

from determine_dt import determine_datatypes
from determine_pk import determine_pk
from determine_stats import determine_stats

logging.basicConfig(level=logging.DEBUG)

dictConfig(
    {
        "version": 1,
        "formatters": {
            "default": {
                "format": "[%(asctime)s] %(levelname)s in %(module)s: %(message)s",
            }
        },
        "handlers": {
            "wsgi": {
                "class": "logging.StreamHandler",
                "stream": "ext://flask.logging.wsgi_errors_stream",
                "formatter": "default",
            }
        },
        "root": {"level": "INFO", "handlers": ["wsgi"]},
    }
)

app = Flask(__name__)

cors = CORS(app, resources={r"/api/*": {"origins": "*"}})

metrics = PrometheusMetrics(app)
metrics.info("app_info", "Application info", version="1.3.0")
app.config["SWAGGER"] = {"openapi": "3.0.1", "title": "Swagger UI", "uiversion": 3}

# ========================= DB Config  ========================= #
app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False
app.config[
    "SQLALCHEMY_DATABASE_URI"
] = "mysql+pymysql://root:dbrepo@metadata-db:3306/fda"
db = SQLAlchemy(app)

# ========================= OS Config  ========================= #
opensearch_client = OpenSearch(
    hosts=["search-db"],
    port=9200,
    http_auth=("admin", "admin"),
    use_ssl=False,
)

swagger_config = {
    "headers": [],
    "specs": [
        {
            "endpoint": "api-analyse",
            "route": "/api-analyse.json",
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
        "title": "Database Repository Analyse Service API",
        "description": "Service that analyses data structures",
        "version": "__APPVERSION__",
        "contact": {
            "name": "Prof. Andreas Rauber",
            "email": "andreas.rauber@tuwien.ac.at",
        },
        "license": {
            "name": "Apache 2.0",
            "url": "https://www.apache.org/licenses/LICENSE-2.0",
        },
    },
    "externalDocs": {
        "description": "Sourcecode Documentation",
        "url": "https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services",
    },
    "servers": [
        {"url": "http://localhost:5000", "description": "Generated server url"},
        {"url": "https://test.dbrepo.tuwien.ac.at", "description": "Sandbox"},
    ],
}

app.json_encoder = LazyJSONEncoder
swagger = Swagger(app, config=swagger_config, template=template)


@app.route("/health", methods=["GET"], endpoint="analyze_health")
@swag_from("as-yml/health.yml")
def health():
    logging.debug("endpoint health, body=%s", request)
    res = dumps({"status": "UP", "message": "Application is up and running"})
    return Response(res, mimetype="application/json"), 200


@app.route("/api/analyse/datatypes", methods=["GET"], endpoint="analyze_analyse_datatypes")
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
        res = dumps({"success": False, "message": str(e)})
        return Response(res, mimetype="application/json"), 400
    except ClientError as e:
        logging.error(f"Failed to determine separator: {e}")
        res = dumps({"success": False, "message": str(e)})
        return Response(res, mimetype="application/json"), 404
    except Exception as e:
        logging.error(f"Failed to determine data types: {e}")
        res = dumps({"success": False, "message": str(e)})
        return Response(res, mimetype="application/json"), 500


@app.route("/api/analyse/keys", methods=["GET"], endpoint="analyze_analyse_keys")
@swag_from("as-yml/analyse_keys.yml")
def analyse_keys():
    filename: str = request.args.get("filename")
    separator: str = request.args.get('separator')

    if filename is None or separator is None:
        return Response(
            json.dumps({'success': False, 'message': "Missing required query parameters 'filename' and 'separator'"}),
            400)

    try:
        res = {
            'keys': determine_pk(filename, separator)
        }
        logging.info(f"Determined list of primary keys: {res}")
        return Response(dumps(res), mimetype="application/json"), 202
    except OSError as e:
        logging.error(f"Failed to determine primary key: {e}")
        res = dumps({"success": False, "message": str(e)})
        return Response(res, mimetype="application/json"), 404
    except Exception as e:
        logging.error(f"Failed to determine primary key: {e}")
        res = dumps({"success": False, "message": str(e)})
        return Response(res, mimetype="application/json"), 500


@app.route("/api/analyse/database/<database_id>/table/<table_id>/statistics", methods=["GET"],
           endpoint="analyse_analyse_table_stat")
@swag_from("as-yml/analyse_table_stat.yml")
def analyse_table_stat(database_id: int = None, table_id: int = None):
    if database_id is None:
        return Response(dumps({"message": "Missing path variable 'database_id'", "status": 400}),
                        mimetype="application/json"), 400
    if table_id is None:
        return Response(dumps({"message": "Missing path variable 'table_id'", "status": 400}),
                        mimetype="application/json"), 400

    try:
        res = determine_stats(db, opensearch_client, database_id=database_id, table_id=table_id)
        logging.info(f"Analysed table statistics: {res}")
        return Response(json.dumps(dataclasses.asdict(res)), mimetype="application/json"), 202
    except OSError:
        return Response(dumps({"message": "Database or table does not exist.", "status": 404}),
                        mimetype="application/json"), 404


rest_server_port = 5000

if __name__ == "__main__":
    http_server = WSGIServer(("", 5000), app)
    http_server.serve_forever()

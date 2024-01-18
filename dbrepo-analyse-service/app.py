import os
from _csv import Error

from flask import Flask, request, Response
from json import dumps
from determine_dt import determine_datatypes
from determine_pk import determine_pk
import logging
from flasgger import Swagger
from flasgger.utils import swag_from
from flasgger import LazyJSONEncoder
from gevent.pywsgi import WSGIServer
from prometheus_flask_exporter import PrometheusMetrics

log_level = os.getenv('LOG_LEVEL', 'INFO')

logging.basicConfig(level=logging.getLevelName(log_level))

from logging.config import dictConfig

dictConfig({
    'version': 1,
    'formatters': {'default': {
        'format': '[%(asctime)s] %(levelname)s in %(module)s: %(message)s',
    }},
    'handlers': {'wsgi': {
        'class': 'logging.StreamHandler',
        'stream': 'ext://flask.logging.wsgi_errors_stream',
        'formatter': 'default'
    }},
    'root': {
        'level': log_level,
        'handlers': ['wsgi']
    }
})

app = Flask(__name__)
metrics = PrometheusMetrics(app)
metrics.info('app_info', 'Application info', version='1.3.0')
app.config["SWAGGER"] = {"openapi": "3.0.1", "title": "Swagger UI", "uiversion": 3}

swagger_config = {
    "headers": [],
    "specs": [
        {
            "endpoint": "api-analyse",
            "route": "/api-analyse.json",
            "rule_filter": lambda rule: rule.endpoint.startswith('analyze'),
            "model_filter": lambda tag: True,  # all in
        },
        {
            "endpoint": "api-mdb",
            "route": "/api-mdb.json",
            "rule_filter": lambda rule: rule.endpoint.startswith('mdb'),
            "model_filter": lambda tag: True,  # all in
        },
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
        "version": "$TAG",
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
            "url": "https://dbrepo2.tuwien.ac.at",
            "description": "Sandbox"
        }
    ]
}

app.json_encoder = LazyJSONEncoder
swagger = Swagger(app, config=swagger_config, template=template)

@app.route('/health', methods=["GET"], endpoint='analyze_health')
@swag_from('as-yml/health.yml')
def health():
    logging.debug('endpoint health, body=%s', request)
    res = dumps({"status": "UP", "message": "Application is up and running"})
    return Response(res, mimetype="application/json"), 200


@app.route('/api/analyse/determinedt', methods=["POST"], endpoint='analyze_determinedt')
@swag_from('as-yml/determinedt.yml')
def determinedt():
    logging.debug('endpoint determine datatype, body=%s', request)
    input_json = request.get_json()
    try:
        filename = str(input_json['filename'])
        enum = False
        if 'enum' in input_json:
            enum = bool(input_json['enum'])
            logging.info("Enum is present in payload and set to %s", enum)
        enum_tol = 0.001
        if 'enum_tol' in input_json:
            enum_tol = float(input_json['enum_tol'])
            logging.info("Enum toleration is present in payload and set to %s", enum_tol)
        separator = None
        if 'separator' in input_json:
            separator = str(input_json['separator'])
            logging.info("Seperator is present in payload and set to %s", separator)
        res = determine_datatypes(filename, enum, enum_tol, separator)
        logging.debug('determine datatype resulted in datatypes %s', res)
        return Response(res, mimetype="application/json"), 200
    except OSError as e:
        logging.error('Failed to determine data types: %s', e)
        res = dumps({"success": False, "message": str(e)})
        return Response(res, mimetype="application/json"), 409
    except Error as e:
        logging.error('Failed to determine separator %s', e)
        res = dumps({"success": False, "message": str(e)})
        return Response(res, mimetype="application/json"), 422
    except Exception as e:
        logging.error('Failed to determine data types: %s', e)
        res = dumps({"success": False, "message": str(e)})
        return Response(res, mimetype="application/json"), 500


@app.route('/api/analyse/determinepk', methods=["POST"], endpoint='analyze_determinepk')
@swag_from('as-yml/determinepk.yml')
def determinepk():
    logging.debug('endpoint determine primary key, body=%s', request)
    input_json = request.get_json()
    try:
        filepath = str(input_json['filepath'])
        seperator = ','
        if 'seperator' in input_json:
            seperator = str(input_json['seperator'])
        res = determine_pk(filepath, seperator)
        logging.debug('determined list of primary keys: %s', res)
        return Response(res, mimetype="application/json"), 200
    except Exception as e:
        logging.error('Failed to determine primary key: %s', e)
        res = dumps({"success": False, "message": str(e)})
        return Response(res, mimetype="application/json"), 500


rest_server_port = 5000

if __name__ == '__main__':
    http_server = WSGIServer(('', 5000), app)
    http_server.serve_forever()

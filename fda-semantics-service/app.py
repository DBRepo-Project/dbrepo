import os
from flask import Flask, request, jsonify
import logging
from logging.config import dictConfig
import py_eureka_client.eureka_client as eureka_client
from flasgger import Swagger
from flasgger.utils import swag_from
from flasgger import LazyJSONEncoder
from list import list_units, get_uri as list_get_uri
from validate import validator, stringmapper
from gevent.pywsgi import WSGIServer
from save import insert_mdb_concepts, insert_mdb_columns_concepts, delete_mdb_columns_concepts
from werkzeug.utils import secure_filename
from pathlib import Path
from onto_feat import search_ontologies, setup_ontology_dir, list_ontologies, ontology_exists, get_ontology, \
    allowed_file
from prometheus_flask_exporter import PrometheusMetrics

dictConfig({
    'version': 1,
    'formatters': {'default': {
        'format': '%(asctime)s %(levelname)-6s %(message)s',
    }},
    'handlers': {'wsgi': {
        'class': 'logging.StreamHandler',
        'stream': 'ext://flask.logging.wsgi_errors_stream',
        'formatter': 'default'
    }},
    'root': {
        'level': 'INFO',
        'handlers': ['wsgi']
    }
})

app = Flask(__name__)
metrics = PrometheusMetrics(app)
metrics.info('app_info', 'Application info', version='1.0.3')
app.config["SWAGGER"] = {"openapi": "3.0.1", "title": "Swagger UI", "uiversion": 3}

swagger_config = {
    "headers": [],
    "specs": [
        {
            "endpoint": "api",
            "route": "/api.json",
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
        "title": "Database Repository Unit / Ontology Service API",
        "description": "Service for assigning concepts to database tables and columns.",
        "version": "1.1.0-alpha",
        "contact": {
            "name": "Prof. Andreas Rauber",
            "email": "andreas.rauber@tuwien.ac.at"
        },
        "license": {
            "name": "Apache 2.0",
            "url": "https://www.apache.org/licenses/LICENSE-2.0"
        }
    },
    "servers": [
        {
            "url": "http://localhost:5010",
            "description": "Generated server url"
        },
        {
            "url": "https://dbrepo1.ec.tuwien.ac.at/api/units",
            "description": "DBRepo Production Server"
        }
    ]
}

app.json_encoder = LazyJSONEncoder
swagger = Swagger(app, config=swagger_config, template=template)


@app.route('/api/semantics/suggest', methods=["POST"], endpoint='units_suggest')
@swag_from('suggest.yml')
def suggest():
    logging.debug('endpoint suggest unit, body=%s', request)
    input_json = request.get_json()
    try:
        unit = str(input_json['ustring'])
        offset = int(input_json['offset'])
        res = list_units(stringmapper(unit), offset)
        logging.info('suggest unit result in units: %s', res)
        return jsonify(res), 200
    except Exception as e:
        logging.error('Failed to suggest units: %s', e)
        res = {"success": False, "message": str(e)}
        return jsonify(res), 500


@app.route('/api/semantics/validate/<unit>', methods=["GET"], endpoint='units_validate')
@swag_from('validate.yml')
def validate(unit):
    logging.debug('endpoint validate unit, unit=%s, body=%s', unit, request)
    try:
        res = validator(unit)
        logging.info('validate unit resulted in unit: %s', res)
        return str(res), 200
    except Exception as e:
        logging.error(e)
        res = {"success": False, "message": str(e)}
        return jsonify(res), 500


@app.route('/api/semantics/uri/<name>', methods=["GET"], endpoint='units_uri')
@swag_from('geturi.yml')
def get_uri(name):
    logging.debug('endpoint get uri, name=%s, body=%s', name, request)
    try:
        res = list_get_uri(name)
        logging.info('get uri resulted in uri: %s', res)
        return jsonify(res), 200
    except Exception as e:
        logging.error('Failed to get uri: %s', e)
        res = {"success": False, "message": str(e)}
        return jsonify(res), 500


@app.route('/api/semantics/saveconcept', methods=["POST"], endpoint='units_saveconcept')
@swag_from('saveconcept.yml')
def save_concept():
    logging.debug('endpoint save concept, body=%s', request)
    input_json = request.get_json()
    try:
        uri = str(input_json['uri'])
        c_name = str(input_json['name'])
        if insert_mdb_concepts(uri, c_name) > 0:
            return jsonify({'uri': uri}), 201
        else:
            return jsonify({'status': 'error'}), 409
    except Exception as e:
        logging.error('Failed to save concept: %s', e)
        res = {"success": False, "message": str(e)}
        return jsonify(res), 500


@app.route('/api/semantics/savecolumnsconcept', methods=["POST"], endpoint='units_savecolumnsconcept')
@swag_from('savecolumnsconcept.yml')
def save_column_concept():
    logging.debug('endpoint save column concept, body=%s', request)
    input_json = request.get_json()
    try:
        uri = input_json['uri']
        cid = int(input_json['cid'])
        tid = int(input_json['tid'])
        cdbid = int(input_json['cdbid'])
        if insert_mdb_columns_concepts(cdbid, tid, cid, uri) > 0:
            return jsonify(), 201
        else:
            return jsonify({'status': 'error'}), 409
    except Exception as e:
        logging.error('Failed to save column concept: %s', e)
        res = {"success": False, "message": str(e)}
        return jsonify(res), 500


@app.route('/api/semantics/deletecolumnsconcept', methods=["POST"], endpoint='units_deletecolumnsconcept')
@swag_from('deletecolumnsconcept.yml')
def save_column_concept():
    logging.debug('endpoint delete column concept, body=%s', request)
    input_json = request.get_json()
    try:
        cid = int(input_json['cid'])
        tid = int(input_json['tid'])
        cdbid = int(input_json['cdbid'])
        if delete_mdb_columns_concepts(cdbid, tid, cid) > 0:
            return jsonify(), 202
        else:
            return jsonify({'status': 'error'}), 409
    except Exception as e:
        logging.error('Failed to delete column concept: %s', e)
        res = {"success": False, "message": str(e)}
        return jsonify(res), 500


@app.route('/api/ontologies/getconcept/<cname>', methods=["GET"], endpoint='ontologies_get_concept')
@swag_from('getconcept.yml')
def get_concept(cname):
    logging.debug('endpoint get concept, cname=%s, body=%s', cname, request)
    try:
        res = search_ontologies(cname)
        logging.info('get concept resulted in concept: %s', res)
        return jsonify(res), 200
    except Exception as e:
        logging.error('Failed to get concept: %s', e)
        res = {"success": False, "message": str(e)}
        return jsonify(res), 500


ONTOLOGIES_DIRECTORY = 'ontologies'


@app.route('/api/ontologies/upload', methods=["POST"], endpoint='ontologies_upload_onto')
@swag_from('ontologies.yml')
def post_ontologies():
    if 'file' not in request.files:
        return "no file", 500
    file = request.files['file']
    if file.filename == '':
        return "no file selected", 400
    if file and allowed_file(file.filename):
        filename = secure_filename(file.filename)
        if ontology_exists(Path(filename).stem):
            return "ontology name already exists", 409
        setup_ontology_dir()
        file.save(os.path.join(ONTOLOGIES_DIRECTORY, filename))
        logging.info('created ontology: %s', filename)
        return "created", 201


@app.route('/api/ontologies/listontologies', methods=["GET"], endpoint='ontologies_get_ontos')
@swag_from('ontology.yml')
def get_ontologies():
    print(list_ontologies())
    return jsonify(list_ontologies())


@app.route('/api/ontologies/<o_name>', methods=["GET"], endpoint='ontologies_get_onto')
@swag_from('ontologybyname.yml')
def get_ontologies(o_name):
    ontology = get_ontology(o_name)
    if ontology is None:
        return "ontology does not exist", 404
    return ontology


rest_server_port = int(os.getenv('PORT_APP', 5010))
rest_server_host = os.getenv('FLASK_RUN_HOST', '0.0.0.0')
eureka_client.init(eureka_server=os.getenv('EUREKA_SERVER', 'http://localhost:9090/eureka/'),
                   app_name=os.getenv('HOSTNAME', 'fda-units-service'),
                   instance_ip=os.getenv('HOSTNAME', 'fda-units-service'),
                   instance_host=os.getenv('HOSTNAME', 'fda-units-service'),
                   instance_port=rest_server_port)

if __name__ == '__main__':
    http_server = WSGIServer((rest_server_host, rest_server_port), app)
    http_server.serve_forever()

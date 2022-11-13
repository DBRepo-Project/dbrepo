import os
import rdflib
from flask import Flask, request, jsonify
import logging
import py_eureka_client.eureka_client as eureka_client
from flasgger import Swagger
from flasgger.utils import swag_from
from flasgger import LazyString, LazyJSONEncoder
from list import list_units, get_uri as list_get_uri
from validate import validator, stringmapper
from gevent.pywsgi import WSGIServer
from save import insert_mdb_concepts, insert_mdb_columns_concepts
from werkzeug.utils import secure_filename
from pathlib import Path
from onto_feat import search_ontologies, setup_ontology_dir, list_ontologies, ontology_exists, get_ontology, allowed_file

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
        'level': 'INFO',
        'handlers': ['wsgi']
    }
})

logging.basicConfig(level=logging.DEBUG)

app = Flask(__name__)
app.config["SWAGGER"] = {"title": "FDA-Units-Service", "uiversion": 3}

swagger_config = {
    "headers": [],
    "specs": [
        {
            "title": "units",
            "endpoint": "api-units",
            "route": "/api-units.json"
        }
    ],
    "static_url_path": "/flasgger_static",
    "swagger_ui": True,
    "specs_route": "/swagger-ui/",
}

template = dict(
    swaggerUiPrefix=LazyString(lambda: request.environ.get("HTTP_X_SCRIPT_NAME", ""))
)

app.json_encoder = LazyJSONEncoder
swagger = Swagger(app, config=swagger_config, template=template)


@app.route('/api/units/suggest', methods=["POST"], endpoint='suggest')
@swag_from('suggest.yml')
def suggest():
    logging.debug('endpoint suggest unit, body=%s', request)
    input_json = request.get_json()
    try:
        unit = str(input_json['ustring'])
        offset = int(input_json['offset'])
        res = list_units(stringmapper(unit), offset)
        logging.debug('suggest unit result in units: %s', res)
        return jsonify(res), 200
    except Exception as e:
        logging.error('Failed to suggest units: %s', e)
        res = {"success": False, "message": str(e)}
        return jsonify(res), 500


@app.route('/api/units/validate/<unit>', methods=["GET"], endpoint='validate')
@swag_from('validate.yml')
def validate(unit):
    logging.debug('endpoint validate unit, unit=%s, body=%s', unit, request)
    try:
        res = validator(unit)
        logging.debug('validate unit resulted in unit: %s', res)
        return str(res), 200
    except Exception as e:
        logging.error(e)
        res = {"success": False, "message": str(e)}
        return jsonify(res)


@app.route('/api/units/uri/<uname>', methods=["GET"], endpoint='uri')
@swag_from('geturi.yml')
def get_uri(uname):
    logging.debug('endpoint get uri, uname=%s, body=%s', uname, request)
    try:
        res = list_get_uri(uname)
        logging.debug('get uri resulted in uri: %s', res)
        return jsonify(res), 200
    except Exception as e:
        logging.error('Failed to get uri: %s', e)
        res = {"success": False, "message": str(e)}
        return jsonify(res), 500


@app.route('/api/units/saveconcept', methods=["POST"], endpoint='saveconcept')
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


@app.route('/api/units/savecolumnsconcept', methods=["POST"], endpoint='savecolumnsconcept')
@swag_from('savecolumnsconcept.yml')
def save_column_concept():
    logging.debug('endpoint save column concept, body=%s', request)
    input_json = request.get_json()
    try:
        uri = str(input_json['uri'])
        cid = int(input_json['cid'])
        tid = int(input_json['tid'])
        cdbid = int(input_json['cdbid'])
        if insert_mdb_columns_concepts(cdbid, tid, cid, uri) > 0:
            return jsonify({'uri': uri}), 201
        else:
            return jsonify({'status': 'error'}), 409
    except Exception as e:
        logging.error('Failed to save column concept: %s', e)
        res = {"success": False, "message": str(e)}
        return jsonify(res), 500

@app.route('/api/units/getconcept/<cname>', methods=["GET"], endpoint='get_concept')
@swag_from('getconcept.yml')
def get_concept(cname):
    logging.debug('endpoint get concept, cname=%s, body=%s', cname, request)
    try:
        res = search_ontologies(cname)
        logging.debug('get concept resulted in concept: %s', res)
        return jsonify(res), 200
    except Exception as e:
        logging.error('Failed to get concept: %s', e)
        res = {"success": False, "message": str(e)}
        return jsonify(res), 500

ONTOLOGIES_DIRECTORY = 'ontologies'

@app.route('/api/ontologies', methods=["POST"], endpoint='upload_onto')
@swag_from('ontologie.yml')
def post_ontologies():
    if 'file' not in request.files:
        return "no file", 500
    file = request.files['file']
    if file.filename == '':
        return "no file selected", 500
    if file and allowed_file(file.filename):
        filename = secure_filename(file.filename)
        if ontology_exists(Path(filename).stem):
            return "ontology name already exists", 500
        setup_ontology_dir()
        file.save(os.path.join(ONTOLOGIES_DIRECTORY, filename))
        logging.debug('created ontology: %s', filename)
        return "created", 200

@app.route('/api/ontologies', methods=["GET"], endpoint='get_ontos')
@swag_from('ontologies.yml')
def get_ontologies():
    print(list_ontologies())
    return jsonify(list_ontologies())

@app.route('/api/ontologies/<name>', methods=["GET"], endpoint='get_onto')
@swag_from('ontologie.yml')
def get_ontologies(name):
    ontology = get_ontology(name)
    if ontology is None:
        return "ontology does not exist", 404
    return ontology

rest_server_port = int(os.getenv("PORT_APP"))
eureka_client.init(eureka_server=os.getenv('EUREKA_SERVER', 'http://localhost:9090/eureka/'),
                   app_name=os.getenv('HOSTNAME', 'fda-units-service'),
                   instance_ip=os.getenv('HOSTNAME', 'fda-units-service'),
                   instance_host=os.getenv('HOSTNAME', 'fda-units-service'),
                   instance_port=rest_server_port)

if __name__ == '__main__':
    http_server = WSGIServer(('', rest_server_port), app)
    http_server.serve_forever()
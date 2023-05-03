import os
import logging
import re
from flask import Flask, request, jsonify
from logging.config import dictConfig
import py_eureka_client.eureka_client as eureka_client
from flasgger import Swagger
from flasgger.utils import swag_from
from flasgger import LazyJSONEncoder
from list import List
from validate import validator
from gevent.pywsgi import WSGIServer
from save import insert_mdb_concepts, insert_mdb_units
from onto_feat import list_ontologies, get_ontology
from prometheus_flask_exporter import PrometheusMetrics
from flask_jwt_extended import jwt_required, JWTManager

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
metrics.info('app_info', 'Application info', version='1.2.0')
app.config['SWAGGER'] = {'openapi': '3.0.0', 'title': 'Swagger UI', 'uiversion': 3}
# https://flask-jwt-extended.readthedocs.io/en/stable/options/
app.config['JWT_ALGORITHM'] = 'HS256'
app.config['JWT_DECODE_ISSUER'] = os.getenv('JWT_ISSUER')
app.config['JWT_PUBLIC_KEY'] = os.getenv('JWT_PUBKEY')
jwt = JWTManager(app)
list = List(offline=False)

swagger_config = {
    'headers': [],
    'specs': [
        {
            'endpoint': 'api',
            'route': '/api-semantics.json',
            'rule_filter': lambda rule: True,
            'model_filter': lambda tag: True,  # all in
        }
    ],
    'static_url_path': '/flasgger_static',
    'swagger_ui': True,
    'specs_route': '/swagger-ui/',
}

template = {
    'openapi': '3.0.0',
    'info': {
        'title': 'Database Repository Unit / Ontology Service API',
        'description': 'Service for assigning concepts to database tables and columns.',
        'version': '1.2.0',
        'contact': {
            'name': 'Prof. Andreas Rauber',
            'email': 'andreas.rauber@tuwien.ac.at'
        },
        'license': {
            'name': 'Apache 2.0',
            'url': 'https://www.apache.org/licenses/LICENSE-2.0'
        }
    },
    "externalDocs": {
        "description": "Sourcecode Documentation",
        "url": "https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services"
    },
    'servers': [
        {
            'url': 'http://localhost:5010',
            'description': 'Generated server url'
        },
        {
            'url': 'https://dbrepo2.ec.tuwien.ac.at',
            'description': 'Sandbox'
        }
    ]
}

app.json_encoder = LazyJSONEncoder
swagger = Swagger(app, config=swagger_config, template=template)


@app.route('/api/semantics/concept', methods=['GET'], endpoint='concepts_suggest')
@swag_from('us-yml/get_concepts.yml')
def suggest():
    query = request.args.get('q')
    logging.debug('endpoint suggest concept, body=%s', request)
    try:
        res = list.list_concepts(query)
        logging.info('suggest concept result in units: %s', res)
        return jsonify(res), 200
    except Exception as e:
        logging.error('Failed to suggest concept: %s', e)
        res = {'success': False, 'message': str(e), 'status': 500}
        return jsonify(res), 500


@app.route('/api/semantics/unit', methods=['GET'], endpoint='units_suggest')
@swag_from('us-yml/get_units.yml')
def suggest():
    query = request.args.get('q')
    logging.debug('endpoint suggest unit, body=%s', request)
    try:
        res = list.list_units(query)
        logging.info('suggest unit result in units: %s', res)
        return jsonify(res), 200
    except Exception as e:
        logging.error('Failed to suggest units: %s', e)
        res = {'success': False, 'message': str(e), 'status': 500}
        return jsonify(res), 500


@app.route('/api/semantics/unit/<unit>/validate', methods=['GET'], endpoint='units_validate')
@swag_from('us-yml/get_unit_validate.yml')
def validate(unit):
    logging.debug('endpoint validate unit, unit=%s, body=%s', unit, request)
    try:
        res = validator(unit)
        logging.info('validate unit resulted in unit: %s', res)
        return str(res), 200
    except Exception as e:
        logging.error(e)
        res = {'success': False, 'message': str(e), 'status': 500}
        return jsonify(res), 500


@app.route('/api/semantics/concept/<concept>/validate', methods=['GET'], endpoint='concepts_validate')
@swag_from('us-yml/get_concept_validate.yml')
def validate(concept):
    logging.debug('endpoint validate concept, concept=%s, body=%s', concept, request)
    try:
        res = validator(concept)
        logging.info('validate concept resulted in unit: %s', res)
        return str(res), 200
    except Exception as e:
        logging.error(e)
        res = {'success': False, 'message': str(e), 'status': 500}
        return jsonify(res), 500


@app.route('/api/semantics/concept', methods=['PUT'], endpoint='concepts_label')
@swag_from('us-yml/put_concept.yml')
def get_concept_label():
    input_json = request.get_json()
    logging.debug('endpoint get label for concept, body=%s', input_json)
    try:
        uri = input_json['uri']
        m = re.search('https?://www.wikidata.org/entity/(Q[0-9]+)', uri)
        if not m:
            logging.error('Failed to get concept label: %s is not a wikidata uri', uri)
            res = {'success': False, 'message': 'Failed to get concept label: is not a wikidata uri', 'status': 400}
            return jsonify(res), 400
        entity = m.group(1)
        res = list.get_concept_label(entity)
        logging.info('suggest concept label result: %s', res)
        return jsonify(res), 200
    except Exception as e:
        logging.error('Failed to suggest concept: %s', e)
        res = {'success': False, 'message': str(e), 'status': 500}
        return jsonify(res), 500


@app.route('/api/semantics/unit', methods=['PUT'], endpoint='units_label')
@swag_from('us-yml/put_units.yml')
def get_concept_label():
    input_json = request.get_json()
    logging.debug('endpoint get label for unit, body=%s', input_json)
    try:
        uri = input_json['uri']
        m = re.search('https?://www.ontology-of-units-of-measure.org/resource/om-2/([a-zA-Z0-9-]+)', uri)
        if not m:
            logging.error('Failed to get unit label: %s is not a wikidata uri', uri)
            res = {'success': False, 'message': 'Failed to get unit label: is not an om2 uri', 'status': 400}
            return jsonify(res), 400
        res = list.get_unit_label(uri)
        if res is None:
            logging.error('Unit label not found')
            res = {'success': False, 'message': 'Unit label not found', 'status': 404}
            return jsonify(res), 404
        logging.info('suggest unit label result: %s', res)
        return jsonify(res), 200
    except Exception as e:
        logging.error('Failed to suggest unit: %s', e)
        res = {'success': False, 'message': str(e), 'status': 500}
        return jsonify(res), 500


@app.route('/api/semantics/concept', methods=['POST'], endpoint='concepts_save')
@swag_from('us-yml/post_concept.yml')
@jwt_required()
def save_concept():
    input_json = request.get_json()
    logging.debug('endpoint save concept, body=%s', input_json)
    try:
        uri = input_json['uri']
        name = input_json['name']
        if uri is None:
            return jsonify({'status': 'error', 'message': 'uri is null'}), 400
        if name is None:
            return jsonify({'status': 'error', 'message': 'name is null'}), 400
        if insert_mdb_concepts(uri, name) > 0:
            return jsonify({'uri': uri}), 201
        else:
            return jsonify({'status': 409}), 409
    except Exception as e:
        logging.error('Failed to save concept: %s', e)
        res = {'success': False, 'message': str(e), 'status': 500}
        return jsonify(res), 500


@app.route('/api/semantics/unit', methods=['POST'], endpoint='units_save')
@swag_from('us-yml/post_unit.yml')
@jwt_required()
def save_unit():
    input_json = request.get_json()
    logging.debug('endpoint save unit, body=%s', input_json)
    try:
        uri = input_json['uri']
        name = input_json['name']
        if uri is None:
            return jsonify({'status': 'error', 'message': 'uri is null'}), 400
        if name is None:
            return jsonify({'status': 'error', 'message': 'name is null'}), 400
        if insert_mdb_units(uri, name) > 0:
            return jsonify({'uri': uri}), 201
        else:
            return jsonify({'status': 'error'}), 409
    except Exception as e:
        logging.error('Failed to save unit: %s', e)
        res = {'success': False, 'message': str(e)}
        return jsonify(res), 500


@app.route('/api/semantics/ontology', methods=['GET'], endpoint='ontologies_get')
@swag_from('us-yml/get_ontologies.yml')
def get_ontologies():
    ontologies = list_ontologies()
    logging.info('Get ontologies resulted in list %d', len(ontologies))
    return jsonify(ontologies), 200


@app.route('/api/semantics/ontology/<name>', methods=['GET'], endpoint='ontologies_get_ontology')
@swag_from('us-yml/get_ontology.yml')
def get_ontologies(name):
    ontology = get_ontology(name)
    if ontology is None:
        return 'ontology does not exist', 404
    logging.info('Get ontology resulted in file %s', ontology)
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

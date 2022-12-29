import os
from flask import Flask, request, jsonify
import logging
from logging.config import dictConfig
import py_eureka_client.eureka_client as eureka_client
from flasgger import Swagger
from flasgger.utils import swag_from
from flasgger import LazyJSONEncoder
from list import list_units, get_uri as list_get_uri, list_concepts
from validate import validator
from gevent.pywsgi import WSGIServer
from save import insert_mdb_concepts, insert_mdb_units
from onto_feat import search_ontologies, list_ontologies, get_ontology
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
app.config['SWAGGER'] = {'openapi': '3.0.1', 'title': 'Swagger UI', 'uiversion': 3}

swagger_config = {
    'headers': [],
    'specs': [
        {
            'endpoint': 'api',
            'route': '/api.json',
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
        'version': '1.1.0-alpha',
        'contact': {
            'name': 'Prof. Andreas Rauber',
            'email': 'andreas.rauber@tuwien.ac.at'
        },
        'license': {
            'name': 'Apache 2.0',
            'url': 'https://www.apache.org/licenses/LICENSE-2.0'
        }
    },
    'servers': [
        {
            'url': 'http://localhost:5010',
            'description': 'Generated server url'
        },
        {
            'url': 'https://dbrepo1.ec.tuwien.ac.at/api/units',
            'description': 'DBRepo Production Server'
        }
    ]
}

app.json_encoder = LazyJSONEncoder
swagger = Swagger(app, config=swagger_config, template=template)


@app.route('/api/semantics/concept', methods=['GET'], endpoint='concepts_suggest')
@swag_from('get_concept.yml')
def suggest():
    query = request.args.get('q')
    logging.debug('endpoint suggest concept, body=%s', request)
    try:
        res = list_concepts(query)
        logging.info('suggest concept result in units: %s', res)
        return jsonify(res), 200
    except Exception as e:
        logging.error('Failed to suggest concept: %s', e)
        res = {'success': False, 'message': str(e)}
        return jsonify(res), 500


@app.route('/api/semantics/unit', methods=['GET'], endpoint='units_suggest')
@swag_from('get_unit.yml')
def suggest():
    query = request.args.get('q')
    logging.debug('endpoint suggest unit, body=%s', request)
    try:
        res = list_units(query)
        logging.info('suggest unit result in units: %s', res)
        return jsonify(res), 200
    except Exception as e:
        logging.error('Failed to suggest units: %s', e)
        res = {'success': False, 'message': str(e)}
        return jsonify(res), 500


@app.route('/api/semantics/unit/<unit>/validate', methods=['GET'], endpoint='units_validate')
@swag_from('get_unit_validate.yml')
def validate(unit):
    logging.debug('endpoint validate unit, unit=%s, body=%s', unit, request)
    try:
        res = validator(unit)
        logging.info('validate unit resulted in unit: %s', res)
        return str(res), 200
    except Exception as e:
        logging.error(e)
        res = {'success': False, 'message': str(e)}
        return jsonify(res), 500


@app.route('/api/semantics/concept/<concept>/validate', methods=['GET'], endpoint='concepts_validate')
@swag_from('get_concept_validate.yml')
def validate(concept):
    logging.debug('endpoint validate concept, concept=%s, body=%s', concept, request)
    try:
        res = validator(concept)
        logging.info('validate concept resulted in unit: %s', res)
        return str(res), 200
    except Exception as e:
        logging.error(e)
        res = {'success': False, 'message': str(e)}
        return jsonify(res), 500


@app.route('/api/semantics/unit/<name>', methods=['GET'], endpoint='units_uri')
@swag_from('get_unit_uri.yml')
def get_uri(name):
    logging.debug('endpoint get uri, name=%s, body=%s', name, request)
    try:
        res = list_get_uri(name)
        logging.info('get uri resulted in uri: %s', res)
        return jsonify(res), 200
    except Exception as e:
        logging.error('Failed to get uri: %s', e)
        res = {'success': False, 'message': str(e)}
        return jsonify(res), 500


@app.route('/api/semantics/concept', methods=['POST'], endpoint='concepts_save')
@swag_from('post_concept.yml')
def save_concept():
    input_json = request.get_json()
    logging.debug('endpoint save concept, body=%s', input_json)
    try:
        uri = str(input_json['uri'])
        c_name = str(input_json['name'])
        if insert_mdb_concepts(uri, c_name) > 0:
            return jsonify({'uri': uri}), 201
        else:
            return jsonify({'status': 'error'}), 409
    except Exception as e:
        logging.error('Failed to save concept: %s', e)
        res = {'success': False, 'message': str(e)}
        return jsonify(res), 500


@app.route('/api/semantics/unit', methods=['POST'], endpoint='units_save')
@swag_from('post_unit.yml')
def save_concept():
    input_json = request.get_json()
    logging.debug('endpoint save unit, body=%s', input_json)
    try:
        uri = str(input_json['uri'])
        c_name = str(input_json['name'])
        if insert_mdb_units(uri, c_name) > 0:
            return jsonify({'uri': uri}), 201
        else:
            return jsonify({'status': 'error'}), 409
    except Exception as e:
        logging.error('Failed to save unit: %s', e)
        res = {'success': False, 'message': str(e)}
        return jsonify(res), 500


@app.route('/api/semantics/concept/<name>', methods=['GET'], endpoint='ontologies_get_concept')
@swag_from('get_concept_uri.yml')
def get_concept(name):
    logging.debug('endpoint get concept, cname=%s, body=%s', name, request)
    try:
        res = search_ontologies(name)
        logging.info('get concept resulted in concept: %s', res)
        return jsonify(res), 200
    except Exception as e:
        logging.error('Failed to get concept: %s', e)
        res = {'success': False, 'message': str(e)}
        return jsonify(res), 500


@app.route('/api/semantics/ontology', methods=['GET'], endpoint='ontologies_get')
@swag_from('get_ontologies.yml')
def get_ontologies():
    ontologies = list_ontologies()
    logging.info('Get ontologies resulted in list %d', len(ontologies))
    return jsonify(ontologies), 200


@app.route('/api/semantics/ontology/<name>', methods=['GET'], endpoint='ontologies_get_ontology')
@swag_from('get_ontology.yml')
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

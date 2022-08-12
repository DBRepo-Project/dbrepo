import os
from flask import Flask, flash, request, redirect, url_for, Response, abort, jsonify
from determine_dt import determine_datatypes
from insert_mdb_col import update_mdb_col
from determine_pk import determine_pk 
#from werkzeug.utils import secure_filename
#from werkzeug import cached_property
import logging
import sys
import py_eureka_client.eureka_client as eureka_client
from flasgger import Swagger
from flasgger.utils import swag_from
from flasgger import LazyString, LazyJSONEncoder
from gevent.pywsgi import WSGIServer

logging.basicConfig(level=logging.DEBUG)
#UPLOAD_FOLDER = '.'
#ALLOWED_EXTENSIONS = {'csv'}

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

app = Flask(__name__)
app.config["SWAGGER"] = {"openapi": "3.0.1", "title": "Swagger UI", "uiversion": 3}

swagger_config = {
    "headers": [],
    "specs": [
        {
            "endpoint": "api-analyze",
            "route": "/api-analyze.json",
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
    "servers": [{
       "url": "http://localhost:5000",
       "description": "Generated server url"
    }]
}

app.json_encoder = LazyJSONEncoder
swagger = Swagger(app, config=swagger_config, template=template)

@app.route('/api/analyse/determinedt', methods=["POST"], endpoint='analyze_determinedt')
@swag_from('as-yml/determinedt.yml')
def determinedt():
    input_json = request.get_json()
    try:
        filepath = str(input_json['filepath'])
        enum = False
        if 'enum' in input_json:
            enum = bool(input_json['enum'])
            print(enum)
        enum_tol = 0.001
        if 'enum_tol' in input_json:
            enum_tol = float(input_json['enum_tol'])
            print(enum_tol)
        separator = None
        if 'separator' in input_json:
            separator = str(input_json['separator'])
        res = determine_datatypes(filepath,enum,enum_tol,separator)
        logging.info('Determined datatypes successfully: %s',res)
        return Response(res, mimetype="application/json"), 200
    except Exception as e:
        logging.error(e)
        res = {"success": False, "message": str(e)}
        return Response(res, mimetype="application/json"), 500

@app.route('/api/analyse/determinepk', methods=["POST"], endpoint='analyze_determinepk')
@swag_from('as-yml/determinepk.yml')
def determinepk():
    input_json = request.get_json()
    try:
        filepath = str(input_json['filepath'])
        seperator = ','
        if 'seperator' in input_json:
            seperator = str(input_json['seperator'])
        res = determine_pk(filepath,seperator)
        logging.info('Determined list of primary keys: %s', res)
        return Response(res, mimetype="application/json"), 200
    except Exception as e:
        logging.error(e)
        res = {"success": False, "message": str(e)}
        return Response(res, mimetype="application/json"), 500

@app.route('/api/analyse/update_mdb_col', methods=["POST"], endpoint='mdb_update_col')
@swag_from('as-yml/updatecol.yml')
def updatecol(): 
    input_json = request.get_json() 
    try: 
        dbid=int(input_json['dbid'])
        tid=int(input_json['tid'])
        cid=int(input_json['cid'])
        res = update_mdb_col(dbid,tid,cid)
        logging.info('Update metadata database entity mdb_columns')
        return Response(res, mimetype="application/json"), 200
    except Exception as e:
        logging.error(e)
        res = {"success": False, "message": "Unknown error"}
        return Response(res, mimetype="application/json"), 500

rest_server_port = 5000
eureka_client.init(eureka_server=os.getenv('EUREKA_SERVER', 'http://localhost:9090/eureka/'),
                   app_name=os.getenv('HOSTNAME', 'analyse-service'),
                   instance_ip=os.getenv('HOSTNAME', 'analyse-service'),
                   instance_host=os.getenv('HOSTNAME', 'analyse-service'),
                   instance_port=rest_server_port)

if __name__ == '__main__':
    http_server = WSGIServer(('', 5000), app)
    http_server.serve_forever()
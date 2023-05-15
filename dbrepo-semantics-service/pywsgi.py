import os
from gevent.pywsgi import WSGIServer
from app import app
import logging

rest_server_port = int(os.getenv('PORT_APP', 5010))
rest_server_host = os.getenv('FLASK_RUN_HOST', '0.0.0.0')
path = os.getenv('READY_FILE', './ready')

logging.basicConfig(format='%(asctime)s %(levelname)-6s %(message)s', level=logging.DEBUG)

http_server = WSGIServer(listener=(rest_server_host, rest_server_port), application=app, log=logging)
http_server.serve_forever()

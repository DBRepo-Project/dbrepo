from gevent import monkey
monkey.patch_all()

import os
from gevent.pywsgi import WSGIServer
from app import app

http_server = WSGIServer(('0.0.0.0', int(os.environ['PORT_APP'])), app)
path = os.getenv('READY_FILE', './ready')
with open(path, 'w') as f:
    print('Service is ready, create file at {}'.format(path))
http_server.serve_forever()

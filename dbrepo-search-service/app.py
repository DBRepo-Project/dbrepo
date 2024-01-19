from gevent.pywsgi import WSGIServer
from app import create_app

app = create_app()

if __name__ == '__main__':
    http_server = WSGIServer(('', 5050), app)
    http_server.serve_forever()

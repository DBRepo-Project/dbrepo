from flask import Flask
from .records_ui import records_bp



def create_app():
    app = Flask(__name__)
    app.config.from_pyfile("config.py")

    app.register_blueprint(records_bp)

    return app
from flask import Flask
from .records_ui import records_bp
from flask_assets import Environment, Bundle
import os



def create_app():
    app = Flask(__name__)
    app.config.from_pyfile("config.py")

    assets = Environment(app)

    # LESS Bundle registrieren
    less_bundle = Bundle(
        'assets/less/theme.less',
        filters='less',
        output='css/theme.css'
    )

    assets.register('theme_css', less_bundle)

    app.register_blueprint(records_bp)

    return app
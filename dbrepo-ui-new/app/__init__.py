from flask import Flask, session, request, redirect, url_for, current_app
from .records_ui import records_bp
from flask_assets import Environment, Bundle
from flask_babelex import Babel, gettext, lazy_gettext
import os



def create_app():
    app = Flask(__name__)
    app.config.from_pyfile("config.py")
    def get_locale():
        return 'de'
    babel = Babel(app)

    app.secret_key = 'super secret key'
    base_dir = os.path.abspath(os.path.dirname(__file__))
    app.config["BABEL_TRANSLATION_DIRECTORIES"] = os.path.abspath(os.path.join(base_dir, "..", "translations"))

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
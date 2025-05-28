from flask import Flask, session, request, redirect, url_for, current_app
from .records_ui import records_bp
from flask_assets import Environment, Bundle
from flask_babel import Babel, gettext as _, get_locale
import os



def create_app():
    app = Flask(__name__)
    app.config.from_pyfile("config.py")
    def get_locale():
        return 'de'
    babel = Babel(app, locale_selector=get_locale)

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



    @app.route('/debug-locale')
    def debug_locale():
        return f"Current locale: {get_locale()}"

    @app.route("/test-translation")
    def test_translation():
        translations_dir = current_app.config.get("BABEL_TRANSLATION_DIRECTORIES", "translations")
        print(translations_dir)
        print(get_locale())

        return _("test")

    return app
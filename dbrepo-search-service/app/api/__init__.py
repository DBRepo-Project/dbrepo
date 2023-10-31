from flask import Blueprint

api_bp = Blueprint("api", __name__, url_prefix="/api/search")

from app.api import routes

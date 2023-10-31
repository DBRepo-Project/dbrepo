"""Configuration classes for the flask application factory
"""

import os

basedir = os.path.abspath(os.path.dirname(__file__))


class TestConfig:
    pass


class Config:
    """
    Default config class for development.

    Defines Host, Port, Username and password to connect to the Opensearch Database
    """

    SECRET_KEY = os.getenv("SECRET_KEY") or os.urandom(32)

    SEARCH_HOST = os.getenv("OPENSEARCH_HOST", "localhost")
    SEARCH_PORT = int(os.getenv("OPENSEARCH_PORT", 9200))
    SEARCH_USERNAME = os.getenv("OPENSEARCH_USERNAME", "admin")
    SEARCH_PASSWORD = os.getenv("OPENSEARCH_PASSWORD", "admin")

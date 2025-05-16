import logging
import os
from logging.config import dictConfig
from typing import List

from dbrepo.RestClient import RestClient
from dbrepo.api.dto import Database
from dbrepo.core.client.dashboard import DashboardServiceClient

logging.addLevelName(level=logging.NOTSET, levelName='TRACE')
logging.basicConfig(level=logging.DEBUG)

# logging configuration
dictConfig({
    'version': 1,
    'formatters': {
        'default': {
            'format': '[%(asctime)s] %(levelname)s in %(module)s: %(message)s',
        },
        'simple': {
            'format': '[%(asctime)s] [%(levelname)s] %(message)s',
        },
        'ecs': {
            'format': '{"@timestamp": "%(asctime)s", "log.level": "%(levelname)s", "log.logger": "%(module)s", "message": "%(message)s", "service_name": "dashboard-service-init", "service_version": "1.9.0"}',
            'datefmt': '%Y-%m-%dT%H:%M:%S'
        },
    },
    'handlers': {
        'wsgi': {
            'class': 'logging.StreamHandler',
            'stream': 'ext://flask.logging.wsgi_errors_stream',
            'formatter': 'simple'
        },
        'file': {
            'class': 'logging.handlers.TimedRotatingFileHandler',
            'formatter': 'ecs',
            'filename': '/var/log/app/service/dashboard/init.log',
            'when': 'm',
            'interval': 1,
            'backupCount': 5,
            'encoding': 'utf8'
        },
    },
    'root': {
        'level': 'DEBUG',
        'handlers': ['wsgi', 'file']
    }
})


def dashboard_client():
    return DashboardServiceClient(os.getenv('DASHBOARD_UI_ENDPOINT', 'http://localhost:3000'),
                                  os.getenv('SYSTEM_USERNAME', 'admin'), os.getenv('SYSTEM_PASSWORD', 'admin'))


def rest_client():
    return RestClient(endpoint=os.getenv("METADATA_SERVICE_ENDPOINT", "http://localhost"),
                      username=os.getenv('SYSTEM_USERNAME', 'admin'), password=os.getenv('SYSTEM_PASSWORD', 'admin'))


def fetch_databases() -> List[Database]:
    databases = []
    for index, database in enumerate(rest_client().get_databases()):
        logging.debug(f"fetching database details for database id: {database.id}")
        databases.append(rest_client().get_database(database_id=database.id))
    logging.info(f"Fetched {len(databases)} database(s)")
    return databases


def upsert_dashboard(database: Database) -> None:
    db = dashboard_client().find(database.dashboard_uid)
    if db is None:
        db = dashboard_client().create(database.internal_name, database.dashboard_uid)
        rest_client().update_database_dashboard(database.id, db['uid'])
    dashboard_client().update(database)


if __name__ == "__main__":
    for database in fetch_databases():
        upsert_dashboard(database)
    logging.info("Finished. Exiting.")

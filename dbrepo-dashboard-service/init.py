import logging
import os
from logging.config import dictConfig
from typing import List

from dbrepo.RestClient import RestClient
from dbrepo.api.dto import Database
from dbrepo.core.client.dashboard import DashboardServiceClient

logging.addLevelName(level=logging.NOTSET, levelName='TRACE')
logging.basicConfig(level=logging.DEBUG)

LOG_PATH = os.getenv("LOG_PATH", "/var/log/app/service/dashboard")

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
        "ecs": {
            "()": "ecs_logging.StdlibFormatter"
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
            'filename': f'{LOG_PATH}/init.log',
            'when': 'm',
            'interval': 1,
            'backupCount': 5,
            'encoding': 'utf8'
        },
    },
    'root': {
        'level': os.getenv("LOG_APPLICATION_LEVEL", "debug").upper(),
        'handlers': ['wsgi', 'file']
    }
})


def dashboard_client():
    return DashboardServiceClient(os.getenv('DASHBOARD_UI_ENDPOINT', 'http://localhost:3000'),
                                  os.getenv('DASHBOARD_UI_USERNAME', 'admin'), os.getenv('DASHBOARD_UI_PASSWORD', 'admin'),
                                  organization_id=int(os.getenv('DASHBOARD_ORG_ID', '1')))


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
    dashboard_client().reconcile(database)


def synchronize_dashboards() -> None:
    failures = 0
    for database in fetch_databases():
        try:
            upsert_dashboard(database)
        except Exception:
            failures += 1
            logging.exception('Dashboard synchronization failed for database %s', database.id)
    if failures:
        raise RuntimeError(f'Dashboard synchronization failed for {failures} database(s)')


if __name__ == "__main__":
    from provision import configure_environment
    configure_environment()
    synchronize_dashboards()
    logging.info("Finished. Exiting.")

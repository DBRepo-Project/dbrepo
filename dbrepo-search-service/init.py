import json
import logging
import os
from logging.config import dictConfig
from typing import List

from dbrepo.RestClient import RestClient
from dbrepo.api.dto import Database
from dbrepo.core.client.search import SearchServiceClient

level = os.getenv("LOG_LEVEL", "DEBUG").upper()
logging.basicConfig(level=level)

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
            'filename': '/var/log/app/service/search/init.log',
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

search_client = SearchServiceClient(host=os.getenv("OPENSEARCH_HOST", "search-db"),
                                    port=int(os.getenv("OPENSEARCH_PORT", "9200")),
                                    username=os.getenv("OPENSEARCH_USERNAME", "admin"),
                                    password=os.getenv("OPENSEARCH_PASSWORD", "admin"))

rest_client = RestClient(endpoint=os.getenv("METADATA_SERVICE_ENDPOINT", "http://metadata-service:8080"),
                         username=os.getenv("SYSTEM_USERNAME", "admin"),
                         password=os.getenv("SYSTEM_PASSWORD", "admin"))


def fetch_databases() -> List[Database]:
    databases = []
    for index, database in enumerate(rest_client.get_databases()):
        database = rest_client.get_database(database_id=database.id)
        logging.debug(f'fetch database details with id: {database.id}')
        databases.append(database)
    logging.debug(f'fetched {len(databases)} database(s)')
    return databases


def save_databases(databases: List[Database]):
    logging.debug(f"save {len(databases)} database(s)")
    for doc in databases:
        search_client.save_database(database_id=doc.id, data=doc)
        for table in doc.tables:
            logging.debug(f'update statistic for table: {doc.internal_name}.{table.internal_name}')
            rest_client.update_table_statistics(database_id=doc.id, table_id=table.id)
        logging.info(f"Saved database with id {doc.id}")


if __name__ == "__main__":
    with open('./database.json', 'r') as f:
        search_client.index_update(mapping=json.load(f))
    save_databases(databases=fetch_databases())
    logging.info("Finished. Exiting.")

import json
import os
import logging
from typing import List

import opensearchpy.exceptions
from dbrepo.RestClient import RestClient
from logging.config import dictConfig
from pathlib import Path

from dbrepo.api.dto import Database
from opensearchpy import OpenSearch

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
            'format': '[%(asctime)s] %(levelname)s: %(message)s',
        },
    },
    'handlers': {'wsgi': {
        'class': 'logging.StreamHandler',
        'stream': 'ext://flask.logging.wsgi_errors_stream',
        'formatter': 'simple'  # default
    }},
    'root': {
        'level': 'DEBUG',
        'handlers': ['wsgi']
    }
})


class App:
    """
    The client to communicate with the OpenSearch database.
    """
    metadata_service_endpoint: str = None
    search_host: str = None
    search_port: int = None
    search_username: str = None
    search_password: str = None
    search_instance: OpenSearch = None

    def __init__(self):
        self.metadata_service_endpoint = os.getenv("METADATA_SERVICE_ENDPOINT", "http://metadata-service:8080")
        self.search_host = os.getenv("OPENSEARCH_HOST", "search-db")
        self.search_port = int(os.getenv("OPENSEARCH_PORT", "9200"))
        self.search_username = os.getenv("OPENSEARCH_USERNAME", "admin")
        self.search_password = os.getenv("OPENSEARCH_PASSWORD", "admin")

    def _instance(self) -> OpenSearch:
        """
        Wrapper method to get the instance singleton.

        @returns: The opensearch instance singleton, if successful.
        """
        if self.search_instance is None:
            self.search_instance = OpenSearch(hosts=[{"host": self.search_host, "port": self.search_port}],
                                              http_compress=True,
                                              http_auth=(self.search_username, self.search_password))
            logging.debug(f"create instance {self.search_host}:{self.search_port}")
        return self.search_instance

    def index_exists(self):
        return self._instance().indices.exists(index="database")

    def database_exists(self, database_id: int):
        try:
            self._instance().get(index="database", id=database_id)
            return True
        except opensearchpy.exceptions.NotFoundError:
            return False

    def index_update(self, is_created: bool) -> bool:
        """

        :param is_created:
        :return: True if the index was updated
        """
        if is_created:
            logging.debug(f"index 'database' does not exist, creating...")
            with open('./database.json', 'r') as f:
                self._instance().indices.create(index="database", body=json.load(f))
            logging.info(f"Created index 'database'")
            return True
        mapping = dict(self._instance().indices.get_mapping(index="database"))
        identifier_props = mapping["database"]["mappings"]["properties"]["identifiers"]["properties"]
        if "status" in identifier_props:
            logging.debug(f"found mapping database.identifiers.status: detected current mapping")
            return False
        logging.debug(f"index 'database' exists, updating mapping...")
        with open('./database.json', 'r') as f:
            self._instance().indices.put_mapping(index="database", body=json.load(f))
        logging.info(f"Updated index 'database'")
        return True

    def fetch_databases(self) -> List[Database]:
        logging.debug(f"fetching database from endpoint: {self.metadata_service_endpoint}")
        client = RestClient(endpoint=self.metadata_service_endpoint)
        databases = []
        for index, database in enumerate(client.get_databases()):
            logging.debug(f"fetching database {index}/{len(databases)} details for database id: {database.id}")
            databases.append(client.get_database(database_id=database.id))
        logging.debug(f"fetched {len(databases)} database(s)")
        return databases

    def save_databases(self, databases: List[Database]):
        logging.debug(f"save {len(databases)} database(s)")
        for doc in databases:
            doc: Database = doc
            try:
                self._instance().delete(index="database", id=doc.id)
                logging.debug(f"deleted database with id {doc.id}")
            except opensearchpy.NotFoundError:
                logging.warning(f"Database with id {doc.id} does not exist, skip.")
            self._instance().create(index="database", id=doc.id, body=doc.model_dump())
            logging.debug(f"created database with id {doc.id}")


if __name__ == "__main__":
    app = App()
    create = not app.index_exists()
    update = app.index_update(is_created=create)
    app.save_databases(databases=app.fetch_databases())
    logging.info("Finished. Exiting.")

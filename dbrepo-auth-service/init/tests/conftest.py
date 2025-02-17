import logging
import os
from os.path import abspath

import mariadb
import pytest
from testcontainers.mysql import MySqlContainer

schema_location = abspath("../../dbrepo-metadata-db/1_setup-schema.sql")


@pytest.fixture(scope="session", autouse=True)
def session(request):
    """
    Create one MariaDB container per test run only (root:dbrepo)
    :param request: /
    :return: The MariaDB container
    """
    logging.debug("[fixture] creating mariadb container")
    container = (MySqlContainer(image="mariadb:11.3.2",
                                MYSQL_DATABASE=os.getenv('METADATA_DB', 'dbrepo'),
                                MYSQL_ROOT_PASSWORD=os.getenv('METADATA_DB_PASSWORD', 'dbrepo'))
                 .with_env('MARIADB_DATABASE', os.getenv('METADATA_DB', 'dbrepo'))
                 .with_env('MARIADB_ROOT_PASSWORD', os.getenv('METADATA_DB_PASSWORD', 'dbrepo'))
                 .with_volume_mapping(schema_location, "/docker-entrypoint-initdb.d/1_setup-schema.sql"))
    logging.debug("[fixture] starting mariadb container")
    container.start()

    os.environ['METADATA_HOST'] = '127.0.0.1'
    os.environ['METADATA_PORT'] = container.get_exposed_port(3306)

    # destructor
    def stop_mariadb():
        container.stop()

    request.addfinalizer(stop_mariadb)
    return container


def execute_single_result(query: str) -> any:
    conn = mariadb.connect(user=os.getenv('METADATA_USERNAME', 'root'),
                           password=os.getenv('METADATA_DB_PASSWORD', 'dbrepo'),
                           host=os.getenv('METADATA_HOST', 'metadata-db'),
                           port=int(os.getenv('METADATA_PORT', '3306')),
                           database=os.getenv('METADATA_DB', 'dbrepo'))
    cursor = conn.cursor()
    cursor.execute(query)
    rows = cursor.fetchone()
    conn.commit()
    conn.close()
    return rows

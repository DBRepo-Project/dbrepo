import os

import pytest
import logging
import json

from minio.deleteobjects import DeleteObject
from testcontainers.minio import MinioContainer
from testcontainers.mysql import MySqlContainer
from testcontainers.opensearch import OpenSearchContainer


@pytest.fixture(scope="session")
def session(request):
    """
    Create one minIO container per test run only
    :param request: /
    :return: The minIO container
    """
    logging.debug("[fixture] creating container")
    container = MinioContainer(access_key="seaweedfsadmin", secret_key="seaweedfsadmin")
    logging.debug("[fixture] starting container")
    container.start()
    # set the environment for the client
    endpoint = (
        "http://"
        + container.get_container_host_ip()
        + ":"
        + container.get_exposed_port(9000)
    )
    os.environ["S3_STORAGE_ENDPOINT"] = endpoint
    client = container.get_client()
    # create buckets
    logging.debug("[fixture] make buckets dbrepo-upload, dbrepo-download")
    client.make_bucket("dbrepo-upload")
    client.make_bucket("dbrepo-download")

    # destructor
    def stop_minio():
        container.stop()

    request.addfinalizer(stop_minio)
    return container


@pytest.fixture(scope="function", autouse=True)
def cleanup(request, session):
    """
    Clean up after each test by removing the buckets and re-adding them (=so they are empty again)
    :param request: /
    :param session: /
    :return:
    """
    logging.info("[fixture] truncate buckets")
    for bucket in ["dbrepo-upload", "dbrepo-download"]:
        objects = []
        for obj in session.get_client().list_objects(bucket):
            objects.append(DeleteObject(obj.object_name))
        logging.info(f"request to remove objects {objects}")
        errors = session.get_client().remove_objects(bucket, objects)
        for error in errors:
            raise ConnectionError(
                f"Failed to delete object with key {error.object_name} of bucket {bucket}"
            )


@pytest.fixture(scope="function")
def metadata_db_container():
    metadata_db = MySqlContainer(
        "bitnami/mariadb:10.5",
        MYSQL_USER="root",
        MYSQL_PASSWORD="dbrepo",
        MYSQL_DATABASE="fda",
    )
    metadata_db._name = "metadata-db-test"
    metadata_db.ports = {"3306": 33060}
    metadata_db.env = {
        "MYSQL_USER": metadata_db.MYSQL_USER,
        "MARIADB_ROOT_PASSWORD": metadata_db.MYSQL_ROOT_PASSWORD,
        "MARIADB_DATABASE": metadata_db.MYSQL_DATABASE,
    }
    # volume that mounts db schema from metadata-db
    metadata_db.with_volume_mapping(
        os.path.abspath("../dbrepo-metadata-db/setup-schema.sql"), "/schema.sql"
    )
    # volume for script that initializes schema and inserts test values
    metadata_db.with_volume_mapping(
        os.path.abspath("./test/init-db.sh"),
        "/docker-entrypoint-initdb.d/init-db.sh",
    )

    # validate creation of schema and data
    with metadata_db:
        print(
            metadata_db.exec(
                f"mariadb -u{metadata_db.MYSQL_USER} -p{metadata_db.MYSQL_ROOT_PASSWORD} fda -e 'SELECT * FROM mdb_databases;'"
            )
        )
        yield metadata_db


@pytest.fixture(scope="function")
def data_db_container():
    data_db = MySqlContainer(
        "bitnami/mariadb:10.5",
        MYSQL_USER="root",
        MYSQL_PASSWORD="dbrepo",
    )
    data_db._name = "data-db-test"
    data_db.ports = {"3306": 33061}
    data_db.env = {
        "MYSQL_USER": data_db.MYSQL_USER,
        "MARIADB_ROOT_PASSWORD": data_db.MYSQL_ROOT_PASSWORD,
    }
    # volume that mounts csv for data import
    data_db.with_volume_mapping(
        os.path.abspath("./data/test_stats/test_stats_01.csv"), "/test_stats_01.csv"
    )
    # volume for script to create a test data db and import values from a csv
    data_db.with_volume_mapping(
        os.path.abspath("./test/init-data-db.sh"),
        "/docker-entrypoint-initdb.d/init-data-db.sh",
    )

    with data_db:
        yield data_db


@pytest.fixture(scope="function")
def opensearch_container():
    os_container = OpenSearchContainer("opensearchproject/opensearch:2.10.0")

    with os_container:
        client = os_container.get_client()
        index_mapping_path = os.path.join(
            "..", "dbrepo-search-db", "init", "indices", "database.json"
        )
        with open(index_mapping_path, "r") as file:
            mapping = json.load(file)
            client.indices.create(index="database", body=mapping)

        yield os_container


@pytest.fixture(scope="function")
def all_containers(opensearch_container, metadata_db_container, data_db_container):
    yield opensearch_container, metadata_db_container, data_db_container

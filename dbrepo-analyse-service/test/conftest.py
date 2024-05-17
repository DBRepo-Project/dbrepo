import os

import pytest
import logging
import json

from app import app
from minio.deleteobjects import DeleteObject
from testcontainers.minio import MinioContainer
from testcontainers.opensearch import OpenSearchContainer

logging.basicConfig(level=logging.DEBUG)


@pytest.fixture(scope="session")
def app_context():
    with app.app_context():
        yield


@pytest.fixture(scope="session")
def session(request, app_context):
    """
    Create one minIO container per test run only
    :param request: /
    :param app_context: The Flask app context
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
    logging.debug(f"[fixture] setting s3 endpoint {endpoint}")
    app.config["S3_ENDPOINT"] = endpoint
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

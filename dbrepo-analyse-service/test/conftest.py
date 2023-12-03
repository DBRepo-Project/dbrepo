import os

import pytest
import logging

from minio.deleteobjects import DeleteObject
from testcontainers.minio import MinioContainer


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
    endpoint = 'http://' + container.get_container_host_ip() + ':' + container.get_exposed_port(9000)
    os.environ['S3_STORAGE_ENDPOINT'] = endpoint
    client = container.get_client()
    # create buckets
    logging.debug('[fixture] make buckets dbrepo-upload, dbrepo-download')
    client.make_bucket('dbrepo-upload')
    client.make_bucket('dbrepo-download')

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
        logging.info(f'request to remove objects {objects}')
        errors = session.get_client().remove_objects(bucket, objects)
        for error in errors:
            raise ConnectionError(f'Failed to delete object with key {error.object_name} of bucket {bucket}')

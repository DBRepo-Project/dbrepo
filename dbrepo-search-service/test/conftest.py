import logging

import pytest
from app import app
from flask import current_app

from testcontainers.opensearch import OpenSearchContainer


@pytest.fixture(scope="session", autouse=True)
def session(request):
    """
    Create one OpenSearch container per test run only (admin:admin)
    :param request: /
    :return: The OpenSearch container
    """
    logging.debug("[fixture] creating opensearch container")
    container = OpenSearchContainer()
    logging.debug("[fixture] starting opensearch container")
    container.start()

    with app.app_context():
        current_app.config['OPENSEARCH_HOST'] = container.get_container_host_ip()
        current_app.config['OPENSEARCH_PORT'] = container.get_exposed_port(9200)

    # destructor
    def stop_opensearch():
        container.stop()

    request.addfinalizer(stop_opensearch)
    return container

# @pytest.fixture(scope="function", autouse=True)
# def cleanup(request, session):
#     """
#     Clean up after each test by removing the buckets and re-adding them (=so they are empty again)
#     :param request: /
#     :param session: /
#     :return:
#     """
#     logging.info("[fixture] truncate buckets")
#     for bucket in ["dbrepo-upload", "dbrepo-download"]:
#         objects = []
#         for obj in session.get_client().list_objects(bucket):
#             objects.append(DeleteObject(obj.object_name))
#         logging.info(f'request to remove objects {objects}')
#         errors = session.get_client().remove_objects(bucket, objects)
#         for error in errors:
#             raise ConnectionError(f'Failed to delete object with key {error.object_name} of bucket {bucket}')

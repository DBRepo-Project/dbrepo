import logging
import os

import pytest
import json

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

    os.environ['OPENSEARCH_HOST'] = container.get_container_host_ip()
    os.environ['OPENSEARCH_PORT'] = container.get_exposed_port(9200)
    os.environ['OPENSEARCH_USERNAME'] = 'admin'
    os.environ['OPENSEARCH_PASSWORD'] = 'admin'

    # destructor
    def stop_opensearch():
        container.stop()

    request.addfinalizer(stop_opensearch)
    return container


@pytest.fixture(scope="function", autouse=True)
def cleanup(request, session):
    """
    Clean up after each test by removing the index and re-adding it (=so it's empty again)
    :param request: /
    :param session: /
    :return:
    """
    logging.info("[fixture] clean schema")
    with open('./init/database.json', 'r') as f:
        if session.get_client().indices.exists(index="database"):
            session.get_client().indices.delete(index="database")
        session.get_client().indices.create(index="database", body=json.load(f))

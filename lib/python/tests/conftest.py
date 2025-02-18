import logging
import os

import pytest
from testcontainers.generic import ServerContainer

logging.basicConfig(level=logging.DEBUG)


@pytest.fixture(scope="session", autouse=True)
def session(request):
    """
    Create one TUSd container per test run only
    :param request: /
    :return: The TUSd container
    """
    logging.debug("[fixture] creating container")
    container = ServerContainer(port=8080, image='tusproject/tusd:v2.4.0').with_command("-base-path=/api/upload/files/")
    logging.debug("[fixture] starting container")
    container.start()
    # set the environment for the client
    endpoint = f"http://{container.get_container_host_ip()}:{container.get_exposed_port(8080)}/api/upload/files"
    logging.debug(f'set upload endpoint {endpoint}')
    os.environ['REST_UPLOAD_ENDPOINT'] = endpoint

    # destructor
    def stop_tusd():
        container.stop()

    request.addfinalizer(stop_tusd)
    return container

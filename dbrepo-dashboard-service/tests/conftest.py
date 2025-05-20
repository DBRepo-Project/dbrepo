import logging
import os

import pytest
from dbrepo.core.client.dashboard import DashboardServiceClient

from tests.grafana import GrafanaContainer

logging.basicConfig(level=logging.DEBUG)


@pytest.fixture(scope="session")
def session(request):
    """
    Create one Grafana container per test run only (admin:admin)
    :param request: /
    :return: The Grafana container
    """
    logging.debug("[fixture] creating grafana container")
    container = GrafanaContainer()
    logging.debug("[fixture] starting grafana container")
    container.start()
    os.environ['DASHBOARD_UI_ENDPOINT'] = container.get_url()
    os.environ['SYSTEM_USERNAME'] = 'admin'
    os.environ['SYSTEM_PASSWORD'] = 'admin'

    # destructor
    def stop_grafana():
        container.stop()

    request.addfinalizer(stop_grafana)
    return container


@pytest.fixture(autouse=True)
def cleanup(request, session):
    """
    Clean up after each test by removing dashboards (=so it's empty again)
    :param request: /
    :param session: /
    :return:
    """
    dashboard_client = DashboardServiceClient(os.getenv('DASHBOARD_UI_ENDPOINT', 'http://localhost:3000'),
                                              os.getenv('SYSTEM_USERNAME', 'admin'),
                                              os.getenv('SYSTEM_PASSWORD', 'admin'))
    logging.info("[fixture] clean dashboards")
    for dashboard in dashboard_client.get_client().search.search_dashboards():
        dashboard_client.get_client().dashboard.delete_dashboard(dashboard['uid'])
        logging.debug(f"[fixture] deleted dashboard {dashboard['uid']}")

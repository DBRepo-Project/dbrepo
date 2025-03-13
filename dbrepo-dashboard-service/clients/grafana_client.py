import logging
import os

import requests
from requests import Response

from grafana_client import GrafanaApi

url = os.getenv('DASHBOARD_UI_ENDPOINT', 'http://localhost:3000')
username = os.getenv('SYSTEM_USERNAME', 'admin')
password = os.getenv('SYSTEM_PASSWORD', 'admin')


def connect() -> GrafanaApi:
    return GrafanaApi.from_url(url=f'{url}', credential=(username, password))


def generic_get(api_url: str) -> Response:
    request_url = url + api_url
    logging.debug(f'generic get url={request_url}, auth=({username}, <reacted>)')
    return requests.get(request_url, auth=(username, password))


def generic_post(api_url: str, payload: dict) -> Response:
    request_url = url + api_url
    logging.debug(f'generic post url={request_url}, payload={payload}, auth=({username}, <reacted>)')
    return requests.post(request_url, json=payload, auth=(username, password))

import logging
import uuid

import pytest

from dbrepo.RestClient import RestClient
from dbrepo.api.dto import Database

logging.basicConfig(level=logging.DEBUG)


def pytest_configure(config):
    TestKeyValue.username = str(uuid.uuid4()).replace("-", "")[0:10]
    TestKeyValue.password = str(uuid.uuid4()).replace("-", "")[0:10]


@pytest.fixture(scope='session', name='rest_client')
def user_rest_client() -> RestClient:
    TestKeyValue.user_id = RestClient().create_user(username=f'{TestKeyValue.username}',
                                                    password=f'{TestKeyValue.password}',
                                                    email=f'{TestKeyValue.username}@example.com').id
    return RestClient(username=TestKeyValue.username, password=TestKeyValue.password)


@pytest.fixture(scope='session', name='database')
def database() -> Database:
    name = str(uuid.uuid4()).replace("-", "")[0:10]
    return RestClient(username=TestKeyValue.username,
                      password=TestKeyValue.password).create_database(name=name, container_id=1, is_public=True,
                                                                      is_schema_public=True)


class TestKeyValue:
    user_id: str = None
    username: str = None
    password: str = None
    database_name: str = None

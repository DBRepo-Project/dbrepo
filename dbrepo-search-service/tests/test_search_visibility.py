import time
from unittest.mock import patch
from uuid import uuid4

import jwt
import pytest
import requests
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from dbrepo.api.dto import DatabaseAccess, View, ViewColumn
from dbrepo.core.client.search import SearchServiceClient

from app import app
from tests.test_app import req


@pytest.fixture
def credentials(monkeypatch):
    monkeypatch.setenv('METADATA_SERVICE_ENDPOINT', 'http://metadata.invalid')
    key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    signing = jwt.jwk_from_pem(key.private_bytes(serialization.Encoding.PEM,
                                               serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))
    public = dict(signing.to_dict(), kid='visibility-test', use='sig', alg='RS256')
    with patch('dbrepo.core.client.auth._realm_keys', return_value=[public]):
        def authorization(role):
            if role == 'anonymous':
                return {}
            token = jwt.JWT().encode({'iat': int(time.time()), 'uid': str(uuid4()), 'preferred_username': role,
                                      'realm_access': {'roles': ['system'] if role == 'system' else []}},
                                     signing, alg='RS256', optional_headers={'kid': 'visibility-test'})
            return {'Authorization': f'Bearer {token}'}
        yield authorization


@pytest.fixture
def indexed(session, requests_mock):
    database = req.model_copy(deep=True)
    database.name = 'Shared catalog'
    database.owner.username = 'owner'
    database.tables[0].owner.username = 'other-table-owner'
    hidden = database.tables[0].model_copy(deep=True)
    hidden.id = str(uuid4())
    hidden.name = 'hidden_table_marker'
    hidden.internal_name = 'hidden_rows'
    hidden.is_public = hidden.is_schema_public = False
    hidden.columns[0].id = str(uuid4())
    hidden.columns[0].table_id = hidden.id
    hidden.columns[0].name = 'hidden_column_marker'
    database.tables.append(hidden)
    column = ViewColumn(id=str(uuid4()), database_id=database.id, name='visible_view_column', ord=0,
                        internal_name='visible_column', type='bigint', is_null_allowed=False)
    visible_view = View(id=str(uuid4()), name='visible_view', query='SELECT 1', database_id=database.id,
                        query_hash='hash', owner=database.owner, internal_name='visible_view', is_public=True,
                        is_schema_public=True, initial_view=False, columns=[column])
    hidden_view = visible_view.model_copy(deep=True)
    hidden_view.id = str(uuid4())
    hidden_view.name = 'hidden_view_marker'
    hidden_view.is_public = hidden_view.is_schema_public = False
    database.views = [visible_view, hidden_view]
    database.accesses = [DatabaseAccess(type='read', user=database.owner.model_copy(update={'username': 'acl_marker'}))]
    SearchServiceClient().save_database(database.id, database)
    SearchServiceClient()._instance().indices.refresh(index='database')
    requests_mock.get('http://metadata.invalid/api/v1/database', json=[{'id': database.id}])
    return database


def current_payload(indexed, role):
    current = indexed.model_copy(deep=True)
    current.name = 'Current authorized catalog'
    if role in ('anonymous', 'nonmember'):
        current.tables = current.tables[:1]
        current.views = current.views[:1]
    if role not in ('owner', 'system'):
        current.accesses = []
    return current.model_dump(mode='json')


def invoke(client, route, headers):
    if route == 'fuzzy':
        return client.get('/api/v1/search?q=Current', headers=headers)
    return client.post(f'/api/v1/search/{route}', json={}, headers=headers)


@pytest.mark.parametrize('role', ['anonymous', 'nonmember', 'member', 'owner', 'system'])
@pytest.mark.parametrize('route', ['fuzzy', 'database', 'table', 'column', 'view'])
def test_matching_results_use_current_metadata(indexed, credentials, requests_mock, role, route):
    headers = credentials(role)
    endpoint = requests_mock.get('http://metadata.invalid/api/v1/database/batch',
                                json=[current_payload(indexed, role)])
    with app.test_client() as client:
        response = invoke(client, route, headers)
    assert response.status_code == 200
    body = response.get_json()
    assert body
    text = response.get_data(as_text=True)
    hidden = role not in ('anonymous', 'nonmember')
    marker = {'column': 'hidden_column_marker', 'view': 'hidden_view_marker'}.get(route, 'hidden_table_marker')
    assert (marker in text) == hidden
    assert ('acl_marker' in text) == (role in ('owner', 'system') and route in ('fuzzy', 'database'))
    if route in ('fuzzy', 'database'):
        assert body[0]['name'] == 'Current authorized catalog'
    assert endpoint.last_request.headers.get('Authorization') == headers.get('Authorization')


@pytest.mark.parametrize('route', ['fuzzy', 'database', 'table', 'column', 'view'])
def test_revocation_uses_new_metadata_with_unchanged_index(indexed, credentials, requests_mock, route):
    headers = credentials('member')
    requests_mock.get('http://metadata.invalid/api/v1/database/batch',
                      [{'json': [current_payload(indexed, 'member')]}, {'json': [current_payload(indexed, 'nonmember')]}])
    with app.test_client() as client:
        first = invoke(client, route, headers)
        second = invoke(client, route, headers)
    assert first.status_code == second.status_code == 200
    assert 'hidden_' in first.get_data(as_text=True)
    assert 'hidden_' not in second.get_data(as_text=True)
    assert 'acl_marker' not in second.get_data(as_text=True)


@pytest.mark.parametrize('route', ['fuzzy', 'database', 'table', 'column', 'view', 'identifier', 'concept', 'unit', 'index'])
@pytest.mark.parametrize('status', [401, 403, 404])
def test_failed_authority_batch_never_reaches_projections(indexed, credentials, requests_mock, route, status):
    headers = credentials('member')
    requests_mock.get('http://metadata.invalid/api/v1/database/batch', status_code=status)
    with app.test_client() as client:
        response = client.get('/api/v1/search/database', headers=headers) if route == 'index' else invoke(client, route, headers)
    assert response.status_code == 503


@pytest.mark.parametrize('failure', ['timeout', 'connection', 'server', 'json', 'model', 'wrong_id', 'redirect'])
def test_metadata_failures_do_not_fall_back_to_index(indexed, credentials, requests_mock, failure):
    url = 'http://metadata.invalid/api/v1/database/batch'
    if failure in ('timeout', 'connection'):
        requests_mock.get(url, exc=requests.Timeout if failure == 'timeout' else requests.ConnectionError)
    elif failure == 'server':
        requests_mock.get(url, status_code=503)
    elif failure == 'redirect':
        requests_mock.get(url, status_code=302, headers={'Location': 'http://other.invalid/'})
    elif failure == 'json':
        requests_mock.get(url, text='not JSON')
    elif failure == 'model':
        requests_mock.get(url, json=[{'id': indexed.id}])
    else:
        payload = current_payload(indexed, 'anonymous')
        payload['id'] = str(uuid4())
        requests_mock.get(url, json=[payload])
    with app.test_client() as client:
        response = invoke(client, 'database', credentials('anonymous'))
    assert response.status_code == 503
    assert 'hidden_' not in response.get_data(as_text=True)
    assert 'acl_marker' not in response.get_data(as_text=True)


@pytest.mark.parametrize('role', ['anonymous', 'member'])
def test_environment_credentials_cannot_override_caller(indexed, credentials, requests_mock, monkeypatch, tmp_path, role):
    netrc = tmp_path / 'netrc'
    netrc.write_text('machine metadata.invalid login service-user password service-secret\n')
    monkeypatch.setenv('NETRC', str(netrc))
    monkeypatch.setenv('REST_API_USERNAME', 'service-user')
    monkeypatch.setenv('REST_API_PASSWORD', 'service-secret')
    headers = credentials(role)
    endpoint = requests_mock.get('http://metadata.invalid/api/v1/database/batch',
                                json=[current_payload(indexed, role)])
    with app.test_client() as client:
        assert invoke(client, 'database', headers).status_code == 200
    assert endpoint.last_request.headers.get('Authorization') == headers.get('Authorization')
    assert endpoint.last_request.timeout == (3.05, 5)


def test_unit_search_candidates_use_the_same_authority(indexed, credentials, requests_mock):
    unit = 'http://www.ontology-of-units-of-measure.org/resource/om-2/degreeCelsius'
    headers = credentials('member')
    endpoint = requests_mock.get('http://metadata.invalid/api/v1/database/batch', status_code=403)
    with app.test_client() as client:
        response = client.post('/api/v1/search/column?t1=0&t2=10',
                               json={'unit.uri': unit, 'concept.uri': 'http://example.org/concept'}, headers=headers)
    assert response.status_code == 503
    assert endpoint.last_request.headers.get('Authorization') == headers['Authorization']

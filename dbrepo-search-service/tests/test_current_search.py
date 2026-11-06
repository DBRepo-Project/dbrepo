from concurrent.futures import ThreadPoolExecutor
from copy import deepcopy
from datetime import datetime, timedelta, timezone
from types import SimpleNamespace
from uuid import UUID, uuid4

import pytest
import requests
from opensearchpy import TransportError
from dbrepo.core.client.search import SearchServiceClient

from app import app
from tests.test_search_visibility import credentials, indexed, current_payload
from tests.test_search_projections import projected, projection_payload, TYPES


def authority(requests_mock, records):
    by_id = {record['id']: record for record in records}
    listed = requests_mock.get('http://metadata.invalid/api/v1/database',
                               json=[{'id': identifier} for identifier in by_id])

    def batch(request, context):
        return [deepcopy(by_id[identifier]) for identifier in request.qs.get('id', []) if identifier in by_id]

    batched = requests_mock.get('http://metadata.invalid/api/v1/database/batch', json=batch)
    return listed, batched


def test_current_extra_fields_match_without_entering_response(indexed, credentials, requests_mock):
    current = current_payload(indexed, 'anonymous')
    current['current_extra'] = 'freshuniquetoken'
    backend = SearchServiceClient()._instance()
    backend.update(index='database', id=indexed.id, body={'doc': {'current_extra': current['current_extra']}}, refresh=True)
    authority(requests_mock, [current])
    with app.test_client() as client:
        response = client.get('/api/v1/search?q=freshuniquetoken')
        hidden = client.get('/api/v1/search?q=hidden_table_marker')
    assert response.status_code == 200
    assert response.headers['X-Count'] == '1'
    assert response.get_json()[0]['id'] == indexed.id
    assert 'current_extra' not in response.get_json()[0]
    assert hidden.status_code == 200
    assert hidden.get_json() == []
    assert hidden.headers['X-Count'] == '0'


def test_one_hundred_authorized_records_use_one_batch(indexed, credentials, requests_mock):
    records = []
    for number in range(1, 101):
        record = current_payload(indexed, 'member')
        record.update(id=str(UUID(int=number)), tables=[], views=[], accesses=[], is_public=False, is_schema_public=False)
        records.append(record)
    listed, batched = authority(requests_mock, records)
    headers = credentials('member')
    with app.test_client() as client:
        response = client.post('/api/v1/search/database', json={}, headers=headers)
    assert response.status_code == 200
    assert response.headers['X-Count'] == '100'
    assert len(response.get_json()) == 100
    assert listed.call_count == batched.call_count == 1
    assert listed.last_request.headers['Authorization'] == headers['Authorization']
    assert batched.last_request.headers['Authorization'] == headers['Authorization']
    assert SearchServiceClient()._instance().count(index=SearchServiceClient.QUERY_INDEX)['count'] == 0


@pytest.mark.parametrize("kind", ["database", "table"])
def test_final_page_beyond_one_thousand_records(indexed, credentials, requests_mock, kind):
    records = []
    for number in reversed(range(1, 1006)):
        record = current_payload(indexed, 'anonymous')
        record.update(id=str(UUID(int=number)), views=[], accesses=[])
        record['tables'][0].update(id=str(UUID(int=number)), database_id=record['id'])
        records.append(record)
    authority(requests_mock, records)
    backend = SearchServiceClient()._instance()
    stale = []
    for number in range(2000, 3100):
        stale.extend([{'index': {'_index': 'database', '_id': str(UUID(int=number))}},
                      {'id': str(UUID(int=number)), 'name': 'Current hidden catalog'}])
    backend.bulk(body=stale, refresh=True)
    with app.test_client() as client:
        default = client.get(f'/api/v1/search/{kind}')
        final = client.get(f'/api/v1/search/{kind}?page=101&results_per_page=10')
        beyond = client.get(f'/api/v1/search/{kind}?page=102&results_per_page=10')
        complete = client.post(f'/api/v1/search/{kind}', json={})
    assert default.status_code == complete.status_code == 200
    assert len(default.get_json()) == 50
    assert len(complete.get_json()) == 1005
    assert default.headers['X-Count'] == complete.headers['X-Count'] == '1005'
    assert final.status_code == 200
    assert [record['id'] for record in final.get_json()] == [str(UUID(int=number)) for number in range(1001, 1006)]
    assert final.headers['X-Count'] == '1005'
    assert final.headers['X-Total-Pages'] == '101'
    assert beyond.status_code == 200
    assert beyond.get_json() == []
    assert beyond.headers['X-Count'] == '1005'


@pytest.mark.parametrize('marker,field,authorized_role', [
    ('hidden_table_marker', 'tables.name', 'member'),
    ('hidden_column_marker', 'tables.columns.name', 'member'),
    ('acl_marker', 'accesses.user.username', 'owner'),
    ('foreign-draft', 'identifiers.publisher', 'system'),
])
@pytest.mark.parametrize('kind', ['fuzzy'] + TYPES)
def test_hidden_only_markers_cannot_influence_matches_or_counts(projected, credentials, requests_mock,
                                                              marker, field, authorized_role, kind):
    current = projection_payload(projected, 'anonymous')
    # Preserve the marker on the private column; projection fixtures rename current columns.
    allowed = current_payload(projected, authorized_role)
    if authorized_role != 'system':
        allowed['identifiers'] = [record for record in allowed['identifiers'] if record['status'] == 'published']
    authority(requests_mock, [allowed])
    headers = credentials(authorized_role)
    with app.test_client() as client:
        allowed_response = (client.get('/api/v1/search', query_string={'q': marker}, headers=headers)
                            if kind == 'fuzzy' else client.post(f'/api/v1/search/{kind}', json={field: marker}, headers=headers))
        authority(requests_mock, [current])
        denied = (client.get('/api/v1/search', query_string={'q': marker}) if kind == 'fuzzy'
                  else client.post(f'/api/v1/search/{kind}', json={field: marker}))
    assert allowed_response.status_code == 200
    assert allowed_response.get_json()
    assert int(allowed_response.headers['X-Count']) > 0
    assert denied.status_code == 200
    assert denied.get_json() == []
    assert denied.headers['X-Count'] == '0'


def test_mapped_extra_field_removed_from_current_body_stops_matching(indexed, credentials, requests_mock):
    payload = current_payload(indexed, 'anonymous')
    payload['extra_current_field'] = 'authorizedextramarker'
    backend = SearchServiceClient()._instance()
    backend.update(index='database', id=indexed.id, body={'doc': {'extra_current_field': payload['extra_current_field']}}, refresh=True)
    authority(requests_mock, [payload])
    with app.test_client() as client:
        first = client.get('/api/v1/search?q=authorizedextramarker')
        del payload['extra_current_field']
        authority(requests_mock, [payload])
        second = client.get('/api/v1/search?q=authorizedextramarker')
    assert first.status_code == second.status_code == 200
    assert first.headers['X-Count'] == '1'
    assert second.headers['X-Count'] == '0'
    assert backend.get(index='database', id=indexed.id)['_source']['extra_current_field'] == 'authorizedextramarker'


def test_unmapped_fields_remain_no_match_after_mapping_growth(indexed, credentials, requests_mock):
    payload = current_payload(indexed, 'anonymous')
    payload['never_indexed_field'] = 'currentonlytoken'
    authority(requests_mock, [payload])
    SearchServiceClient().general_search({})
    backend = SearchServiceClient()._instance()
    backend.indices.put_mapping(index='database', body={'properties': {'later_mapped_field': {'type': 'text'}}})
    payload['later_mapped_field'] = 'mappedcurrenttoken'
    authority(requests_mock, [payload])
    with app.test_client() as client:
        response = client.post('/api/v1/search/database', json={'never_indexed_field': 'currentonlytoken'})
        mapped = client.post('/api/v1/search/database', json={'later_mapped_field': 'mappedcurrenttoken'})
    assert response.status_code == mapped.status_code == 200
    assert response.get_json() == []
    assert [item['id'] for item in mapped.get_json()] == [indexed.id]
    assert 'never_indexed_field' not in mapped.get_json()[0]


@pytest.mark.parametrize('kind', TYPES)
@pytest.mark.parametrize('method', ['get', 'post'])
def test_projection_pages_sort_visible_children_and_expose_totals(projected, credentials, requests_mock, kind, method):
    payload = projection_payload(projected, 'member')
    authority(requests_mock, [payload])
    headers = credentials('member')
    headers['Origin'] = 'https://browser.invalid'
    with app.test_client() as client:
        def invoke(page=None):
            query = {} if page is None else {'page': page, 'results_per_page': 1}
            return (client.get(f'/api/v1/search/{kind}', query_string=query, headers=headers) if method == 'get'
                    else client.post(f'/api/v1/search/{kind}', query_string=query, headers=headers, json={}))
        full = invoke()
        data = full.get_json()
        assert full.status_code == 200
        identity = 'uri' if kind in ('concept', 'unit') else 'id'
        assert [item[identity] for item in data] == sorted(item[identity] for item in data)
        payload['tables'].reverse()
        payload['views'].reverse()
        payload['identifiers'].reverse()
        payload['subsets'].reverse()
        authority(requests_mock, [payload])
        last = invoke(len(data))
        beyond = invoke(len(data) + 1)
    assert last.status_code == beyond.status_code == 200
    assert [item[identity] for item in last.get_json()] == [item[identity] for item in data[-1:]]
    assert beyond.get_json() == []
    for response in (full, last, beyond):
        assert response.headers['X-Count'] == str(len(data))
        assert 'X-Count' in response.headers['Access-Control-Expose-Headers']
    assert last.headers['X-Total-Pages'] == str(len(data))


@pytest.mark.parametrize('route', ['get', 'post', 'fuzzy'])
@pytest.mark.parametrize('query', [{'page': '0'}, {'page': '-1'}, {'page': '1.5'}, {'page': 'nan'},
                                  {'results_per_page': '0'}, {'results_per_page': '-2'},
                                  {'results_per_page': '501'}, {'results_per_page': 'two'}])
def test_invalid_pagination_rejected_without_authority_calls(credentials, requests_mock, route, query):
    with app.test_client() as client:
        response = (client.post('/api/v1/search/database', json={}, query_string=query) if route == 'post'
                    else client.get('/api/v1/search' if route == 'fuzzy' else '/api/v1/search/database',
                                    query_string=dict(query, q='Current')))
    assert response.status_code == 400
    assert requests_mock.call_count == 0
    assert 'X-Count' not in response.headers


def test_fuzzy_optional_pagination_and_headers_are_per_response(indexed, credentials, requests_mock):
    records = []
    for number in [3, 1, 2]:
        record = current_payload(indexed, 'anonymous')
        record['id'] = str(UUID(int=number))
        records.append(record)
    authority(requests_mock, records)
    with app.test_client() as client:
        whole = client.get('/api/v1/search?q=Current')
        page = client.get('/api/v1/search?q=Current&page=2&results_per_page=2')
        beyond = client.get('/api/v1/search?q=Current&page=3&results_per_page=2')
        whole_again = client.get('/api/v1/search?q=Current')
    assert len(whole.get_json()) == 3
    assert page.get_json() == [whole.get_json()[-1]]
    assert beyond.get_json() == []
    assert all(response.headers['X-Count'] == '3' for response in (whole, page, beyond, whole_again))
    assert page.headers['X-Page'] == '2'
    assert page.headers['X-Page-Size'] == page.headers['X-Total-Pages'] == '2'
    assert 'X-Page' not in whole_again.headers


@pytest.mark.parametrize('endpoint', ['http://user:secret@metadata.invalid', 'http://@metadata.invalid',
                                      'http://%75ser@metadata.invalid', 'http://:secret@metadata.invalid'])
@pytest.mark.parametrize('role', ['anonymous', 'member'])
def test_metadata_url_userinfo_is_rejected_without_sending_credentials(credentials, requests_mock, monkeypatch,
                                                                       endpoint, role):
    monkeypatch.setenv('METADATA_SERVICE_ENDPOINT', endpoint)
    with app.test_client() as client:
        response = client.post('/api/v1/search/database', json={}, headers=credentials(role))
    assert response.status_code == 503
    assert requests_mock.call_count == 0


@pytest.mark.parametrize('role', ['anonymous', 'member'])
def test_list_and_batch_preserve_prepared_caller_auth(indexed, credentials, requests_mock, monkeypatch, tmp_path, role):
    netrc = tmp_path / 'netrc'
    netrc.write_text('machine metadata.invalid login service-user password service-secret\n')
    monkeypatch.setenv('NETRC', str(netrc))
    monkeypatch.setenv('REST_API_USERNAME', 'service-user')
    monkeypatch.setenv('REST_API_PASSWORD', 'service-secret')
    listed, batched = authority(requests_mock, [current_payload(indexed, role)])
    headers = credentials(role)
    with app.test_client() as client:
        response = client.post('/api/v1/search/database', json={}, headers=headers)
    assert response.status_code == 200
    for call in (listed.last_request, batched.last_request):
        assert call.headers.get('Authorization') == headers.get('Authorization')
        assert call.timeout == (3.05, 5)
        assert not call.allow_redirects


@pytest.mark.parametrize('payload', [{}, [None], [{}], [{'id': 'invalid'}], [{'id': 123}]])
def test_malformed_current_list_fails_explicitly(credentials, requests_mock, payload):
    requests_mock.get('http://metadata.invalid/api/v1/database', json=payload)
    with app.test_client() as client:
        response = client.post('/api/v1/search/database', json={})
    assert response.status_code == 503


@pytest.mark.parametrize('mode', ['duplicate', 'unsolicited', 'malformed', 'not_list'])
def test_batch_identity_and_shape_are_checked(indexed, credentials, requests_mock, mode):
    payload = current_payload(indexed, 'anonymous')
    result = [payload]
    if mode == 'duplicate':
        result.append(payload)
    elif mode == 'unsolicited':
        payload['id'] = str(uuid4())
    elif mode == 'malformed':
        result = [None]
    else:
        result = payload
    requests_mock.get('http://metadata.invalid/api/v1/database/batch', json=result)
    with app.test_client() as client:
        response = client.post('/api/v1/search/database', json={})
    assert response.status_code == 503


def test_query_mapping_survives_later_source_numeric_mapping(indexed, credentials, requests_mock):
    backend = SearchServiceClient()._instance()
    backend.indices.delete(index=SearchServiceClient.QUERY_INDEX, ignore=404)
    # Remove source mapping so the first query uses the DTO's double type for numeric statistics.
    backend.indices.delete(index='database')
    payload = current_payload(indexed, 'anonymous')
    authority(requests_mock, [payload])
    with app.test_client() as client:
        first = client.post('/api/v1/search/database', json={})
        backend.index(index='database', id=indexed.id, body=payload, refresh=True)
        second = client.post('/api/v1/search/database', json={})
    assert first.status_code == second.status_code == 200
    assert first.get_json() == second.get_json()
    field = backend.indices.get_mapping(index=SearchServiceClient.QUERY_INDEX)[SearchServiceClient.QUERY_INDEX]
    assert field['mappings']['properties']['tables']['properties']['columns']['properties']['val_min']['type'] == 'double'


@pytest.mark.parametrize('failure', ['timeout', 'partial', 'incomplete_shards', 'missing', 'expired', 'bad_slots', 'bad_fields'])
def test_native_failures_never_become_empty_success(indexed, credentials, requests_mock, monkeypatch, failure):
    authority(requests_mock, [current_payload(indexed, 'anonymous')])
    backend = SearchServiceClient()._instance()
    monkeypatch.setattr(SearchServiceClient, '_instance', lambda self: backend)
    original = backend.search

    def search(**kwargs):
        identifier = kwargs['body']['query']['bool']['filter'][0]['ids']['values'][0]
        if failure == 'missing':
            backend.delete(index=SearchServiceClient.QUERY_INDEX, id=identifier, refresh=True)
        elif failure == 'expired':
            backend.update(index=SearchServiceClient.QUERY_INDEX, id=identifier,
                           body={'doc': {SearchServiceClient.EXPIRY_FIELD: '2000-01-01T00:00:00Z'}}, refresh=True)
        result = original(**kwargs)
        if failure == 'timeout':
            result['timed_out'] = True
        elif failure == 'partial':
            result['_shards']['failed'] = 1
        elif failure == 'incomplete_shards':
            result['_shards']['successful'] = 0
        elif failure == 'bad_slots':
            result['hits']['hits'][0]['fields'] = {'_percolator_document_slot': [100]}
        elif failure == 'bad_fields':
            result['hits']['hits'][0]['fields'] = []
        return result

    monkeypatch.setattr(backend, 'search', search)
    with app.test_client() as client:
        response = client.post('/api/v1/search/database', json={})
    assert response.status_code == 503
    assert backend.count(index=SearchServiceClient.QUERY_INDEX)['count'] == 0


def test_uncertain_query_creation_still_deletes_known_uuid(indexed, credentials, requests_mock, monkeypatch):
    authority(requests_mock, [current_payload(indexed, 'anonymous')])
    backend = SearchServiceClient()._instance()
    monkeypatch.setattr(SearchServiceClient, '_instance', lambda self: backend)
    original = backend.index

    def lost_response(**kwargs):
        original(**kwargs)
        raise TransportError(503, 'synthetic lost response')

    monkeypatch.setattr(backend, 'index', lost_response)
    with app.test_client() as client:
        response = client.post('/api/v1/search/database', json={})
    assert response.status_code == 503
    assert backend.count(index=SearchServiceClient.QUERY_INDEX)['count'] == 0


def test_deadline_after_native_write_fails_and_cleans_up(indexed, credentials, requests_mock, monkeypatch):
    authority(requests_mock, [current_payload(indexed, 'anonymous')])
    backend = SearchServiceClient()._instance()
    monkeypatch.setattr(SearchServiceClient, '_instance', lambda self: backend)
    clock = [0]
    monkeypatch.setattr('dbrepo.core.client.search.time', SimpleNamespace(monotonic=lambda: clock[0]))
    original = backend.index

    def expires_after_write(**kwargs):
        result = original(**kwargs)
        clock[0] = SearchServiceClient.REQUEST_BUDGET + 1
        return result

    monkeypatch.setattr(backend, 'index', expires_after_write)
    with app.test_client() as client:
        response = client.post('/api/v1/search/database', json={})
    assert response.status_code == 503
    assert backend.count(index=SearchServiceClient.QUERY_INDEX)['count'] == 0


def test_failed_delete_is_explicit_and_next_request_removes_expired_residue(indexed, credentials, requests_mock, monkeypatch):
    authority(requests_mock, [current_payload(indexed, 'anonymous')])
    backend = SearchServiceClient()._instance()
    monkeypatch.setattr(SearchServiceClient, '_instance', lambda self: backend)
    original = backend.delete
    attempted = []

    def unavailable(**kwargs):
        attempted.append(kwargs['id'])
        raise TransportError(503, 'synthetic delete outage')

    monkeypatch.setattr(backend, 'delete', unavailable)
    try:
        with app.test_client() as client:
            response = client.post('/api/v1/search/database', json={})
        assert response.status_code == 503
        assert len(attempted) == 1
        document = backend.get(index=SearchServiceClient.QUERY_INDEX, id=attempted[0])['_source']
        assert set(document) == {SearchServiceClient.QUERY_FIELD, SearchServiceClient.EXPIRY_FIELD}
        backend.update(index=SearchServiceClient.QUERY_INDEX, id=attempted[0],
                       body={'doc': {SearchServiceClient.EXPIRY_FIELD: '2000-01-01T00:00:00Z'}}, refresh=True)
        monkeypatch.setattr(backend, 'delete', original)
        with app.test_client() as client:
            assert client.post('/api/v1/search/database', json={}).status_code == 200
        assert backend.count(index=SearchServiceClient.QUERY_INDEX)['count'] == 0
    finally:
        for identifier in attempted:
            original(index=SearchServiceClient.QUERY_INDEX, id=identifier, ignore=404, refresh=True)


def test_cleanup_is_bounded_and_preserves_active_queries(indexed, credentials, requests_mock):
    authority(requests_mock, [current_payload(indexed, 'anonymous')])
    backend = SearchServiceClient()._instance()
    client = SearchServiceClient()
    import time
    client._query_index(backend, time.monotonic() + 30)
    active = str(uuid4())
    expired = [str(uuid4()) for _ in range(3)]
    body = []
    for identifier in expired + [active]:
        body.extend([{'index': {'_index': client.QUERY_INDEX, '_id': identifier}},
                     {client.QUERY_FIELD: {'match_all': {}}, client.EXPIRY_FIELD:
                      (datetime.now(timezone.utc) + timedelta(minutes=5)).isoformat() if identifier == active
                      else '2000-01-01T00:00:00Z'}])
    backend.bulk(body=body, refresh=True)
    client.CLEANUP_LIMIT = 2
    try:
        client._query_index(backend, time.monotonic() + 30)
        assert backend.count(index=client.QUERY_INDEX)['count'] == 2
        assert backend.get(index=client.QUERY_INDEX, id=active)['found']
        client._query_index(backend, time.monotonic() + 30)
        assert backend.count(index=client.QUERY_INDEX)['count'] == 1
        assert backend.get(index=client.QUERY_INDEX, id=active)['found']
    finally:
        for identifier in expired + [active]:
            backend.delete(index=client.QUERY_INDEX, id=identifier, ignore=404, refresh=True)


def test_concurrent_requests_isolate_temporary_queries(indexed, credentials, requests_mock):
    records = []
    for number in range(1, 5):
        payload = current_payload(indexed, 'anonymous')
        payload.update(id=str(UUID(int=number)), name=f'uniqueterm{number}')
        records.append(payload)
    authority(requests_mock, records)
    with ThreadPoolExecutor(max_workers=4) as executor:
        results = list(executor.map(lambda number: SearchServiceClient().general_search({'name': f'uniqueterm{number}'}),
                                    range(1, 5)))
    assert [[record.id for record in result] for result in results] == [[str(UUID(int=number))] for number in range(1, 5)]
    assert SearchServiceClient()._instance().count(index=SearchServiceClient.QUERY_INDEX)['count'] == 0


@pytest.mark.parametrize('operation', ['create', 'put_mapping'])
def test_unacknowledged_query_mapping_fails_explicitly(indexed, credentials, requests_mock, monkeypatch, operation):
    authority(requests_mock, [current_payload(indexed, 'anonymous')])
    backend = SearchServiceClient()._instance()
    monkeypatch.setattr(SearchServiceClient, '_instance', lambda self: backend)
    if operation == 'create':
        backend.indices.delete(index=SearchServiceClient.QUERY_INDEX, ignore=404)
    else:
        SearchServiceClient().general_search({})
        backend.indices.put_mapping(index='database', body={'properties': {'ack_test_field': {'type': 'text'}}})
    original = getattr(backend.indices, operation)

    def unacknowledged(**kwargs):
        result = original(**kwargs)
        result['acknowledged'] = False
        return result

    monkeypatch.setattr(backend.indices, operation, unacknowledged)
    with app.test_client() as client:
        response = client.post('/api/v1/search/database', json={})
    assert response.status_code == 503
    assert backend.count(index=SearchServiceClient.QUERY_INDEX)['count'] == 0


def test_keyword_field_and_none_filters_remain_supported(indexed, credentials, requests_mock):
    payload = current_payload(indexed, 'anonymous')
    authority(requests_mock, [payload])
    with app.test_client() as client:
        response = client.post('/api/v1/search/database', json={'name.keyword': payload['name'], 'unknown': None})
        invalid = client.post('/api/v1/search/database', json={'tables.columns.val_min': 'not-a-number'})
    assert response.status_code == 200
    assert [item['id'] for item in response.get_json()] == [indexed.id]
    assert invalid.status_code == 503


@pytest.mark.parametrize('failure', ['timeout', 'connection', 'server'])
def test_current_list_backend_failure_is_explicit(credentials, requests_mock, failure):
    if failure == 'server':
        requests_mock.get('http://metadata.invalid/api/v1/database', status_code=503)
    else:
        requests_mock.get('http://metadata.invalid/api/v1/database',
                          exc=requests.Timeout if failure == 'timeout' else requests.ConnectionError)
    with app.test_client() as client:
        response = client.post('/api/v1/search/database', json={})
    assert response.status_code == 503


def test_field_list_does_not_include_temporary_query_index(indexed, credentials, requests_mock):
    authority(requests_mock, [current_payload(indexed, 'anonymous')])
    with app.test_client() as client:
        assert client.post('/api/v1/search/database', json={}).status_code == 200
        fields = client.get('/api/v1/search/database/fields')
    assert fields.status_code == 200
    assert fields.get_json()
    assert SearchServiceClient.QUERY_FIELD not in fields.get_data(as_text=True)
    assert SearchServiceClient.EXPIRY_FIELD not in fields.get_data(as_text=True)

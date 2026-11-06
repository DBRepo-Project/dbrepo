from uuid import uuid4

import pytest
from dbrepo.api.dto import Identifier, Links
from dbrepo.core.client.search import SearchServiceClient

from app import app
from tests.test_search_visibility import credentials, indexed, current_payload

ROLES = ['anonymous', 'nonmember', 'member', 'owner', 'system']
TYPES = ['database', 'table', 'view', 'column', 'identifier', 'user', 'concept', 'unit']
OM = 'http://www.ontology-of-units-of-measure.org/resource/om-2/'
CONCEPT = 'https://example.org/current-concept'


@pytest.fixture
def projected(indexed, session):
    def identifier(kind, label, owner='owner', status='published'):
        return Identifier(id=str(uuid4()), database_id=indexed.id, type=kind, status=status,
                          owner=indexed.owner.model_copy(update={'username': owner}),
                          links=Links(self='https://example.org/record', self_html='https://example.org/record'),
                          publication_year=2026, publisher=label, creators=[], titles=[], descriptions=[])
    indexed.identifiers = [identifier('database', 'database-record'),
                           identifier('database', 'foreign-draft', 'another-user', 'draft'),
                           identifier('database', 'owner-draft', 'owner', 'draft'),
                           identifier('database', 'member-draft', 'member', 'draft')]
    indexed.subsets = [identifier('subset', 'subset-record')]
    for i, table in enumerate(indexed.tables):
        table.identifiers = [identifier('table', f'table-record-{i}')]
        column = table.columns[0]
        column.concept.name = f'stale-concept-{i}'
        column.unit.name = f'stale-unit-{i}'
    for i, view in enumerate(indexed.views):
        view.identifiers = [identifier('view', f'view-record-{i}')]
    SearchServiceClient().save_database(indexed.id, indexed)
    SearchServiceClient()._instance().indices.refresh(index='database')
    return indexed


def projection_payload(projected, role):
    payload = current_payload(projected, role)
    payload['identifiers'] = [record for record in payload['identifiers']
                              if record['status'] == 'published' or role == 'system'
                              or record['owner']['username'] == role]
    for i, table in enumerate(payload['tables']):
        column = table['columns'][0]
        column['name'] = f'current-column-{i}'
        column['concept_uri'] = CONCEPT + f'/{i}'
        column['unit_uri'] = OM + ('metre' if i == 0 else 'centimetre')
    return payload


def request_projection(client, method, kind, headers, data=None):
    if method == 'get':
        return client.get(f'/api/v1/search/{kind}', headers=headers)
    return client.post(f'/api/v1/search/{kind}', json={} if data is None else data, headers=headers)


@pytest.mark.parametrize('role', ROLES)
@pytest.mark.parametrize('kind', TYPES)
@pytest.mark.parametrize('method', ['get', 'post'])
def test_real_matches_project_current_typed_results(projected, credentials, requests_mock, role, kind, method):
    payload = projection_payload(projected, role)
    endpoint = requests_mock.get('http://metadata.invalid/api/v1/database/batch', json=[payload])
    headers = credentials(role)
    with app.test_client() as client:
        response = request_projection(client, method, kind, headers, {'id': projected.id})
    assert response.status_code == 200
    body = response.get_json()
    assert body
    count = 1 if role in ('anonymous', 'nonmember') else 2
    if kind == 'database':
        assert body == [payload]
    elif kind == 'table':
        assert body == sorted(payload['tables'], key=lambda item: item['id'])
    elif kind == 'view':
        assert body == sorted(payload['views'], key=lambda item: item['id'])
    elif kind == 'column':
        assert [item['id'] for item in body] == sorted(table.columns[0].id for table in projected.tables[:count])
        assert sorted(item['name'] for item in body) == [f'current-column-{i}' for i in range(count)]
    elif kind == 'user':
        assert body == [payload['owner']]
        assert body[0]['id'] == projected.owner.id
    elif kind == 'identifier':
        labels = ['database-record', 'subset-record', 'table-record-0', 'view-record-0']
        if count == 2:
            labels += ['table-record-1', 'view-record-1']
        if role in ('member', 'owner'):
            labels += [f'{role}-draft']
        if role == 'system':
            labels += ['foreign-draft', 'owner-draft', 'member-draft']
        assert sorted(item['publisher'] for item in body) == sorted(labels)
        records = projected.identifiers + projected.subsets + [record for table in projected.tables for record in table.identifiers] + [record for view in projected.views for record in view.identifiers]
        assert {item['id'] for item in body} == {record.id for record in records if record.publisher in labels}
        assert all(item['database_id'] == projected.id for item in body)
    elif kind == 'concept':
        assert body == [{'uri': CONCEPT + f'/{i}'} for i in range(count)]
    elif kind == 'unit':
        assert body == sorted([{'uri': OM + unit} for unit in ['metre', 'centimetre'][:count]], key=lambda item: item['uri'])
    if role in ('anonymous', 'nonmember'):
        assert projected.tables[1].id not in response.get_data(as_text=True)
        assert projected.views[1].id not in response.get_data(as_text=True)
    if kind in ('concept', 'unit'):
        assert 'stale-' not in response.get_data(as_text=True)
    assert endpoint.last_request.headers.get('Authorization') == headers.get('Authorization')


@pytest.mark.parametrize('kind', TYPES)
@pytest.mark.parametrize('method', ['get', 'post'])
def test_all_projections_reflect_revoked_access(projected, credentials, requests_mock, kind, method):
    requests_mock.get('http://metadata.invalid/api/v1/database/batch',
                      [{'json': [projection_payload(projected, 'member')]}, {'json': []}])
    with app.test_client() as client:
        first = request_projection(client, method, kind, credentials('member'))
        second = request_projection(client, method, kind, credentials('member'))
    assert first.status_code == second.status_code == 200
    assert first.get_json()
    assert second.get_json() == []


@pytest.mark.parametrize('kind', ['identifier', 'table', 'view', 'column', 'concept', 'unit'])
def test_empty_optional_collections(projected, credentials, requests_mock, kind):
    payload = projection_payload(projected, 'anonymous')
    payload.update(identifiers=None, subsets=None, tables=None, views=None)
    requests_mock.get('http://metadata.invalid/api/v1/database/batch', json=[payload])
    with app.test_client() as client:
        response = request_projection(client, 'post', kind, {})
    assert response.status_code == 200
    assert response.get_json() == []


@pytest.mark.parametrize('role', ROLES)
@pytest.mark.parametrize('kind', TYPES)
def test_unit_independent_real_matches_use_current_columns(projected, credentials, requests_mock, role, kind):
    payload = projection_payload(projected, role)
    for i, table in enumerate(payload['tables']):
        column = table['columns'][0]
        column['concept_uri'] = CONCEPT
        column['val_min'], column['val_max'] = ((-0.5, 1.5) if i == 0 else (-50, 150))
    requests_mock.get('http://metadata.invalid/api/v1/database/batch', json=[payload])
    data = {'t1': -1.25, 't2': 2.5, 'tables.columns.concept.uri': CONCEPT,
            'tables.columns.unit.uri': OM + 'metre'}
    with app.test_client() as client:
        response = request_projection(client, 'post', kind, credentials(role), data)
    assert response.status_code == 200
    body = response.get_json()
    assert body
    if kind == 'column':
        count = 1 if role in ('anonymous', 'nonmember') else 2
        assert [item['id'] for item in body] == sorted(table.columns[0].id for table in projected.tables[:count])
        assert sorted(item['val_max'] for item in body) == [1.5, 150][:count]
    if role in ('anonymous', 'nonmember'):
        assert projected.tables[1].id not in response.get_data(as_text=True)


@pytest.mark.parametrize('mismatch', ['concept', 'unit', 'range', 'missing_stats', 'split_columns', 'incompatible_base', 'unknown_unit', 'unknown_namespace'])
def test_unit_search_requires_one_matching_column(projected, credentials, requests_mock, mismatch):
    payload = projection_payload(projected, 'anonymous')
    column = payload['tables'][0]['columns'][0]
    column.update(concept_uri=CONCEPT, unit_uri=OM + 'metre', val_min=0, val_max=10)
    requested_unit = OM + 'metre'
    if mismatch == 'concept':
        column['concept_uri'] = 'https://example.org/other'
    elif mismatch == 'unit':
        column['unit_uri'] = OM + 'second-Time'
    elif mismatch == 'range':
        column['val_max'] = 11
    elif mismatch == 'missing_stats':
        column['val_min'] = None
    elif mismatch == 'incompatible_base':
        requested_unit = OM + 'one'
        column['unit_uri'] = OM + 'bit'
    elif mismatch == 'unknown_unit':
        column['unit_uri'] = OM + 'noSuchUnit'
    elif mismatch == 'unknown_namespace':
        column['unit_uri'] = 'https://example.org/metre'
    else:
        other = dict(column, id=str(uuid4()), concept_uri='https://example.org/other')
        column['val_max'] = 11
        payload['tables'][0]['columns'].append(other)
    requests_mock.get('http://metadata.invalid/api/v1/database/batch', json=[payload])
    with app.test_client() as client:
        response = client.post('/api/v1/search/column?t1=0&t2=10',
                               json={'concept.uri': CONCEPT, 'unit.uri': requested_unit})
    assert response.status_code == 200
    assert response.get_json() == []


@pytest.mark.parametrize('bounds', [(None, 1), (2, 1), ('nan', 1), (0, 'inf'), ('no', 10)])
def test_invalid_semantic_range_returns_bad_request(projected, bounds):
    with app.test_client() as client:
        response = client.post('/api/v1/search/column', json={'t1': bounds[0], 't2': bounds[1],
                               'concept.uri': CONCEPT, 'unit.uri': OM + 'metre'})
    assert response.status_code == 400


@pytest.mark.parametrize('requested,actual,lower,upper,minimum,maximum', [
    ('degreeCelsius', 'kelvin', 0, 100, 273.15, 373.15),
    ('kelvin', 'degreeCelsius', 273.15, 373.15, 0, 100),
    ('degreeFahrenheit', 'degreeCelsius', 32, 212, 0, 100),
    ('degreeCelsius', 'degreeFahrenheit', 0, 100, 32, 212),
    ('degreeCelsius', 'degreeCelsius', -20, 0, -20, 0),
    ('metre', 'centimetre', 0, 2, 0, 200),
    ('degreeFahrenheit', 'degreeCelsius', -40, 32, -40, 0),
    ('kelvin', 'degreeCelsius', 273.25, 274.15, 0.1, 1),
    ('metre', 'centimetre', 0.145, 0.291, 14.5, 29.1),
])
def test_converted_ranges_include_expected_values(projected, credentials, requests_mock,
                                                 requested, actual, lower, upper, minimum, maximum):
    payload = projection_payload(projected, 'anonymous')
    column = payload['tables'][0]['columns'][0]
    column.update(concept_uri=CONCEPT, unit_uri=OM + actual, val_min=minimum, val_max=maximum)
    requests_mock.get('http://metadata.invalid/api/v1/database/batch', json=[payload])
    with app.test_client() as client:
        response = client.post('/api/v1/search/column', json={'t1': lower, 't2': upper,
                               'concept.uri': CONCEPT, 'unit.uri': OM + requested})
    assert response.status_code == 200
    assert [item['id'] for item in response.get_json()] == [column['id']]
    assert response.get_json()[0]['val_min'] == minimum


@pytest.mark.parametrize('field', ['concept', 'unit'])
@pytest.mark.parametrize('storage', ['scalar', 'legacy'])
def test_semantic_facets_match_actual_index_fields(projected, credentials, requests_mock, session, field, storage):
    column = projected.tables[0].columns[0]
    uri = CONCEPT if field == 'concept' else OM + 'metre'
    if storage == 'scalar':
        setattr(column, field, None)
        setattr(column, f'{field}_uri', uri)
    else:
        getattr(column, field).uri = uri
    projected.tables = projected.tables[:1]
    SearchServiceClient().save_database(projected.id, projected)
    SearchServiceClient()._instance().indices.refresh(index='database')
    requests_mock.get('http://metadata.invalid/api/v1/database/batch', json=[projected.model_dump(mode='json')])
    with app.test_client() as client:
        response = client.post(f'/api/v1/search/{field}', json={f'tables.columns.{field}.uri': uri})
    assert response.status_code == 200
    assert [item['uri'] for item in response.get_json()] == [uri]


@pytest.mark.parametrize('minimum,maximum', [(31.999, 212), (32, 212.001)])
def test_temperature_range_excludes_values_outside_bounds(projected, credentials, requests_mock, minimum, maximum):
    payload = projection_payload(projected, 'anonymous')
    column = payload['tables'][0]['columns'][0]
    column.update(concept_uri=CONCEPT, unit_uri=OM + 'degreeFahrenheit', val_min=minimum, val_max=maximum)
    requests_mock.get('http://metadata.invalid/api/v1/database/batch', json=[payload])
    with app.test_client() as client:
        response = client.post('/api/v1/search/column', json={'t1': 0, 't2': 100,
                               'tables.columns.concept_uri': CONCEPT,
                               'tables.columns.unit_uri': OM + 'degreeCelsius'})
    assert response.status_code == 200
    assert response.get_json() == []

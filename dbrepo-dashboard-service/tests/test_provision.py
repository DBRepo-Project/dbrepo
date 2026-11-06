import json
from pathlib import Path
from uuid import uuid4

import pytest

from dbrepo.core.client.dashboard import DashboardServiceClient
from provision import GrafanaSetup, export_system_dashboards, configure_oauth

TEMPLATES = Path(__file__).resolve().parents[2] / 'dbrepo-dashboard-ui' / 'dashboards' / 'System'
URLS = {'dbrepojson0': 'http://backend.invalid', 'dbrepometrics0': 'http://metric-db:9090',
        'dbrepoopensearch0': 'http://search-db:9200'}


@pytest.fixture
def setup(session):
    admin = GrafanaSetup(session.get_url(), 'admin', 'admin')
    research = admin.request('POST', '/orgs', {'name': 'research-' + uuid4().hex})['orgId']
    result = GrafanaSetup(session.get_url(), 'admin', 'admin', research)
    result.monitor_name = 'monitor-' + uuid4().hex
    yield result
    monitor = admin.request('GET', '/orgs/name/' + result.monitor_name, missing=True)
    if monitor:
        admin.request('DELETE', '/orgs/' + str(monitor['id']))
    admin.request('DELETE', '/orgs/' + str(research))


def configure(setup, **kwargs):
    return setup.configure(URLS['dbrepojson0'], setup.monitor_name, dashboards_dir=TEMPLATES, **kwargs)


def source(setup, uid='operator-source', url='http://operator.invalid', name='Operator source'):
    setup.request('POST', '/datasources', {'uid': uid, 'name': name, 'type': 'prometheus', 'access': 'proxy', 'url': url})


def test_native_setup_and_org_scope_preserve_unrelated_resources(setup):
    source(setup)
    monitor = configure(setup)
    research = {item['uid']: item for item in setup.request('GET', '/datasources')}
    assert set(research) == {'operator-source', 'dbrepojson0'}
    assert research['dbrepojson0']['basicAuth'] is False
    assert research['dbrepojson0'].get('basicAuthUser', '') == ''
    assert research['dbrepojson0']['jsonData']['oauthPassThru'] is True
    assert research['dbrepojson0']['jsonData']['auth_method'] == 'oauthPassThru'
    detail = setup.request('GET', '/datasources/uid/dbrepojson0')
    assert not detail['secureJsonFields'].get('basicAuthPassword', False)
    assert {item['uid'] for item in setup.request('GET', '/datasources', org=monitor)} == {
        'dbrepometrics0', 'dbrepoopensearch0'}
    assert len(setup.request('GET', '/search?type=dash-db', org=monitor)) == 4
    first = setup.request('GET', '/search?type=dash-db', org=monitor)[0]
    edited = setup.request('GET', '/dashboards/uid/' + first['uid'], org=monitor)['dashboard']
    edited['panels'].append({'id': 98765, 'type': 'text', 'title': 'Later operator edit'})
    setup.request('POST', '/dashboards/db', {'dashboard': edited, 'folderUid': 'dbrepo-system', 'overwrite': True}, org=monitor)
    assert configure(setup) == monitor
    assert setup.request('GET', '/dashboards/uid/' + first['uid'], org=monitor)['dashboard']['panels'][-1]['id'] == 98765
    setup.request('POST', f'/user/using/{monitor}')
    try:
        client = DashboardServiceClient(setup.endpoint + '/', 'admin', 'admin', organization_id=setup.research_id)
        created = client.get_client().dashboard.update_dashboard({'dashboard': {'title': 'research-only',
                    'uid': 'r-' + uuid4().hex, 'panels': []}, 'overwrite': False})
        assert setup.request('GET', '/dashboards/uid/' + created['uid'])['dashboard']['title'] == 'research-only'
        assert setup.request('GET', '/dashboards/uid/' + created['uid'], org=monitor, missing=True) is None
        assert client.generic_get('/api/org').json()['id'] == setup.research_id
        client.generic_post('/api/dashboards/db', {'dashboard': {'title': 'generic-research', 'panels': []}, 'overwrite': False})
        assert len(setup.request('GET', '/search?type=dash-db')) == 2
    finally:
        setup.request('POST', '/user/using/1')


@pytest.mark.parametrize('kind', ['backend', 'name', 'folder', 'member'])
def test_preflight_conflicts_leave_sources_unchanged(setup, kind):
    monitor = setup.request('POST', '/orgs', {'name': setup.monitor_name})['orgId']
    if kind == 'backend':
        source(setup, 'dbrepometrics0', 'http://operator.invalid', 'dbrepo-metrics')
    elif kind == 'name':
        source(setup, 'operator-owned', 'http://operator.invalid', 'dbrepo-metrics')
    elif kind == 'folder':
        setup.request('POST', '/folders', {'uid': 'dbrepo-system', 'title': 'Operator folder'}, org=monitor)
    else:
        username = 'org-admin-' + uuid4().hex
        setup.request('POST', '/admin/users', {'login': username, 'email': username + '@test.invalid', 'password': 'synthetic-password'})
        setup.request('POST', f'/orgs/{monitor}/users', {'loginOrEmail': username, 'role': 'Admin'})
    before = setup.request('GET', '/datasources')
    with pytest.raises(ValueError):
        configure(setup)
    assert setup.request('GET', '/datasources') == before
    assert setup.request('GET', '/datasources', org=monitor) == []


def test_export_preserves_edited_panels_and_only_verified_source_cleanup(setup, tmp_path):
    source(setup, 'dbrepometrics0', URLS['dbrepometrics0'], 'dbrepo-metrics')
    source(setup)
    template = json.loads(next(TEMPLATES.glob('*.json')).read_text())
    template['id'] = None
    template['panels'].append({'id': 98765, 'type': 'text', 'title': 'Operator addition'})
    folder = setup.request('POST', '/folders', {'uid': 'legacy-system', 'title': 'System'})
    setup.request('POST', '/dashboards/db', {'dashboard': template, 'folderUid': folder['uid'], 'overwrite': True})
    target = tmp_path / 'backup.json'
    export_system_dashboards(setup, TEMPLATES, target, URLS)
    backup = json.loads(target.read_text())
    assert backup['research_org_id'] == setup.research_id
    assert backup['dashboards'][0]['panels'][-1]['id'] == 98765
    cleanup = json.loads(Path(str(target) + '.datasources.yaml').read_text())
    assert cleanup == {'apiVersion': 1, 'deleteDatasources': [{'name': 'dbrepo-metrics', 'orgId': setup.research_id}]}
    assert target.stat().st_mode & 0o777 == 0o600
    with pytest.raises(ValueError):
        export_system_dashboards(setup, TEMPLATES, target, URLS)
    with pytest.raises(ValueError, match='export required'):
        configure(setup)
    monitor = configure(setup, migration_export=target)
    moved = setup.request('GET', '/dashboards/uid/' + template['uid'], org=monitor)
    assert moved['dashboard']['panels'][-1]['id'] == 98765
    assert setup.request('GET', '/dashboards/uid/' + template['uid'], missing=True) is None
    assert setup.request('GET', '/datasources/uid/operator-source')['url'] == 'http://operator.invalid'


@pytest.mark.parametrize('endpoint', ['http://u:p@example.invalid', 'http://@example.invalid',
                                      'http://example.invalid?x=1', 'http://example.invalid/#x'])
def test_client_rejects_ambiguous_endpoint(endpoint):
    with pytest.raises(ValueError):
        DashboardServiceClient(endpoint, 'admin', 'synthetic')


@pytest.mark.parametrize('organization', [0, -1, True, '1'])
def test_client_rejects_invalid_org(organization):
    with pytest.raises(ValueError):
        DashboardServiceClient('http://example.invalid/dashboard/', 'admin', 'synthetic', organization_id=organization)


def test_oauth_setup_has_exact_callback_pkce_and_dedicated_secret(requests_mock):
    endpoint = 'http://keycloak.invalid'
    client_url = endpoint + '/admin/realms/dbrepo/clients'
    requests_mock.post(endpoint + '/realms/master/protocol/openid-connect/token', json={'access_token': 'synthetic'})
    requests_mock.get(client_url, json=[])
    create = requests_mock.post(client_url, status_code=201)
    configure_oauth(endpoint, 'admin', 'synthetic', 'dbrepo-grafana', 'a' * 48,
                    'https://public.invalid', dashboard_url='https://public.invalid/dashboard/')
    body = create.last_request.json()
    assert body['redirectUris'] == ['https://public.invalid/dashboard/login/generic_oauth']
    assert body['webOrigins'] == ['https://public.invalid']
    assert body['secret'] == 'a' * 48
    assert body['defaultClientScopes'] == ['basic', 'profile', 'email', 'roles']
    assert body['attributes']['pkce.code.challenge.method'] == 'S256'
    assert body['directAccessGrantsEnabled'] is False
    assert body['implicitFlowEnabled'] is False
    assert body['serviceAccountsEnabled'] is False
    requests_mock.get(client_url, json=[{'id': 'other', 'clientId': 'dbrepo-grafana'}])
    with pytest.raises(ValueError, match='unmanaged'):
        configure_oauth(endpoint, 'admin', 'synthetic', 'dbrepo-grafana', 'a' * 48, 'https://public.invalid')
    assert create.call_count == 1


@pytest.mark.parametrize('prefix', ['', '/dashboard', '/dashboard/'])
def test_sdk_and_generic_client_keep_endpoint_prefix(requests_mock, prefix):
    expected = 'http://grafana.invalid' + prefix.rstrip('/') + '/api/search'
    response = requests_mock.get(expected, json=[])
    client = DashboardServiceClient('http://grafana.invalid' + prefix, 'admin', 'synthetic', organization_id=3)
    assert client.get_client().client.url + '/search' == expected
    assert client.get_client().client.s.headers['X-Grafana-Org-Id'] == '3'
    assert client.generic_get('/api/search').json() == []
    assert response.last_request.headers['X-Grafana-Org-Id'] == '3'

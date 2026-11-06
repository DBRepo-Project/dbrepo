"""One-shot setup for the configured research and administrator-only monitoring organizations.

Run with public Grafana traffic and DBRepo permission mutations paused. Restart every
Grafana replica after migrating old datasources before reopening traffic.
"""
import json
import os
from pathlib import Path
from urllib.parse import quote, urlsplit

import requests


MONITORING = {
    'dbrepometrics0': ('dbrepo-metrics', 'prometheus'),
    'dbrepoopensearch0': ('dbrepo-opensearch', 'grafana-opensearch-datasource'),
}
RESEARCH = {'dbrepojson0': ('dbrepo-json', 'yesoreyeram-infinity-datasource')}


def http_url(value):
    url = urlsplit(value)
    if (url.scheme not in ('http', 'https') or not url.hostname or url.username is not None or
            url.password is not None or url.query or url.fragment):
        raise ValueError('An HTTP URL without credentials, query or fragment is required')
    return value.rstrip('/')


class GrafanaSetup:
    def __init__(self, endpoint, username, password, research_id=1):
        self.endpoint = http_url(endpoint)
        if type(research_id) is not int or research_id < 1:
            raise ValueError('A positive research organization ID is required')
        self.research_id = research_id
        self.session = requests.Session()
        self.session.trust_env = False
        self.session.auth = (username, password)

    def request(self, method, path, body=None, org=None, missing=False):
        response = self.session.request(method, self.endpoint + '/api' + path, json=body,
                                        headers={'X-Grafana-Org-Id': str(org or self.research_id)}, timeout=(3, 15))
        if missing and response.status_code == 404:
            return None
        response.raise_for_status()
        return response.json()

    def monitoring_org(self, name, configured_id=None):
        if configured_id is not None and (type(configured_id) is not int or configured_id < 1):
            raise ValueError('A positive monitoring organization ID is required')
        # Resolve without changing the administrator's mutable active organization.
        org = self.request('GET', f'/orgs/{configured_id}' if configured_id else '/orgs/name/' + quote(name, safe=''),
                           missing=configured_id is None)
        if org is None:
            return None
        if org['id'] == self.research_id or org['name'] != name:
            raise ValueError('Monitoring organization identity does not match configuration')
        for member in self.request('GET', f'/orgs/{org["id"]}/users'):
            user = self.request('GET', f'/users/{member["userId"]}')
            if user.get('isGrafanaAdmin') is not True:
                raise ValueError('Monitoring organization contains a non-server-administrator; operator review required')
        return org['id']

    def check_sources(self, org, expected, urls):
        sources = {item['uid']: item for item in self.request('GET', '/datasources', org=org)}
        expected_names = {name: uid for uid, (name, _) in expected.items()}
        for item in sources.values():
            if item.get('name') in expected_names and expected_names[item['name']] != item['uid']:
                raise ValueError('Managed datasource name belongs to another UID; no resources changed')
        for uid, (name, kind) in expected.items():
            item = sources.get(uid)
            if item and (item.get('type') != kind or item.get('name') != name or item.get('orgId') != org or
                         item.get('url', '').rstrip('/') != urls[uid]):
                raise ValueError(f'Datasource identity conflict for {uid}; no resources changed')
        return sources

    def put_source(self, org, payload, existing):
        uid = payload['uid']
        self.request('PUT' if uid in existing else 'POST', '/datasources/uid/' + uid if uid in existing else '/datasources',
                     payload, org=org)

    def configure(self, public_base, monitoring_name, monitoring_id=None, metrics_url='http://metric-db:9090',
                  search_url='http://search-db:9200', dashboards_dir=None, migration_export=None):
        public_base = http_url(public_base)
        metrics_url, search_url = http_url(metrics_url), http_url(search_url)
        research = self.request('GET', f'/orgs/{self.research_id}')
        if research.get('id') != self.research_id:
            raise ValueError('Research organization does not match configuration')
        monitor = self.monitoring_org(monitoring_name, monitoring_id)
        urls = {'dbrepojson0': public_base, 'dbrepometrics0': metrics_url, 'dbrepoopensearch0': search_url}
        original = self.check_sources(self.research_id, RESEARCH | MONITORING, urls)
        destination = self.check_sources(monitor, MONITORING, urls) if monitor else {}
        folder = self.request('GET', '/folders/dbrepo-system', org=monitor, missing=True) if monitor else None
        if folder and (folder.get('title') != 'System' or folder.get('parentUid')):
            raise ValueError('Monitoring System folder conflict; no resources changed')
        dashboards = [json.loads(path.read_text()) for path in sorted(Path(dashboards_dir).glob('*.json'))] if dashboards_dir else []
        if not dashboards:
            raise ValueError('System dashboard templates are missing')
        if any(uid in original for uid in MONITORING) and not migration_export:
            raise ValueError('Export existing System dashboards before removing legacy provisioning; migration export required')
        if any(item.get('readOnly') for uid, item in original.items() if uid in RESEARCH | MONITORING):
            raise ValueError('Legacy datasources are read-only; export and apply the one-shot deleteDatasources maintenance transition first')
        if migration_export:
            exported = json.loads(Path(migration_export).read_text())
            if exported.get('research_org_id') != self.research_id or not isinstance(exported.get('dashboards'), list):
                raise ValueError('Migration export does not match the research organization')
            originals = {item['uid']: item for item in dashboards}
            seen = set()
            for item in exported['dashboards']:
                uid = item.get('uid')
                if uid not in originals or uid in seen or item.get('title') != originals[uid]['title']:
                    raise ValueError('Unexpected dashboard identity in migration export')
                seen.add(uid)
                originals[uid] = item
            dashboards = list(originals.values())
        retained = {}
        # A known UID in an unexpected folder/title is not ours to overwrite or delete.
        for dashboard in dashboards:
            for org in [self.research_id] + ([monitor] if monitor else []):
                found = self.request('GET', '/dashboards/uid/' + dashboard['uid'], org=org, missing=True)
                if found and (found['dashboard']['title'] != dashboard['title'] or
                              found['meta'].get('folderTitle') != 'System'):
                    raise ValueError(f'Dashboard identity conflict for {dashboard["uid"]}; operator review required')
                if found and org == monitor:
                    if found['meta'].get('folderUid') != 'dbrepo-system':
                        raise ValueError('Monitoring dashboard belongs to an unexpected folder; operator review required')
                    retained[dashboard['uid']] = found['dashboard']
        if monitor is None:
            monitor = self.request('POST', '/orgs', {'name': monitoring_name})['orgId']
            # Re-read membership before populating a newly allocated organization.
            if self.monitoring_org(monitoring_name, monitor) != monitor:
                raise ValueError('New monitoring organization could not be verified')
        # Explicitly clear the old Basic fields as well as changing the authentication method.
        self.put_source(self.research_id, {
            'uid': 'dbrepojson0', 'name': 'dbrepo-json', 'type': 'yesoreyeram-infinity-datasource',
            'access': 'proxy', 'url': public_base, 'basicAuth': False, 'basicAuthUser': '',
            'jsonData': {'auth_method': 'oauthPassThru', 'oauthPassThru': True,
                         'httpHeaderName1': 'Accept', 'allowedHosts': [public_base]},
            'secureJsonData': {'basicAuthPassword': '', 'httpHeaderValue1': 'application/json'},
        }, original)
        self.put_source(monitor, {'uid': 'dbrepometrics0', 'name': 'dbrepo-metrics', 'type': 'prometheus',
                                  'access': 'proxy', 'url': metrics_url}, destination)
        self.put_source(monitor, {'uid': 'dbrepoopensearch0', 'name': 'dbrepo-opensearch',
                                  'type': 'grafana-opensearch-datasource', 'access': 'proxy', 'url': search_url,
                                  'basicAuth': False, 'jsonData': {'flavor': 'opensearch', 'logLevelField': 'level',
                                  'logMessageField': 'message', 'timeField': '@timestamp', 'version': '2.18.0'}}, destination)
        if dashboards:
            if folder is None:
                self.request('POST', '/folders', {'uid': 'dbrepo-system', 'title': 'System'}, org=monitor)
            for dashboard in dashboards:
                if dashboard['uid'] not in retained:
                    dashboard['id'] = None
                    self.request('POST', '/dashboards/db', {'dashboard': dashboard, 'folderUid': 'dbrepo-system',
                                                           'overwrite': False}, org=monitor)
                old = self.request('GET', '/dashboards/uid/' + dashboard['uid'], missing=True)
                if old:
                    self.request('DELETE', '/dashboards/uid/' + dashboard['uid'])
        for uid in MONITORING:
            if uid in original:
                self.request('DELETE', '/datasources/uid/' + uid)
        return monitor


def configure_oauth(endpoint, admin, password, client_id, secret, public_base, realm='dbrepo', dashboard_url=None):
    if not secret or len(secret) < 32:
        raise ValueError('Configure a dedicated random DASHBOARD_OAUTH_SECRET of at least 32 characters')
    callback_base = http_url(dashboard_url or http_url(public_base) + '/dashboard')
    callback = callback_base + '/login/generic_oauth'
    origin = urlsplit(callback_base)
    origin = origin.scheme + '://' + origin.netloc
    endpoint = http_url(endpoint)
    session = requests.Session()
    session.trust_env = False
    token = session.post(endpoint + '/realms/master/protocol/openid-connect/token',
                         data={'client_id': 'admin-cli', 'grant_type': 'password', 'username': admin, 'password': password},
                         timeout=(3, 15))
    token.raise_for_status()
    session.headers['Authorization'] = 'Bearer ' + token.json()['access_token']
    path = endpoint + '/admin/realms/' + quote(realm, safe='') + '/clients'
    found = session.get(path, params={'clientId': client_id}, timeout=(3, 15))
    found.raise_for_status()
    clients = found.json()
    if len(clients) > 1 or clients and (clients[0].get('clientId') != client_id or
                                     clients[0].get('attributes', {}).get('dbrepo.managed') != 'grafana'):
        raise ValueError('OAuth client ID is already used by an unmanaged client; operator review required')
    config = {'clientId': client_id, 'enabled': True, 'protocol': 'openid-connect', 'publicClient': False,
              'clientAuthenticatorType': 'client-secret', 'secret': secret, 'standardFlowEnabled': True,
              'implicitFlowEnabled': False, 'directAccessGrantsEnabled': False, 'serviceAccountsEnabled': False,
              'redirectUris': [callback], 'webOrigins': [origin], 'defaultClientScopes': ['basic', 'profile', 'email', 'roles'],
              'attributes': {'pkce.code.challenge.method': 'S256', 'dbrepo.managed': 'grafana'}}
    response = session.put(path + '/' + clients[0]['id'], json=config, timeout=(3, 15)) if clients else \
        session.post(path, json=config, timeout=(3, 15))
    response.raise_for_status()


def configure_environment():
    public_base = os.environ['BASE_URL']
    configure_oauth(os.environ['AUTH_SERVICE_ENDPOINT'], os.environ['AUTH_SERVICE_ADMIN'],
                    os.environ['AUTH_SERVICE_ADMIN_PASSWORD'], os.getenv('DASHBOARD_OAUTH_CLIENT_ID', 'dbrepo-grafana'),
                    os.environ['DASHBOARD_OAUTH_SECRET'], public_base, dashboard_url=os.getenv('DASHBOARD_PUBLIC_URL'))
    setup = GrafanaSetup(os.environ['DASHBOARD_UI_ENDPOINT'], os.environ['DASHBOARD_UI_USERNAME'],
                         os.environ['DASHBOARD_UI_PASSWORD'], int(os.getenv('DASHBOARD_ORG_ID', '1')))
    research = setup.request('GET', f'/orgs/{setup.research_id}')
    if research.get('name') != os.getenv('DASHBOARD_RESEARCH_ORG_NAME', 'Main Org.'):
        raise ValueError('Anonymous research organization name does not match the configured ID')
    monitor = os.getenv('DASHBOARD_MONITORING_ORG_ID')
    return setup.configure(os.getenv('DASHBOARD_DATA_ENDPOINT', public_base), os.getenv('DASHBOARD_MONITORING_ORG_NAME', 'DBRepo Monitoring'),
                           int(monitor) if monitor else None, os.getenv('DASHBOARD_METRICS_URL', 'http://metric-db:9090'),
                           os.getenv('DASHBOARD_SEARCH_URL', 'http://search-db:9200'),
                           os.getenv('DASHBOARD_SYSTEM_DASHBOARDS', '/app/system-dashboards'),
                           os.getenv('DASHBOARD_MIGRATION_EXPORT') or None)


def export_system_dashboards(setup, dashboards_dir, target, urls):
    """Save current managed dashboard JSON before removing its file provider."""
    templates = [json.loads(path.read_text()) for path in sorted(Path(dashboards_dir).glob('*.json'))]
    if not templates:
        raise ValueError('System dashboard templates are missing')
    known = {item['uid'] for item in templates}
    page = 1
    while True:
        results = setup.request('GET', f'/search?type=dash-db&limit=1000&page={page}')
        for item in results:
            if item.get('folderTitle') == 'System' and item['uid'] not in known:
                raise ValueError('Additional System dashboard found; move UI-created dashboards to another folder or export and migrate extra provider dashboards before removing the provider')
        if len(results) < 1000:
            break
        page += 1
    sources = setup.check_sources(setup.research_id, RESEARCH | MONITORING, urls)
    cleanup_target = str(target) + '.datasources.yaml'
    if Path(target).exists() or Path(cleanup_target).exists():
        raise ValueError('Export or cleanup file already exists; choose a new backup path')
    dashboards = []
    for template in templates:
        found = setup.request('GET', '/dashboards/uid/' + template['uid'], missing=True)
        if found:
            if found['dashboard']['title'] != template['title'] or found['meta'].get('folderTitle') != 'System':
                raise ValueError('Existing System dashboard identity conflict; operator review required')
            dashboards.append(found['dashboard'])
    # Do not overwrite an earlier backup. It can contain operator-supplied panel settings.
    with open(target, 'x', opener=lambda path, flags: os.open(path, flags, 0o600)) as output:
        json.dump({'research_org_id': setup.research_id, 'dashboards': dashboards}, output, indent=2)
    # JSON is valid YAML. Grafana processes this one-shot file before adding datasources.
    with open(cleanup_target, 'x', opener=lambda path, flags: os.open(path, flags, 0o600)) as output:
        json.dump({'apiVersion': 1, 'deleteDatasources': [
            {'name': sources[uid]['name'], 'orgId': setup.research_id}
            for uid in RESEARCH | MONITORING if uid in sources]}, output, indent=2)


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser(description='Export System dashboards before a protected Grafana upgrade')
    parser.add_argument('operation', choices=['export'])
    parser.add_argument('target')
    args = parser.parse_args()
    setup = GrafanaSetup(os.environ['DASHBOARD_UI_ENDPOINT'], os.environ['DASHBOARD_UI_USERNAME'],
                         os.environ['DASHBOARD_UI_PASSWORD'], int(os.getenv('DASHBOARD_ORG_ID', '1')))
    export_system_dashboards(setup, os.getenv('DASHBOARD_SYSTEM_DASHBOARDS', '/app/system-dashboards'), args.target,
                             {'dbrepojson0': http_url(os.getenv('DASHBOARD_LEGACY_DATA_ENDPOINT', os.environ['BASE_URL'])),
                              'dbrepometrics0': http_url(os.getenv('DASHBOARD_METRICS_URL', 'http://metric-db:9090')),
                              'dbrepoopensearch0': http_url(os.getenv('DASHBOARD_SEARCH_URL', 'http://search-db:9200'))})

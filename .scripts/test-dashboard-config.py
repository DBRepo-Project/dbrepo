#!/usr/bin/env python3
"""Render caller-authentication settings without using deployment secrets."""
import json
import os
from pathlib import Path
import subprocess
import yaml

ROOT = Path(__file__).resolve().parents[1]
ENV = {key: os.environ[key] for key in ('PATH', 'HOME') if key in os.environ}
ENV.update(BASE_URL='https://research.example', DASHBOARD_OAUTH_SECRET='synthetic-dedicated-' + 'x' * 40,
           AUTH_SERVICE_ADMIN='synthetic-admin', AUTH_SERVICE_ADMIN_PASSWORD='synthetic-password')
COMPOSE = ('docker-compose.yml', '.docker/docker-compose.yml',
           '.live/config/includes.chroot_after_packages/etc/skel/docker-compose.yml')


def render_helm(*values):
    result = subprocess.run(['helm', 'template', 'audit', str(ROOT / 'helm/dbrepo'), *values],
                            capture_output=True, text=True, env=ENV)
    return result


def check():
    for file in COMPOSE:
        output = subprocess.run(['docker', 'compose', '--env-file', '/dev/null', '-f', str(ROOT / file),
                                 'config', '--format', 'json'], capture_output=True, text=True, check=True, env=ENV)
        services = json.loads(output.stdout)['services']
        grafana = services['dbrepo-dashboard-ui']['environment']
        setup = services['dbrepo-dashboard-service-init']['environment']
        runtime = services['dbrepo-dashboard-service']['environment']
        assert setup['DASHBOARD_DATA_ENDPOINT'] == 'http://gateway-service:8080'
        assert grafana['GF_SERVER_ROOT_URL'] == 'https://research.example/dashboard/'
        assert grafana['GF_AUTH_GENERIC_OAUTH_AUTH_URL'].startswith('https://research.example/realms/')
        assert grafana['GF_AUTH_GENERIC_OAUTH_CLIENT_SECRET'] == setup['DASHBOARD_OAUTH_SECRET'] == ENV['DASHBOARD_OAUTH_SECRET']
        assert grafana['GF_AUTH_GENERIC_OAUTH_USE_PKCE'] == 'true'
        assert grafana['GF_AUTH_GENERIC_OAUTH_USE_REFRESH_TOKEN'] == 'true'
        assert grafana['GF_AUTH_GENERIC_OAUTH_ALLOW_ASSIGN_GRAFANA_ADMIN'] == 'false'
        assert grafana['GF_AUTH_OAUTH_ALLOW_INSECURE_EMAIL_LOOKUP'] == 'false'
        assert grafana['GF_USERS_AUTO_ASSIGN_ORG_ID'] == runtime['DASHBOARD_ORG_ID'] == setup['DASHBOARD_ORG_ID']
        assert 'AUTH_SERVICE_ADMIN_PASSWORD' not in runtime and 'DASHBOARD_OAUTH_SECRET' not in runtime
        assert 'AUTH_SERVICE_ADMIN_PASSWORD' in setup
    rendered = render_helm()
    assert rendered.returncode == 0, rendered.stderr
    docs = [item for item in yaml.safe_load_all(rendered.stdout) if item]
    secret = next(item['stringData'] for item in docs if item['kind'] == 'Secret' and item['metadata']['name'] == 'dashboard-ui-secret')
    assert len(secret['GF_AUTH_GENERIC_OAUTH_CLIENT_SECRET']) == 64
    assert secret['GF_AUTH_GENERIC_OAUTH_CLIENT_ID'] == 'dbrepo-grafana'
    assert secret['GF_AUTH_GENERIC_OAUTH_USE_REFRESH_TOKEN'] == 'true'
    job = next(item for item in docs if item['kind'] == 'Job' and item['metadata']['name'].startswith('dashboard-service-setup-job'))
    env = {item['name']: item for item in job['spec']['template']['spec']['containers'][0]['env']}
    assert env['DASHBOARD_DATA_ENDPOINT']['value'] == 'http://gateway-service'
    assert env['DASHBOARD_OAUTH_SECRET']['valueFrom']['secretKeyRef'] == {
        'name': 'dashboard-ui-secret', 'key': 'GF_AUTH_GENERIC_OAUTH_CLIENT_SECRET'}
    for bundled in ['authservice', 'dashboardui']:
        rejected = render_helm('--set', bundled + '.enabled=false')
        assert rejected.returncode != 0 and 'Dashboard setup requires bundled' in rejected.stderr
        explicit = render_helm('--set', bundled + '.enabled=false', '--set', 'dashboardservice.setupJob.enabled=false')
        assert explicit.returncode == 0, explicit.stderr
        assert 'name: dashboard-service-setup-job-' not in explicit.stdout
    supplied = render_helm('--set-string', 'dashboardui.oauthClientSecret=synthetic-explicit-secret',
                          '--set', 'dashboardui.migrationExportSecret=migration-backup')
    assert supplied.returncode == 0, supplied.stderr
    supplied_docs = list(yaml.safe_load_all(supplied.stdout))
    configured = next(item for item in supplied_docs if item and item.get('kind') == 'Secret' and item['metadata']['name'] == 'dashboard-ui-secret')
    assert configured['stringData']['GF_AUTH_GENERIC_OAUTH_CLIENT_SECRET'] == 'synthetic-explicit-secret'
    assert 'secretName: "migration-backup"' in supplied.stdout
    for directory in ['dbrepo-dashboard-ui/provisioning', '.live/config/includes.chroot_after_packages/etc/skel/config/provisioning']:
        for file in (ROOT / directory / 'datasources').glob('*.yaml'):
            assert yaml.safe_load(file.read_text())['datasources'] == []
        assert yaml.safe_load((ROOT / directory / 'dashboards/provider.yaml').read_text())['providers'] == []
    print('PASS: Compose and Helm caller-authentication, org scope, setup secrets and external-service prerequisites')


if __name__ == '__main__':
    check()

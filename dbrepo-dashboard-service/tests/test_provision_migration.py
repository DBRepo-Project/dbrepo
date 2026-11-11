"""Exercise a formerly file-provisioned upgrade using native Grafana APIs."""
import json
import shutil
import socket
import time
from pathlib import Path
import pytest
import requests
from testcontainers.core.container import DockerContainer
from provision import GrafanaSetup, export_system_dashboards
from tests.test_provision import TEMPLATES, URLS


@pytest.fixture
def legacy_grafana(tmp_path):
    provisioning = tmp_path / 'provisioning'
    (provisioning / 'datasources').mkdir(parents=True)
    (provisioning / 'dashboards').mkdir()
    assets = tmp_path / 'System'
    shutil.copytree(TEMPLATES, assets)
    sources = [{'name': name, 'uid': uid, 'type': kind, 'orgId': 1, 'url': URLS[uid], 'access': 'proxy',
                'basicAuth': uid == 'dbrepojson0', 'basicAuthUser': 'retired-synthetic',
                'secureJsonData': {'basicAuthPassword': 'retired-synthetic-password'}}
               for uid, name, kind in [('dbrepojson0', 'dbrepo-json', 'yesoreyeram-infinity-datasource'),
                                      ('dbrepometrics0', 'dbrepo-metrics', 'prometheus'),
                                      ('dbrepoopensearch0', 'dbrepo-opensearch', 'grafana-opensearch-datasource')]]
    (provisioning / 'datasources/legacy.yaml').write_text(json.dumps({'apiVersion': 1, 'datasources': sources}))
    (provisioning / 'dashboards/provider.yaml').write_text(json.dumps({'apiVersion': 1, 'providers': [
        {'name': 'System', 'orgId': 1, 'folder': 'System', 'type': 'file', 'disableDeletion': False,
         'allowUiUpdates': True, 'options': {'path': '/app/System'}}]}))
    with socket.socket() as reservation:
        reservation.bind(('127.0.0.1', 0))
        port = reservation.getsockname()[1]
    container = DockerContainer('grafana/grafana:11.6.16').with_bind_ports(3000, port)
    container.with_env('GF_PLUGINS_PREINSTALL_DISABLED', 'true').with_env('GF_LOG_LEVEL', 'warn')
    container.with_volume_mapping(str(provisioning), '/etc/grafana/provisioning', mode='ro')
    container.with_volume_mapping(str(assets), '/app/System', mode='ro')
    with container:
        setup = GrafanaSetup(f'http://{container.get_container_host_ip()}:{port}', 'admin', 'admin')
        wait(setup, lambda: len(setup.request('GET', '/datasources')) == 3 and
             len(setup.request('GET', '/search?type=dash-db')) == 4)
        yield container, setup, provisioning, assets


def wait(setup, ready=lambda: True):
    deadline = time.monotonic() + 60
    while time.monotonic() < deadline:
        try:
            if requests.get(setup.endpoint + '/api/health', timeout=2).status_code == 200 and ready():
                return
        except requests.RequestException:
            pass
        time.sleep(.2)
    raise AssertionError('Grafana did not become ready')


def test_extra_provider_dashboard_blocks_export_before_removal(legacy_grafana, tmp_path):
    container, setup, provisioning, assets = legacy_grafana
    (assets / 'operator-extra.json').write_text(json.dumps({'uid': 'operator-provisioned',
                                'title': 'Operator provisioned', 'panels': [], 'schemaVersion': 40}))
    container.get_wrapped_container().restart()
    wait(setup, lambda: setup.request('GET', '/dashboards/uid/operator-provisioned', missing=True) is not None)
    assert setup.request('GET', '/dashboards/uid/operator-provisioned')['dashboard']['title'] == 'Operator provisioned'
    target = tmp_path / 'backup.json'
    with pytest.raises(ValueError, match='Additional System'):
        export_system_dashboards(setup, TEMPLATES, target, URLS)
    assert not target.exists()
    assert not Path(str(target) + '.datasources.yaml').exists()
    assert len(setup.request('GET', '/datasources')) == 3


def test_cleanup_migration_preserves_edits_and_unrelated_system_dashboard(legacy_grafana, tmp_path):
    container, setup, provisioning, assets = legacy_grafana
    before = setup.request('GET', '/datasources')
    assert len(before) == 3 and all(item['readOnly'] for item in before)
    managed = setup.request('GET', '/dashboards/uid/keycloak-dashboard')
    managed['dashboard']['panels'].append({'id': 98765, 'type': 'text', 'title': 'Operator addition'})
    setup.request('POST', '/dashboards/db', {'dashboard': managed['dashboard'],
                  'folderId': managed['meta']['folderId'], 'overwrite': True})
    setup.request('POST', '/dashboards/db', {'dashboard': {'uid': 'operator-ui', 'title': 'Operator UI', 'panels': []},
                  'folderId': managed['meta']['folderId'], 'overwrite': False})
    setup.request('POST', '/datasources', {'uid': 'operator-source', 'name': 'Operator source',
                  'type': 'prometheus', 'url': 'http://operator.invalid', 'access': 'proxy'})
    target = tmp_path / 'backup.json'
    with pytest.raises(ValueError, match='Additional System'):
        export_system_dashboards(setup, TEMPLATES, target, URLS)
    # Operator resolves the ambiguous System-folder resource before destructive provider removal.
    folder = setup.request('POST', '/folders', {'uid': 'operator-folder', 'title': 'Operator folder'})
    operator_dashboard = setup.request('GET', '/dashboards/uid/operator-ui')['dashboard']
    setup.request('POST', '/dashboards/db', {'dashboard': operator_dashboard, 'folderUid': folder['uid'], 'overwrite': True})
    export_system_dashboards(setup, TEMPLATES, target, URLS)
    assert len(json.loads(target.read_text())['dashboards']) == 4
    (provisioning / 'datasources/legacy.yaml').write_text('apiVersion: 1\ndatasources: []\n')
    (provisioning / 'dashboards/provider.yaml').write_text('apiVersion: 1\nproviders: []\n')
    cleanup = provisioning / 'datasources/cleanup.yaml'
    shutil.copyfile(str(target) + '.datasources.yaml', cleanup)
    container.get_wrapped_container().restart()
    wait(setup, lambda: {item['uid'] for item in setup.request('GET', '/datasources')} == {'operator-source'})
    cleanup.unlink()
    assert {item['uid'] for item in setup.request('GET', '/datasources')} == {'operator-source'}
    assert setup.request('GET', '/dashboards/uid/operator-ui')['dashboard']['title'] == 'Operator UI'
    monitor = setup.configure(URLS['dbrepojson0'], 'DBRepo Monitoring', dashboards_dir=TEMPLATES,
                              migration_export=target)
    migrated = setup.request('GET', '/dashboards/uid/keycloak-dashboard', org=monitor)
    assert migrated['dashboard']['panels'][-1]['id'] == 98765
    migrated['dashboard']['panels'].append({'id': 98766, 'type': 'text', 'title': 'Later operator addition'})
    setup.request('POST', '/dashboards/db', {'dashboard': migrated['dashboard'], 'folderUid': 'dbrepo-system',
                                           'overwrite': True}, org=monitor)
    for export in [target, None]:
        assert setup.configure(URLS['dbrepojson0'], 'DBRepo Monitoring', dashboards_dir=TEMPLATES,
                               migration_export=export) == monitor
        assert setup.request('GET', '/dashboards/uid/keycloak-dashboard', org=monitor)['dashboard']['panels'][-1]['id'] == 98766
    container.get_wrapped_container().restart()
    wait(setup)
    assert {item['uid'] for item in setup.request('GET', '/datasources')} == {'operator-source', 'dbrepojson0'}
    assert {item['uid'] for item in setup.request('GET', '/datasources', org=monitor)} == {'dbrepometrics0', 'dbrepoopensearch0'}
    assert setup.request('GET', '/dashboards/uid/operator-ui')['dashboard']['title'] == 'Operator UI'

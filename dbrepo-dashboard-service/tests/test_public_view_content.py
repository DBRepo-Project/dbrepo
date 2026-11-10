from concurrent.futures import ThreadPoolExecutor
from threading import Event
from unittest.mock import patch
from uuid import uuid4

import pytest
from grafana_client.client import GrafanaClientError
from dbrepo.api.dto import ColumnType, View, ViewColumn
from dbrepo.core.client.dashboard import DashboardServiceClient
from tests.test_permissions import create, database, effective, user, headers, grafana
from app import app


def content_database(uid, owner):
    value = database(uid, owner, enabled=True)
    value.views = []
    for public, schema_public in ((False, False), (True, False), (False, True)):
        marker = ('public' if public or schema_public else 'private') + '-marker-' + uuid4().hex
        value.views.append(View(id=str(uuid4()), database_id=value.id, owner=value.owner,
            name=marker, internal_name=marker, query='SELECT synthetic', query_hash='synthetic',
            is_public=public, is_schema_public=schema_public, initial_view=False,
            columns=[ViewColumn(id=str(uuid4()), database_id=value.id, ord=0,
                name=marker+'-column', internal_name=marker+'-column', type=ColumnType.INT,
                is_null_allowed=True)]))
    return value


def assert_projection(uid, body, public):
    response = effective(uid)
    assert response.status_code == (200 if public else 403)
    if public:
        assert body.views[0].name not in response.text
        assert body.views[0].columns[0].name not in response.text
        for view in body.views[1:]:
            assert view.name in response.text
            assert view.columns[0].name in response.text


@pytest.mark.parametrize('schema_only', [False, True])
def test_public_content_filters_private_views_and_keeps_private_dashboard(headers, grafana, schema_only):
    owner = user()
    with app.test_client() as client:
        uid = create(client, headers, owner['login'])
        body = content_database(uid, owner['login'])
        grafana.update(body)
        private = effective(uid, owner)
        assert private.status_code == 200 and body.views[0].name in private.text
        body.is_public = not schema_only
        body.is_schema_public = schema_only
        grafana.update(body)
        assert_projection(uid, body, True)


def test_late_private_content_snapshot_cannot_restore_private_panels(headers, grafana):
    owner = user()
    with app.test_client() as client:
        uid = create(client, headers, owner['login'])
        body = content_database(uid, owner['login'])
        grafana.update(body)
        stale = body.model_copy(deep=True)
        body.is_public = True
        grafana.update(body)
        assert_projection(uid, body, True)
        response = client.put(f'/api/v1/dashboard/{uid}/content', headers=headers,
                              json=stale.model_dump())
        assert response.status_code == 202
        assert_projection(uid, body, True)


def test_disabled_publication_removes_generated_private_content(headers, grafana):
    owner = user()
    with app.test_client() as client:
        uid = create(client, headers, owner['login'])
        body = content_database(uid, owner['login'])
        grafana.update(body)
        dashboard = grafana.find(uid)['dashboard']
        dashboard['panels'].insert(0, {'type':'text', 'title':'Manual owner notes'})
        grafana.client.dashboard.update_dashboard(dict(folderUid='', overwrite=True, dashboard=dashboard))
        body.is_public = True
        body.is_dashboard_enabled = False
        grafana.update(body)
        response = effective(uid)
        assert response.status_code == 200
        assert body.views[0].name not in response.text
        assert body.views[0].columns[0].name not in response.text
        assert 'Manual owner notes' in response.text


@pytest.mark.parametrize('enabled', [False, True])
def test_inflight_private_write_cannot_overwrite_published_content(headers, grafana, enabled):
    owner = user()
    with app.test_client() as client:
        uid = create(client, headers, owner['login'])
        body = content_database(uid, owner['login'])
        grafana.update(body)
        reached, release = Event(), Event()
        original = grafana.client.dashboard.update_dashboard

        def delayed(payload):
            reached.set()
            assert release.wait(15), 'Publication did not release the old write'
            return original(payload)

        with ThreadPoolExecutor(max_workers=1) as workers, patch.object(
                grafana.client.dashboard, 'update_dashboard', side_effect=delayed):
            future = workers.submit(grafana.update_content, body.model_copy(deep=True))
            try:
                assert reached.wait(15), 'Old content did not reach Grafana write'
                current = body.model_copy(deep=True)
                current.is_public = True
                current.is_dashboard_enabled = enabled
                DashboardServiceClient(grafana.endpoint, 'admin', 'admin').update(current)
            finally:
                release.set()
            with pytest.raises(GrafanaClientError) as failure:
                future.result(timeout=15)
            assert failure.value.status_code in (409, 412)
        if enabled:
            assert_projection(uid, current, True)
        else:
            response = effective(uid)
            assert response.status_code == 200
            assert body.views[0].name not in response.text


def test_view_privacy_change_removes_old_panels(headers, grafana):
    owner = user()
    with app.test_client() as client:
        uid = create(client, headers, owner['login'], public=True)
        body = content_database(uid, owner['login'])
        body.is_public = True
        grafana.update(body)
        assert_projection(uid, body, True)
        body.views[1].is_public = False
        grafana.update_content(body)
        response = effective(uid)
        assert response.status_code == 200
        assert body.views[1].name not in response.text
        assert body.views[1].columns[0].name not in response.text
        assert body.views[2].name in response.text

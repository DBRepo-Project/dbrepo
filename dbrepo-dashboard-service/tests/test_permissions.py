import os
from uuid import uuid4
from unittest.mock import patch

import pytest
import requests
from dbrepo.api.dto import AccessType, DatabaseAccess, UserBrief
from dbrepo.core.client.dashboard import DashboardServiceClient
from dbrepo.core.api.exceptions import DashboardAccessError

from app import app
from tests import test_integration_app as fixtures


@pytest.fixture
def headers():
    signer = fixtures.AppIntegrationTest()
    signer.setUp()
    yield {'Authorization': f'Bearer {signer.token(["system"])}'}
    signer.doCleanups()


@pytest.fixture
def grafana(session):
    return DashboardServiceClient(session.get_url(), 'admin', 'admin')


def native(method, path, payload=None, auth=('admin', 'admin')):
    return requests.request(method, os.environ['DASHBOARD_UI_ENDPOINT'] + path,
                            json=payload, auth=auth, timeout=10)


def user(role='Viewer', login=None, email=None):
    login = login or 'user-' + uuid4().hex
    response = native('POST', '/api/admin/users', {'login': login, 'email': email or login + '@test.invalid',
                                                  'password': 'synthetic-password'})
    response.raise_for_status()
    result = dict(response.json(), login=login)
    native('PATCH', f'/api/org/users/{result["id"]}', {'role': role}).raise_for_status()
    return result


def effective(uid, account=None):
    auth = (account['login'], 'synthetic-password') if account else None
    return native('GET', f'/api/dashboards/uid/{uid}', auth=auth)


def create(client, headers, owner, public=False, schema_public=False):
    result = client.post('/api/v1/dashboard', headers=headers,
                         json={'database_name': 'synthetic', 'owner_username': owner,
                               'is_public': public, 'is_schema_public': schema_public})
    assert result.status_code == 201, result.data
    return result.json['uid']


def database(uid, owner, enabled=False, public=False):
    result = fixtures.req.model_copy(deep=True)
    result.dashboard_uid = uid
    result.owner = UserBrief(id=str(uuid4()), username=owner)
    result.is_dashboard_enabled = enabled
    result.is_public = public
    result.is_schema_public = public
    result.accesses = []
    return result


def access(client, headers, uid, login, permission):
    return client.put(f'/api/v1/dashboard/{uid}/access/{login}', headers=headers,
                      json={'permission': permission})


@pytest.mark.parametrize('public,schema_public', [(False, False), (True, False), (False, True)])
def test_creation_keeps_public_policy_and_owner_access(headers, grafana, public, schema_public):
    owner, editor = user(), user('Editor')
    with app.test_client() as client:
        uid = create(client, headers, owner['login'], public, schema_public)
    assert effective(uid).status_code == (200 if public or schema_public else 403)
    assert effective(uid, owner).json()['meta']['canAdmin'] is True
    editor_response = effective(uid, editor)
    assert editor_response.status_code == (200 if public or schema_public else 403)
    if editor_response.status_code == 200:
        assert editor_response.json()['meta']['canEdit'] is False
    assert native('GET', f'/api/dashboards/uid/{uid}').status_code == 200


def test_missing_owner_creation_and_later_activation(headers, grafana):
    login = 'later-' + uuid4().hex
    with app.test_client() as client:
        uid = create(client, headers, login)
        body = database(uid, login, enabled=True)
        assert effective(uid).status_code == 403
        assert client.head(f'/api/v1/dashboard/user/{login}', headers=headers).status_code == 404
        assert client.put(f'/api/v1/dashboard/{uid}', headers=headers, json=body.model_dump()).status_code == 404
        owner = user(login=login)
        assert client.head(f'/api/v1/dashboard/user/{login}', headers=headers).status_code == 204
        assert client.put(f'/api/v1/dashboard/{uid}', headers=headers, json=body.model_dump()).status_code == 202
    assert effective(uid, owner).json()['meta']['canAdmin'] is True


def test_native_grant_downgrade_revoke_and_unrelated_grant(headers, grafana):
    owner, member, unrelated = user(), user(), user()
    with app.test_client() as client:
        uid = create(client, headers, owner['login'])
        assert access(client, headers, uid, unrelated['login'], 'View').status_code == 202
        for permission, can_edit in [('View', False), ('Editor', True), ('View', False)]:
            assert access(client, headers, uid, member['login'], permission).status_code == 202
            assert effective(uid, member).json()['meta']['canEdit'] is can_edit
        assert access(client, headers, uid, member['login'], '').status_code == 202
        assert access(client, headers, uid, member['login'], '').status_code == 202
    assert effective(uid, member).status_code == 403
    assert effective(uid, unrelated).status_code == 200
    assert effective(uid, owner).json()['meta']['canAdmin'] is True


def test_missing_account_grant_then_manual_account_retry(headers, grafana):
    login = 'missing-' + uuid4().hex
    with app.test_client() as client:
        uid = create(client, headers, 'absent-owner')
        assert access(client, headers, uid, login, 'View').status_code == 404
        member = user(login=login)
        assert access(client, headers, uid, login, 'View').status_code == 202
    assert effective(uid, member).status_code == 200


def test_stale_dashboard_update_cannot_restore_revoked_member(headers, grafana):
    owner, member = user(), user()
    with app.test_client() as client:
        uid = create(client, headers, owner['login'])
        assert access(client, headers, uid, member['login'], 'View').status_code == 202
        captured = database(uid, owner['login'])
        captured.accesses = [DatabaseAccess(type=AccessType.READ,
                                           user=UserBrief(id=str(uuid4()), username=member['login']))]
        assert access(client, headers, uid, member['login'], '').status_code == 202
        assert effective(uid, member).status_code == 403
        assert client.put(f'/api/v1/dashboard/{uid}', headers=headers,
                          json=captured.model_dump()).status_code == 202
    assert effective(uid, member).status_code == 403


def test_revoking_disabled_account_removes_retained_permission(headers, grafana):
    member = user()
    with app.test_client() as client:
        uid = create(client, headers, 'absent-owner')
        assert access(client, headers, uid, member['login'], 'View').status_code == 202
        native('POST', f'/api/admin/users/{member["id"]}/disable').raise_for_status()
        assert access(client, headers, uid, member['login'], 'View').status_code == 404
        assert access(client, headers, uid, member['login'], '').status_code == 202
        native('POST', f'/api/admin/users/{member["id"]}/enable').raise_for_status()
    assert effective(uid, member).status_code == 403


def test_login_lookup_does_not_grant_an_email_alias(headers, grafana):
    alias = 'alias-' + uuid4().hex + '@test.invalid'
    other = user(email=alias)
    with app.test_client() as client:
        uid = create(client, headers, 'absent-owner')
        assert access(client, headers, uid, alias, 'Admin').status_code == 404
        assert client.head(f'/api/v1/dashboard/user/{alias}', headers=headers).status_code == 404
    assert effective(uid, other).status_code == 403


def test_private_disabled_update_clears_warmed_roles_and_preserves_manual_panels(headers, grafana):
    viewer, editor = user(), user('Editor')
    with app.test_client() as client:
        uid = create(client, headers, 'absent-owner', public=True)
        content = grafana.find(uid)['dashboard']
        content['panels'] = [{'id': 1, 'type': 'text', 'title': 'Manual panel'}]
        grafana.client.dashboard.update_dashboard(dict(folderUid='', overwrite=True, dashboard=content))
        assert effective(uid, viewer).status_code == effective(uid, editor).status_code == 200
        body = database(uid, 'absent-owner')
        assert client.put(f'/api/v1/dashboard/{uid}', headers=headers, json=body.model_dump()).status_code == 202
    assert effective(uid).status_code == effective(uid, viewer).status_code == effective(uid, editor).status_code == 403
    result = grafana.find(uid)['dashboard']
    assert result['panels'][0]['title'] == 'Manual panel'
    assert 'managed' not in result['tags']


def test_only_viewer_acl_is_revoked(headers, grafana):
    with app.test_client() as client:
        uid = create(client, headers, 'absent-owner')
        native('POST', f'/api/dashboards/uid/{uid}/permissions', {'items': [{'role': 'Viewer', 'permission': 1}]}).raise_for_status()
        assert effective(uid).status_code == 200
        body = database(uid, 'absent-owner')
        assert client.put(f'/api/v1/dashboard/{uid}', headers=headers, json=body.model_dump()).status_code == 202
    assert effective(uid).status_code == 403


def test_two_transfers_downgrade_then_remove_previous_owner(headers, grafana):
    first, second, third, unrelated = user(), user(), user(), user()
    with app.test_client() as client:
        uid = create(client, headers, first['login'])
        assert access(client, headers, uid, unrelated['login'], 'View').status_code == 202
        body = database(uid, second['login'])
        body.accesses = [DatabaseAccess(type=AccessType.WRITE_ALL, user=UserBrief(id=str(uuid4()), username=first['login']))]
        assert client.put(f'/api/v1/dashboard/{uid}', headers=headers,
                          json=body.model_dump()).status_code == 202
        assert effective(uid, first).json()['meta']['canEdit'] is True
        assert effective(uid, first).json()['meta']['canAdmin'] is False
        assert effective(uid, second).json()['meta']['canAdmin'] is True
        body.owner = UserBrief(id=str(uuid4()), username=third['login'])
        assert client.put(f'/api/v1/dashboard/{uid}', headers=headers,
                          json=body.model_dump()).status_code == 202
    assert effective(uid, second).status_code == 403
    assert effective(uid, third).json()['meta']['canAdmin'] is True
    assert effective(uid, unrelated).status_code == 200


@pytest.mark.parametrize('payload', [{}, {'permission': None}, {'permission': True}, {'permission': {}}, {'permission': 'Owner'}])
def test_invalid_permission_fails(headers, grafana, payload):
    with app.test_client() as client:
        response = client.put('/api/v1/dashboard/unknown/access/member', headers=headers, json=payload)
    assert response.status_code == 400


def test_missing_dashboard_and_unauthorized_preflight_fail(headers, grafana):
    with app.test_client() as client:
        assert access(client, headers, 'does-not-exist', 'absent', 'View').status_code == 404
        assert client.head('/api/v1/dashboard/user/absent').status_code == 401


def test_inherited_folder_access_is_rejected_explicitly(headers, grafana):
    folder = 'folder-' + uuid4().hex
    native('POST', '/api/folders', {'uid': folder, 'title': folder}).raise_for_status()
    try:
        uid = native('POST', '/api/dashboards/db',
                     {'folderUid': folder, 'dashboard': {'title': 'synthetic', 'panels': []}}).json()['uid']
        native('POST', f'/api/folders/{folder}/permissions',
               {'items': [{'role': 'Viewer', 'permission': 1}]}).raise_for_status()
        assert effective(uid).status_code == 200
        with app.test_client() as client:
            response = client.put(f'/api/v1/dashboard/{uid}', headers=headers,
                                  json=database(uid, 'absent').model_dump())
        assert response.status_code == 409
        assert effective(uid).status_code == 200  # Inherited access is an operator-managed limitation.
    finally:
        native('DELETE', '/api/folders/' + folder).raise_for_status()


def test_creation_failure_never_publishes_database_title(grafana):
    with patch.object(grafana, '_set_permission', side_effect=DashboardAccessError('synthetic failure')):
        with patch.object(grafana, 'delete', side_effect=DashboardAccessError('synthetic cleanup outage')):
            with pytest.raises(DashboardAccessError, match='synthetic failure'):
                grafana.create('PRIVATE_TITLE_MARKER')
    dashboards = grafana.client.search.search_dashboards()
    assert len(dashboards) == 1
    response = effective(dashboards[0]['uid'])
    assert 'PRIVATE_TITLE_MARKER' not in response.text


def test_transport_error_is_not_success(headers, grafana):
    with app.test_client() as client:
        uid = create(client, headers, 'absent-owner')
        with patch('dbrepo.core.client.dashboard.requests.get', side_effect=requests.Timeout('synthetic')):
            assert access(client, headers, uid, 'absent', 'View').status_code == 502


@pytest.mark.parametrize('payload', [None, [], 'dashboard', True, 42])
def test_creation_requires_json_object(headers, grafana, payload):
    import json
    with app.test_client() as client:
        response = client.post('/api/v1/dashboard', headers=headers, data=json.dumps(payload),
                               content_type='application/json')
    assert response.status_code == 400


def test_renamed_member_revoke_is_explicit_error(headers, grafana):
    member = user()
    with app.test_client() as client:
        uid = create(client, headers, 'absent-owner')
        assert access(client, headers, uid, member['login'], 'View').status_code == 202
        renamed = member['login'] + '-renamed'
        native('PUT', f'/api/users/{member["id"]}', {'login': renamed}).raise_for_status()
        assert access(client, headers, uid, member['login'], '').status_code == 404
        member['login'] = renamed
        assert effective(uid, member).status_code == 200
        assert access(client, headers, uid, renamed, '').status_code == 202
    assert effective(uid, member).status_code == 403


def test_owner_sync_removes_renamed_admin_and_preserves_read_edit(headers, grafana):
    old, current, reader, editor = user(), user(), user(), user()
    with app.test_client() as client:
        uid = create(client, headers, old['login'])
        assert access(client, headers, uid, reader['login'], 'View').status_code == 202
        assert access(client, headers, uid, editor['login'], 'Edit').status_code == 202
        old['login'] += '-renamed'
        native('PUT', f'/api/users/{old["id"]}', {'login': old['login']}).raise_for_status()
        assert effective(uid, old).json()['meta']['canAdmin'] is True
        body = database(uid, current['login'])
        assert client.put(f'/api/v1/dashboard/{uid}', headers=headers, json=body.model_dump()).status_code == 202
    assert effective(uid, old).status_code == 403
    assert effective(uid, current).json()['meta']['canAdmin'] is True
    assert effective(uid, reader).json()['meta']['canEdit'] is False
    assert effective(uid, editor).json()['meta']['canEdit'] is True
    assert native('GET', f'/api/dashboards/uid/{uid}').json()['meta']['canAdmin'] is True


def test_stale_content_update_does_not_restore_former_owner(headers, grafana):
    old, current = user(), user()
    with app.test_client() as client:
        uid = create(client, headers, old['login'])
        captured = database(uid, old['login'], enabled=True)
        assert client.put(f'/api/v1/dashboard/{uid}', headers=headers,
                          json=database(uid, current['login']).model_dump()).status_code == 202
        assert effective(uid, old).status_code == 403
        assert client.put(f'/api/v1/dashboard/{uid}/content', headers=headers,
                          json=captured.model_dump()).status_code == 202
    assert effective(uid, old).status_code == 403
    assert effective(uid, current).json()['meta']['canAdmin'] is True


def test_missing_owner_upgrade_restricts_legacy_roles_before_error(headers, grafana):
    editor = user('Editor')
    uid = grafana.client.dashboard.update_dashboard({'dashboard': {'title': 'Legacy', 'panels': []}, 'folderUid': ''})['uid']
    assert effective(uid).status_code == effective(uid, editor).status_code == 200
    with app.test_client() as client:
        response = client.put(f'/api/v1/dashboard/{uid}', headers=headers,
                              json=database(uid, 'absent-owner', enabled=True).model_dump())
    assert response.status_code == 404
    assert effective(uid).status_code == effective(uid, editor).status_code == 403


def test_initializer_reconciles_existing_members(headers, grafana):
    import init
    owner, member = user(), user()
    body = database('init-' + uuid4().hex, owner['login'])
    body.accesses = [DatabaseAccess(type=AccessType.READ, user=UserBrief(id=str(uuid4()), username=member['login']))]
    init.upsert_dashboard(body)
    assert effective(body.dashboard_uid).status_code == 403
    assert effective(body.dashboard_uid, owner).json()['meta']['canAdmin'] is True
    assert effective(body.dashboard_uid, member).status_code == 200


def test_initializer_attempts_remaining_databases_after_failure(grafana):
    import init
    first, second = database('first', 'absent'), database('second', 'absent')
    with patch.object(init, 'fetch_databases', return_value=[first, second]):
        with patch.object(init, 'upsert_dashboard', side_effect=[DashboardAccessError('missing owner'), None]) as update:
            with pytest.raises(RuntimeError, match='1 database'):
                init.synchronize_dashboards()
    assert [call.args[0].dashboard_uid for call in update.call_args_list] == ['first', 'second']


def test_concurrent_targeted_grants_preserve_both_users(headers, grafana):
    from concurrent.futures import ThreadPoolExecutor
    members = [user(), user()]
    with app.test_client() as client:
        uid = create(client, headers, 'absent-owner')
    def grant(member):
        with app.test_client() as client:
            return access(client, headers, uid, member['login'], 'View').status_code
    with ThreadPoolExecutor(max_workers=2) as executor:
        assert list(executor.map(grant, members)) == [202, 202]
    assert all(effective(uid, member).status_code == 200 for member in members)


def test_transferred_owner_access_retry_preserves_native_admin(headers, grafana):
    old_owner, new_owner = user(), user()
    with app.test_client() as client:
        uid = create(client, headers, old_owner['login'])
        body = database(uid, new_owner['login'])
        assert client.put(f'/api/v1/dashboard/{uid}', headers=headers, json=body.model_dump()).status_code == 202
        assert effective(uid, new_owner).json()['meta']['canAdmin'] is True
        for _ in range(2):
            assert access(client, headers, uid, new_owner['login'], 'Admin').status_code == 202
            assert effective(uid, new_owner).json()['meta']['canAdmin'] is True
        assert effective(uid, old_owner).status_code == 403

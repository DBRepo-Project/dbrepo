import logging

from grafana_client.client import GrafanaException
from werkzeug.exceptions import NotFound

from api.dto import Permission
from clients import grafana_client

statistics_row_title = '${table_id}'


def remove_anonymous_read_access(uid: str) -> None:
    grafana = grafana_client.connect()
    permissions = grafana.dashboard.get_permissions_by_uid(uid)
    viewer_role = [permission for permission in permissions if
                   'permissionName' in permission and permission['permissionName'] != 'View']
    if len(viewer_role) == 0:
        logging.warning(f'Failed to find permissionName=View')
        return None
    try:
        response = grafana_client.generic_post(f'/api/access-control/dashboards/{uid}/builtInRoles/Viewer',
                                               Permission(permission='').model_dump())
        if response.status_code != 200:
            raise OSError(f'Failed to remove anonymous read access: {response.content}')
    except GrafanaException as e:
        raise OSError(f'Failed to remove anonymous read access: {e.message}')
    logging.info(f"Removed anonymous read access from dashboard with uid: {uid}")


def update_access(uid: str, username: str, permission: Permission) -> None:
    try:
        response = grafana_client.generic_get(f'/api/users/lookup?loginOrEmail={username}')
        if response.status_code == 404:
            raise NotFound(f"Failed to find user: {username}")
        if response.status_code != 200:
            raise OSError(f"Failed to add access to user: {username}")
        grafana_client.generic_post(f"/api/access-control/dashboards/{uid}/users/{response.json()['id']}",
                                    permission.model_dump())
    except GrafanaException as e:
        logging.error(f'Failed to add access: {e.message}')
    logging.info(f"Add access for dashboard with uid: {uid}")

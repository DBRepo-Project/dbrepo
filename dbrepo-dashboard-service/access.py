import logging

from grafana_client.client import GrafanaException

from api.dto import Permission
from dbrepo.api.dto import Database
from clients import grafana_client

statistics_row_title = '${table_id}'


def update_anonymous_read_access(uid: str, is_public: bool, is_schema_public: bool) -> None:
    grafana = grafana_client.connect()
    permissions = grafana.dashboard.get_permissions_by_uid(uid)
    viewer_role = [permission for permission in permissions if
                   'permissionName' in permission and permission['permissionName'] != 'View']
    permission = ''
    if is_public or is_schema_public:
        permission = 'View'
    if len(viewer_role) == 0:
        logging.warning(f'Failed to find permissionName=View')
        return None
    try:
        response = grafana_client.generic_post(f'/api/access-control/dashboards/{uid}/builtInRoles/Viewer',
                                               Permission(permission=permission).model_dump())
        if response.status_code != 200:
            raise OSError(f'Failed to update anonymous read access: {response.content}')
    except GrafanaException as e:
        raise OSError(f'Failed to update anonymous read access: {e.message}')
    logging.info(f"Updated anonymous read access for dashboard with uid: {uid}")

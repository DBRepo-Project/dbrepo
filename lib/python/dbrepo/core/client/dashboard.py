import logging
from typing import List
from urllib.parse import quote, urlencode

import requests
from grafana_client import GrafanaApi
from grafana_client.client import GrafanaClientError
from requests import Response

from dbrepo.api.dto import Database, ColumnType, ViewColumn, View, AccessType
from dbrepo.core.api.exceptions import DashboardAccessError

statistics_row_title = 'Generated Dashboard'
auto_generated_description = 'Auto-generated'

number_types = [ColumnType.SERIAL, ColumnType.BIT, ColumnType.SMALLINT, ColumnType.MEDIUMINT, ColumnType.INT,
                ColumnType.BIGINT, ColumnType.FLOAT, ColumnType.DOUBLE, ColumnType.DECIMAL]

time_types = [ColumnType.DATE, ColumnType.TIME, ColumnType.TIMESTAMP, ColumnType.YEAR]

bool_types = [ColumnType.TINYINT, ColumnType.BOOL]

section_height = 3 * 8


def map_link(title: str, url: str, icon: str = 'info', open_new_window: bool = True) -> dict:
    return dict(targetBlank=open_new_window,
                asDropdown=False,
                includeVars=False,
                keepTime=False,
                tags=[],
                type='link',
                icon=icon,
                title=title,
                url=url)


def _get_managed_offset_y(dashboard: dict) -> int | None:
    idx = [panel['title'] for panel in dashboard['panels']].index(statistics_row_title)
    if idx == -1:
        return None
    offset_y = dashboard['panels'][idx]['gridPos']['y']
    logging.debug(f'managed panel y-offset: {offset_y}')
    return offset_y


def _get_start_index(dashboard: dict) -> int | None:
    return [panel['title'] for panel in dashboard['panels']].index(statistics_row_title)


def map_column_conversion(column: ViewColumn) -> dict:
    destinationType = 'string'
    dateFormat = None
    if column.type in number_types:
        destinationType = 'number'
    elif column.type in time_types:
        destinationType = 'time'
        if column.type == ColumnType.YEAR:
            dateFormat = 'YYYY'
        elif column.type == ColumnType.TIME:
            dateFormat = 'HH:mm:ss'
        else:
            dateFormat = 'YYYY-MM-dd'
    elif column.type in bool_types:
        destinationType = 'boolean'
    return dict(targetField=column.internal_name,
                destinationType=destinationType,
                dateFormat=dateFormat)


def map_row(title: str, x: int = 0, y: int = 0) -> dict:
    return dict(collapsed=False,
                title=title,
                type='row',
                panels=[],
                targets=[],
                parser='backend',
                gridPos=dict(h=1,
                             w=24,
                             x=x,
                             y=y))


def map_preview_image_panel(database_id: str, w: int = 4, h: int = 4, x: int = 20, y: int = 0) -> dict:
    return dict(title='Preview Image',
                type='text',
                description=auto_generated_description,
                gridPos=dict(h=h,
                             w=w,
                             x=x,
                             y=y),
                fieldConfig=dict(defaults=dict(),
                                 overrides=[]),
                options=dict(mode="markdown",
                             code=dict(language="plaintext",
                                       showLineNumbers=False,
                                       showMiniMap=False),
                             content=f'<img src="/api/v1/database/{database_id}/image" alt="" width="90" />'))


class DashboardServiceClient:

    def __init__(self, endpoint: str, username: str, password: str, base_url: str = 'http://localhost',
                 datasource_uid: str = 'dbrepojson0'):
        self.client: GrafanaApi = GrafanaApi.from_url(url=f'{endpoint}', credential=(username, password))
        self.endpoint = endpoint
        self.username = username
        self.password = password
        self.base_url = base_url
        self.datasource_uid = datasource_uid

    def get_client(self):
        return self.client

    def generic_get(self, api_url: str) -> Response:
        request_url = self.endpoint + api_url
        logging.debug(f'generic get url={request_url}, auth=({self.username}, <reacted>)')
        return requests.get(request_url, auth=(self.username, self.password), timeout=(3, 10))

    def generic_post(self, api_url: str, payload: dict) -> Response:
        request_url = self.endpoint + api_url
        logging.debug(f'generic post url={request_url}, payload={payload}, auth=({self.username}, <reacted>)')
        return requests.post(request_url, json=payload, auth=(self.username, self.password), timeout=(3, 10))

    def find(self, uid: str):
        """
        Finds a dashboard with the given uid.

        @return The dashboard, if successful. Otherwise, `None`.
        """
        if uid is None:
            return None
        try:
            return self.client.dashboard.get_dashboard(uid)
        except GrafanaClientError as error:
            if error.status_code != 404:
                raise
            logging.warning(f"Failed to find dashboard with uid: {uid}")
            return None

    def create(self, database_name: str, uid: str = '', owner_username: str | None = None,
               is_public: bool = False, is_schema_public: bool = False) -> dict:
        dashboard = dict(uid=uid,
                         title='New dashboard',
                         tags=['managed'],
                         timezone='browser',
                         refresh='30m',
                         preload=False,
                         panels=[])
        dashboard['panels'] = []
        payload = dict(folderUid='',
                       overwrite=False,
                       dashboard=dashboard)
        dashboard = self.client.dashboard.update_dashboard(payload)
        try:
            self.update_anonymous_read_access(dashboard['uid'], False, False)
            owner = None
            if owner_username:
                owner = self.find_user(owner_username)
                if owner is not None and not owner.get('isDisabled'):
                    self._set_user_permission(dashboard['uid'], owner['id'], 'Admin')
            self._sync_owner_permissions(dashboard['uid'], owner, [])
            content = self.find(dashboard['uid'])['dashboard']
            content['title'] = f'{database_name} Overview'
            self.client.dashboard.update_dashboard(dict(folderUid='', overwrite=True, dashboard=content))
            self.update_anonymous_read_access(dashboard['uid'], is_public, is_schema_public)
        except Exception:
            # Do not leave a newly created dashboard with default access after a failed setup.
            try:
                self.delete(dashboard['uid'])
            except Exception:
                logging.exception('Failed to remove dashboard after unsuccessful creation')
            raise
        logging.info(f"Created dashboard with uid: {dashboard['uid']}")
        return dashboard

    def delete(self, uid: str) -> None:
        self.client.dashboard.delete_dashboard(uid)

    def update(self, database: Database) -> None:
        dashboard = self.find(database.dashboard_uid)
        if dashboard is None:
            if not database.dashboard_uid:
                raise DashboardAccessError('Database has no dashboard UID', 404)
            self.create(database.internal_name, database.dashboard_uid)
            dashboard = self.find(database.dashboard_uid)
        self._require_root(dashboard)
        # Secure legacy dashboards even when a missing owner prevents activation.
        self.update_anonymous_read_access(database.dashboard_uid, False, False)
        owner = self.find_user(database.owner.username)
        if (database.is_dashboard_enabled and not (database.is_public or database.is_schema_public) and
                (owner is None or owner.get('isDisabled'))):
            raise DashboardAccessError('The dashboard owner needs an existing Grafana account with the same login', 404)
        if owner is not None and not owner.get('isDisabled'):
            self._set_user_permission(database.dashboard_uid, owner['id'], 'Admin')
        self._sync_owner_permissions(database.dashboard_uid, owner, database.accesses or [])
        if database.is_dashboard_enabled:
            self._update_content(database, dashboard['dashboard'])
        elif 'managed' in dashboard['dashboard'].get('tags', []):
            dashboard['dashboard']['tags'].remove('managed')
            self.client.dashboard.update_dashboard(dict(folderUid='', overwrite=True, dashboard=dashboard['dashboard']))
        self.update_anonymous_read_access(database.dashboard_uid, database.is_public, database.is_schema_public)

    def update_content(self, database: Database) -> None:
        if not database.is_dashboard_enabled:
            return
        dashboard = self.find(database.dashboard_uid)
        self._require_root(dashboard)
        self._update_content(database, dashboard['dashboard'])

    def reconcile(self, database: Database) -> None:
        """Maintenance only: callers must stop concurrent DBRepo access/owner/visibility changes."""
        self.update(database)
        for access in database.accesses or []:
            if access.user.username != database.owner.username:
                self.update_access(database.dashboard_uid, access.user.username,
                                   'View' if access.type == AccessType.READ else 'Edit')

    def _update_content(self, database: Database, dashboard: dict) -> None:
        # update metadata
        if len(database.identifiers) > 0 and len(database.identifiers[0].titles) > 0:
            dashboard['title'] = database.identifiers[0].titles[0].title
        if len(database.identifiers) > 0 and len(database.identifiers[0].descriptions) > 0:
            dashboard['description'] = database.identifiers[0].descriptions[0].description
        dashboard['links'] = self.map_links(database)
        # update panels
        dashboard['panels'] = self.get_panels(dashboard, database)
        payload = dict(folderUid='',
                       overwrite=True,
                       dashboard=dashboard)
        response = self.client.dashboard.update_dashboard(payload)
        logging.info(f"Updated dashboard with uid: {response['uid']}")

    def map_links(self, database: Database) -> List[dict]:
        links = []
        if len(database.identifiers) > 0:
            links.append(map_link('Database', f"{self.base_url}/pid/{database.identifiers[0].id}"))
        else:
            links.append(map_link('Database', f"{self.base_url}/database/{database.id}"))
        return links

    def update_anonymous_read_access(self, uid: str, is_public: bool, is_schema_public: bool) -> None:
        self._require_root(self.find(uid))
        permission = 'View' if is_public or is_schema_public else ''
        for role in ('Editor', 'Viewer'):
            self._set_permission(f'/api/access-control/dashboards/{quote(uid, safe="")}/builtInRoles/{role}', permission)
        logging.info(f"Updated anonymous read access for dashboard with uid: {uid}")

    @staticmethod
    def _require_root(dashboard: dict | None) -> None:
        if dashboard is None:
            raise DashboardAccessError('Dashboard not found', 404)
        if dashboard.get('meta', {}).get('folderUid'):
            raise DashboardAccessError('Managed dashboards must remain in General; inherited folder permissions cannot be synchronized', 409)

    def find_user(self, username: str) -> dict | None:
        response = self.generic_get('/api/users/lookup?' + urlencode({'loginOrEmail': username}))
        if response.status_code == 404:
            return None
        response.raise_for_status()
        user = response.json()
        if user.get('login') != username:
            return None
        if not isinstance(user.get('id'), int) or user['id'] <= 0:
            raise DashboardAccessError('Grafana returned an invalid user')
        # The lookup response does not reliably reflect the account's disabled state.
        current = self.generic_get(f'/api/users/{user["id"]}')
        if current.status_code == 404:
            return None
        current.raise_for_status()
        details = current.json()
        if details.get('id') != user['id'] or details.get('login') != username:
            return None
        if not isinstance(details.get('isDisabled'), bool):
            raise DashboardAccessError('Grafana returned an invalid user state')
        return details

    def require_user(self, username: str) -> None:
        user = self.find_user(username)
        if user is None or user.get('isDisabled'):
            raise DashboardAccessError('An existing Grafana account with the same login is required', 404)

    def _set_permission(self, path: str, permission: str) -> None:
        response = self.generic_post(path, {'permission': permission})
        response.raise_for_status()
        if response.status_code != 200:
            raise DashboardAccessError('Grafana did not confirm the permission update')

    def _set_user_permission(self, uid: str, user_id: int, permission: str) -> None:
        self._set_permission(f'/api/access-control/dashboards/{quote(uid, safe="")}/users/{user_id}', permission)

    def _sync_owner_permissions(self, uid: str, owner: dict | None, accesses: list) -> None:
        response = self.generic_get(f'/api/access-control/dashboards/{quote(uid, safe="")}')
        response.raise_for_status()
        permissions = response.json()
        if not isinstance(permissions, list):
            raise DashboardAccessError('Grafana returned invalid dashboard permissions')
        grants = {access.user.username: 'View' if access.type == AccessType.READ else 'Edit' for access in accesses}
        owner_id = owner['id'] if owner is not None else None
        for permission in permissions:
            user_id = permission.get('userId')
            if (user_id and user_id != owner_id and permission.get('permission') == 'Admin' and
                    permission.get('isInherited') is False):
                # Managed dashboards reserve direct Admin for their current DBRepo owner.
                self._set_user_permission(uid, user_id, grants.get(permission.get('userLogin'), ''))

    def update_access(self, uid: str, username: str, permission: str) -> None:
        if permission not in ('View', 'Edit', 'Editor', 'Admin', ''):
            raise DashboardAccessError('Invalid dashboard permission', 400)
        self._require_root(self.find(uid))
        user = self.find_user(username)
        if user is None:
            raise DashboardAccessError('An existing Grafana account with the same login is required', 404)
        if user.get('isDisabled') and permission:
            raise DashboardAccessError('The Grafana account is disabled', 404)
        self._set_user_permission(uid, user['id'], 'Edit' if permission == 'Editor' else permission)

    def _map_timeseries_panel(self, database_id: str, view: View, panel_type: str, h: int = 8, w: int = 12, x: int = 12,
                              y: int = 8) -> dict:
        datasource = dict(uid=self.datasource_uid,
                          type='yesoreyeram-infinity-datasource')
        fillOpacity = 0
        if panel_type == 'histogram':
            fillOpacity = 60
        return dict(title=panel_type.capitalize(),
                    description=auto_generated_description,
                    type=panel_type,
                    datasource=datasource,
                    targets=[dict(datasource=datasource,
                                  format='table',
                                  global_query_id='',
                                  hide=False,
                                  refId='A',
                                  root_selector='',
                                  source='url',
                                  type='json',
                                  url=f'/api/v1/database/{database_id}/view/{view.id}/data',
                                  parser='backend',
                                  url_options=dict(data='',
                                                   method='GET'))],
                    gridPos=dict(h=h,
                                 w=w,
                                 x=x,
                                 y=y),
                    options=dict(legend=dict(displayMode='list',
                                             placement='bottom',
                                             showLegend=True),
                                 tooltip=dict(mode='single',
                                              sort='none')),
                    fieldConfig=dict(
                        defaults=dict(color=dict(mode='palette-classic'),
                                      custom=dict(
                                          axisBorderShow=False,
                                          axisCenteredZero=False,
                                          axisColorMode='text',
                                          axisLabel='',
                                          axisPlacement='auto',
                                          barAlignment=0,
                                          drawStyle='line',
                                          fillOpacity=fillOpacity,
                                          gradientMode='none',
                                          hideFrom=dict(legend=False,
                                                        tooltip=False,
                                                        viz=False),
                                          insertNulls=False,
                                          lineInterpolation='linear',
                                          lineWidth=1,
                                          pointSize=5,
                                          scaleDistribution=dict(type='linear'),
                                          showPoints='auto',
                                          spanNulls=False,
                                          stacking=dict(group='A',
                                                        mode='none'),
                                          thresholdsStyle=dict(mode='absolute')))),
                    transformations=[dict(id='convertFieldType',
                                          options=dict(fields=dict(),
                                                       conversions=[map_column_conversion(column) for column in
                                                                    view.columns]))])

    def _map_number_panel(self, database_id: str, view_id: str, title: str, field: str, x: int = 18,
                          y: int = 0) -> dict:
        datasource = dict(uid=self.datasource_uid,
                          type='yesoreyeram-infinity-datasource')
        return dict(title=title,
                    type='stat',
                    datasource=datasource,
                    targets=[dict(datasource=datasource,
                                  columns=[],
                                  filters=[],
                                  format='table',
                                  global_query_id='',
                                  hide=False,
                                  refId='A',
                                  root_selector='',
                                  source='url',
                                  type='json',
                                  url=f'/api/v1/database/{database_id}/view/{view_id}/statistic',
                                  parser='backend',
                                  url_options=dict(data='',
                                                   method='GET'))],
                    fieldConfig=dict(defaults=dict(mappings=[],
                                                   thresholds=dict(mode='absolute',
                                                                   steps=[dict(color='blue',
                                                                               value=None)]),
                                                   unit=''),
                                     overrides=[]),
                    transformations=[dict(id='extractFields',
                                          options=dict(delimiter=',',
                                                       source=field,
                                                       format='auto',
                                                       replace=False,
                                                       keepTime=False)),
                                     dict(id='filterFieldsByName',
                                          options=dict(include=dict(names=[field])))],
                    gridPos=dict(h=4,
                                 w=6,
                                 x=x,
                                 y=y),
                    options=dict(colorMode='background',
                                 graphMode='area',
                                 justifyMode='auto',
                                 orientation='auto',
                                 reduceOptions=dict(calcs=[],
                                                    fields='/.*/',
                                                    values=True),
                                 showPercentChange=False,
                                 textMode='auto',
                                 wideLayout=True))

    def map_overview_panel(self, database_id: str, view_id: str, x: int = 0, y: int = 4) -> dict:
        datasource = dict(uid=self.datasource_uid,
                          type='yesoreyeram-infinity-datasource')
        return dict(title='Datasource Preview',
                    type='table',
                    gridPos=dict(h=8,
                                 w=18,
                                 x=x,
                                 y=y),
                    fieldConfig=dict(
                        defaults=dict(
                            color=dict(mode='palette-classic'),
                            custom=dict(axisBorderShow=False,
                                        axisCenteredZero=False,
                                        axisColorMode='text',
                                        axisLabel='',
                                        axisPlacement='auto',
                                        barAlignment=0,
                                        drawStyle='line',
                                        fillOpacity=0,
                                        gradientMode='none',
                                        hideFrom=dict(
                                            legend=False,
                                            tooltip=False,
                                            viz=False),
                                        insertNulls=False,
                                        lineInterpolation='linear',
                                        lineWidth=1,
                                        pointSize=5,
                                        scaleDistribution=dict(
                                            type='linear'),
                                        showPoints='auto',
                                        spanNulls=False,
                                        stacking=dict(group='A',
                                                      mode='none'),
                                        thresholdsStyle=dict(
                                            mode='off'))),
                        overrides=[]),
                    options=dict(legend=dict(displayMode='list',
                                             placement='bottom',
                                             showLegend=True,
                                             calcs=[]),
                                 tooltip=dict(mode='single',
                                              sort='none')),
                    targets=[dict(format='json',
                                  columns=[],
                                  datasource=datasource,
                                  filters=[],
                                  global_query_id='',
                                  refId='A',
                                  root_selector='',
                                  source='url',
                                  type='json',
                                  url=f'/api/v1/database/{database_id}/view/{view_id}/data',
                                  parser='backend',
                                  url_options=dict(data='',
                                                   method='GET'))],
                    links=[dict(title='Cite',
                                url=f'{self.base_url}/database/{database_id}/view/{view_id}/data',
                                targetBlank=True)],
                    datasource=datasource)

    def map_statistics_panel(self, database_id: str, view_id: str, w: int = 12, h: int = 8, x: int = 0,
                             y: int = 8) -> dict:
        datasource = dict(uid=self.datasource_uid,
                          type='yesoreyeram-infinity-datasource')
        return dict(title='Statistics',
                    type='table',
                    gridPos=dict(h=h,
                                 w=w,
                                 x=x,
                                 y=y),
                    datasource=datasource,
                    targets=[dict(datasource=datasource,
                                  columns=[],
                                  filters=[],
                                  format='table',
                                  global_query_id='',
                                  hide=False,
                                  refId='A',
                                  root_selector='columns',
                                  source='url',
                                  type='json',
                                  url=f'/api/v1/database/{database_id}/view/{view_id}/statistic',
                                  parser='backend',
                                  url_options=dict(data='',
                                                   method='GET'))],
                    options=dict(cellHeight="sm",
                                 showHeader=True,
                                 footer=dict(countRows=False,
                                             fields="",
                                             reducer=["sum"],
                                             show=False)),
                    transformations=[dict(id="organize",
                                          options=dict(excludeByName=dict(),
                                                       includeByName=dict(),
                                                       indexByName=dict(name=0,
                                                                        val_min=1,
                                                                        val_max=2,
                                                                        mean=3,
                                                                        median=4,
                                                                        std_dev=5),
                                                       renameByName=dict(name="Name",
                                                                         mean="Mean",
                                                                         median="Median",
                                                                         std_dev="std.dev",
                                                                         val_min="Minimum",
                                                                         val_max="Maximum")))],
                    fieldConfig=dict(defaults=dict(custom=dict(align="auto",
                                                               filterable="true",
                                                               cellOptions=dict(type="auto"),
                                                               inspect=False),
                                                   mappings=[],
                                                   thresholds=dict(mode="absolute",
                                                                   steps=[dict(color="green",
                                                                               value=None),
                                                                          dict(color="red",
                                                                               value=80)
                                                                          ])),
                                     overrides=[]))

    def map_timeseries_panel(self, database_id: str, view: View, h: int = 8, w: int = 12, x: int = 12,
                             y: int = 8) -> dict:
        return self._map_timeseries_panel(database_id, view, 'timeseries', h, w, x, y)

    def map_pie_panel(self, database_id: str, view: View, h: int = 8, w: int = 12, x: int = 12, y: int = 8) -> dict:
        return self._map_timeseries_panel(database_id, view, 'piechart', h, w, x, y)

    def map_histogram_panel(self, database_id: str, view: View, h: int = 8, w: int = 12, x: int = 12,
                            y: int = 8) -> dict:
        return self._map_timeseries_panel(database_id, view, 'histogram', h, w, x, y)

    def map_data_sources_panel(self, database_id: str, x: int = 0, y: int = 0) -> dict:
        datasource = dict(uid=self.datasource_uid,
                          type='yesoreyeram-infinity-datasource')
        return dict(title='Datasources',
                    description=auto_generated_description,
                    type='stat',
                    datasource=datasource,
                    targets=[dict(datasource=datasource,
                                  columns=[],
                                  filters=[],
                                  format='table',
                                  global_query_id='',
                                  hide=False,
                                  refId='A',
                                  root_selector='$count(id)',
                                  source='url',
                                  type='json',
                                  url=f'/api/v1/database/{database_id}/view',
                                  parser='backend',
                                  url_options=dict(data='',
                                                   method='GET'))],
                    fieldConfig=dict(defaults=dict(mappings=[],
                                                   thresholds=dict(mode='absolute',
                                                                   steps=[dict(color='blue',
                                                                               value=None)])),
                                     overrides=[]),
                    transformations=[],
                    gridPos=dict(h=4,
                                 w=6,
                                 x=x,
                                 y=y),
                    options=dict(colorMode='background',
                                 graphMode='area',
                                 justifyMode='auto',
                                 orientation='auto',
                                 reduceOptions=dict(calcs=[],
                                                    fields='/.*/',
                                                    values=True),
                                 showPercentChange=False,
                                 textMode='auto',
                                 wideLayout=True))

    def map_rows_panel(self, database_id: str, view_id: str, x: int = 18, y: int = 0) -> dict:
        return self._map_number_panel(database_id, view_id, 'Rows', 'total_rows', x, y)

    def map_columns_panel(self, database_id: str, view_id: str, x: int = 18, y: int = 0) -> dict:
        return self._map_number_panel(database_id, view_id, 'Variables', 'total_columns', x, y)

    def get_panels(self, dashboard: dict, database: Database) -> [dict]:
        panels = dashboard['panels']
        managed_offset = 1
        try:
            managed_offset = _get_managed_offset_y(dashboard)
            end_index = _get_start_index(dashboard)
            logging.debug(f'splicing managed panels after index: {end_index}')
            panels = panels[:end_index]
        except ValueError:
            logging.warning(f'No managed panels found')
        original_panels_size = len(panels)
        panels.append(map_row(statistics_row_title, 0, managed_offset + 0))
        panels.append(self.map_data_sources_panel(database.id, y=managed_offset))
        if database.preview_image is not None:
            panels.append(map_preview_image_panel(database.id, y=managed_offset))
        for i, view in enumerate(database.views):
            # section
            panels.append(map_row(view.name, 0, y=i * section_height + managed_offset + 4))
            panels.append(self.map_overview_panel(database.id, view.id, 0, y=i * section_height + managed_offset + 8))
            panels.append(self.map_rows_panel(database.id, view.id, 18, y=i * section_height + managed_offset + 4))
            panels.append(self.map_columns_panel(database.id, view.id, 18, y=i * section_height + managed_offset + 8))
            panels.append(self.map_statistics_panel(database.id, view.id, h=8, w=12, x=0,
                                                    y=i * section_height + managed_offset + 12))
            panels.append(self.map_histogram_panel(database.id, view, h=8, w=12, x=12,
                                                   y=i * section_height + managed_offset + 12))
            panels.append(self.map_timeseries_panel(database.id, view, h=8, w=8, x=0,
                                                    y=i * section_height + managed_offset + 20))
            panels.append(self.map_pie_panel(database.id, view, h=8, w=8, x=8,
                                             y=i * section_height + managed_offset + 20))
        logging.info(f'Added {len(panels) - original_panels_size} managed panel(s)')
        return panels

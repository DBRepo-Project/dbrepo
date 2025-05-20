import logging

import requests
from grafana_client import GrafanaApi
from grafana_client.client import GrafanaClientError, GrafanaException
from requests import Response

from dbrepo.api.dto import Database, ColumnType, ViewColumn, View
from dbrepo.core.api.dto import Permission

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
                             content=f'<img src="/api/database/{database_id}/image" alt="" width="90" />'))


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
        return requests.get(request_url, auth=(self.username, self.password))

    def generic_post(self, api_url: str, payload: dict) -> Response:
        request_url = self.endpoint + api_url
        logging.debug(f'generic post url={request_url}, payload={payload}, auth=({self.username}, <reacted>)')
        return requests.post(request_url, json=payload, auth=(self.username, self.password))

    def find(self, uid: str):
        """
        Finds a dashboard with the given uid.

        @return The dashboard, if successful. Otherwise, `None`.
        """
        if uid is None:
            return None
        try:
            return self.client.dashboard.get_dashboard(uid)
        except GrafanaClientError:
            logging.warning(f"Failed to find dashboard with uid: {uid}")
            return None

    def create(self, database_name: str, uid: str = '') -> dict:
        dashboard = dict(uid=uid,
                         title=f'{database_name} Overview',
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
        logging.info(f"Created dashboard with uid: {dashboard['uid']}")
        return dashboard

    def delete(self, uid: str) -> None:
        self.client.dashboard.delete_dashboard(uid)

    def update(self, database: Database) -> None:
        dashboard = self.find(database.dashboard_uid)
        if dashboard is None:
            self.create(database.internal_name, database.dashboard_uid)
            dashboard = self.find(database.dashboard_uid)
        dashboard = dashboard['dashboard']
        # update metadata
        if not database.is_dashboard_enabled and 'managed' in dashboard['tags']:
            dashboard['tags'].remove('managed')
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

    def map_links(self, database: Database) -> [dict]:
        links = []
        if len(database.identifiers) > 0:
            links.append(map_link('Database', f"{self.base_url}/pid/{database.identifiers[0].id}"))
        else:
            links.append(map_link('Database', f"{self.base_url}/database/{database.id}"))
        return links

    def update_anonymous_read_access(self, uid: str, is_public: bool, is_schema_public: bool) -> None:
        permissions = self.client.dashboard.get_permissions_by_uid(uid)
        viewer_role = [permission for permission in permissions if
                       'permissionName' in permission and permission['permissionName'] != 'View']
        permission = ''
        if is_public or is_schema_public:
            permission = 'View'
        if len(viewer_role) == 0:
            logging.warning(f'Failed to find permissionName=View')
            return None
        try:
            response = self.generic_post(f'/api/access-control/dashboards/{uid}/builtInRoles/Viewer',
                                         Permission(permission=permission).model_dump())
            if response.status_code != 200:
                raise OSError(f'Failed to update anonymous read access: {response.content}')
        except GrafanaException as e:
            raise OSError(f'Failed to update anonymous read access: {e.message}')
        logging.info(f"Updated anonymous read access for dashboard with uid: {uid}")

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
                                  url=f'/api/database/{database_id}/view/{view.id}/data',
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
                                  url=f'/api/database/{database_id}/view/{view_id}/statistic',
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
                                  url=f'/api/database/{database_id}/view/{view_id}/data',
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
                                  url=f'/api/database/{database_id}/view/{view_id}/statistic',
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
                                  url=f'/api/database/{database_id}/view',
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

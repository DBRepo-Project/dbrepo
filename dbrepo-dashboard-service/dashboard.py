import logging
import os

from dbrepo.api.dto import Database, Table
from grafana_client.client import GrafanaException

from clients import grafana_client

statistics_row_title = '${table_id}'

base_url = os.getenv('BASE_URL', 'http://localhost')
datasource_uid = os.getenv('JSON_DATASOURCE_NAME', 'dbrepojson0')


def map_link(title: str, url: str) -> dict:
    return dict(targetBlank=True,
                asDropdown=False,
                includeVars=False,
                keepTime=False,
                tags=[],
                type='link',
                icon='info',
                title=title,
                url=url)


def map_statistics_row(dashboard: dict) -> dict | None:
    filtered_panels = [panel for panel in dashboard['panels'] if
                       panel['type'] == 'row' and panel['title'] == statistics_row_title and 'id' in panel]
    if len(filtered_panels) > 0 and filtered_panels[0]['id'] is not None:
        return filtered_panels[0]
    logging.warning(f'Failed to find statistics row id')
    return None


def map_links(database: Database) -> [dict]:
    links = []
    if len(database.identifiers) > 0:
        links.append(map_link('Database', f"{base_url}/pid/{database.identifiers[0].id}"))
    else:
        links.append(map_link('Database', f"{base_url}/database/{database.id}"))
    return links


def map_templating(database: Database) -> dict:
    options = [dict(selected=False,
                    text=table.name,
                    value=str(table.id)) for table in database.tables]
    selected = dict(selected=True,
                    text=[table.name for table in database.tables],
                    value=[str(table.id) for table in database.tables])
    datasource = dict(uid=datasource_uid,
                      type='yesoreyeram-infinity-datasource')
    return dict(list=[dict(description='',
                           name='table_id',
                           hide=0,
                           includeAll=True,
                           multi=True,
                           datasource=datasource,
                           refresh=1,
                           regex='',
                           sort=0,
                           definition='dbrepo-json- (infinity) json',
                           query=dict(queryType='infinity',
                                      query='',
                                      infinityQuery=dict(format='table',
                                                         filters=[],
                                                         parser='backend',
                                                         refId='variable',
                                                         root_selector='',
                                                         source='url',
                                                         type='json',
                                                         url=f"/api/database/{database.id}/table",
                                                         columns=[dict(selector='id',
                                                                       text='value',
                                                                       type='number'),
                                                                  dict(
                                                                      selector='internal_name',
                                                                      text='name',
                                                                      type='string')],
                                                         url_options=dict(data='',
                                                                          method='GET'))),
                           label='Table ID',
                           skipUrlSync=False,
                           type='query',
                           current=selected,
                           options=options)])


def map_timeseries_panel(database: Database, table: Table) -> dict:
    datasource = dict(uid=datasource_uid,
                      type='yesoreyeram-infinity-datasource')
    return dict(
        title=table['name'],
        type='timeseries',
        datasource=datasource,
        targets=[dict(datasource=datasource,
                      format='table',
                      global_query_id='',
                      hide=False,
                      refId='A',
                      root_selector='',
                      source='url',
                      type='json',
                      url=f"/api/database/{database['id']}/table/{table['id']}",
                      url_options=dict(data='',
                                       method='GET'))],
        gridPos=dict(h=8,
                     w=12,
                     x=0,
                     y=0),
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
                              fillOpacity=0,
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
                              thresholdsStyle=dict(mode='absolute')))))


def map_panels(dashboard: dict, database_id: int | None = None) -> [dict]:
    datasource = dict(uid=datasource_uid,
                      type='yesoreyeram-infinity-datasource')
    if map_statistics_row(dashboard) is None:
        dashboard['panels'].append(dict(collapsed=False,
                                        repeat='table_id',
                                        repeatDirection='h',
                                        title=statistics_row_title,
                                        type='row',
                                        panels=[],
                                        targets=[dict(refId='A',
                                                      datasource=datasource)],
                                        gridPos=dict(h=1,
                                                     w=24,
                                                     x=0,
                                                     y=0)))
        dashboard['panels'].append(dict(title='Sample',
                                        type='table',
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
                                                      url='/api/database/' + str(
                                                          database_id) + '/table/${table_id}/data',
                                                      url_options=dict(data='',
                                                                       method='GET'))],
                                        datasource=datasource,
                                        gridPos=dict(h=4,
                                                     w=12,
                                                     x=0,
                                                     y=0)))
    return dashboard['panels']


def find(uid: str):
    grafana = grafana_client.connect()
    try:
        return grafana.dashboard.get_dashboard(uid)
    except GrafanaException:
        return None


def create(database_name: str, uid: str = '') -> dict:
    grafana = grafana_client.connect()
    dashboard = dict(uid=uid,
                     title=f'{database_name} Overview',
                     tags=['generated', 'dbrepo'],
                     timezone='browser',
                     fiscalYearStartMonth=1,
                     panels=[])
    dashboard['panels'] = map_panels(dashboard)
    payload = dict(folderUid='',
                   overwrite=False,
                   dashboard=dashboard)
    dashboard = grafana.dashboard.update_dashboard(payload)
    logging.info(f"Created dashboard with uid: {dashboard['uid']}")
    return dashboard


def find(uid: str) -> dict | None:
    grafana = grafana_client.connect()
    try:
        return grafana.dashboard.get_dashboard(uid)['dashboard']
    except GrafanaException:
        return None


def delete(uid: str) -> None:
    grafana = grafana_client.connect()
    grafana.dashboard.delete_dashboard(uid)


def update(database: Database) -> dict:
    grafana = grafana_client.connect()
    dashboard = find(database.dashboard_uid)
    # update metadata
    if len(database.identifiers) > 0 and len(database.identifiers[0].titles) > 0:
        dashboard['title'] = database.identifiers[0].titles[0].title
    if len(database.identifiers) > 0 and len(database.identifiers[0].descriptions) > 0:
        dashboard['description'] = database.identifiers[0].descriptions[0].description
    dashboard['links'] = map_links(database)
    dashboard['templating'] = map_templating(database)
    # update panels
    dashboard['panels'] = map_panels(dashboard, database.id)
    payload = dict(folderUid='',
                   overwrite=True,
                   dashboard=dashboard)
    dashboard = grafana.dashboard.update_dashboard(payload)
    logging.info(f"Updated dashboard with uid: {dashboard['uid']}")
    return dashboard

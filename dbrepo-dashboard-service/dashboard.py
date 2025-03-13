import logging
import os

from dbrepo.api.dto import Database, View

from clients import grafana_client

statistics_row_title = '${view_id}'

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
                       panel['type'] == 'row' and panel['title'] == statistics_row_title]
    if len(filtered_panels) == 0:
        logging.warning(f"Failed to find statistics row title {statistics_row_title} in: {filtered_panels}")
        return None
    return filtered_panels[0]


def map_links(database: Database) -> [dict]:
    links = []
    if len(database.identifiers) > 0:
        links.append(map_link('Database', f"{base_url}/pid/{database.identifiers[0].id}"))
    else:
        links.append(map_link('Database', f"{base_url}/database/{database.id}"))
    return links


def map_templating(database: Database) -> dict:
    options = [dict(selected=False,
                    text=view.name,
                    value=str(view.id)) for view in database.views]
    selected = dict(selected=True,
                    text=[view.name for view in database.views],
                    value=[str(view.id) for view in database.views])
    datasource = dict(uid=datasource_uid,
                      type='yesoreyeram-infinity-datasource')
    return dict(list=[dict(description='',
                           name='view_id',
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
                                                         url=f"/api/database/{database.id}/view",
                                                         columns=[dict(selector='id',
                                                                       text='value',
                                                                       type='string'),
                                                                  dict(
                                                                      selector='internal_name',
                                                                      text='name',
                                                                      type='string')],
                                                         url_options=dict(data='',
                                                                          method='GET'))),
                           label='Datasource',
                           skipUrlSync=False,
                           type='query',
                           current=selected,
                           options=options)])


def map_timeseries_panel(database: Database, view: View) -> dict:
    datasource = dict(uid=datasource_uid,
                      type='yesoreyeram-infinity-datasource')
    return dict(
        title=view['name'],
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
                      url=f"/api/database/{database['id']}/view/{view['id']}",
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


def map_statistics_panel(database_id: str, view: View) -> dict:
    datasource = dict(uid=datasource_uid,
                      type='yesoreyeram-infinity-datasource')
    return dict(
        title=view.name,
        type='table',
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
                      url=f"/api/database/{database_id}/view/{view.id}/data",
                      url_options=dict(data='',
                                       method='GET'))],
        options=dict(cellHeight="sm",
                     showHeader=True,
                     footer=dict(countRows=False,
                                 fields="",
                                 reducer=["sum"],
                                 show=False)),
        gridPos=dict(h=8,
                     w=12,
                     x=12,
                     y=0),
        transformations=dict(id="organize",
                             options=dict(excludeByName=dict(),
                                          includeByName=dict(),
                                          indexByName=dict(
                                              HEADER_AVG=3,
                                              HEADER_COL=0,
                                              HEADER_STDDEV=4,
                                              HEADER_MAX=2,
                                              HEADER_MIN=1))),
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
                         overrides=[dict(matcher=dict(id="byName",
                                                      options="HEADER_COL"),
                                         properties=[dict(id="custom.align",
                                                          value="center")]),
                                    dict(matcher=dict(id="byName",
                                                      options="HEADER_MIN"),
                                         properties=[dict(id="custom.width",
                                                          value=115)]),
                                    dict(matcher=dict(id="byName",
                                                      options="HEADER_MAX"),
                                         properties=[dict(id="custom.width",
                                                          value=115)]),
                                    dict(matcher=dict(id="byName",
                                                      options="HEADER_AVG"),
                                         properties=[dict(id="custom.width",
                                                          value=115)]),
                                    dict(matcher=dict(id="byName",
                                                      options="HEADER_STDDEV"),
                                         properties=[dict(id="custom.width",
                                                          value=115)])
                                    ]))


def map_overview_panel(database_id: str) -> dict:
    datasource = dict(uid=datasource_uid,
                      type='yesoreyeram-infinity-datasource')
    return dict(title='Preview',
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
                              url='/api/database/' + database_id + '/view/${view_id}/data',
                              url_options=dict(data='',
                                               method='GET'))],
                datasource=datasource,
                gridPos=dict(h=4,
                             w=12,
                             x=0,
                             y=0))


def map_row() -> dict:
    datasource = dict(uid=datasource_uid,
                      type='yesoreyeram-infinity-datasource')
    return dict(collapsed=False,
                repeat='view_id',
                repeatDirection='h',
                title=statistics_row_title,
                type='row',
                panels=[],
                targets=[dict(refId='A',
                              datasource=datasource)],
                gridPos=dict(h=1,
                             w=24,
                             x=0,
                             y=0))


def map_panels(dashboard: dict, database: Database) -> [dict]:
    if map_statistics_row(dashboard) is None:
        dashboard['panels'].append(map_row())
        dashboard['panels'].append(map_overview_panel(database.id))
        for view in database.views:
            dashboard['panels'].append(map_statistics_panel(database.id, view))
    return dashboard['panels']


def find(uid: str):
    grafana = grafana_client.connect()
    return grafana.dashboard.get_dashboard(uid)


def create(database_name: str, uid: str = '') -> dict:
    grafana = grafana_client.connect()
    dashboard = dict(uid=uid,
                     title=f'{database_name} Overview',
                     tags=['dbrepo'],
                     timezone='browser',
                     fiscalYearStartMonth=1,
                     panels=[])
    dashboard['panels'] = []
    payload = dict(folderUid='',
                   overwrite=False,
                   dashboard=dashboard)
    dashboard = grafana.dashboard.update_dashboard(payload)
    logging.info(f"Created dashboard with uid: {dashboard['uid']}")
    return dashboard


def delete(uid: str) -> None:
    grafana = grafana_client.connect()
    grafana.dashboard.delete_dashboard(uid)


def update(database: Database) -> None:
    grafana = grafana_client.connect()
    dashboard = find(database.dashboard_uid)['dashboard']
    # update metadata
    if len(database.identifiers) > 0 and len(database.identifiers[0].titles) > 0:
        dashboard['title'] = database.identifiers[0].titles[0].title
    if len(database.identifiers) > 0 and len(database.identifiers[0].descriptions) > 0:
        dashboard['description'] = database.identifiers[0].descriptions[0].description
    dashboard['links'] = map_links(database)
    dashboard['templating'] = map_templating(database)
    # update panels
    dashboard['panels'] = map_panels(dashboard, database)
    payload = dict(folderUid='',
                   overwrite=True,
                   dashboard=dashboard)
    response = grafana.dashboard.update_dashboard(payload)
    logging.info(f"Updated dashboard with uid: {response['uid']}")

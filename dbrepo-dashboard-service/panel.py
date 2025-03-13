import os

datasource_uid = os.getenv('JSON_DATASOURCE_NAME', 'dbrepojson0')

statistics_row_title = '${view_id}'


def _get_start_index(dashboard: dict) -> int:
    return [panel['title'] for panel in dashboard['panels']].index(statistics_row_title)


def get_panels(dashboard: dict) -> [dict]:
    return []


def map_timeseries_panel(database_id: str) -> dict:
    datasource = dict(uid=datasource_uid,
                      type='yesoreyeram-infinity-datasource')
    return dict(title='${view_id}',
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
                              url='/api/database/' + database_id + '/view/${view_id}/data',
                              url_options=dict(data='',
                                               method='GET'))],
                gridPos=dict(h=8,
                             w=12,
                             x=12,
                             y=8),
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


def map_number_panel(database_id: str, title: str, root_selector: str, y: int = 0) -> dict:
    datasource = dict(uid=datasource_uid,
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
                              root_selector=root_selector,
                              source='url',
                              type='json',
                              url='/api/database/' + database_id + '/view/${view_id}/statistic',
                              url_options=dict(data='',
                                               method='GET'))],
                fieldConfig=dict(defaults=dict(mappings=[],
                                               thresholds=dict(mode='absolute',
                                                               steps=[dict(color='blue',
                                                                           value=None)]),
                                               unit=''),
                                 overrides=[]),
                gridPos=dict(h=4,
                             w=6,
                             x=18,
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


def map_statistics_panel(database_id: str) -> dict:
    datasource = dict(uid=datasource_uid,
                      type='yesoreyeram-infinity-datasource')
    return dict(title='Statistics',
                type='table',
                gridPos=dict(h=8,
                             w=12,
                             x=0,
                             y=8),
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
                              url='/api/database/' + database_id + '/view/${view_id}/statistic',
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


def map_overview_panel(database_id: str) -> dict:
    datasource = dict(uid=datasource_uid,
                      type='yesoreyeram-infinity-datasource')
    return dict(title='Datasource Preview',
                type='table',
                gridPos=dict(h=8,
                             w=18,
                             x=0,
                             y=4),
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
                datasource=datasource)


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
    if get_statistics_row(dashboard) is None:
        dashboard['panels'].append(map_row())  # repeating
        dashboard['panels'].append(map_overview_panel(database.id))  # left top
        dashboard['panels'].append(map_number_panel(database.id, 'Total Entries', 'rows', 0))  # right top
        dashboard['panels'].append(map_number_panel(database.id, 'Variables', '$count(columns)', 4))  # right top
        dashboard['panels'].append(map_statistics_panel(database.id))  # left
        dashboard['panels'].append(map_timeseries_panel(database.id))  # middle
    return dashboard['panels']

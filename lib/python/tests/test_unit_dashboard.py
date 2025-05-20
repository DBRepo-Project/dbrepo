import json
import unittest

from dbrepo.api.dto import ColumnType, ViewColumn, View, UserBrief
from dbrepo.core.client.dashboard import map_link, _get_managed_offset_y, _get_start_index, map_column_conversion, \
    map_row, map_preview_image_panel, auto_generated_description, DashboardServiceClient

exp = View(id="1b3449d2-780e-4683-9af0-8733e608a4aa",
           name="Data",
           internal_name="data",
           database_id="6bd39359-b154-456d-b9c2-caa516a45732",
           initial_view=False,
           query="SELECT id FROM mytable WHERE deg > 0",
           query_hash="94c74728b11a690e51d64719868824735f0817b7",
           owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'),
           is_public=True,
           is_schema_public=True,
           columns=[ViewColumn(id="1b3449d2-780e-4683-9af0-8733e608a4aa",
                               ord=0,
                               name="id",
                               internal_name="id",
                               database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                               type=ColumnType.BIGINT,
                               is_null_allowed=False)],
           identifiers=[])


class DashboardUnitTest(unittest.TestCase):

    def test_map_link_succeeds(self):
        # test
        response = map_link(title="Link", url="http://example.com")
        self.assertEqual("Link", response['title'])
        self.assertEqual("http://example.com", response['url'])
        self.assertEqual("info", response['icon'])
        self.assertTrue(response['targetBlank'])
        self.assertEqual("link", response['type'])

    def test_get_managed_offset_y_with_unmanaged_content_succeeds(self):
        with open('./tests/grafana/managed_dashboard_with_unmanaged_content.json', 'r') as f:
            dashboard = json.load(f)
            # test
            response = _get_managed_offset_y(dashboard)
            self.assertEqual(20, response)

    def test_get_managed_offset_y_succeeds(self):
        with open('./tests/grafana/managed_dashboard.json', 'r') as f:
            dashboard = json.load(f)
            # test
            response = _get_managed_offset_y(dashboard)
            self.assertEqual(0, response)

    def test_get_start_index_with_unmanaged_content_succeeds(self):
        with open('./tests/grafana/managed_dashboard_with_unmanaged_content.json', 'r') as f:
            dashboard = json.load(f)
            # test
            response = _get_start_index(dashboard)
            self.assertEqual(4, response)

    def test_get_start_index_succeeds(self):
        with open('./tests/grafana/managed_dashboard.json', 'r') as f:
            dashboard = json.load(f)
            # test
            response = _get_start_index(dashboard)
            self.assertEqual(0, response)

    def test_map_column_conversion_numbers_succeeds(self):
        column = ViewColumn(id='41e6b7e4-1295-4664-84c0-74f2b70cb031', ord=0, name='col', internal_name='col',
                            database_id='39ff4138-ebe1-4978-9ddf-930b118427cb', is_null_allowed=False,
                            type=ColumnType.BOOL)
        # test
        for number_type in [ColumnType.SERIAL, ColumnType.BIT, ColumnType.SMALLINT, ColumnType.MEDIUMINT,
                            ColumnType.INT, ColumnType.BIGINT, ColumnType.FLOAT, ColumnType.DOUBLE, ColumnType.DECIMAL]:
            column.type = number_type
            response = map_column_conversion(column)
            self.assertEqual('number', response['destinationType'])

    def test_map_column_conversion_time_succeeds(self):
        column = ViewColumn(id='41e6b7e4-1295-4664-84c0-74f2b70cb031', ord=0, name='col', internal_name='col',
                            database_id='39ff4138-ebe1-4978-9ddf-930b118427cb', is_null_allowed=False,
                            type=ColumnType.BOOL)
        # test
        for time_type in [ColumnType.DATE, ColumnType.TIME, ColumnType.TIMESTAMP, ColumnType.YEAR]:
            column.type = time_type
            response = map_column_conversion(column)
            self.assertEqual('time', response['destinationType'])
            if time_type == ColumnType.YEAR:
                self.assertEqual('YYYY', response['dateFormat'])
            elif time_type == ColumnType.TIME:
                self.assertEqual('HH:mm:ss', response['dateFormat'])
            else:
                self.assertEqual('YYYY-MM-dd', response['dateFormat'])

    def test_map_column_conversion_boolean_succeeds(self):
        column = ViewColumn(id='41e6b7e4-1295-4664-84c0-74f2b70cb031', ord=0, name='col', internal_name='col',
                            database_id='39ff4138-ebe1-4978-9ddf-930b118427cb', is_null_allowed=False,
                            type=ColumnType.BOOL)
        # test
        for bool_type in [ColumnType.TINYINT, ColumnType.BOOL]:
            column.type = bool_type
            response = map_column_conversion(column)
            self.assertEqual('boolean', response['destinationType'])

    def test_map_row_succeeds(self):
        # test
        response = map_row("some row")
        self.assertFalse(response['collapsed'])
        self.assertEqual('some row', response['title'])
        self.assertEqual('row', response['type'])
        self.assertEqual([], response['panels'])
        self.assertEqual([], response['targets'])
        self.assertEqual('backend', response['parser'])
        self.assertEqual(24, response['gridPos']['w'])
        self.assertEqual(1, response['gridPos']['h'])
        self.assertEqual(0, response['gridPos']['x'])
        self.assertEqual(0, response['gridPos']['y'])

    def test_map_preview_image_panel_succeeds(self):
        # test
        response = map_preview_image_panel('39ff4138-ebe1-4978-9ddf-930b118427cb')
        self.assertEqual('Preview Image', response['title'])
        self.assertEqual('text', response['type'])
        self.assertEqual(auto_generated_description, response['description'])
        self.assertEqual(4, response['gridPos']['w'])
        self.assertEqual(4, response['gridPos']['h'])
        self.assertEqual(20, response['gridPos']['x'])
        self.assertEqual(0, response['gridPos']['y'])
        self.assertEqual(dict(), response['fieldConfig']['defaults'])
        self.assertEqual([], response['fieldConfig']['overrides'])
        self.assertEqual('markdown', response['options']['mode'])
        self.assertEqual('plaintext', response['options']['code']['language'])
        self.assertFalse(response['options']['code']['showLineNumbers'])
        self.assertFalse(response['options']['code']['showMiniMap'])
        self.assertTrue('/api/database/39ff4138-ebe1-4978-9ddf-930b118427cb/image' in response['options']['content'])

    def test_map_timeseries_panel_succeeds(self):
        # mock
        client = DashboardServiceClient('http://localhost', 'admin', 'admin')
        datasource = dict(uid='dbrepojson0',
                          type='yesoreyeram-infinity-datasource')
        # test
        response = client.map_timeseries_panel('39ff4138-ebe1-4978-9ddf-930b118427cb', exp)
        self.assertEqual('Timeseries', response['title'])
        self.assertEqual(auto_generated_description, response['description'])
        self.assertEqual('timeseries', response['type'])
        self.assertEqual(datasource, response['datasource'])
        self.assertEqual(datasource, response['targets'][0]['datasource'])
        self.assertEqual('table', response['targets'][0]['format'])
        self.assertFalse(response['targets'][0]['hide'])
        self.assertEqual('A', response['targets'][0]['refId'])
        self.assertEqual('', response['targets'][0]['root_selector'])
        self.assertEqual('url', response['targets'][0]['source'])
        self.assertEqual('json', response['targets'][0]['type'])
        self.assertEqual(f'/api/database/39ff4138-ebe1-4978-9ddf-930b118427cb/view/{exp.id}/data',
                         response['targets'][0]['url'])
        self.assertEqual('backend', response['targets'][0]['parser'])
        self.assertEqual('GET', response['targets'][0]['url_options']['method'])
        self.assertEqual(12, response['gridPos']['w'])
        self.assertEqual(8, response['gridPos']['h'])
        self.assertEqual(12, response['gridPos']['x'])
        self.assertEqual(8, response['gridPos']['y'])

    def test_map_pie_panel_succeeds(self):
        # mock
        client = DashboardServiceClient('http://localhost', 'admin', 'admin')
        # test
        response = client.map_pie_panel('39ff4138-ebe1-4978-9ddf-930b118427cb', exp)
        self.assertEqual('Piechart', response['title'])

    def test_map_histogram_panel_succeeds(self):
        # mock
        client = DashboardServiceClient('http://localhost', 'admin', 'admin')
        # test
        response = client.map_histogram_panel('39ff4138-ebe1-4978-9ddf-930b118427cb', exp)
        self.assertEqual('Histogram', response['title'])


if __name__ == "__main__":
    unittest.main()

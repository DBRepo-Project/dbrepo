import unittest
from unittest.mock import Mock, patch

from dbrepo.api.dto import Database, Table
from dbrepo.core.client.dashboard import DashboardServiceClient


class DashboardClientTest(unittest.TestCase):
    def setUp(self):
        self.client = DashboardServiceClient('http://localhost', 'admin', 'secret')
        self.database = Database.model_construct(
            id='db-1', name='Measurements', internal_name='replica_internal',
            dashboard_uid='dash-1', is_dashboard_enabled=True, identifiers=[],
            preview_image=None, views=[], tables=[
                Table.model_construct(id='table-1', name='Readings', archived_at=None),
                Table.model_construct(id='table-2', name='Archived', archived_at='2026-01-01'),
            ])

    def test_update_keeps_title_and_custom_panels(self):
        custom = {'title': 'Custom', 'type': 'text', 'gridPos': {'y': 0, 'h': 4}}
        self.client.find = Mock(return_value={'dashboard': {
            'title': 'replica_internal Overview', 'panels': [custom], 'tags': ['managed']}})
        self.client.client.dashboard.update_dashboard = Mock(return_value={'uid': 'dash-1'})
        self.client.update(self.database)
        dashboard = self.client.client.dashboard.update_dashboard.call_args.args[0]['dashboard']
        self.assertEqual('replica_internal Overview', dashboard['title'])
        self.assertEqual(custom, dashboard['panels'][0])
        managed_row = dashboard['panels'][1]
        self.assertGreaterEqual(managed_row['gridPos']['y'], 4)

    def test_tables_without_views_do_not_add_previews(self):
        panels = self.client.get_panels({'panels': []}, self.database)
        previews = [p for p in panels if p['type'] == 'table']
        self.assertEqual([], previews)
        self.assertNotIn('Tables', [p['title'] for p in panels])
        self.assertIn('Datasources', [p['title'] for p in panels])

    def test_regeneration_does_not_duplicate_panels(self):
        first = self.client.get_panels({'panels': []}, self.database)
        second = self.client.get_panels({'panels': first}, self.database)
        self.assertEqual(first, second)
        self.assertEqual(2, len(first))

    def test_links_use_site_url_from_environment(self):
        with patch.dict('os.environ', {'BASE_URL': 'https://site.example'}):
            client = DashboardServiceClient('http://grafana', 'admin', 'secret')
        self.assertEqual('https://site.example/database/db-1',
                         client.map_links(self.database)[0]['url'])

    def test_explicit_site_url_takes_precedence(self):
        with patch.dict('os.environ', {'BASE_URL': 'https://other.example'}):
            client = DashboardServiceClient('http://grafana', 'admin', 'secret', 'https://chosen.example')
        self.assertEqual('https://chosen.example', client.base_url)


if __name__ == '__main__':
    unittest.main()

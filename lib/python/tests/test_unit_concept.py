import unittest
from unittest.mock import patch

from dbrepo.RestClient import RestClient


class RemovedCatalogTest(unittest.TestCase):
    def test_removed_catalog_has_actionable_error_without_http(self):
        with patch.object(RestClient, '_wrapper') as request:
            with self.assertRaisesRegex(NotImplementedError, 'update_table_column'):
                RestClient().get_concepts()
            request.assert_not_called()

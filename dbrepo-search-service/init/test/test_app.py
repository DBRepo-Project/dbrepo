import unittest

from app import App
from clients.opensearch_client import OpenSearchClient


class AppTest(unittest.TestCase):

    def test_index_update_succeeds(self):
        # test
        app = App()
        app.index_update()

    def test_index_update_not_exists_succeeds(self):
        # mock
        client = OpenSearchClient()
        client._instance().indices.delete(index="database")

        # test
        app = App()
        app.index_update()

import unittest

import requests_mock
from dbrepo.api.dto import UserBrief, DatabaseBrief, Database, ContainerBrief, ImageBrief

from app import fetch_databases

exp = DatabaseBrief(
    id="6bd39359-b154-456d-b9c2-caa516a45732",
    name='test',
    owner_id='8638c043-5145-4be8-a3e4-4b79991b0a16',
    contact=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'),
    internal_name='test_abcd',
    is_public=True,
    is_schema_public=True
)


class AppIntegrationTest(unittest.TestCase):

    def test_fetch_databases_succeeds(self):
        db = Database(
            id="6bd39359-b154-456d-b9c2-caa516a45732",
            name='test',
            owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'),
            contact=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'),
            exchange_name='dbrepo',
            internal_name='test_abcd',
            is_public=True,
            is_schema_public=True,
            is_dashboard_enabled=True,
            container=ContainerBrief(
                id="44d811a8-4019-46ba-bd57-ea10a2eb0c74",
                name='MariaDB Galera 11.1.3',
                internal_name='mariadb',
                image=ImageBrief(
                    id="b104648b-54d2-4d72-9834-8e0e6d428b39",
                    name='mariadb',
                    version='11.2.2',
                    default=True)
            )
        )
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/v1/database', json=[exp.model_dump()])
            mock.get(f'/api/v1/database/{exp.id}', json=db.model_dump())
            # test
            response = fetch_databases()
            self.assertEqual(1, len(response))
            self.assertEqual(db, response[0])

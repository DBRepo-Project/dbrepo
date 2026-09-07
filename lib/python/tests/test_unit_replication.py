import unittest

import requests_mock

from dbrepo.RestClient import RestClient
from dbrepo.api.exceptions import AuthenticationError, ForbiddenError, MalformedError, NotExistsError


class ReplicationUnitTest(unittest.TestCase):

    def test_get_replication_status_succeeds(self):
        payload = {
            'health': {
                'status': 'UP',
                'metadata_service': {
                    'name': 'metadata-service',
                    'status': 'UP',
                    'http_status': 200,
                    'duration_ms': 7
                },
                'data_service': {
                    'name': 'data-service',
                    'status': 'UP',
                    'http_status': 200,
                    'duration_ms': 11
                },
                'replication_service': {
                    'name': 'replication-service',
                    'status': 'UP',
                    'duration_ms': 0
                }
            },
            'outbox': {
                'total': 3,
                'pending': 1,
                'failed': 0,
                'succeeded': 2,
                'oldest_pending_at': '2026-09-07T10:00:00Z',
                'next_attempt_at': '2026-09-07T10:05:00Z'
            }
        }
        with requests_mock.Mocker() as mock:
            mock.get('/api/replication/status', json=payload)

            response = RestClient(username="system", password="secret").get_replication_status()

            self.assertEqual('UP', response.health.status)
            self.assertEqual('metadata-service', response.health.metadata_service.name)
            self.assertEqual(1, response.outbox.pending)
            self.assertEqual(2026, response.outbox.oldest_pending_at.year)

    def test_get_replication_status_requires_authentication(self):
        with self.assertRaises(AuthenticationError):
            RestClient().get_replication_status()

    def test_get_replication_status_403_fails(self):
        with requests_mock.Mocker() as mock:
            mock.get('/api/replication/status', status_code=403)

            with self.assertRaises(ForbiddenError):
                RestClient(username="system", password="secret").get_replication_status()

    def test_synchronise_database_replication_succeeds(self):
        database_id = '6bd39359-b154-456d-b9c2-caa516a45732'
        payload = {
            'status': 'completed',
            'tables': 2,
            'pages': 3,
            'tuples': 150,
            'replicaWrites': 300
        }
        with requests_mock.Mocker() as mock:
            mock.post(f'/api/replication/data/synchronise/database/{database_id}', json=payload)

            response = RestClient(username="system", password="secret").synchronise_database_replication(
                database_id=database_id,
                page_size=50)

            self.assertEqual(2, response.tables)
            self.assertEqual(300, response.replica_writes)
            self.assertEqual('50', mock.last_request.qs['pagesize'][0])

    def test_synchronise_database_replication_400_fails(self):
        database_id = '6bd39359-b154-456d-b9c2-caa516a45732'
        with requests_mock.Mocker() as mock:
            mock.post(f'/api/replication/data/synchronise/database/{database_id}', status_code=400)

            with self.assertRaises(MalformedError):
                RestClient(username="system", password="secret").synchronise_database_replication(
                    database_id=database_id,
                    page_size=0)

    def test_synchronise_table_replication_succeeds(self):
        database_id = '6bd39359-b154-456d-b9c2-caa516a45732'
        table_id = 'd39a0f4d-502c-47dc-b0f2-9089d8e9c935'
        payload = {
            'status': 'completed',
            'pages': 2,
            'tuples': 101,
            'replicaWrites': 202
        }
        with requests_mock.Mocker() as mock:
            mock.post(f'/api/replication/data/synchronise/database/{database_id}/table/{table_id}', json=payload)

            response = RestClient(username="system", password="secret").synchronise_table_replication(
                database_id=database_id,
                table_id=table_id)

            self.assertEqual(2, response.pages)
            self.assertEqual(202, response.replica_writes)

    def test_synchronise_table_replication_404_fails(self):
        database_id = '6bd39359-b154-456d-b9c2-caa516a45732'
        table_id = 'd39a0f4d-502c-47dc-b0f2-9089d8e9c935'
        with requests_mock.Mocker() as mock:
            mock.post(f'/api/replication/data/synchronise/database/{database_id}/table/{table_id}', status_code=404)

            with self.assertRaises(NotExistsError):
                RestClient(username="system", password="secret").synchronise_table_replication(
                    database_id=database_id,
                    table_id=table_id)


if __name__ == "__main__":
    unittest.main()

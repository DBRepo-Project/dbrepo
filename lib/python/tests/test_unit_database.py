import unittest

import requests_mock

from dbrepo.RestClient import RestClient
from dbrepo.api.dto import Database, DatabaseAccess, AccessType, DatabaseBrief, UserBrief, \
    ContainerBrief, ImageBrief
from dbrepo.api.exceptions import ResponseCodeError, NotExistsError, ForbiddenError, MalformedError, \
    AuthenticationError, QueryStoreError, ServiceConnectionError, ServiceError


class DatabaseUnitTest(unittest.TestCase):

    def test_get_databases_empty_succeeds(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database', json=[])
            # test
            response = RestClient().get_databases()
            self.assertEqual([], response)

    def test_get_databases_succeeds(self):
        exp = [
            DatabaseBrief(
                id="6bd39359-b154-456d-b9c2-caa516a45732",
                name='test',
                owner_id='8638c043-5145-4be8-a3e4-4b79991b0a16',
                contact=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'),
                internal_name='test_abcd',
                is_public=True,
                is_schema_public=True
            )
        ]
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database', json=[exp[0].model_dump()])
            # test
            response = RestClient().get_databases()
            self.assertEqual(exp, response)

    def test_get_databases_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database', status_code=401)
            # test
            try:
                RestClient().get_databases()
            except ResponseCodeError:
                pass

    def test_get_databases_count_succeeds(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.head('/api/database', headers={'X-Count': '100'})
            # test
            response = RestClient().get_databases_count()
            self.assertEqual(100, response)

    def test_get_databases_count_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.head('/api/database', status_code=401)
            # test
            try:
                RestClient().get_databases_count()
            except ResponseCodeError:
                pass

    def test_get_database_succeeds(self):
        exp = Database(
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
            mock.get('/api/database/6bd39359-b154-456d-b9c2-caa516a45732', json=exp.model_dump())
            # test
            response = RestClient().get_database(database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            self.assertEqual(exp, response)

    def test_get_database_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/6bd39359-b154-456d-b9c2-caa516a45732', status_code=403)
            # test
            try:
                RestClient().get_database(database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            except ForbiddenError as e:
                pass

    def test_get_database_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/6bd39359-b154-456d-b9c2-caa516a45732', status_code=404)
            # test
            try:
                RestClient().get_database(database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            except NotExistsError as e:
                pass

    def test_get_database_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/6bd39359-b154-456d-b9c2-caa516a45732', status_code=202)
            # test
            try:
                RestClient().get_database(database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            except ResponseCodeError as e:
                pass

    def test_create_database_succeeds(self):
        exp = Database(
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
            mock.post('/api/database', json=exp.model_dump(), status_code=201)
            # test
            client = RestClient(username="a", password="b")
            response = RestClient(username="a", password="b").create_database(name='test',
                                                                              container_id="44d811a8-4019-46ba-bd57-ea10a2eb0c74",
                                                                              is_public=True)
            self.assertEqual(response.name, 'test')

    def test_create_database_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database', status_code=400)
            # test
            try:
                RestClient(username="a", password="b").create_database(name='test',
                                                                       container_id="44d811a8-4019-46ba-bd57-ea10a2eb0c74",
                                                                       is_public=True)
            except MalformedError as e:
                pass

    def test_create_database_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database', status_code=403)
            # test
            try:
                RestClient(username="a", password="b").create_database(name='test',
                                                                       container_id="44d811a8-4019-46ba-bd57-ea10a2eb0c74",
                                                                       is_public=True)
            except ForbiddenError as e:
                pass

    def test_create_database_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database', status_code=404)
            # test
            try:
                RestClient(username="a", password="b").create_database(name='test',
                                                                       container_id="44d811a8-4019-46ba-bd57-ea10a2eb0c74",
                                                                       is_public=True)
            except NotExistsError as e:
                pass

    def test_create_database_409_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database', status_code=409)
            # test
            try:
                RestClient(username="a", password="b").create_database(name='test',
                                                                       container_id="44d811a8-4019-46ba-bd57-ea10a2eb0c74",
                                                                       is_public=True)
            except QueryStoreError as e:
                pass

    def test_create_database_502_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database', status_code=502)
            # test
            try:
                RestClient(username="a", password="b").create_database(name='test',
                                                                       container_id="44d811a8-4019-46ba-bd57-ea10a2eb0c74",
                                                                       is_public=True)
            except ServiceConnectionError as e:
                pass

    def test_create_database_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database', status_code=503)
            # test
            try:
                RestClient(username="a", password="b").create_database(name='test',
                                                                       container_id="44d811a8-4019-46ba-bd57-ea10a2eb0c74",
                                                                       is_public=True)
            except ServiceError as e:
                pass

    def test_create_database_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database', status_code=202)
            # test
            try:
                RestClient(username="a", password="b").create_database(name='test',
                                                                       container_id="44d811a8-4019-46ba-bd57-ea10a2eb0c74",
                                                                       is_public=True)
            except ResponseCodeError as e:
                pass

    def test_create_database_anonymous_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database', status_code=404)
            # test
            try:
                RestClient().create_database(name='test',
                                             container_id="44d811a8-4019-46ba-bd57-ea10a2eb0c74",
                                             is_public=True)
            except AuthenticationError as e:
                pass

    def test_update_database_visibility_succeeds(self):
        exp = Database(
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
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/visibility', json=exp.model_dump(),
                     status_code=202)
            # test
            response = RestClient(username="a", password="b").update_database_visibility(
                database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                is_public=True,
                is_schema_public=True)
            self.assertEqual(response.is_public, True)

    def test_update_database_visibility_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/visibility', status_code=400)
            # test
            try:
                RestClient(username="a", password="b").update_database_visibility(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    is_public=True,
                    is_schema_public=True)
            except MalformedError:
                pass

    def test_update_database_visibility_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/visibility', status_code=403)
            # test
            try:
                response = RestClient(username="a", password="b").update_database_visibility(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    is_public=True,
                    is_schema_public=True)
            except ForbiddenError:
                pass

    def test_update_database_visibility_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/visibility', status_code=404)
            # test
            try:
                response = RestClient(username="a", password="b").update_database_visibility(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    is_public=True,
                    is_schema_public=True)
            except NotExistsError:
                pass

    def test_update_database_visibility_502_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/visibility', status_code=502)
            # test
            try:
                RestClient(username="a", password="b").update_database_visibility(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    is_public=True,
                    is_schema_public=True)
            except ServiceConnectionError:
                pass

    def test_update_database_visibility_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/visibility', status_code=503)
            # test
            try:
                RestClient(username="a", password="b").update_database_visibility(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    is_public=True,
                    is_schema_public=True)
            except ServiceError:
                pass

    def test_update_database_visibility_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/visibility', status_code=200)
            # test
            try:
                RestClient(username="a", password="b").update_database_visibility(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    is_public=True,
                    is_schema_public=True)
            except ResponseCodeError:
                pass

    def test_update_database_visibility_anonymous_fails(self):
        # test
        try:
            RestClient().update_database_visibility(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                    is_public=True, is_schema_public=True)
        except AuthenticationError:
            pass

    def test_update_database_owner_succeeds(self):
        exp = Database(
            id="6bd39359-b154-456d-b9c2-caa516a45732",
            name='test',
            owner=UserBrief(id='abdbf897-e599-4e5a-a3f0-7529884ea011', username='mweise'),
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
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/owner', json=exp.model_dump(), status_code=202)
            # test
            client = RestClient(username="a", password="b")
            response = RestClient(username="a", password="b").update_database_owner(
                database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            self.assertEqual(response.owner.id, 'abdbf897-e599-4e5a-a3f0-7529884ea011')

    def test_update_database_owner_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/owner', status_code=400)
            # test
            try:
                RestClient(username="a", password="b").update_database_owner(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except MalformedError:
                pass

    def test_update_database_owner_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/owner', status_code=403)
            # test
            try:
                RestClient(username="a", password="b").update_database_owner(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except ForbiddenError:
                pass

    def test_update_database_owner_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/owner', status_code=404)
            # test
            try:
                RestClient(username="a", password="b").update_database_owner(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except NotExistsError:
                pass

    def test_update_database_owner_502_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/owner', status_code=502)
            # test
            try:
                RestClient(username="a", password="b").update_database_owner(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except ServiceConnectionError:
                pass

    def test_update_database_owner_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/owner', status_code=503)
            # test
            try:
                RestClient(username="a", password="b").update_database_owner(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except ServiceError:
                pass

    def test_update_database_owner_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/owner', status_code=200)
            # test
            try:
                RestClient(username="a", password="b").update_database_owner(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except ResponseCodeError:
                pass

    def test_update_database_owner_anonymous_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/owner', status_code=404)
            # test
            try:
                RestClient().update_database_owner(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                   user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except AuthenticationError:
                pass

    def test_update_database_schema_succeeds(self):
        exp = DatabaseBrief(
            id="6bd39359-b154-456d-b9c2-caa516a45732",
            name='test',
            owner_id='8638c043-5145-4be8-a3e4-4b79991b0a16',
            contact=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'),
            internal_name='test_abcd',
            is_public=True,
            is_schema_public=True
        )
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/metadata/table', json=exp.model_dump())
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/metadata/view', json=exp.model_dump())
            # test
            response = RestClient(username='foo', password='bar').update_database_schema(
                database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            self.assertEqual(exp, response)

    def test_update_database_schema_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/metadata/table', status_code=400)
            # test
            try:
                RestClient(username='foo', password='bar').update_database_schema(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            except MalformedError:
                pass

    def test_update_database_schema_view_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/metadata/table', json={}, status_code=200)
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/metadata/view', status_code=400)
            # test
            try:
                RestClient(username='foo', password='bar').update_database_schema(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            except MalformedError:
                pass

    def test_update_database_schema_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/metadata/table', status_code=403)
            # test
            try:
                RestClient(username='foo', password='bar').update_database_schema(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            except ForbiddenError:
                pass

    def test_update_database_schema_view_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/metadata/table', json={}, status_code=200)
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/metadata/view', status_code=403)
            # test
            try:
                RestClient(username='foo', password='bar').update_database_schema(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            except ForbiddenError:
                pass

    def test_update_database_schema_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/metadata/table', status_code=404)
            # test
            try:
                RestClient(username='foo', password='bar').update_database_schema(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            except NotExistsError:
                pass

    def test_update_database_schema_view_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/metadata/table', json={}, status_code=200)
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/metadata/view', status_code=404)
            # test
            try:
                RestClient(username='foo', password='bar').update_database_schema(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            except NotExistsError:
                pass

    def test_update_database_schema_502_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/metadata/table', status_code=502)
            # test
            try:
                RestClient(username='foo', password='bar').update_database_schema(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            except ServiceConnectionError:
                pass

    def test_update_database_schema_view_502_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/metadata/table', json={}, status_code=200)
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/metadata/view', status_code=502)
            # test
            try:
                RestClient(username='foo', password='bar').update_database_schema(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            except ServiceConnectionError:
                pass

    def test_update_database_schema_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/metadata/table', status_code=503)
            # test
            try:
                RestClient(username='foo', password='bar').update_database_schema(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            except ServiceError:
                pass

    def test_update_database_schema_view_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/metadata/table', json={}, status_code=200)
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/metadata/view', status_code=503)
            # test
            try:
                RestClient(username='foo', password='bar').update_database_schema(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            except ServiceError:
                pass

    def test_update_database_schema_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/metadata/table', status_code=202)
            # test
            try:
                RestClient(username='foo', password='bar').update_database_schema(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            except ResponseCodeError:
                pass

    def test_update_database_schema_view_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/metadata/table', json={}, status_code=200)
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/metadata/view', status_code=202)
            # test
            try:
                RestClient(username='foo', password='bar').update_database_schema(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            except ResponseCodeError:
                pass

    def test_update_database_schema_anonymous_fails(self):
        # test
        try:
            RestClient().update_database_schema(database_id="6bd39359-b154-456d-b9c2-caa516a45732")
        except AuthenticationError:
            pass

    def test_get_database_access_succeeds(self):
        exp = DatabaseAccess(type=AccessType.READ,
                             user=UserBrief(id='abdbf897-e599-4e5a-a3f0-7529884ea011', username='other'))
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/access', json=exp.model_dump())
            # test
            response = RestClient().get_database_access(database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            self.assertEqual(response, AccessType.READ)

    def test_get_database_access_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/access', status_code=403)
            # test
            try:
                RestClient().get_database_access(database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            except ForbiddenError:
                pass

    def test_get_database_access_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/access', status_code=404)
            # test
            try:
                RestClient().get_database_access(database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            except NotExistsError:
                pass

    def test_get_database_access_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/access', status_code=202)
            # test
            try:
                RestClient().get_database_access(database_id="6bd39359-b154-456d-b9c2-caa516a45732")
            except ResponseCodeError:
                pass

    def test_create_database_access_succeeds(self):
        exp = DatabaseAccess(type=AccessType.READ,
                             user=UserBrief(id='abdbf897-e599-4e5a-a3f0-7529884ea011', username='other'))
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/access/abdbf897-e599-4e5a-a3f0-7529884ea011',
                      json=exp.model_dump(),
                      status_code=202)
            # test
            response = RestClient(username="a", password="b").create_database_access(
                database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                type=AccessType.READ,
                user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            self.assertEqual(response, exp.type)

    def test_create_database_access_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/access/abdbf897-e599-4e5a-a3f0-7529884ea011',
                      status_code=400)
            # test
            try:
                RestClient(username="a", password="b").create_database_access(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    type=AccessType.READ,
                    user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except MalformedError:
                pass

    def test_create_database_access_anonymous_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/access/abdbf897-e599-4e5a-a3f0-7529884ea011',
                      status_code=400)
            # test
            try:
                RestClient().create_database_access(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                    type=AccessType.READ,
                                                    user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except AuthenticationError:
                pass

    def test_create_database_access_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/access/abdbf897-e599-4e5a-a3f0-7529884ea011',
                      status_code=403)
            # test
            try:
                RestClient(username="a", password="b").create_database_access(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    type=AccessType.READ,
                    user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except ForbiddenError:
                pass

    def test_create_database_access_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/access/abdbf897-e599-4e5a-a3f0-7529884ea011',
                      status_code=404)
            # test
            try:
                RestClient(username="a", password="b").create_database_access(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    type=AccessType.READ,
                    user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except NotExistsError:
                pass

    def test_create_database_access_502_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/access/abdbf897-e599-4e5a-a3f0-7529884ea011',
                      status_code=502)
            # test
            try:
                RestClient(username="a", password="b").create_database_access(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    type=AccessType.READ,
                    user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except ServiceConnectionError:
                pass

    def test_create_database_access_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/access/abdbf897-e599-4e5a-a3f0-7529884ea011',
                      status_code=503)
            # test
            try:
                RestClient(username="a", password="b").create_database_access(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    type=AccessType.READ,
                    user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except ServiceError:
                pass

    def test_create_database_access_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/access/abdbf897-e599-4e5a-a3f0-7529884ea011',
                      status_code=200)
            # test
            try:
                RestClient(username="a", password="b").create_database_access(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    type=AccessType.READ,
                    user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except ResponseCodeError:
                pass

    def test_update_database_access_succeeds(self):
        exp = DatabaseAccess(type=AccessType.READ,
                             user=UserBrief(id='abdbf897-e599-4e5a-a3f0-7529884ea011', username='other'))
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/access/abdbf897-e599-4e5a-a3f0-7529884ea011',
                     json=exp.model_dump(),
                     status_code=202)
            # test
            response = RestClient(username="a", password="b").update_database_access(
                database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                type=AccessType.READ,
                user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            self.assertEqual(response, exp.type)

    def test_update_database_access_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/access/abdbf897-e599-4e5a-a3f0-7529884ea011',
                     status_code=400)
            # test
            try:
                RestClient(username="a", password="b").update_database_access(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    type=AccessType.READ,
                    user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except MalformedError:
                pass

    def test_update_database_access_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/access/abdbf897-e599-4e5a-a3f0-7529884ea011',
                     status_code=403)
            # test
            try:
                RestClient(username="a", password="b").update_database_access(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    type=AccessType.READ,
                    user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except ForbiddenError:
                pass

    def test_update_database_access_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/access/abdbf897-e599-4e5a-a3f0-7529884ea011',
                     status_code=404)
            # test
            try:
                RestClient(username="a", password="b").update_database_access(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    type=AccessType.READ,
                    user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except NotExistsError:
                pass

    def test_update_database_access_502_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/access/abdbf897-e599-4e5a-a3f0-7529884ea011',
                     status_code=502)
            # test
            try:
                RestClient(username="a", password="b").update_database_access(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    type=AccessType.READ,
                    user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except ServiceConnectionError:
                pass

    def test_update_database_access_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/access/abdbf897-e599-4e5a-a3f0-7529884ea011',
                     status_code=503)
            # test
            try:
                RestClient(username="a", password="b").update_database_access(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    type=AccessType.READ,
                    user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except ServiceError:
                pass

    def test_update_database_access_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/access/abdbf897-e599-4e5a-a3f0-7529884ea011',
                     status_code=200)
            # test
            try:
                RestClient(username="a", password="b").update_database_access(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    type=AccessType.READ,
                    user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except ResponseCodeError:
                pass

    def test_update_database_access_anonymous_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/6bd39359-b154-456d-b9c2-caa516a45732/access/abdbf897-e599-4e5a-a3f0-7529884ea011',
                     status_code=404)
            # test
            try:
                RestClient().update_database_access(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                    type=AccessType.READ,
                                                    user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except AuthenticationError:
                pass

    def test_delete_database_access_succeeds(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/access/abdbf897-e599-4e5a-a3f0-7529884ea011',
                status_code=202)
            # test
            client = RestClient(username="a", password="b")
            RestClient(username="a", password="b").delete_database_access(
                database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')

    def test_delete_database_access_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/access/abdbf897-e599-4e5a-a3f0-7529884ea011',
                status_code=400)
            # test
            try:
                RestClient(username="a", password="b").delete_database_access(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except MalformedError:
                pass

    def test_delete_database_access_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/access/abdbf897-e599-4e5a-a3f0-7529884ea011',
                status_code=403)
            # test
            try:
                RestClient(username="a", password="b").delete_database_access(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except ForbiddenError:
                pass

    def test_delete_database_access_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/access/abdbf897-e599-4e5a-a3f0-7529884ea011',
                status_code=404)
            # test
            try:
                RestClient(username="a", password="b").delete_database_access(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except NotExistsError:
                pass

    def test_delete_database_access_502_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/access/abdbf897-e599-4e5a-a3f0-7529884ea011',
                status_code=502)
            # test
            try:
                RestClient(username="a", password="b").delete_database_access(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except ServiceConnectionError:
                pass

    def test_delete_database_access_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/access/abdbf897-e599-4e5a-a3f0-7529884ea011',
                status_code=503)
            # test
            try:
                RestClient(username="a", password="b").delete_database_access(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except ServiceError:
                pass

    def test_delete_database_access_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/access/abdbf897-e599-4e5a-a3f0-7529884ea011',
                status_code=200)
            # test
            try:
                RestClient(username="a", password="b").delete_database_access(
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                    user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except ResponseCodeError:
                pass

    def test_delete_database_access_anonymous_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete(
                '/api/database/6bd39359-b154-456d-b9c2-caa516a45732/access/abdbf897-e599-4e5a-a3f0-7529884ea011',
                status_code=404)
            # test
            try:
                RestClient().delete_database_access(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                    user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except AuthenticationError:
                pass


if __name__ == "__main__":
    unittest.main()

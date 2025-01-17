import unittest

import requests_mock
from pydantic_core import ValidationError

from dbrepo.RestClient import RestClient
from dbrepo.api.dto import Database, Container, Image, DatabaseAccess, AccessType, DatabaseBrief, UserBrief, \
    ContainerBrief, ImageBrief
from dbrepo.api.exceptions import ResponseCodeError, NotExistsError, ForbiddenError, MalformedError, AuthenticationError


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
                id=1,
                name='test',
                owner_id='8638c043-5145-4be8-a3e4-4b79991b0a16',
                contact=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'),
                internal_name='test_abcd',
                is_public=True,
                is_schema_public=True)
        ]
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database', json=[exp[0].model_dump()])
            # test
            response = RestClient().get_databases()
            self.assertEqual(exp, response)

    def test_get_database_succeeds(self):
        exp = Database(
            id=1,
            name='test',
            owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'),
            contact=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'),
            exchange_name='dbrepo',
            internal_name='test_abcd',
            is_public=True,
            is_schema_public=True,
            container=ContainerBrief(
                id=1,
                name='MariaDB Galera 11.1.3',
                internal_name='mariadb',
                image=ImageBrief(
                    id=1,
                    name='mariadb',
                    version='11.2.2',
                    jdbc_method='mariadb'
                )
            )
        )
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/1', json=exp.model_dump())
            # test
            response = RestClient().get_database(1)
            self.assertEqual(exp, response)

    def test_get_database_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/1', status_code=404)
            # test
            try:
                response = RestClient().get_database(1)
            except NotExistsError as e:
                pass

    def test_get_database_invalid_dto_fails(self):
        try:
            exp = Database()
        except ValidationError as e:
            pass

    def test_get_database_unauthorized_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/1', status_code=401)
            # test
            try:
                response = RestClient().get_database(1)
            except ResponseCodeError as e:
                pass

    def test_create_database_succeeds(self):
        exp = Database(
            id=1,
            name='test',
            owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'),
            contact=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'),
            exchange_name='dbrepo',
            internal_name='test_abcd',
            is_public=True,
            is_schema_public=True,
            container=ContainerBrief(
                id=1,
                name='MariaDB Galera 11.1.3',
                internal_name='mariadb',
                image=ImageBrief(
                    id=1,
                    name='mariadb',
                    version='11.2.2',
                    jdbc_method='mariadb'
                )
            )
        )
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database', json=exp.model_dump(), status_code=201)
            # test
            client = RestClient(username="a", password="b")
            response = client.create_database(name='test', container_id=1, is_public=True)
            self.assertEqual(response.name, 'test')

    def test_create_database_not_allowed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database', status_code=403)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.create_database(name='test', container_id=1, is_public=True)
            except ForbiddenError as e:
                pass

    def test_create_database_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database', status_code=404)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.create_database(name='test', container_id=1, is_public=True)
            except NotExistsError as e:
                pass

    def test_create_database_not_auth_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database', status_code=404)
            # test
            try:
                response = RestClient().create_database(name='test', container_id=1, is_public=True)
            except AuthenticationError as e:
                pass

    def test_update_database_visibility_succeeds(self):
        exp = Database(
            id=1,
            name='test',
            owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'),
            contact=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'),
            exchange_name='dbrepo',
            internal_name='test_abcd',
            is_public=True,
            is_schema_public=True,
            container=ContainerBrief(
                id=1,
                name='MariaDB Galera 11.1.3',
                internal_name='mariadb',
                image=ImageBrief(
                    id=1,
                    name='mariadb',
                    version='11.2.2',
                    jdbc_method='mariadb'
                )
            )
        )
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/1/visibility', json=exp.model_dump(), status_code=202)
            # test
            client = RestClient(username="a", password="b")
            response = client.update_database_visibility(database_id=1, is_public=True, is_schema_public=True)
            self.assertEqual(response.is_public, True)

    def test_update_database_visibility_not_allowed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/1/visibility', status_code=403)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.update_database_visibility(database_id=1, is_public=True, is_schema_public=True)
            except ForbiddenError:
                pass

    def test_update_database_visibility_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/1/visibility', status_code=404)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.update_database_visibility(database_id=1, is_public=True, is_schema_public=True)
            except NotExistsError:
                pass

    def test_update_database_visibility_not_auth_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/1', status_code=404)
            # test
            try:
                response = RestClient().update_database_visibility(database_id=1, is_public=True, is_schema_public=True)
            except AuthenticationError:
                pass

    def test_update_database_owner_succeeds(self):
        exp = Database(
            id=1,
            name='test',
            owner=UserBrief(id='abdbf897-e599-4e5a-a3f0-7529884ea011', username='mweise'),
            contact=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'),
            exchange_name='dbrepo',
            internal_name='test_abcd',
            is_public=True,
            is_schema_public=True,
            container=ContainerBrief(
                id=1,
                name='MariaDB Galera 11.1.3',
                internal_name='mariadb',
                image=ImageBrief(
                    id=1,
                    name='mariadb',
                    version='11.2.2',
                    jdbc_method='mariadb'
                )
            )
        )
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/1/owner', json=exp.model_dump(), status_code=202)
            # test
            client = RestClient(username="a", password="b")
            response = client.update_database_owner(database_id=1, user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            self.assertEqual(response.owner.id, 'abdbf897-e599-4e5a-a3f0-7529884ea011')

    def test_update_database_owner_not_allowed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/1/owner', status_code=403)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.update_database_owner(database_id=1,
                                                        user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except ForbiddenError:
                pass

    def test_update_database_owner_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/1/owner', status_code=404)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.update_database_owner(database_id=1,
                                                        user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except NotExistsError:
                pass

    def test_update_database_owner_not_auth_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/1/owner', status_code=404)
            # test
            try:
                response = RestClient().update_database_owner(database_id=1,
                                                              user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except AuthenticationError:
                pass

    def test_get_database_access_succeeds(self):
        exp = DatabaseAccess(type=AccessType.READ,
                             user=UserBrief(id='abdbf897-e599-4e5a-a3f0-7529884ea011', username='other'))
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/1/access', json=exp.model_dump())
            # test
            response = RestClient().get_database_access(database_id=1)
            self.assertEqual(response, AccessType.READ)

    def test_get_database_access_not_allowed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/1/access', status_code=403)
            # test
            try:
                response = RestClient().get_database_access(database_id=1)
            except ForbiddenError:
                pass

    def test_get_database_access_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/database/1/access', status_code=404)
            # test
            try:
                response = RestClient().get_database_access(database_id=1)
            except NotExistsError:
                pass

    def test_create_database_access_succeeds(self):
        exp = DatabaseAccess(type=AccessType.READ,
                             user=UserBrief(id='abdbf897-e599-4e5a-a3f0-7529884ea011', username='other'))
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/1/access/abdbf897-e599-4e5a-a3f0-7529884ea011', json=exp.model_dump(),
                      status_code=202)
            # test
            client = RestClient(username="a", password="b")
            response = client.create_database_access(database_id=1, type=AccessType.READ,
                                                     user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            self.assertEqual(response, exp.type)

    def test_create_database_access_malformed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/1/access/abdbf897-e599-4e5a-a3f0-7529884ea011', status_code=400)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.create_database_access(database_id=1, type=AccessType.READ,
                                                         user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except MalformedError:
                pass

    def test_create_database_access_not_auth_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/1/access/abdbf897-e599-4e5a-a3f0-7529884ea011', status_code=400)
            # test
            try:
                response = RestClient().create_database_access(database_id=1, type=AccessType.READ,
                                                               user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except AuthenticationError:
                pass

    def test_create_database_access_not_allowed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/1/access/abdbf897-e599-4e5a-a3f0-7529884ea011', status_code=403)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.create_database_access(database_id=1, type=AccessType.READ,
                                                         user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except ForbiddenError:
                pass

    def test_create_database_access_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/database/1/access/abdbf897-e599-4e5a-a3f0-7529884ea011', status_code=404)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.create_database_access(database_id=1, type=AccessType.READ,
                                                         user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except NotExistsError:
                pass

    def test_update_database_access_succeeds(self):
        exp = DatabaseAccess(type=AccessType.READ,
                             user=UserBrief(id='abdbf897-e599-4e5a-a3f0-7529884ea011', username='other'))
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/1/access/abdbf897-e599-4e5a-a3f0-7529884ea011', json=exp.model_dump(),
                     status_code=202)
            # test
            client = RestClient(username="a", password="b")
            response = client.update_database_access(database_id=1, type=AccessType.READ,
                                                     user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            self.assertEqual(response, exp.type)

    def test_update_database_access_malformed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/1/access/abdbf897-e599-4e5a-a3f0-7529884ea011', status_code=400)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.update_database_access(database_id=1, type=AccessType.READ,
                                                         user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except MalformedError:
                pass

    def test_update_database_access_not_allowed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/1/access/abdbf897-e599-4e5a-a3f0-7529884ea011', status_code=403)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.update_database_access(database_id=1, type=AccessType.READ,
                                                         user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except ForbiddenError:
                pass

    def test_update_database_access_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/1/access/abdbf897-e599-4e5a-a3f0-7529884ea011', status_code=404)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.update_database_access(database_id=1, type=AccessType.READ,
                                                         user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except NotExistsError:
                pass

    def test_update_database_access_not_auth_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/database/1/access/abdbf897-e599-4e5a-a3f0-7529884ea011', status_code=404)
            # test
            try:
                response = RestClient().update_database_access(database_id=1, type=AccessType.READ,
                                                               user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except AuthenticationError:
                pass

    def test_delete_database_access_succeeds(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/1/access/abdbf897-e599-4e5a-a3f0-7529884ea011', status_code=202)
            # test
            client = RestClient(username="a", password="b")
            client.delete_database_access(database_id=1, user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')

    def test_delete_database_access_malformed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/1/access/abdbf897-e599-4e5a-a3f0-7529884ea011', status_code=400)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.delete_database_access(database_id=1, user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except MalformedError:
                pass

    def test_delete_database_access_not_allowed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/1/access/abdbf897-e599-4e5a-a3f0-7529884ea011', status_code=403)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.delete_database_access(database_id=1, user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except ForbiddenError:
                pass

    def test_delete_database_access_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/1/access/abdbf897-e599-4e5a-a3f0-7529884ea011', status_code=404)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.delete_database_access(database_id=1, user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except NotExistsError:
                pass

    def test_delete_database_access_not_auth_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/database/1/access/abdbf897-e599-4e5a-a3f0-7529884ea011', status_code=404)
            # test
            try:
                RestClient().delete_database_access(database_id=1, user_id='abdbf897-e599-4e5a-a3f0-7529884ea011')
            except AuthenticationError:
                pass


if __name__ == "__main__":
    unittest.main()

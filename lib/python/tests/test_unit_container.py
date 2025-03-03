import unittest

import requests_mock

from dbrepo.RestClient import RestClient
from dbrepo.api.dto import Container, Image, ContainerBrief, ImageBrief, DataType, Operator
from dbrepo.api.exceptions import ResponseCodeError, NotExistsError, AuthenticationError, MalformedError, \
    ForbiddenError, NameExistsError


class ContainerUnitTest(unittest.TestCase):

    def test_create_container_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = Container(id="44d811a8-4019-46ba-bd57-ea10a2eb0c74",
                            name="MariaDB 10.11.3",
                            internal_name="mariadb_10_11_3",
                            host="data-db",
                            port=12345,
                            image=Image(id="b104648b-54d2-4d72-9834-8e0e6d428b39",
                                        registry="docker.io",
                                        name="mariadb",
                                        version="10.11.3",
                                        default_port=3306,
                                        dialect="org.hibernate.dialect.MariaDBDialect",
                                        driver_class="org.mariadb.jdbc.Driver",
                                        jdbc_method="mariadb",
                                        default=True,
                                        operators=[
                                            Operator(id="0917b17e-5d68-4ddf-94f6-f178f74a0dae",
                                                     display_name="XOR",
                                                     value="XOR",
                                                     documentation="https://mariadb.com/kb/en/xor/")],
                                        data_types=[
                                            DataType(id="22975809-5496-4d67-9fd4-6689f0030f82",
                                                     display_name="SERIAL",
                                                     value="serial",
                                                     documentation="https://mariadb.com/kb/en/bigint/",
                                                     is_quoted=False,
                                                     is_buildable=True)]))
            # mock
            mock.post('/api/container', json=exp.model_dump(), status_code=201)
            # test
            response = RestClient(username="foo", password="bar").create_container(name='MariaDB 10.11.3',
                                                                                   host='data-db2',
                                                                                   image_id="b104648b-54d2-4d72-9834-8e0e6d428b39",
                                                                                   privileged_username='root',
                                                                                   privileged_password='dbrepo',
                                                                                   port=3306)
            self.assertEqual(exp, response)

    def test_create_container_anonymous_fails(self):
        # test
        try:
            response = RestClient().create_container(name='MariaDB 10.11.3', host='data-db2',
                                                     image_id="b104648b-54d2-4d72-9834-8e0e6d428b39",
                                                     privileged_username='root', privileged_password='dbrepo',
                                                     port=3306)
        except AuthenticationError:
            pass

    def test_create_container_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/container', status_code=400)
            # test
            try:
                response = RestClient(username="foo", password="bar").create_container(name='MariaDB 10.11.3',
                                                                                       host='data-db2',
                                                                                       image_id="b104648b-54d2-4d72-9834-8e0e6d428b39",
                                                                                       privileged_username='root',
                                                                                       privileged_password='dbrepo',
                                                                                       port=3306)
            except MalformedError:
                pass

    def test_create_container_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/container', status_code=403)
            # test
            try:
                response = RestClient(username="foo", password="bar").create_container(name='MariaDB 10.11.3',
                                                                                       host='data-db2',
                                                                                       image_id="b104648b-54d2-4d72-9834-8e0e6d428b39",
                                                                                       privileged_username='root',
                                                                                       privileged_password='dbrepo',
                                                                                       port=3306)
            except ForbiddenError:
                pass

    def test_create_container_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/container', status_code=404)
            # test
            try:
                response = RestClient(username="foo", password="bar").create_container(name='MariaDB 10.11.3',
                                                                                       host='data-db2',
                                                                                       image_id="b104648b-54d2-4d72-9834-8e0e6d428b39",
                                                                                       privileged_username='root',
                                                                                       privileged_password='dbrepo',
                                                                                       port=3306)
            except NotExistsError:
                pass

    def test_create_container_409_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/container', status_code=409)
            # test
            try:
                response = RestClient(username="foo", password="bar").create_container(name='MariaDB 10.11.3',
                                                                                       host='data-db2',
                                                                                       image_id="b104648b-54d2-4d72-9834-8e0e6d428b39",
                                                                                       privileged_username='root',
                                                                                       privileged_password='dbrepo',
                                                                                       port=3306)
            except NameExistsError:
                pass

    def test_create_container_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/container', status_code=200)
            # test
            try:
                response = RestClient(username="foo", password="bar").create_container(name='MariaDB 10.11.3',
                                                                                       host='data-db2',
                                                                                       image_id="b104648b-54d2-4d72-9834-8e0e6d428b39",
                                                                                       privileged_username='root',
                                                                                       privileged_password='dbrepo',
                                                                                       port=3306)
            except ResponseCodeError:
                pass

    def test_get_containers_empty_succeeds(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/container', json=[])
            # test
            response = RestClient().get_containers()
            self.assertEqual([], response)

    def test_get_containers_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [
                ContainerBrief(id="44d811a8-4019-46ba-bd57-ea10a2eb0c74",
                               name="MariaDB 10.11.3",
                               internal_name="mariadb_10_11_3",
                               running=True,
                               image=ImageBrief(id="b104648b-54d2-4d72-9834-8e0e6d428b39",
                                                name="mariadb",
                                                version="10.11.3",
                                                default=True),
                               hash="f829dd8a884182d0da846f365dee1221fd16610a14c81b8f9f295ff162749e50")
            ]
            # mock
            mock.get('/api/container', json=[exp[0].model_dump()])
            # test
            response = RestClient().get_containers()
            self.assertEqual(exp, response)

    def test_get_containers_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/container', status_code=204)
            # test
            try:
                response = RestClient().get_containers()
            except ResponseCodeError:
                pass

    def test_get_container_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = Container(id="",
                            name="MariaDB 10.11.3",
                            internal_name="mariadb_10_11_3",
                            host="data-db",
                            port=12345,
                            image=Image(id="b104648b-54d2-4d72-9834-8e0e6d428b39",
                                        registry="docker.io",
                                        name="mariadb",
                                        version="10.11.3",
                                        default_port=3306,
                                        dialect="org.hibernate.dialect.MariaDBDialect",
                                        driver_class="org.mariadb.jdbc.Driver",
                                        jdbc_method="mariadb",
                                        default=True,
                                        operators=[
                                            Operator(id="0917b17e-5d68-4ddf-94f6-f178f74a0dae",
                                                     display_name="XOR",
                                                     value="XOR",
                                                     documentation="https://mariadb.com/kb/en/xor/")],
                                        data_types=[
                                            DataType(id="22975809-5496-4d67-9fd4-6689f0030f82",
                                                     display_name="SERIAL",
                                                     value="serial",
                                                     documentation="https://mariadb.com/kb/en/bigint/",
                                                     is_quoted=False,
                                                     is_buildable=True)]))
            # mock
            mock.get('/api/container/44d811a8-4019-46ba-bd57-ea10a2eb0c74', json=exp.model_dump())
            # test
            response = RestClient().get_container(container_id="44d811a8-4019-46ba-bd57-ea10a2eb0c74")
            self.assertEqual(exp, response)

    def test_get_container_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/container/44d811a8-4019-46ba-bd57-ea10a2eb0c74', status_code=404)
            # test
            try:
                response = RestClient().get_container(container_id="44d811a8-4019-46ba-bd57-ea10a2eb0c74")
            except NotExistsError:
                pass

    def test_get_container_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/container/44d811a8-4019-46ba-bd57-ea10a2eb0c74', status_code=401)
            # test
            try:
                response = RestClient().get_container(container_id="44d811a8-4019-46ba-bd57-ea10a2eb0c74")
            except ResponseCodeError:
                pass

    def test_delete_container_succeeds(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/container/44d811a8-4019-46ba-bd57-ea10a2eb0c74', status_code=202)
            # test
            RestClient(username='foo', password='bar').delete_container(
                container_id="44d811a8-4019-46ba-bd57-ea10a2eb0c74")

    def test_delete_container_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/container/44d811a8-4019-46ba-bd57-ea10a2eb0c74', status_code=400)
            # test
            try:
                RestClient(username='foo', password='bar').delete_container(
                    container_id="44d811a8-4019-46ba-bd57-ea10a2eb0c74")
            except MalformedError:
                pass

    def test_delete_container_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/container/44d811a8-4019-46ba-bd57-ea10a2eb0c74', status_code=403)
            # test
            try:
                RestClient(username='foo', password='bar').delete_container(
                    container_id="44d811a8-4019-46ba-bd57-ea10a2eb0c74")
            except ForbiddenError:
                pass

    def test_delete_container_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/container/44d811a8-4019-46ba-bd57-ea10a2eb0c74', status_code=404)
            # test
            try:
                RestClient(username='foo', password='bar').delete_container(
                    container_id="44d811a8-4019-46ba-bd57-ea10a2eb0c74")
            except NotExistsError:
                pass

    def test_delete_container_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.delete('/api/container/44d811a8-4019-46ba-bd57-ea10a2eb0c74', status_code=200)
            # test
            try:
                RestClient(username='foo', password='bar').delete_container(
                    container_id="44d811a8-4019-46ba-bd57-ea10a2eb0c74")
            except ResponseCodeError:
                pass

    def test_delete_container_anonymous_fails(self):
        # test
        try:
            RestClient().delete_container(container_id="44d811a8-4019-46ba-bd57-ea10a2eb0c74")
        except AuthenticationError:
            pass


if __name__ == "__main__":
    unittest.main()

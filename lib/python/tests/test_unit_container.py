import unittest

import requests_mock
import datetime

from dbrepo.RestClient import RestClient
from dbrepo.api.dto import Container, Image, ContainerBrief, ImageBrief, DataType
from dbrepo.api.exceptions import ResponseCodeError, NotExistsError


class ContainerUnitTest(unittest.TestCase):

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
                ContainerBrief(id=1,
                               name="MariaDB 10.11.3",
                               internal_name="mariadb_10_11_3",
                               running=True,
                               created=datetime.datetime(2024, 3, 26, 10, 11, 0, 0, datetime.timezone.utc),
                               image=ImageBrief(id=1,
                                                name="mariadb",
                                                version="10.11.3",
                                                jdbc_method="mariadb"),
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
            exp = Container(id=1,
                            name="MariaDB 10.11.3",
                            internal_name="mariadb_10_11_3",
                            running=True,
                            host="data-db",
                            port=12345,
                            sidecar_host="data-db-sidecar",
                            sidecar_port=3305,
                            created=datetime.datetime(2024, 3, 26, 10, 11, 0, 0, datetime.timezone.utc),
                            image=Image(id=1,
                                        registry="docker.io",
                                        name="mariadb",
                                        version="10.11.3",
                                        default_port=3306,
                                        dialect="org.hibernate.dialect.MariaDBDialect",
                                        driver_class="org.mariadb.jdbc.Driver",
                                        jdbc_method="mariadb",
                                        data_types=[
                                            DataType(display_name="SERIAL", value="serial",
                                                     documentation="https://mariadb.com/kb/en/bigint/",
                                                     is_quoted=False, is_buildable=True)]),
                            hash="f829dd8a884182d0da846f365dee1221fd16610a14c81b8f9f295ff162749e50")
            # mock
            mock.get('/api/container/1', json=exp.model_dump())
            # test
            response = RestClient().get_container(container_id=1)
            self.assertEqual(exp, response)

    def test_get_container_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/container/1', status_code=404)
            # test
            try:
                response = RestClient().get_container(container_id=1)
            except NotExistsError:
                pass


if __name__ == "__main__":
    unittest.main()

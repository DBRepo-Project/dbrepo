import unittest

import requests_mock

from dbrepo.RestClient import RestClient
from dbrepo.api.dto import UnitBrief
from dbrepo.api.exceptions import ResponseCodeError


class UserUnitTest(unittest.TestCase):

    def test_get_units_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [UnitBrief(id="d8eec1ab-7b37-4eb3-bdf7-b44a8b384c0b",
                             uri='http://www.ontology-of-units-of-measure.org/resource/om-2/CelsiusTemperature',
                             name='Celsius Temperature')]
            # mock
            mock.get('/api/unit', json=[exp[0].model_dump()])
            # test
            response = RestClient().get_units()
            self.assertEqual(exp, response)

    def test_get_units_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/unit', status_code=202)
            # test
            try:
                RestClient().get_units()
            except ResponseCodeError:
                pass


if __name__ == "__main__":
    unittest.main()

import unittest

import requests_mock

from dbrepo.RestClient import RestClient
from dbrepo.api.dto import ConceptBrief
from dbrepo.api.exceptions import ResponseCodeError


class ContainerUnitTest(unittest.TestCase):

    def test_get_concepts_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [ConceptBrief(id="65586aef-f5b0-446f-b2e1-9dc2a3c0c359",
                                uri="http://dbpedia.org/page/Category:Precipitation",
                                name="Precipitation")]
            # mock
            mock.get('/api/concept', json=[exp[0].model_dump()])
            # test
            response = RestClient().get_concepts()
            self.assertEqual(exp, response)

    def test_get_concepts_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/concept', status_code=202)
            # test
            try:
                RestClient().get_concepts()
            except ResponseCodeError:
                pass


if __name__ == "__main__":
    unittest.main()

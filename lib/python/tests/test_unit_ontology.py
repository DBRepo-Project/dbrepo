import unittest

import requests_mock

from dbrepo.RestClient import RestClient
from dbrepo.api.dto import OntologyBrief


class OntologyUnitTest(unittest.TestCase):

    def test_get_ontologies_empty_succeeds(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/ontology', json=[])
            # test
            response = RestClient().get_ontologies()
            self.assertEqual([], response)

    def test_get_ontologies_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [OntologyBrief(id="ec6ee082-b6b2-4d65-b931-2f1735f25759",
                                 uri="http://www.ontology-of-units-of-measure.org/resource/om-2/",
                                 prefix="om",
                                 sparql=False,
                                 rdf=True,
                                 uri_pattern="http://www.ontology-of-units-of-measure.org/resource/om-2/.*")]
            # mock
            mock.get('/api/ontology', json=[exp[0].model_dump()])
            # test
            response = RestClient().get_ontologies()
            self.assertEqual(exp, response)


if __name__ == "__main__":
    unittest.main()

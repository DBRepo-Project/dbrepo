import unittest

import requests_mock
import datetime

from dbrepo.RestClient import RestClient

from dbrepo.api.dto import Identifier, IdentifierType, CreateIdentifierTitle, CreateIdentifierCreator, \
    IdentifierCreator, IdentifierTitle, IdentifierDescription, CreateIdentifierDescription, Language, \
    CreateIdentifierFunder, CreateRelatedIdentifier, RelatedIdentifierRelation, RelatedIdentifierType, IdentifierFunder, \
    RelatedIdentifier
from dbrepo.api.exceptions import MalformedError, ForbiddenError, NotExistsError, ExternalSystemError, \
    AuthenticationError


class IdentifierTest(unittest.TestCase):

    def test_create_identifier_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = Identifier(id=10,
                             database_id=1,
                             view_id=32,
                             publication_year=2024,
                             publisher='TU Wien',
                             created=datetime.datetime(2024, 1, 1, 0, 0, 0, 0, datetime.timezone.utc),
                             last_modified=datetime.datetime(2024, 1, 1, 0, 0, 0, 0, datetime.timezone.utc),
                             type=IdentifierType.VIEW,
                             language=Language.EN,
                             descriptions=[IdentifierDescription(id=2, description='Test Description')],
                             titles=[IdentifierTitle(id=3, title='Test Title')],
                             funders=[IdentifierFunder(id=4, funder_name='FWF')],
                             related_identifiers=[
                                 RelatedIdentifier(id=7, value='10.12345/abc', relation=RelatedIdentifierRelation.CITES,
                                                   type=RelatedIdentifierType.DOI)],
                             creators=[IdentifierCreator(id=5, creator_name='Carberry, Josiah')])
            # mock
            mock.post('/api/identifier', json=exp.model_dump(), status_code=201)
            # test
            client = RestClient(username="a", password="b")
            response = client.create_identifier(database_id=1, type=IdentifierType.VIEW,
                                                titles=[CreateIdentifierTitle(title='Test Title')],
                                                publisher='TU Wien', publication_year=2024,
                                                language=Language.EN,
                                                funders=[CreateIdentifierFunder(funder_name='FWF')],
                                                related_identifiers=[CreateRelatedIdentifier(value='10.12345/abc',
                                                                                             relation=RelatedIdentifierRelation.CITES,
                                                                                             type=RelatedIdentifierType.DOI)],
                                                descriptions=[
                                                    CreateIdentifierDescription(description='Test Description')],
                                                creators=[
                                                    CreateIdentifierCreator(creator_name='Carberry, Josiah')])
            self.assertEqual(exp, response)

    def test_create_identifier_malformed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/identifier', status_code=400)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.create_identifier(database_id=1, type=IdentifierType.VIEW,
                                                    titles=[CreateIdentifierTitle(title='Test Title')],
                                                    publisher='TU Wien', publication_year=2024,
                                                    creators=[
                                                        CreateIdentifierCreator(creator_name='Carberry, Josiah')])
            except MalformedError:
                pass

    def test_create_identifier_not_allowed_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/identifier', status_code=403)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.create_identifier(database_id=1, type=IdentifierType.VIEW,
                                                    titles=[CreateIdentifierTitle(title='Test Title')],
                                                    publisher='TU Wien', publication_year=2024,
                                                    creators=[
                                                        CreateIdentifierCreator(creator_name='Carberry, Josiah')])
            except ForbiddenError:
                pass

    def test_create_identifier_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/identifier', status_code=404)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.create_identifier(database_id=1, type=IdentifierType.VIEW,
                                                    titles=[CreateIdentifierTitle(title='Test Title')],
                                                    publisher='TU Wien', publication_year=2024,
                                                    creators=[
                                                        CreateIdentifierCreator(creator_name='Carberry, Josiah')])
            except NotExistsError:
                pass

    def test_create_identifier_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/identifier', status_code=503)
            # test
            try:
                client = RestClient(username="a", password="b")
                response = client.create_identifier(database_id=1, type=IdentifierType.VIEW,
                                                    titles=[CreateIdentifierTitle(title='Test Title')],
                                                    publisher='TU Wien', publication_year=2024,
                                                    creators=[
                                                        CreateIdentifierCreator(creator_name='Carberry, Josiah')])
            except ExternalSystemError:
                pass

    def test_create_identifier_not_auth_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/identifier', status_code=503)
            # test
            try:
                response = RestClient().create_identifier(database_id=1, type=IdentifierType.VIEW,
                                                          titles=[CreateIdentifierTitle(title='Test Title')],
                                                          publisher='TU Wien', publication_year=2024,
                                                          creators=[
                                                              CreateIdentifierCreator(creator_name='Carberry, Josiah')])
            except AuthenticationError:
                pass

    def test_suggest_identifier_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = Identifier(id=10,
                             database_id=1,
                             publication_year=2024,
                             publisher='TU Wien',
                             created=datetime.datetime(2024, 1, 1, 0, 0, 0, 0, datetime.timezone.utc),
                             last_modified=datetime.datetime(2024, 1, 1, 0, 0, 0, 0, datetime.timezone.utc),
                             type=IdentifierType.VIEW,
                             creators=[IdentifierCreator(id=5, creator_name='Carberry, Josiah',
                                                         name_identifier='https://orcid.org/0000-0002-1825-0097')])
            # mock
            mock.get('/api/identifier?url=https://orcid.org/0000-0002-1825-0097', json=exp.model_dump())
            # test
            response = RestClient().suggest_identifier("https://orcid.org/0000-0002-1825-0097")
            self.assertEqual(exp, response)

    def test_suggest_identifier_not_found_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/identifier?url=https://orcid.org/0000-0002-1825-0097', status_code=404)
            # test
            try:
                response = RestClient().suggest_identifier("https://orcid.org/0000-0002-1825-0097")
            except NotExistsError:
                pass

    def test_get_identifiers_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [Identifier(id=10,
                              database_id=1,
                              view_id=32,
                              publication_year=2024,
                              publisher='TU Wien',
                              created=datetime.datetime(2024, 1, 1, 0, 0, 0, 0, datetime.timezone.utc),
                              last_modified=datetime.datetime(2024, 1, 1, 0, 0, 0, 0, datetime.timezone.utc),
                              type=IdentifierType.VIEW,
                              language=Language.EN,
                              descriptions=[IdentifierDescription(id=2, description='Test Description')],
                              titles=[IdentifierTitle(id=3, title='Test Title')],
                              funders=[IdentifierFunder(id=4, funder_name='FWF')],
                              related_identifiers=[
                                  RelatedIdentifier(id=7, value='10.12345/abc',
                                                    relation=RelatedIdentifierRelation.CITES,
                                                    type=RelatedIdentifierType.DOI)],
                              creators=[IdentifierCreator(id=5, creator_name='Carberry, Josiah')])]
            # mock
            mock.get('/api/pid', json=[exp[0].model_dump()], headers={"Accept": "application/json"})
            # test
            response = RestClient().get_identifiers()
            self.assertEqual(exp, response)

    def test_get_identifiers_ld_json_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [{"@context": "https://schema.org/", "@type": "Dataset", "url": "http://localhost/database/2/info",
                    "citation": "http://localhost/pid/2", "hasPart": [], "version": "2024-03-21T12:05:46.000Z",
                    "name": "sdfsdf", "description": "sfsdf", "identifier": ["http://localhost/pid/2"],
                    "license": "https://creativecommons.org/licenses/by/4.0/legalcode", "creator": [
                    {"name": "Weise, Martin", "@type": "Person", "sameAs": "https://orcid.org/0000-0003-4216-302X",
                     "givenName": "Martin", "familyName": "Weise"}], "temporalCoverage": 2024}]
            # mock
            mock.get('/api/pid', json=exp, headers={"Accept": "application/ld+json"})
            # test
            response = RestClient().get_identifiers(ld=True)
            self.assertEqual(exp, response)


if __name__ == "__main__":
    unittest.main()

import unittest

import requests_mock

from dbrepo.RestClient import RestClient
from dbrepo.api.dto import Identifier, IdentifierType, SaveIdentifierTitle, Creator, IdentifierTitle, \
    IdentifierDescription, SaveIdentifierDescription, Language, SaveIdentifierFunder, SaveRelatedIdentifier, \
    RelatedIdentifierRelation, RelatedIdentifierType, IdentifierFunder, RelatedIdentifier, UserBrief, \
    IdentifierStatusType, CreateIdentifierCreator, CreateIdentifierTitle, CreateIdentifierFunder, \
    CreateRelatedIdentifier, CreateIdentifierDescription, SaveIdentifierCreator
from dbrepo.api.exceptions import MalformedError, ForbiddenError, NotExistsError, AuthenticationError, \
    ServiceConnectionError, ServiceError, ResponseCodeError, FormatNotAvailable, RequestError


class IdentifierUnitTest(unittest.TestCase):

    def test_create_identifier_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = Identifier(id=10,
                             database_id=1,
                             view_id=32,
                             publication_year=2024,
                             publisher='TU Wien',
                             type=IdentifierType.VIEW,
                             language=Language.EN,
                             descriptions=[IdentifierDescription(id=2, description='Test Description')],
                             titles=[IdentifierTitle(id=3, title='Test Title')],
                             funders=[IdentifierFunder(id=4, funder_name='FWF')],
                             related_identifiers=[
                                 RelatedIdentifier(id=7, value='10.12345/abc', relation=RelatedIdentifierRelation.CITES,
                                                   type=RelatedIdentifierType.DOI)],
                             creators=[Creator(id=5, creator_name='Carberry, Josiah')],
                             status=IdentifierStatusType.PUBLISHED,
                             owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'))
            # mock
            mock.post('/api/identifier', json=exp.model_dump(), status_code=201)
            # test
            client = RestClient(username="a", password="b")
            response = client.create_identifier(
                database_id=1, type=IdentifierType.VIEW,
                titles=[CreateIdentifierTitle(title='Test Title')],
                publisher='TU Wien', publication_year=2024,
                language=Language.EN,
                funders=[CreateIdentifierFunder(funder_name='FWF')],
                related_identifiers=[CreateRelatedIdentifier(value='10.12345/abc',
                                                             relation=RelatedIdentifierRelation.CITES,
                                                             type=RelatedIdentifierType.DOI)],
                descriptions=[CreateIdentifierDescription(description='Test Description')],
                creators=[CreateIdentifierCreator(creator_name='Carberry, Josiah')])
            self.assertEqual(exp, response)

    def test_create_identifier_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/identifier', status_code=400)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.create_identifier(
                    database_id=1, type=IdentifierType.VIEW,
                    titles=[CreateIdentifierTitle(title='Test Title')],
                    descriptions=[CreateIdentifierDescription(description='Test')],
                    publisher='TU Wien', publication_year=2024,
                    creators=[CreateIdentifierCreator(creator_name='Carberry, Josiah')])
            except MalformedError:
                pass

    def test_create_identifier_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/identifier', status_code=403)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.create_identifier(
                    database_id=1, type=IdentifierType.VIEW,
                    titles=[CreateIdentifierTitle(title='Test Title')],
                    descriptions=[CreateIdentifierDescription(description='Test')],
                    publisher='TU Wien', publication_year=2024,
                    creators=[CreateIdentifierCreator(creator_name='Carberry, Josiah')])
            except ForbiddenError:
                pass

    def test_create_identifier_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/identifier', status_code=404)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.create_identifier(
                    database_id=1, type=IdentifierType.VIEW,
                    titles=[CreateIdentifierTitle(title='Test Title')],
                    descriptions=[CreateIdentifierDescription(description='Test')],
                    publisher='TU Wien', publication_year=2024,
                    creators=[CreateIdentifierCreator(creator_name='Carberry, Josiah')])
            except NotExistsError:
                pass

    def test_create_identifier_502_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/identifier', status_code=502)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.create_identifier(
                    database_id=1, type=IdentifierType.VIEW,
                    titles=[CreateIdentifierTitle(title='Test Title')],
                    descriptions=[CreateIdentifierDescription(description='Test')],
                    publisher='TU Wien', publication_year=2024,
                    creators=[CreateIdentifierCreator(creator_name='Carberry, Josiah')])
            except ServiceConnectionError:
                pass

    def test_create_identifier_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/identifier', status_code=503)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.create_identifier(
                    database_id=1, type=IdentifierType.VIEW,
                    titles=[CreateIdentifierTitle(title='Test Title')],
                    descriptions=[CreateIdentifierDescription(description='Test')],
                    publisher='TU Wien', publication_year=2024,
                    creators=[CreateIdentifierCreator(creator_name='Carberry, Josiah')])
            except ServiceError:
                pass

    def test_create_identifier_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.post('/api/identifier', status_code=200)
            # test
            try:
                client = RestClient(username="a", password="b")
                client.create_identifier(
                    database_id=1, type=IdentifierType.VIEW,
                    titles=[CreateIdentifierTitle(title='Test Title')],
                    descriptions=[CreateIdentifierDescription(description='Test')],
                    publisher='TU Wien', publication_year=2024,
                    creators=[CreateIdentifierCreator(creator_name='Carberry, Josiah')])
            except ResponseCodeError:
                pass

    def test_create_identifier_anonymous_fails(self):
        # test
        try:
            RestClient().create_identifier(
                database_id=1, type=IdentifierType.VIEW,
                titles=[CreateIdentifierTitle(title='Test Title')],
                descriptions=[CreateIdentifierDescription(description='Test')],
                publisher='TU Wien', publication_year=2024,
                creators=[CreateIdentifierCreator(creator_name='Carberry, Josiah')])
        except AuthenticationError:
            pass

    def test_get_identifiers_view_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [Identifier(id=10,
                              database_id=1,
                              view_id=32,
                              publication_year=2024,
                              publisher='TU Wien',
                              type=IdentifierType.VIEW,
                              language=Language.EN,
                              descriptions=[IdentifierDescription(id=2, description='Test Description')],
                              titles=[IdentifierTitle(id=3, title='Test Title')],
                              funders=[IdentifierFunder(id=4, funder_name='FWF')],
                              related_identifiers=[RelatedIdentifier(id=7, value='10.12345/abc',
                                                                     relation=RelatedIdentifierRelation.CITES,
                                                                     type=RelatedIdentifierType.DOI)],
                              creators=[Creator(id=5, creator_name='Carberry, Josiah')],
                              status=IdentifierStatusType.PUBLISHED,
                              owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'))]
            # mock
            mock.get('/api/identifiers', json=[exp[0].model_dump()], headers={"Accept": "application/json"})
            # test
            response = RestClient().get_identifiers(database_id=1, view_id=32, type=IdentifierType.VIEW,
                                                    status=IdentifierStatusType.PUBLISHED)
            self.assertEqual(exp, response)

    def test_get_identifiers_subset_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = []
            # mock
            mock.get('/api/identifiers', json=[], headers={"Accept": "application/json"})
            # test
            response = RestClient().get_identifiers(database_id=1, subset_id=2)
            self.assertEqual(exp, response)

    def test_get_identifiers_table_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = []
            # mock
            mock.get('/api/identifiers', json=[], headers={"Accept": "application/json"})
            # test
            response = RestClient().get_identifiers(database_id=1, table_id=3)
            self.assertEqual(exp, response)

    def test_get_identifiers_view_param_database_fails(self):
        # test
        try:
            RestClient().get_identifiers(view_id=1)
        except RequestError:
            pass

    def test_get_identifiers_subset_param_database_fails(self):
        # test
        try:
            RestClient().get_identifiers(subset_id=1)
        except RequestError:
            pass

    def test_get_identifiers_table_param_database_fails(self):
        # test
        try:
            RestClient().get_identifiers(table_id=1)
        except RequestError:
            pass

    def test_get_identifiers_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/identifiers', status_code=404)
            # test
            try:
                RestClient().get_identifiers()
            except NotExistsError:
                pass

    def test_get_identifiers_406_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/identifiers', status_code=406)
            # test
            try:
                RestClient().get_identifiers()
            except FormatNotAvailable:
                pass

    def test_get_identifiers_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/identifiers', status_code=202)
            # test
            try:
                RestClient().get_identifiers()
            except ResponseCodeError:
                pass

    def test_update_identifier_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = Identifier(id=10,
                             database_id=1,
                             view_id=32,
                             publication_year=2024,
                             publisher='TU Wien',
                             type=IdentifierType.VIEW,
                             language=Language.EN,
                             descriptions=[IdentifierDescription(id=2, description='Test Description')],
                             titles=[IdentifierTitle(id=3, title='Test Title')],
                             funders=[IdentifierFunder(id=4, funder_name='FWF')],
                             related_identifiers=[
                                 RelatedIdentifier(id=7, value='10.12345/abc', relation=RelatedIdentifierRelation.CITES,
                                                   type=RelatedIdentifierType.DOI)],
                             creators=[Creator(id=5, creator_name='Carberry, Josiah')],
                             status=IdentifierStatusType.PUBLISHED,
                             owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'))
            # mock
            mock.put('/api/identifier/10', json=exp.model_dump(), status_code=202)
            # test
            client = RestClient(username="a", password="b")
            response = client.update_identifier(identifier_id=10,
                                                database_id=1, type=IdentifierType.VIEW,
                                                titles=[SaveIdentifierTitle(id=10, title='Test Title')],
                                                publisher='TU Wien', publication_year=2024,
                                                language=Language.EN,
                                                funders=[SaveIdentifierFunder(id=2, funder_name='FWF')],
                                                related_identifiers=[SaveRelatedIdentifier(id=2,
                                                                                           value='10.12345/abc',
                                                                                           relation=RelatedIdentifierRelation.CITES,
                                                                                           type=RelatedIdentifierType.DOI)],
                                                descriptions=[SaveIdentifierDescription(id=2,
                                                                                        description='Test Description')],
                                                creators=[SaveIdentifierCreator(id=30,
                                                                                creator_name='Carberry, Josiah')])
            self.assertEqual(exp, response)

    def test_update_identifier_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/identifier/10', status_code=400)
            # test
            client = RestClient(username="a", password="b")
            try:
                client.update_identifier(identifier_id=10,
                                         database_id=1, type=IdentifierType.VIEW,
                                         titles=[SaveIdentifierTitle(id=10, title='Test Title')],
                                         publisher='TU Wien', publication_year=2024,
                                         language=Language.EN,
                                         funders=[SaveIdentifierFunder(id=2, funder_name='FWF')],
                                         related_identifiers=[SaveRelatedIdentifier(id=2,
                                                                                    value='10.12345/abc',
                                                                                    relation=RelatedIdentifierRelation.CITES,
                                                                                    type=RelatedIdentifierType.DOI)],
                                         descriptions=[SaveIdentifierDescription(id=2,
                                                                                 description='Test Description')],
                                         creators=[SaveIdentifierCreator(id=30,
                                                                         creator_name='Carberry, Josiah')])
            except MalformedError:
                pass

    def test_update_identifier_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/identifier/10', status_code=403)
            # test
            client = RestClient(username="a", password="b")
            try:
                client.update_identifier(identifier_id=10,
                                         database_id=1, type=IdentifierType.VIEW,
                                         titles=[SaveIdentifierTitle(id=10, title='Test Title')],
                                         publisher='TU Wien', publication_year=2024,
                                         language=Language.EN,
                                         funders=[SaveIdentifierFunder(id=2, funder_name='FWF')],
                                         related_identifiers=[SaveRelatedIdentifier(id=2,
                                                                                    value='10.12345/abc',
                                                                                    relation=RelatedIdentifierRelation.CITES,
                                                                                    type=RelatedIdentifierType.DOI)],
                                         descriptions=[SaveIdentifierDescription(id=2,
                                                                                 description='Test Description')],
                                         creators=[SaveIdentifierCreator(id=30,
                                                                         creator_name='Carberry, Josiah')])
            except ForbiddenError:
                pass

    def test_update_identifier_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/identifier/10', status_code=404)
            # test
            client = RestClient(username="a", password="b")
            try:
                client.update_identifier(identifier_id=10,
                                         database_id=1, type=IdentifierType.VIEW,
                                         titles=[SaveIdentifierTitle(id=10, title='Test Title')],
                                         publisher='TU Wien', publication_year=2024,
                                         language=Language.EN,
                                         funders=[SaveIdentifierFunder(id=2, funder_name='FWF')],
                                         related_identifiers=[SaveRelatedIdentifier(id=2,
                                                                                    value='10.12345/abc',
                                                                                    relation=RelatedIdentifierRelation.CITES,
                                                                                    type=RelatedIdentifierType.DOI)],
                                         descriptions=[SaveIdentifierDescription(id=2,
                                                                                 description='Test Description')],
                                         creators=[SaveIdentifierCreator(id=30,
                                                                         creator_name='Carberry, Josiah')])
            except NotExistsError:
                pass

    def test_update_identifier_502_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/identifier/10', status_code=502)
            # test
            client = RestClient(username="a", password="b")
            try:
                client.update_identifier(identifier_id=10,
                                         database_id=1, type=IdentifierType.VIEW,
                                         titles=[SaveIdentifierTitle(id=10, title='Test Title')],
                                         publisher='TU Wien', publication_year=2024,
                                         language=Language.EN,
                                         funders=[SaveIdentifierFunder(id=2, funder_name='FWF')],
                                         related_identifiers=[SaveRelatedIdentifier(id=2,
                                                                                    value='10.12345/abc',
                                                                                    relation=RelatedIdentifierRelation.CITES,
                                                                                    type=RelatedIdentifierType.DOI)],
                                         descriptions=[SaveIdentifierDescription(id=2,
                                                                                 description='Test Description')],
                                         creators=[SaveIdentifierCreator(id=30,
                                                                         creator_name='Carberry, Josiah')])
            except ServiceConnectionError:
                pass

    def test_update_identifier_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/identifier/10', status_code=503)
            # test
            client = RestClient(username="a", password="b")
            try:
                client.update_identifier(identifier_id=10,
                                         database_id=1, type=IdentifierType.VIEW,
                                         titles=[SaveIdentifierTitle(id=10, title='Test Title')],
                                         publisher='TU Wien', publication_year=2024,
                                         language=Language.EN,
                                         funders=[SaveIdentifierFunder(id=2, funder_name='FWF')],
                                         related_identifiers=[SaveRelatedIdentifier(id=2,
                                                                                    value='10.12345/abc',
                                                                                    relation=RelatedIdentifierRelation.CITES,
                                                                                    type=RelatedIdentifierType.DOI)],
                                         descriptions=[SaveIdentifierDescription(id=2,
                                                                                 description='Test Description')],
                                         creators=[SaveIdentifierCreator(id=30,
                                                                         creator_name='Carberry, Josiah')])
            except ServiceError:
                pass

    def test_update_identifier_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/identifier/10', status_code=200)
            # test
            client = RestClient(username="a", password="b")
            try:
                client.update_identifier(identifier_id=10,
                                         database_id=1, type=IdentifierType.VIEW,
                                         titles=[SaveIdentifierTitle(id=10, title='Test Title')],
                                         publisher='TU Wien', publication_year=2024,
                                         language=Language.EN,
                                         funders=[SaveIdentifierFunder(id=2, funder_name='FWF')],
                                         related_identifiers=[SaveRelatedIdentifier(id=2,
                                                                                    value='10.12345/abc',
                                                                                    relation=RelatedIdentifierRelation.CITES,
                                                                                    type=RelatedIdentifierType.DOI)],
                                         descriptions=[SaveIdentifierDescription(id=2,
                                                                                 description='Test Description')],
                                         creators=[SaveIdentifierCreator(id=30,
                                                                         creator_name='Carberry, Josiah')])
            except ResponseCodeError:
                pass

    def test_update_identifier_anonymous_fails(self):
        # test
        try:
            RestClient().update_identifier(identifier_id=10,
                                           database_id=1, type=IdentifierType.VIEW,
                                           titles=[SaveIdentifierTitle(id=10, title='Test Title')],
                                           publisher='TU Wien', publication_year=2024,
                                           language=Language.EN,
                                           funders=[SaveIdentifierFunder(id=2, funder_name='FWF')],
                                           related_identifiers=[SaveRelatedIdentifier(id=2,
                                                                                      value='10.12345/abc',
                                                                                      relation=RelatedIdentifierRelation.CITES,
                                                                                      type=RelatedIdentifierType.DOI)],
                                           descriptions=[SaveIdentifierDescription(id=2,
                                                                                   description='Test Description')],
                                           creators=[SaveIdentifierCreator(id=30,
                                                                           creator_name='Carberry, Josiah')])
        except AuthenticationError:
            pass

    def test_publish_identifier_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = Identifier(id=10,
                             database_id=1,
                             view_id=32,
                             publication_year=2024,
                             publisher='TU Wien',
                             type=IdentifierType.VIEW,
                             language=Language.EN,
                             descriptions=[IdentifierDescription(id=2, description='Test Description')],
                             titles=[IdentifierTitle(id=3, title='Test Title')],
                             funders=[IdentifierFunder(id=4, funder_name='FWF')],
                             related_identifiers=[
                                 RelatedIdentifier(id=7, value='10.12345/abc', relation=RelatedIdentifierRelation.CITES,
                                                   type=RelatedIdentifierType.DOI)],
                             creators=[Creator(id=5, creator_name='Carberry, Josiah')],
                             status=IdentifierStatusType.PUBLISHED,
                             owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'))
            # mock
            mock.put('/api/identifier/10/publish', json=exp.model_dump(), status_code=202)
            # test
            client = RestClient(username="a", password="b")
            response = client.publish_identifier(identifier_id=10)
            self.assertEqual(exp, response)

    def test_publish_identifier_400_fails(self):
        with requests_mock.Mocker() as mock:
            exp = Identifier(id=10,
                             database_id=1,
                             view_id=32,
                             publication_year=2024,
                             publisher='TU Wien',
                             type=IdentifierType.VIEW,
                             language=Language.EN,
                             descriptions=[IdentifierDescription(id=2, description='Test Description')],
                             titles=[IdentifierTitle(id=3, title='Test Title')],
                             funders=[IdentifierFunder(id=4, funder_name='FWF')],
                             related_identifiers=[
                                 RelatedIdentifier(id=7, value='10.12345/abc', relation=RelatedIdentifierRelation.CITES,
                                                   type=RelatedIdentifierType.DOI)],
                             creators=[Creator(id=5, creator_name='Carberry, Josiah')],
                             status=IdentifierStatusType.PUBLISHED,
                             owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'))
            # mock
            mock.put('/api/identifier/10/publish', json=exp.model_dump(), status_code=400)
            # test
            try:
                RestClient(username="a", password="b").publish_identifier(identifier_id=10)
            except MalformedError:
                pass

    def test_publish_identifier_403_fails(self):
        with requests_mock.Mocker() as mock:
            exp = Identifier(id=10,
                             database_id=1,
                             view_id=32,
                             publication_year=2024,
                             publisher='TU Wien',
                             type=IdentifierType.VIEW,
                             language=Language.EN,
                             descriptions=[IdentifierDescription(id=2, description='Test Description')],
                             titles=[IdentifierTitle(id=3, title='Test Title')],
                             funders=[IdentifierFunder(id=4, funder_name='FWF')],
                             related_identifiers=[
                                 RelatedIdentifier(id=7, value='10.12345/abc', relation=RelatedIdentifierRelation.CITES,
                                                   type=RelatedIdentifierType.DOI)],
                             creators=[Creator(id=5, creator_name='Carberry, Josiah')],
                             status=IdentifierStatusType.PUBLISHED,
                             owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'))
            # mock
            mock.put('/api/identifier/10/publish', json=exp.model_dump(), status_code=403)
            # test
            try:
                RestClient(username="a", password="b").publish_identifier(identifier_id=10)
            except ForbiddenError:
                pass

    def test_publish_identifier_404_fails(self):
        with requests_mock.Mocker() as mock:
            exp = Identifier(id=10,
                             database_id=1,
                             view_id=32,
                             publication_year=2024,
                             publisher='TU Wien',
                             type=IdentifierType.VIEW,
                             language=Language.EN,
                             descriptions=[IdentifierDescription(id=2, description='Test Description')],
                             titles=[IdentifierTitle(id=3, title='Test Title')],
                             funders=[IdentifierFunder(id=4, funder_name='FWF')],
                             related_identifiers=[
                                 RelatedIdentifier(id=7, value='10.12345/abc', relation=RelatedIdentifierRelation.CITES,
                                                   type=RelatedIdentifierType.DOI)],
                             creators=[Creator(id=5, creator_name='Carberry, Josiah')],
                             status=IdentifierStatusType.PUBLISHED,
                             owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'))
            # mock
            mock.put('/api/identifier/10/publish', json=exp.model_dump(), status_code=404)
            # test
            try:
                RestClient(username="a", password="b").publish_identifier(identifier_id=10)
            except NotExistsError:
                pass

    def test_publish_identifier_502_fails(self):
        with requests_mock.Mocker() as mock:
            exp = Identifier(id=10,
                             database_id=1,
                             view_id=32,
                             publication_year=2024,
                             publisher='TU Wien',
                             type=IdentifierType.VIEW,
                             language=Language.EN,
                             descriptions=[IdentifierDescription(id=2, description='Test Description')],
                             titles=[IdentifierTitle(id=3, title='Test Title')],
                             funders=[IdentifierFunder(id=4, funder_name='FWF')],
                             related_identifiers=[
                                 RelatedIdentifier(id=7, value='10.12345/abc', relation=RelatedIdentifierRelation.CITES,
                                                   type=RelatedIdentifierType.DOI)],
                             creators=[Creator(id=5, creator_name='Carberry, Josiah')],
                             status=IdentifierStatusType.PUBLISHED,
                             owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'))
            # mock
            mock.put('/api/identifier/10/publish', json=exp.model_dump(), status_code=502)
            # test
            try:
                RestClient(username="a", password="b").publish_identifier(identifier_id=10)
            except ServiceConnectionError:
                pass

    def test_publish_identifier_503_fails(self):
        with requests_mock.Mocker() as mock:
            exp = Identifier(id=10,
                             database_id=1,
                             view_id=32,
                             publication_year=2024,
                             publisher='TU Wien',
                             type=IdentifierType.VIEW,
                             language=Language.EN,
                             descriptions=[IdentifierDescription(id=2, description='Test Description')],
                             titles=[IdentifierTitle(id=3, title='Test Title')],
                             funders=[IdentifierFunder(id=4, funder_name='FWF')],
                             related_identifiers=[
                                 RelatedIdentifier(id=7, value='10.12345/abc', relation=RelatedIdentifierRelation.CITES,
                                                   type=RelatedIdentifierType.DOI)],
                             creators=[Creator(id=5, creator_name='Carberry, Josiah')],
                             status=IdentifierStatusType.PUBLISHED,
                             owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'))
            # mock
            mock.put('/api/identifier/10/publish', json=exp.model_dump(), status_code=503)
            # test
            try:
                RestClient(username="a", password="b").publish_identifier(identifier_id=10)
            except ServiceError:
                pass

    def test_publish_identifier_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            exp = Identifier(id=10,
                             database_id=1,
                             view_id=32,
                             publication_year=2024,
                             publisher='TU Wien',
                             type=IdentifierType.VIEW,
                             language=Language.EN,
                             descriptions=[IdentifierDescription(id=2, description='Test Description')],
                             titles=[IdentifierTitle(id=3, title='Test Title')],
                             funders=[IdentifierFunder(id=4, funder_name='FWF')],
                             related_identifiers=[
                                 RelatedIdentifier(id=7, value='10.12345/abc', relation=RelatedIdentifierRelation.CITES,
                                                   type=RelatedIdentifierType.DOI)],
                             creators=[Creator(id=5, creator_name='Carberry, Josiah')],
                             status=IdentifierStatusType.PUBLISHED,
                             owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'))
            # mock
            mock.put('/api/identifier/10/publish', json=exp.model_dump(), status_code=200)
            # test
            try:
                RestClient(username="a", password="b").publish_identifier(identifier_id=10)
            except ResponseCodeError:
                pass

    def test_publish_identifier_anonymous_fails(self):
        # test
        try:
            RestClient().publish_identifier(identifier_id=10)
        except AuthenticationError:
            pass


if __name__ == "__main__":
    unittest.main()

import unittest

import requests_mock

from dbrepo.RestClient import RestClient
from dbrepo.api.dto import Identifier, IdentifierType, SaveIdentifierTitle, Creator, IdentifierTitle, \
    IdentifierDescription, SaveIdentifierDescription, Language, SaveIdentifierFunder, SaveRelatedIdentifier, \
    RelatedIdentifierRelation, RelatedIdentifierType, IdentifierFunder, RelatedIdentifier, UserBrief, \
    IdentifierStatusType, CreateIdentifierCreator, CreateIdentifierTitle, CreateIdentifierFunder, \
    CreateRelatedIdentifier, CreateIdentifierDescription, SaveIdentifierCreator, Links
from dbrepo.api.exceptions import MalformedError, ForbiddenError, NotExistsError, AuthenticationError, \
    ServiceConnectionError, ServiceError, ResponseCodeError, FormatNotAvailable, RequestError


class IdentifierUnitTest(unittest.TestCase):

    def test_create_identifier_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = Identifier(id="f6171539-a479-4829-9b9b-a6b474e1c7d3",
                             database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                             view_id="e5229d24-584a-43e8-b9f6-d349c3053f9c",
                             links=Links(self="/api/identifier/f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                         self_html="/pid/f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                         data="/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/e5229d24-584a-43e8-b9f6-d349c3053f9c/data"),
                             publication_year=2024,
                             publisher='TU Wien',
                             type=IdentifierType.VIEW,
                             language=Language.EN,
                             descriptions=[IdentifierDescription(id="d8bdc933-655c-46bd-9903-ede3928a304b",
                                                                 description='Test Description')],
                             titles=[IdentifierTitle(id="ffaf9e0c-c37d-4655-bd68-80cd991cf24c",
                                                     title='Test Title')],
                             funders=[IdentifierFunder(id="d0dc801d-cfb7-4b07-9b20-2d7af39c913c",
                                                       funder_name='FWF')],
                             related_identifiers=[
                                 RelatedIdentifier(id="6655eba7-b0ac-4bc4-9f09-6355fec8d815",
                                                   value='10.12345/abc',
                                                   relation=RelatedIdentifierRelation.CITES,
                                                   type=RelatedIdentifierType.DOI)],
                             creators=[Creator(id="70539dff-c549-4c95-8257-9c750decf232",
                                               creator_name='Carberry, Josiah')],
                             status=IdentifierStatusType.PUBLISHED,
                             owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'))
            # mock
            mock.post('/api/identifier', json=exp.model_dump(), status_code=201)
            # test
            client = RestClient(username="a", password="b")
            response = client.create_identifier(
                database_id="6bd39359-b154-456d-b9c2-caa516a45732", type=IdentifierType.VIEW,
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
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732", type=IdentifierType.VIEW,
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
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732", type=IdentifierType.VIEW,
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
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732", type=IdentifierType.VIEW,
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
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732", type=IdentifierType.VIEW,
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
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732", type=IdentifierType.VIEW,
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
                    database_id="6bd39359-b154-456d-b9c2-caa516a45732", type=IdentifierType.VIEW,
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
                database_id="6bd39359-b154-456d-b9c2-caa516a45732", type=IdentifierType.VIEW,
                titles=[CreateIdentifierTitle(title='Test Title')],
                descriptions=[CreateIdentifierDescription(description='Test')],
                publisher='TU Wien', publication_year=2024,
                creators=[CreateIdentifierCreator(creator_name='Carberry, Josiah')])
        except AuthenticationError:
            pass

    def test_get_identifiers_view_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [Identifier(id="f6171539-a479-4829-9b9b-a6b474e1c7d3",
                              database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                              view_id="e5229d24-584a-43e8-b9f6-d349c3053f9c",
                              publication_year=2024,
                              publisher='TU Wien',
                              type=IdentifierType.VIEW,
                              language=Language.EN,
                              links=Links(self="/api/identifier/f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                          self_html="/pid/f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                          data="/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/e5229d24-584a-43e8-b9f6-d349c3053f9c/data"),
                              descriptions=[IdentifierDescription(id="d8bdc933-655c-46bd-9903-ede3928a304b",
                                                                  description='Test Description')],
                              titles=[IdentifierTitle(id="f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                                      title='Test Title')],
                              funders=[IdentifierFunder(id="d0dc801d-cfb7-4b07-9b20-2d7af39c913c",
                                                        funder_name='FWF')],
                              related_identifiers=[
                                  RelatedIdentifier(id="6655eba7-b0ac-4bc4-9f09-6355fec8d815", value='10.12345/abc',
                                                    relation=RelatedIdentifierRelation.CITES,
                                                    type=RelatedIdentifierType.DOI)],
                              creators=[Creator(id="70539dff-c549-4c95-8257-9c750decf232",
                                                creator_name='Carberry, Josiah')],
                              status=IdentifierStatusType.PUBLISHED,
                              owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'))]
            # mock
            mock.get('/api/identifier', json=[exp[0].model_dump()], headers={"Accept": "application/json"})
            # test
            response = RestClient().get_identifiers(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                    view_id="e5229d24-584a-43e8-b9f6-d349c3053f9c",
                                                    type=IdentifierType.VIEW,
                                                    status=IdentifierStatusType.PUBLISHED)
            self.assertEqual(exp, response)

    def test_get_identifiers_subset_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = []
            # mock
            mock.get('/api/identifier', json=[], headers={"Accept": "application/json"})
            # test
            response = RestClient().get_identifiers(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                    subset_id="0831bf54-9dd9-46fe-8c2c-c539332ea177")
            self.assertEqual(exp, response)

    def test_get_identifiers_table_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = []
            # mock
            mock.get('/api/identifier', json=[], headers={"Accept": "application/json"})
            # test
            response = RestClient().get_identifiers(database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                    table_id="b3230b86-4743-498d-9015-3fad58049692")
            self.assertEqual(exp, response)

    def test_get_identifiers_view_param_database_fails(self):
        # test
        try:
            RestClient().get_identifiers(view_id="e5229d24-584a-43e8-b9f6-d349c3053f9c")
        except RequestError:
            pass

    def test_get_identifiers_subset_param_database_fails(self):
        # test
        try:
            RestClient().get_identifiers(subset_id="0831bf54-9dd9-46fe-8c2c-c539332ea177")
        except RequestError:
            pass

    def test_get_identifiers_table_param_database_fails(self):
        # test
        try:
            RestClient().get_identifiers(table_id="b3230b86-4743-498d-9015-3fad58049692")
        except RequestError:
            pass

    def test_get_identifiers_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/identifier', status_code=404)
            # test
            try:
                RestClient().get_identifiers()
            except NotExistsError:
                pass

    def test_get_identifiers_406_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/identifier', status_code=406)
            # test
            try:
                RestClient().get_identifiers()
            except FormatNotAvailable:
                pass

    def test_get_identifiers_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/identifier', status_code=202)
            # test
            try:
                RestClient().get_identifiers()
            except ResponseCodeError:
                pass

    def test_update_identifier_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = Identifier(id="f6171539-a479-4829-9b9b-a6b474e1c7d3",
                             database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                             view_id="e5229d24-584a-43e8-b9f6-d349c3053f9c",
                             publication_year=2024,
                             publisher='TU Wien',
                             type=IdentifierType.VIEW,
                             language=Language.EN,
                             links=Links(self="/api/identifier/f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                         self_html="/pid/f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                         data="/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/e5229d24-584a-43e8-b9f6-d349c3053f9c/data"),
                             descriptions=[IdentifierDescription(id="d8bdc933-655c-46bd-9903-ede3928a304b",
                                                                 description='Test Description')],
                             titles=[IdentifierTitle(id="f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                                     title='Test Title')],
                             funders=[IdentifierFunder(id="d0dc801d-cfb7-4b07-9b20-2d7af39c913c",
                                                       funder_name='FWF')],
                             related_identifiers=[
                                 RelatedIdentifier(id="6655eba7-b0ac-4bc4-9f09-6355fec8d815",
                                                   value='10.12345/abc',
                                                   relation=RelatedIdentifierRelation.CITES,
                                                   type=RelatedIdentifierType.DOI)],
                             creators=[Creator(id="70539dff-c549-4c95-8257-9c750decf232",
                                               creator_name='Carberry, Josiah')],
                             status=IdentifierStatusType.PUBLISHED,
                             owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'))
            # mock
            mock.put('/api/identifier/f6171539-a479-4829-9b9b-a6b474e1c7d3', json=exp.model_dump(), status_code=202)
            # test
            client = RestClient(username="a", password="b")
            response = client.update_identifier(identifier_id="f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                                database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                type=IdentifierType.VIEW,
                                                titles=[SaveIdentifierTitle(id="f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                                                            title='Test Title')],
                                                publisher='TU Wien', publication_year=2024,
                                                language=Language.EN,
                                                funders=[SaveIdentifierFunder(id="d0dc801d-cfb7-4b07-9b20-2d7af39c913c",
                                                                              funder_name='FWF')],
                                                related_identifiers=[
                                                    SaveRelatedIdentifier(id="6655eba7-b0ac-4bc4-9f09-6355fec8d815",
                                                                          value='10.12345/abc',
                                                                          relation=RelatedIdentifierRelation.CITES,
                                                                          type=RelatedIdentifierType.DOI)],
                                                descriptions=[
                                                    SaveIdentifierDescription(id="d8bdc933-655c-46bd-9903-ede3928a304b",
                                                                              description='Test Description')],
                                                creators=[
                                                    SaveIdentifierCreator(id="6bf894bc-8f55-4b5d-83cf-198b29253260",
                                                                          creator_name='Carberry, Josiah')])
            self.assertEqual(exp, response)

    def test_update_identifier_400_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/identifier/f6171539-a479-4829-9b9b-a6b474e1c7d3', status_code=400)
            # test
            client = RestClient(username="a", password="b")
            try:
                client.update_identifier(identifier_id="f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                         database_id="6bd39359-b154-456d-b9c2-caa516a45732", type=IdentifierType.VIEW,
                                         titles=[SaveIdentifierTitle(id="f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                                                     title='Test Title')],
                                         publisher='TU Wien', publication_year=2024,
                                         language=Language.EN,
                                         funders=[SaveIdentifierFunder(id="d0dc801d-cfb7-4b07-9b20-2d7af39c913c",
                                                                       funder_name='FWF')],
                                         related_identifiers=[
                                             SaveRelatedIdentifier(id="6655eba7-b0ac-4bc4-9f09-6355fec8d815",
                                                                   value='10.12345/abc',
                                                                   relation=RelatedIdentifierRelation.CITES,
                                                                   type=RelatedIdentifierType.DOI)],
                                         descriptions=[
                                             SaveIdentifierDescription(id="d8bdc933-655c-46bd-9903-ede3928a304b",
                                                                       description='Test Description')],
                                         creators=[SaveIdentifierCreator(id="6bf894bc-8f55-4b5d-83cf-198b29253260",
                                                                         creator_name='Carberry, Josiah')])
            except MalformedError:
                pass

    def test_update_identifier_403_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/identifier/f6171539-a479-4829-9b9b-a6b474e1c7d3', status_code=403)
            # test
            client = RestClient(username="a", password="b")
            try:
                client.update_identifier(identifier_id="f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                         database_id="6bd39359-b154-456d-b9c2-caa516a45732", type=IdentifierType.VIEW,
                                         titles=[SaveIdentifierTitle(id="f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                                                     title='Test Title')],
                                         publisher='TU Wien', publication_year=2024,
                                         language=Language.EN,
                                         funders=[SaveIdentifierFunder(id="d0dc801d-cfb7-4b07-9b20-2d7af39c913c",
                                                                       funder_name='FWF')],
                                         related_identifiers=[
                                             SaveRelatedIdentifier(id="6655eba7-b0ac-4bc4-9f09-6355fec8d815",
                                                                   value='10.12345/abc',
                                                                   relation=RelatedIdentifierRelation.CITES,
                                                                   type=RelatedIdentifierType.DOI)],
                                         descriptions=[
                                             SaveIdentifierDescription(id="d8bdc933-655c-46bd-9903-ede3928a304b",
                                                                       description='Test Description')],
                                         creators=[SaveIdentifierCreator(id="6bf894bc-8f55-4b5d-83cf-198b29253260",
                                                                         creator_name='Carberry, Josiah')])
            except ForbiddenError:
                pass

    def test_update_identifier_404_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/identifier/f6171539-a479-4829-9b9b-a6b474e1c7d3', status_code=404)
            # test
            client = RestClient(username="a", password="b")
            try:
                client.update_identifier(identifier_id="f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                         database_id="6bd39359-b154-456d-b9c2-caa516a45732", type=IdentifierType.VIEW,
                                         titles=[SaveIdentifierTitle(id="f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                                                     title='Test Title')],
                                         publisher='TU Wien', publication_year=2024,
                                         language=Language.EN,
                                         funders=[SaveIdentifierFunder(id="d0dc801d-cfb7-4b07-9b20-2d7af39c913c",
                                                                       funder_name='FWF')],
                                         related_identifiers=[
                                             SaveRelatedIdentifier(id="6655eba7-b0ac-4bc4-9f09-6355fec8d815",
                                                                   value='10.12345/abc',
                                                                   relation=RelatedIdentifierRelation.CITES,
                                                                   type=RelatedIdentifierType.DOI)],
                                         descriptions=[
                                             SaveIdentifierDescription(id="d8bdc933-655c-46bd-9903-ede3928a304b",
                                                                       description='Test Description')],
                                         creators=[SaveIdentifierCreator(id="6bf894bc-8f55-4b5d-83cf-198b29253260",
                                                                         creator_name='Carberry, Josiah')])
            except NotExistsError:
                pass

    def test_update_identifier_502_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/identifier/f6171539-a479-4829-9b9b-a6b474e1c7d3', status_code=502)
            # test
            client = RestClient(username="a", password="b")
            try:
                client.update_identifier(identifier_id="f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                         database_id="6bd39359-b154-456d-b9c2-caa516a45732", type=IdentifierType.VIEW,
                                         titles=[SaveIdentifierTitle(id="f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                                                     title='Test Title')],
                                         publisher='TU Wien', publication_year=2024,
                                         language=Language.EN,
                                         funders=[SaveIdentifierFunder(id="d0dc801d-cfb7-4b07-9b20-2d7af39c913c",
                                                                       funder_name='FWF')],
                                         related_identifiers=[
                                             SaveRelatedIdentifier(id="6655eba7-b0ac-4bc4-9f09-6355fec8d815",
                                                                   value='10.12345/abc',
                                                                   relation=RelatedIdentifierRelation.CITES,
                                                                   type=RelatedIdentifierType.DOI)],
                                         descriptions=[
                                             SaveIdentifierDescription(id="d8bdc933-655c-46bd-9903-ede3928a304b",
                                                                       description='Test Description')],
                                         creators=[SaveIdentifierCreator(id="6bf894bc-8f55-4b5d-83cf-198b29253260",
                                                                         creator_name='Carberry, Josiah')])
            except ServiceConnectionError:
                pass

    def test_update_identifier_503_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/identifier/f6171539-a479-4829-9b9b-a6b474e1c7d3', status_code=503)
            # test
            client = RestClient(username="a", password="b")
            try:
                client.update_identifier(identifier_id="f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                         database_id="6bd39359-b154-456d-b9c2-caa516a45732", type=IdentifierType.VIEW,
                                         titles=[SaveIdentifierTitle(id="f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                                                     title='Test Title')],
                                         publisher='TU Wien', publication_year=2024,
                                         language=Language.EN,
                                         funders=[SaveIdentifierFunder(id="d0dc801d-cfb7-4b07-9b20-2d7af39c913c",
                                                                       funder_name='FWF')],
                                         related_identifiers=[
                                             SaveRelatedIdentifier(id="6655eba7-b0ac-4bc4-9f09-6355fec8d815",
                                                                   value='10.12345/abc',
                                                                   relation=RelatedIdentifierRelation.CITES,
                                                                   type=RelatedIdentifierType.DOI)],
                                         descriptions=[
                                             SaveIdentifierDescription(id="d8bdc933-655c-46bd-9903-ede3928a304b",
                                                                       description='Test Description')],
                                         creators=[SaveIdentifierCreator(id="6bf894bc-8f55-4b5d-83cf-198b29253260",
                                                                         creator_name='Carberry, Josiah')])
            except ServiceError:
                pass

    def test_update_identifier_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.put('/api/identifier/f6171539-a479-4829-9b9b-a6b474e1c7d3', status_code=200)
            # test
            client = RestClient(username="a", password="b")
            try:
                client.update_identifier(identifier_id="f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                         database_id="6bd39359-b154-456d-b9c2-caa516a45732", type=IdentifierType.VIEW,
                                         titles=[SaveIdentifierTitle(id="f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                                                     title='Test Title')],
                                         publisher='TU Wien', publication_year=2024,
                                         language=Language.EN,
                                         funders=[SaveIdentifierFunder(id="d0dc801d-cfb7-4b07-9b20-2d7af39c913c",
                                                                       funder_name='FWF')],
                                         related_identifiers=[
                                             SaveRelatedIdentifier(id="6655eba7-b0ac-4bc4-9f09-6355fec8d815",
                                                                   value='10.12345/abc',
                                                                   relation=RelatedIdentifierRelation.CITES,
                                                                   type=RelatedIdentifierType.DOI)],
                                         descriptions=[
                                             SaveIdentifierDescription(id="d8bdc933-655c-46bd-9903-ede3928a304b",
                                                                       description='Test Description')],
                                         creators=[SaveIdentifierCreator(id="6bf894bc-8f55-4b5d-83cf-198b29253260",
                                                                         creator_name='Carberry, Josiah')])
            except ResponseCodeError:
                pass

    def test_update_identifier_anonymous_fails(self):
        # test
        try:
            RestClient().update_identifier(identifier_id="f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                           database_id="6bd39359-b154-456d-b9c2-caa516a45732", type=IdentifierType.VIEW,
                                           titles=[SaveIdentifierTitle(id="f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                                                       title='Test Title')],
                                           publisher='TU Wien', publication_year=2024,
                                           language=Language.EN,
                                           funders=[SaveIdentifierFunder(id="d0dc801d-cfb7-4b07-9b20-2d7af39c913c",
                                                                         funder_name='FWF')],
                                           related_identifiers=[
                                               SaveRelatedIdentifier(id="6655eba7-b0ac-4bc4-9f09-6355fec8d815",
                                                                     value='10.12345/abc',
                                                                     relation=RelatedIdentifierRelation.CITES,
                                                                     type=RelatedIdentifierType.DOI)],
                                           descriptions=[
                                               SaveIdentifierDescription(id="d8bdc933-655c-46bd-9903-ede3928a304b",
                                                                         description='Test Description')],
                                           creators=[SaveIdentifierCreator(id="6bf894bc-8f55-4b5d-83cf-198b29253260",
                                                                           creator_name='Carberry, Josiah')])
        except AuthenticationError:
            pass

    def test_publish_identifier_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = Identifier(id="f6171539-a479-4829-9b9b-a6b474e1c7d3",
                             database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                             view_id="e5229d24-584a-43e8-b9f6-d349c3053f9c",
                             publication_year=2024,
                             publisher='TU Wien',
                             type=IdentifierType.VIEW,
                             language=Language.EN,
                             links=Links(self="/api/identifier/f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                         self_html="/pid/f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                         data="/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/e5229d24-584a-43e8-b9f6-d349c3053f9c/data"),
                             descriptions=[IdentifierDescription(id="d8bdc933-655c-46bd-9903-ede3928a304b",
                                                                 description='Test Description')],
                             titles=[IdentifierTitle(id="f6171539-a479-4829-9b9b-a6b474e1c7d3", title='Test Title')],
                             funders=[IdentifierFunder(id="d0dc801d-cfb7-4b07-9b20-2d7af39c913c", funder_name='FWF')],
                             related_identifiers=[
                                 RelatedIdentifier(id="6655eba7-b0ac-4bc4-9f09-6355fec8d815", value='10.12345/abc',
                                                   relation=RelatedIdentifierRelation.CITES,
                                                   type=RelatedIdentifierType.DOI)],
                             creators=[
                                 Creator(id="70539dff-c549-4c95-8257-9c750decf232", creator_name='Carberry, Josiah')],
                             status=IdentifierStatusType.PUBLISHED,
                             owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'))
            # mock
            mock.put('/api/identifier/f6171539-a479-4829-9b9b-a6b474e1c7d3/publish', json=exp.model_dump(),
                     status_code=202)
            # test
            client = RestClient(username="a", password="b")
            response = client.publish_identifier(identifier_id="f6171539-a479-4829-9b9b-a6b474e1c7d3")
            self.assertEqual(exp, response)

    def test_publish_identifier_400_fails(self):
        with requests_mock.Mocker() as mock:
            exp = Identifier(id="f6171539-a479-4829-9b9b-a6b474e1c7d3",
                             database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                             view_id="e5229d24-584a-43e8-b9f6-d349c3053f9c",
                             publication_year=2024,
                             publisher='TU Wien',
                             type=IdentifierType.VIEW,
                             language=Language.EN,
                             links=Links(self="/api/identifier/f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                         self_html="/pid/f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                         data="/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/e5229d24-584a-43e8-b9f6-d349c3053f9c/data"),
                             descriptions=[IdentifierDescription(id="d8bdc933-655c-46bd-9903-ede3928a304b",
                                                                 description='Test Description')],
                             titles=[IdentifierTitle(id="f6171539-a479-4829-9b9b-a6b474e1c7d3", title='Test Title')],
                             funders=[IdentifierFunder(id="d0dc801d-cfb7-4b07-9b20-2d7af39c913c", funder_name='FWF')],
                             related_identifiers=[
                                 RelatedIdentifier(id="6655eba7-b0ac-4bc4-9f09-6355fec8d815", value='10.12345/abc',
                                                   relation=RelatedIdentifierRelation.CITES,
                                                   type=RelatedIdentifierType.DOI)],
                             creators=[
                                 Creator(id="70539dff-c549-4c95-8257-9c750decf232", creator_name='Carberry, Josiah')],
                             status=IdentifierStatusType.PUBLISHED,
                             owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'))
            # mock
            mock.put('/api/identifier/f6171539-a479-4829-9b9b-a6b474e1c7d3/publish', json=exp.model_dump(),
                     status_code=400)
            # test
            try:
                RestClient(username="a", password="b").publish_identifier(
                    identifier_id="f6171539-a479-4829-9b9b-a6b474e1c7d3")
            except MalformedError:
                pass

    def test_publish_identifier_403_fails(self):
        with requests_mock.Mocker() as mock:
            exp = Identifier(id="f6171539-a479-4829-9b9b-a6b474e1c7d3",
                             database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                             view_id="e5229d24-584a-43e8-b9f6-d349c3053f9c",
                             publication_year=2024,
                             publisher='TU Wien',
                             type=IdentifierType.VIEW,
                             language=Language.EN,
                             links=Links(self="/api/identifier/f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                         self_html="/pid/f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                         data="/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/e5229d24-584a-43e8-b9f6-d349c3053f9c/data"),
                             descriptions=[IdentifierDescription(id="d8bdc933-655c-46bd-9903-ede3928a304b",
                                                                 description='Test Description')],
                             titles=[IdentifierTitle(id="f6171539-a479-4829-9b9b-a6b474e1c7d3", title='Test Title')],
                             funders=[IdentifierFunder(id="d0dc801d-cfb7-4b07-9b20-2d7af39c913c", funder_name='FWF')],
                             related_identifiers=[
                                 RelatedIdentifier(id="6655eba7-b0ac-4bc4-9f09-6355fec8d815", value='10.12345/abc',
                                                   relation=RelatedIdentifierRelation.CITES,
                                                   type=RelatedIdentifierType.DOI)],
                             creators=[
                                 Creator(id="70539dff-c549-4c95-8257-9c750decf232", creator_name='Carberry, Josiah')],
                             status=IdentifierStatusType.PUBLISHED,
                             owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'))
            # mock
            mock.put('/api/identifier/f6171539-a479-4829-9b9b-a6b474e1c7d3/publish', json=exp.model_dump(),
                     status_code=403)
            # test
            try:
                RestClient(username="a", password="b").publish_identifier(
                    identifier_id="f6171539-a479-4829-9b9b-a6b474e1c7d3")
            except ForbiddenError:
                pass

    def test_publish_identifier_404_fails(self):
        with requests_mock.Mocker() as mock:
            exp = Identifier(id="f6171539-a479-4829-9b9b-a6b474e1c7d3",
                             database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                             view_id="e5229d24-584a-43e8-b9f6-d349c3053f9c",
                             publication_year=2024,
                             publisher='TU Wien',
                             type=IdentifierType.VIEW,
                             language=Language.EN,
                             links=Links(self="/api/identifier/f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                         self_html="/pid/f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                         data="/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/e5229d24-584a-43e8-b9f6-d349c3053f9c/data"),
                             descriptions=[IdentifierDescription(id="d8bdc933-655c-46bd-9903-ede3928a304b",
                                                                 description='Test Description')],
                             titles=[IdentifierTitle(id="f6171539-a479-4829-9b9b-a6b474e1c7d3", title='Test Title')],
                             funders=[IdentifierFunder(id="d0dc801d-cfb7-4b07-9b20-2d7af39c913c", funder_name='FWF')],
                             related_identifiers=[
                                 RelatedIdentifier(id="6655eba7-b0ac-4bc4-9f09-6355fec8d815", value='10.12345/abc',
                                                   relation=RelatedIdentifierRelation.CITES,
                                                   type=RelatedIdentifierType.DOI)],
                             creators=[
                                 Creator(id="70539dff-c549-4c95-8257-9c750decf232", creator_name='Carberry, Josiah')],
                             status=IdentifierStatusType.PUBLISHED,
                             owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'))
            # mock
            mock.put('/api/identifier/f6171539-a479-4829-9b9b-a6b474e1c7d3/publish', json=exp.model_dump(),
                     status_code=404)
            # test
            try:
                RestClient(username="a", password="b").publish_identifier(
                    identifier_id="f6171539-a479-4829-9b9b-a6b474e1c7d3")
            except NotExistsError:
                pass

    def test_publish_identifier_502_fails(self):
        with requests_mock.Mocker() as mock:
            exp = Identifier(id="f6171539-a479-4829-9b9b-a6b474e1c7d3",
                             database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                             view_id="e5229d24-584a-43e8-b9f6-d349c3053f9c",
                             publication_year=2024,
                             publisher='TU Wien',
                             type=IdentifierType.VIEW,
                             language=Language.EN,
                             links=Links(self="/api/identifier/f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                         self_html="/pid/f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                         data="/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/e5229d24-584a-43e8-b9f6-d349c3053f9c/data"),
                             descriptions=[IdentifierDescription(id="d8bdc933-655c-46bd-9903-ede3928a304b",
                                                                 description='Test Description')],
                             titles=[IdentifierTitle(id="f6171539-a479-4829-9b9b-a6b474e1c7d3", title='Test Title')],
                             funders=[IdentifierFunder(id="d0dc801d-cfb7-4b07-9b20-2d7af39c913c", funder_name='FWF')],
                             related_identifiers=[
                                 RelatedIdentifier(id="6655eba7-b0ac-4bc4-9f09-6355fec8d815", value='10.12345/abc',
                                                   relation=RelatedIdentifierRelation.CITES,
                                                   type=RelatedIdentifierType.DOI)],
                             creators=[
                                 Creator(id="70539dff-c549-4c95-8257-9c750decf232", creator_name='Carberry, Josiah')],
                             status=IdentifierStatusType.PUBLISHED,
                             owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'))
            # mock
            mock.put('/api/identifier/f6171539-a479-4829-9b9b-a6b474e1c7d3/publish', json=exp.model_dump(),
                     status_code=502)
            # test
            try:
                RestClient(username="a", password="b").publish_identifier(
                    identifier_id="f6171539-a479-4829-9b9b-a6b474e1c7d3")
            except ServiceConnectionError:
                pass

    def test_publish_identifier_503_fails(self):
        with requests_mock.Mocker() as mock:
            exp = Identifier(id="f6171539-a479-4829-9b9b-a6b474e1c7d3",
                             database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                             view_id="e5229d24-584a-43e8-b9f6-d349c3053f9c",
                             publication_year=2024,
                             publisher='TU Wien',
                             type=IdentifierType.VIEW,
                             language=Language.EN,
                             links=Links(self="/api/identifier/f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                         self_html="/pid/f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                         data="/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/e5229d24-584a-43e8-b9f6-d349c3053f9c/data"),
                             descriptions=[IdentifierDescription(id="d8bdc933-655c-46bd-9903-ede3928a304b",
                                                                 description='Test Description')],
                             titles=[IdentifierTitle(id="f6171539-a479-4829-9b9b-a6b474e1c7d3", title='Test Title')],
                             funders=[IdentifierFunder(id="d0dc801d-cfb7-4b07-9b20-2d7af39c913c", funder_name='FWF')],
                             related_identifiers=[
                                 RelatedIdentifier(id="6655eba7-b0ac-4bc4-9f09-6355fec8d815", value='10.12345/abc',
                                                   relation=RelatedIdentifierRelation.CITES,
                                                   type=RelatedIdentifierType.DOI)],
                             creators=[
                                 Creator(id="70539dff-c549-4c95-8257-9c750decf232", creator_name='Carberry, Josiah')],
                             status=IdentifierStatusType.PUBLISHED,
                             owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'))
            # mock
            mock.put('/api/identifier/f6171539-a479-4829-9b9b-a6b474e1c7d3/publish', json=exp.model_dump(),
                     status_code=503)
            # test
            try:
                RestClient(username="a", password="b").publish_identifier(
                    identifier_id="f6171539-a479-4829-9b9b-a6b474e1c7d3")
            except ServiceError:
                pass

    def test_publish_identifier_unknown_fails(self):
        with requests_mock.Mocker() as mock:
            exp = Identifier(id="f6171539-a479-4829-9b9b-a6b474e1c7d3",
                             database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                             view_id="e5229d24-584a-43e8-b9f6-d349c3053f9c",
                             publication_year=2024,
                             publisher='TU Wien',
                             type=IdentifierType.VIEW,
                             language=Language.EN,
                             links=Links(self="/api/identifier/f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                         self_html="/pid/f6171539-a479-4829-9b9b-a6b474e1c7d3",
                                         data="/api/database/6bd39359-b154-456d-b9c2-caa516a45732/view/e5229d24-584a-43e8-b9f6-d349c3053f9c/data"),
                             descriptions=[IdentifierDescription(id="d8bdc933-655c-46bd-9903-ede3928a304b",
                                                                 description='Test Description')],
                             titles=[IdentifierTitle(id="f6171539-a479-4829-9b9b-a6b474e1c7d3", title='Test Title')],
                             funders=[IdentifierFunder(id="d0dc801d-cfb7-4b07-9b20-2d7af39c913c", funder_name='FWF')],
                             related_identifiers=[
                                 RelatedIdentifier(id="6655eba7-b0ac-4bc4-9f09-6355fec8d815", value='10.12345/abc',
                                                   relation=RelatedIdentifierRelation.CITES,
                                                   type=RelatedIdentifierType.DOI)],
                             creators=[
                                 Creator(id="70539dff-c549-4c95-8257-9c750decf232", creator_name='Carberry, Josiah')],
                             status=IdentifierStatusType.PUBLISHED,
                             owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'))
            # mock
            mock.put('/api/identifier/f6171539-a479-4829-9b9b-a6b474e1c7d3/publish', json=exp.model_dump(),
                     status_code=200)
            # test
            try:
                RestClient(username="a", password="b").publish_identifier(
                    identifier_id="f6171539-a479-4829-9b9b-a6b474e1c7d3")
            except ResponseCodeError:
                pass

    def test_publish_identifier_anonymous_fails(self):
        # test
        try:
            RestClient().publish_identifier(identifier_id="f6171539-a479-4829-9b9b-a6b474e1c7d3")
        except AuthenticationError:
            pass


if __name__ == "__main__":
    unittest.main()

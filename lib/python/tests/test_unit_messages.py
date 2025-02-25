import unittest

import requests_mock

from dbrepo.RestClient import RestClient
from dbrepo.api.dto import Message


class ImageUnitTest(unittest.TestCase):

    def test_get_message_empty_succeeds(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/message', json=[])
            # test
            response = RestClient().get_messages()
            self.assertEqual([], response)

    def test_get_images_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [Message(id="97e46776-aef2-4a6f-9e82-9d2ae556745f", type="info")]
            # mock
            mock.get('/api/message', json=[exp[0].model_dump()])
            # test
            response = RestClient().get_messages()
            self.assertEqual(exp, response)


if __name__ == "__main__":
    unittest.main()

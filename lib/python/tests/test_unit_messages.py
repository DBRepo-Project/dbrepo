import unittest

import requests_mock

from dbrepo.RestClient import RestClient

from dbrepo.api.dto import ImageBrief, BannerMessage


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
            exp = [BannerMessage(id=1, type="info")]
            # mock
            mock.get('/api/message', json=[exp[0].model_dump()])
            # test
            response = RestClient().get_messages()
            self.assertEqual(exp, response)


if __name__ == "__main__":
    unittest.main()

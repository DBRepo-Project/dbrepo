import unittest

import requests_mock

from dbrepo.RestClient import RestClient
from dbrepo.api.dto import ImageBrief


class ImageUnitTest(unittest.TestCase):

    def test_get_images_empty_succeeds(self):
        with requests_mock.Mocker() as mock:
            # mock
            mock.get('/api/image', json=[])
            # test
            response = RestClient().get_images()
            self.assertEqual([], response)

    def test_get_images_succeeds(self):
        with requests_mock.Mocker() as mock:
            exp = [ImageBrief(id="5d065194-2196-42e3-83e6-d8fc658c4b66",
                              name="mariadb",
                              version="11.1.3")]
            # mock
            mock.get('/api/image', json=[exp[0].model_dump()])
            # test
            response = RestClient().get_images()
            self.assertEqual(exp, response)


if __name__ == "__main__":
    unittest.main()

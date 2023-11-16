#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Mon Jan  9 08:46:04 2023

@author: Martin Weise
"""
import unittest

from botocore.exceptions import ClientError

from clients.minio_client import MinioClient


class MinioClientTest(unittest.TestCase):

    # @Test
    def test_upload_file_succeeds(self):

        # test
        response = MinioClient().upload_file(filename="testdt01.csv", path="./data/")
        self.assertTrue(response)

    # @Test
    def test_upload_file_notFound_fails(self):

        # test
        try:
            MinioClient().upload_file(filename="testdt06.csv", path="./data/")
        except FileNotFoundError:
            pass
        except Exception:
            self.fail('unexpected exception raised')
        else:
            self.fail('FileNotFoundError not raised')

    # @Test
    def test_download_file_succeeds(self):

        # mock
        MinioClient().upload_file(filename="testdt01.csv", path="./data/", bucket="dbrepo-upload")

        # test
        response = MinioClient().download_file(filename="testdt01.csv")
        self.assertTrue(response)

    # @Test
    def test_download_file_notFound_fails(self):

        # test
        try:
            MinioClient().download_file(filename="testdt01.csv")
        except ClientError:
            pass
        except Exception:
            self.fail('unexpected exception raised')
        else:
            self.fail('ClientError not raised')

    # @Test
    def test_get_file_succeeds(self):

        # mock
        MinioClient().upload_file(filename="testdt01.csv", path="./data/", bucket="dbrepo-upload")

        # test
        response = MinioClient().get_file(bucket="dbrepo-upload", filename="testdt01.csv")
        self.assertIsNotNone(response)

    # @Test
    def test_get_file_notFound_fails(self):

        # test
        try:
            MinioClient().get_file(bucket="dbrepo-upload", filename="idonotexist.csv")
        except ClientError:
            pass
        except Exception:
            self.fail('unexpected exception raised')
        else:
            self.fail('ClientError not raised')

    # @Test
    def test_bucket_exists_succeeds(self):

        # test
        response = MinioClient().bucket_exists_or_exit("dbrepo-upload")
        self.assertIsNotNone(response)

    # @Test
    def test_bucket_exists_notExists_fails(self):

        # test
        try:
            MinioClient().bucket_exists_or_exit("idnonotexist")
        except FileNotFoundError:
            pass
        except Exception:
            self.fail('unexpected exception raised')
        else:
            self.fail('FileNotFoundError not raised')

    # @Test
    def test_bucket_exists_notExists_fails(self):

        # test
        try:
            MinioClient().bucket_exists_or_exit("idnonotexist")
        except FileNotFoundError:
            pass
        except Exception:
            self.fail('unexpected exception raised')
        else:
            self.fail('FileNotFoundError not raised')


if __name__ == '__main__':
    unittest.main()

#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Mon Jan  9 08:46:04 2023

@author: Martin Weise
"""
import unittest

from botocore.exceptions import ClientError

from clients.s3_client import S3Client


class S3ClientTest(unittest.TestCase):

    # @Test
    def test_upload_file_succeeds(self):

        # test
        response = S3Client().upload_file(filename="testdt01.csv", path="./data/")
        self.assertTrue(response)

    # @Test
    def test_upload_bucket_notFound_fails(self):

        # test
        try:
            S3Client().upload_file(filename="testdt01.csv", path="./data/", bucket="invalidbucket")
        except ConnectionRefusedError:
            pass
        except Exception:
            self.fail('unexpected exception raised')
        else:
            self.fail('ConnectionRefusedError not raised')

    # @Test
    def test_upload_file_notFound_fails(self):

        # test
        try:
            S3Client().upload_file(filename="testdt06.csv", path="./data/")
        except FileNotFoundError:
            pass
        except Exception:
            self.fail('unexpected exception raised')
        else:
            self.fail('FileNotFoundError not raised')

    # @Test
    def test_download_file_succeeds(self):

        # mock
        S3Client().upload_file(filename="testdt01.csv", path="./data/", bucket="dbrepo-upload")

        # test
        S3Client().download_file(filename="testdt01.csv")

    # @Test
    def test_download_file_notFound_fails(self):

        # test
        try:
            S3Client().download_file(filename="testdt01.csv")
        except ClientError:
            pass
        except Exception:
            self.fail('unexpected exception raised')
        else:
            self.fail('ClientError not raised')

    # @Test
    def test_download_bucket_notFound_fails(self):

        # test
        try:
            S3Client().download_file(filename="testdt01.csv", bucket="invalidbucket")
        except ClientError:
            pass
        except Exception:
            self.fail('unexpected exception raised')
        else:
            self.fail('ClientError not raised')

    # @Test
    def test_get_file_succeeds(self):

        # mock
        S3Client().upload_file(filename="testdt01.csv", path="./data/", bucket="dbrepo-upload")

        # test
        response = S3Client().get_file(bucket="dbrepo-upload", filename="testdt01.csv")
        self.assertIsNotNone(response)

    # @Test
    def test_get_file_notFound_fails(self):

        # test
        try:
            S3Client().get_file(bucket="dbrepo-upload", filename="idonotexist.csv")
        except ClientError:
            pass
        except Exception:
            self.fail('unexpected exception raised')
        else:
            self.fail('ClientError not raised')

    # @Test
    def test_bucket_exists_succeeds(self):

        # test
        response = S3Client().bucket_exists_or_exit("dbrepo-upload")
        self.assertIsNotNone(response)

    # @Test
    def test_bucket_exists_notExists_fails(self):

        # test
        try:
            S3Client().bucket_exists_or_exit("idnonotexist")
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
            S3Client().bucket_exists_or_exit("idnonotexist")
        except FileNotFoundError:
            pass
        except Exception:
            self.fail('unexpected exception raised')
        else:
            self.fail('FileNotFoundError not raised')


if __name__ == '__main__':
    unittest.main()

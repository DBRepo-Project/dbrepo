#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Mon Jan  9 08:46:04 2023

@author: Martin Weise
"""
import unittest
import os
import json

from clients.minio_client import MinioClient
from testcontainers.minio import MinioContainer

from determine_pk import determine_pk


def before():
    container = MinioContainer(access_key="minioadmin", secret_key="minioadmin").start()
    endpoint = 'http://' + container.get_container_host_ip() + ':' + container.get_exposed_port(9000)
    os.environ['S3_STORAGE_ENDPOINT'] = endpoint
    client = container.get_client()
    # create buckets
    client.make_bucket('dbrepo-upload')
    client.make_bucket('dbrepo-download')
    return container

class DeterminePrimaryKeyTest(unittest.TestCase):

    # @Test
    def test_determine_pk_largeFileIdFirst_succeeds(self):

        with before() as minio:

            # mock
            MinioClient().upload_file("largefile_idfirst.csv", './data/test_pk/', 'dbrepo-upload')

            # test
            response = determine_pk('largefile_idfirst.csv')
            data = json.loads(response)
            self.assertEqual(1, int(data['id']))

    # @Test
    def test_determine_pk_largeFileIdInBetween_succeeds(self):

        with before() as minio:

            # mock
            MinioClient().upload_file("largefile_idinbtw.csv", './data/test_pk/', 'dbrepo-upload')

            # test
            response = determine_pk('largefile_idinbtw.csv')
            data = json.loads(response)
            self.assertEqual(1, int(data['id']))

    # @Test
    def test_determine_pk_largeFileNoPrimaryKey_fails(self):

        with before() as minio:

            # mock
            MinioClient().upload_file("largefile_no_pk.csv", './data/test_pk/', 'dbrepo-upload')

            # test
            response = determine_pk('largefile_no_pk.csv')
            data = json.loads(response)
            self.assertEqual({}, data)

    # @Test
    def test_determine_pk_largeFileNullInUnique_fails(self):

        with before() as minio:

            # mock
            MinioClient().upload_file("largefile_nullinunique.csv", './data/test_pk/', 'dbrepo-upload')

            # test
            response = determine_pk('largefile_nullinunique.csv')
            data = json.loads(response)
            self.assertFalse('uniquestr' in data)

    # @Test
    def test_determine_pk_smallFileIdFirst_fails(self):

        with before() as minio:

            # mock
            MinioClient().upload_file("smallfile_idfirst.csv", './data/test_pk/', 'dbrepo-upload')

            # test
            response = determine_pk('smallfile_idfirst.csv')
            data = json.loads(response)
            self.assertEqual(1, int(data['id']))

    # @Test
    def test_determine_pk_smallFileIdIntBetween_fails(self):

        with before() as minio:

            # mock
            MinioClient().upload_file("smallfile_idinbtw.csv", './data/test_pk/', 'dbrepo-upload')

            # test
            response = determine_pk('smallfile_idinbtw.csv')
            data = json.loads(response)
            self.assertEqual(1, int(data['id']))

    # @Test
    def test_determine_pk_smallFileNoPrimaryKey_fails(self):

        with before() as minio:

            # mock
            MinioClient().upload_file("smallfile_no_pk.csv", './data/test_pk/', 'dbrepo-upload')

            # test
            response = determine_pk('smallfile_no_pk.csv')
            data = json.loads(response)
            self.assertEqual({}, data)

    # @Test
    def test_determine_pk_smallFileNullInUnique_fails(self):

        with before() as minio:

            # mock
            MinioClient().upload_file("smallfile_nullinunique.csv", './data/test_pk/', 'dbrepo-upload')

            # test
            response = determine_pk('smallfile_nullinunique.csv')
            data = json.loads(response)
            self.assertFalse('uniquestr' in data)


if __name__ == '__main__':
    unittest.main()

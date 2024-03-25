#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Mon Jan  9 08:46:04 2023

@author: Martin Weise
"""
import unittest
import os
import json

from clients.s3_client import S3Client

from determine_pk import determine_pk

class DeterminePrimaryKeyTest(unittest.TestCase):
    # @Test
    def test_determine_pk_largeFileIdFirst_succeeds(self):

        # mock
        S3Client().upload_file("largefile_idfirst.csv", './data/test_pk/', 'dbrepo-upload')

        # test
        response = determine_pk('largefile_idfirst.csv')
        self.assertEqual(1, int(response['id']))

    # @Test
    def test_determine_pk_largeFileIdInBetween_succeeds(self):

        # mock
        S3Client().upload_file("largefile_idinbtw.csv", './data/test_pk/', 'dbrepo-upload')

        # test
        response = determine_pk('largefile_idinbtw.csv')
        self.assertEqual(1, int(response['id']))

    # @Test
    def test_determine_pk_largeFileNoPrimaryKey_fails(self):

        # mock
        S3Client().upload_file("largefile_no_pk.csv", './data/test_pk/', 'dbrepo-upload')

        # test
        response = determine_pk('largefile_no_pk.csv')
        self.assertEqual({}, response)

    # @Test
    def test_determine_pk_largeFileNullInUnique_fails(self):

        # mock
        S3Client().upload_file("largefile_nullinunique.csv", './data/test_pk/', 'dbrepo-upload')

        # test
        response = determine_pk('largefile_nullinunique.csv')
        self.assertFalse('uniquestr' in response)

    # @Test
    def test_determine_pk_smallFileIdFirst_fails(self):

        # mock
        S3Client().upload_file("smallfile_idfirst.csv", './data/test_pk/', 'dbrepo-upload')

        # test
        response = determine_pk('smallfile_idfirst.csv')
        self.assertEqual(1, int(response['id']))

    # @Test
    def test_determine_pk_smallFileIdIntBetween_fails(self):

        # mock
        S3Client().upload_file("smallfile_idinbtw.csv", './data/test_pk/', 'dbrepo-upload')

        # test
        response = determine_pk('smallfile_idinbtw.csv')
        self.assertEqual(1, int(response['id']))

    # @Test
    def test_determine_pk_smallFileNoPrimaryKey_fails(self):

        # mock
        S3Client().upload_file("smallfile_no_pk.csv", './data/test_pk/', 'dbrepo-upload')

        # test
        response = determine_pk('smallfile_no_pk.csv')
        self.assertEqual({}, response)

    # @Test
    def test_determine_pk_smallFileNullInUnique_fails(self):

        # mock
        S3Client().upload_file("smallfile_nullinunique.csv", './data/test_pk/', 'dbrepo-upload')

        # test
        response = determine_pk('smallfile_nullinunique.csv')
        self.assertFalse('uniquestr' in response)


if __name__ == '__main__':
    unittest.main()

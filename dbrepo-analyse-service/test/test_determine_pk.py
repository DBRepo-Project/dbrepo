#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Mon Jan  9 08:46:04 2023

@author: Martin Weise
"""
import unittest
import sys
import json
from determine_pk import determine_pk

sys.path.append("..")


class DeterminePrimaryKeyTest(unittest.TestCase):

    # @Test
    def test_determine_pk_largeFileIdFirst_succeeds(self):
        # test
        response = determine_pk('data/test_pk/largefile_idfirst.csv')
        data = json.loads(response)
        self.assertEqual(1, int(data['id']))

    # @Test
    def test_determine_pk_largeFileIdInBetween_succeeds(self):
        # test
        response = determine_pk('data/test_pk/largefile_idinbtw.csv')
        data = json.loads(response)
        self.assertEqual(1, int(data['id']))

    # @Test
    def test_determine_pk_largeFileNoPrimaryKey_fails(self):
        # test
        response = determine_pk('data/test_pk/largefile_no_pk.csv')
        data = json.loads(response)
        self.assertEqual({}, data)

    # @Test
    def test_determine_pk_largeFileNullInUnique_fails(self):
        # test
        response = determine_pk('data/test_pk/largefile_nullinunique.csv')
        data = json.loads(response)
        self.assertFalse('uniquestr' in data)

    # @Test
    def test_determine_pk_smallFileIdFirst_fails(self):
        # test
        response = determine_pk('data/test_pk/smallfile_idfirst.csv')
        data = json.loads(response)
        self.assertEqual(1, int(data['id']))

    # @Test
    def test_determine_pk_smallFileIdIntBetween_fails(self):
        # test
        response = determine_pk('data/test_pk/smallfile_idinbtw.csv')
        data = json.loads(response)
        self.assertEqual(1, int(data['id']))

    # @Test
    def test_determine_pk_smallFileNoPrimaryKey_fails(self):
        # test
        response = determine_pk('data/test_pk/smallfile_no_pk.csv')
        data = json.loads(response)
        self.assertEqual({}, data)

    # @Test
    def test_determine_pk_smallFileNullInUnique_fails(self):
        # test
        response = determine_pk('data/test_pk/smallfile_nullinunique.csv')
        data = json.loads(response)
        self.assertFalse('uniquestr' in data)


if __name__ == '__main__':
    unittest.main()

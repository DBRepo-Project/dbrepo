#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Sun Dec  5 19:41:04 2021

@author: Martin Weise
"""
import unittest
import sys
from app import app

sys.path.append("..")


class AppUnitTest(unittest.TestCase):

    def test_save_concept_uri_and_name_null_fails(self):
        with app.test_client() as client:
            payload = {'uri': None, 'name': None}

            # test
            response = client.post('/api/semantics/concept', json=payload, content_type='application/json')
            self.assertEqual(400, response.status_code)

    def test_save_concept_uri_null_fails(self):
        with app.test_client() as client:
            payload = {'uri': None, 'name': 'second'}

            # test
            response = client.post('/api/semantics/concept', json=payload, content_type='application/json')
            self.assertEqual(400, response.status_code)

    def test_save_concept_name_null_fails(self):
        with app.test_client() as client:
            payload = {'uri': 'http://www.ontology-of-units-of-measure.org/resource/om-2/second-Time', 'name': None}

            # test
            response = client.post('/api/semantics/concept', json=payload, content_type='application/json')
            self.assertEqual(400, response.status_code)

    def test_save_unit_uri_and_name_null_fails(self):
        with app.test_client() as client:
            payload = {'uri': None, 'name': None}

            # test
            response = client.post('/api/semantics/concept', json=payload, content_type='application/json')
            self.assertEqual(400, response.status_code)

    def test_save_unit_uri_null_fails(self):
        with app.test_client() as client:
            payload = {'uri': None, 'name': 'bell tree'}

            # test
            response = client.post('/api/semantics/concept', json=payload, content_type='application/json')
            self.assertEqual(400, response.status_code)

    def test_save_unit_name_null_fails(self):
        with app.test_client() as client:
            payload = {'uri': 'http://www.wikidata.org/entity/Q12273079', 'name': None}

            # test
            response = client.post('/api/semantics/concept', json=payload, content_type='application/json')
            self.assertEqual(400, response.status_code)


if __name__ == '__main__':
    unittest.main()

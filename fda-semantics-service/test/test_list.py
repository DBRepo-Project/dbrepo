#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Sun Dec  5 19:41:04 2021

@author: Martin Weise
"""
import unittest
import sys
from list import List

sys.path.append("..")

list = List(offline=True)


class ListUnitTest(unittest.TestCase):

    def test_list_units_succeeds(self):
        exp = ['metre', 'ampere per square metre', 'square metre', 'ampere per metre', 'mole per cubic metre',
               'candela per square metre', 'cubic metre', 'kilometre per second per megaparsec',
               'reciprocal metre', 'metre per second squared']

        # test
        response = list.list_units('metre')
        body = [unit["name"] for unit in response]
        self.assertEqual(exp, body)

    def test_list_units_fails(self):
        exp = []

        # test
        response = list.list_units('smurf')
        body = [unit["name"] for unit in response]
        self.assertEqual(exp, body)

    def test_get_unit_uri_succeeds(self):
        exp = {"uri": "http://www.ontology-of-units-of-measure.org/resource/om-2/second-Time"}

        # test
        response = list.get_unit_uri("second")
        self.assertEqual(exp, response)

    def test_get_unit_uri_hasBraces_succeeds(self):
        exp = {"uri": "http://www.ontology-of-units-of-measure.org/resource/om-2/minute-HourAngle"}

        # test
        response = list.get_unit_uri("minute (hour angle)")
        self.assertEqual(exp, response)

    def test_get_concept_uri_succeeds(self):
        exp = {"uri": "https://www.wikidata.org/wiki/Q149892"}

        # test
        response = list.get_concept_uri("Canis")
        self.assertEqual(exp, response)

    def test_get_concept_uri_hasSpaces_succeeds(self):
        exp = {"uri": "https://www.wikidata.org/wiki/Q225"}

        # test
        response = list.get_concept_uri("Bosnia and Herzegovina")
        self.assertEqual(exp, response)


if __name__ == '__main__':
    unittest.main()

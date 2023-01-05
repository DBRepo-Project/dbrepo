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
        exp = ['metre', 'metre of mercury'].sort()

        # test
        response = list.list_units('metre')
        body = [unit["name"] for unit in response].sort()
        self.assertEqual(exp, body)

    def test_list_concepts_succeeds(self):
        exp = ['volumetric flask', 'vacuum flask cooker'].sort()

        # test
        response = list.list_concepts('flask')
        body = [unit["name"] for unit in response].sort()
        self.assertEqual(exp, body)

    def test_list_units_fails(self):
        exp = []

        # test
        response = list.list_units('time')
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

    def test_get_concept_uri_hasSpaces_succeeds(self):
        exp = {"uri": "http://www.wikidata.org/entity/Q998319"}

        # test
        response = list.get_concept_uri("flight recorder")
        self.assertEqual(exp, response)

    def test_get_concept_uri_succeeds(self):
        exp = {"uri": "http://www.wikidata.org/entity/Q235783"}

        # test
        response = list.get_concept_uri("flashlight")
        self.assertEqual(exp, response)


if __name__ == '__main__':
    unittest.main()

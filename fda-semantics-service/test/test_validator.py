#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Sun Dec  5 19:41:04 2021

@author: Cornelia Michlits
"""
import unittest
import sys
from validate import validator, stringmapper

sys.path.append("..")

exp_out_True = True
exp_out_False = False


class TestStringMethods(unittest.TestCase):

    # metre is SI Unit
    def test_validator_true(self):
        self.assertEqual(exp_out_True, validator('metre'))

    # diameter is measure, but no SI Unit
    def test_validator_no_SI_Unit(self):
        self.assertEqual(exp_out_False, validator('diameter'))

    # misspelling
    def test_validator_misspelling(self):
        self.assertEqual(exp_out_False, validator('metreee'))

    # Divided unit
    def test_validator_dividedunit(self):
        self.assertEqual(exp_out_True, validator(stringmapper('mole per metre')))

    # Prefixed unit
    def test_validator_prefixedunit(self):
        self.assertEqual(exp_out_True, validator(stringmapper('zettamole')))


if __name__ == '__main__':
    unittest.main()

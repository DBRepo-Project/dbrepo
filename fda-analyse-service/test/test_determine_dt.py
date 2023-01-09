#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Mon Jan  9 08:46:04 2023

@author: Martin Weise
"""
import unittest
import sys
import json
from _csv import Error

from determine_dt import determine_datatypes

sys.path.append("..")


class DetermineDatatypesTest(unittest.TestCase):
    # @Test
    def test_determine_datatypes_succeeds(self):
        exp = {
            'columns': {
                'int': 'number',
                'float': 'decimal',
                'string': 'text',
                'boolean': 'boolean',
                'date': 'date',
                'time': 'timestamp',
                'enum': 'text'  # currently not used
            },
            'separator': ','
        }

        # test
        response = determine_datatypes("data/test_dt/datatypes.csv")
        self.assertEqual(json.dumps(exp), response)

    # @Test
    def test_determine_datatypes_fileDoesNotExist_fails(self):

        # test
        try:
            response = determine_datatypes("data/test_dt/i_do_not_exist.csv")
        except OSError as e:
            pass
        except Exception:
            self.fail('unexpected exception raised')
        else:
            self.fail('ExpectedException not raised')

    # @Test
    def test_determine_datatypes_fileEmpty_fails(self):

        # test
        try:
            response = determine_datatypes("data/test_dt/empty.csv")
        except Error as e:
            pass
        except Exception:
            self.fail('unexpected exception raised')
        else:
            self.fail('ExpectedException not raised')

    # @Test
    def test_determine_datatypes_separatorSemicolon_succeeds(self):

        # test
        response = determine_datatypes("data/test_dt/separator.csv")
        data = json.loads(response)
        self.assertEqual(data['separator'], ';')


if __name__ == '__main__':
    unittest.main()

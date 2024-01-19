#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Mon Jan  9 08:46:04 2023

@author: Martin Weise
"""
import unittest
import json


from clients.s3_client import S3Client
from botocore.exceptions import ClientError
from determine_dt import determine_datatypes


class DetermineDatatypesTest(unittest.TestCase):
    # @Test
    def test_determine_datatypesDateTime_succeeds(self):
        exp = {
            "columns": {
                "Datum": "timestamp",
                "Standort": "varchar",
                "Parameter": "varchar",
                "Intervall": "varchar",
                "Einheit": "varchar",
                "Wert": "decimal",
                "Status": "varchar",
            },
            "separator": ",",
        }

        # mock
        S3Client().upload_file("datetime.csv", './data/test_dt/', 'dbrepo-upload')

        # test
        response = determine_datatypes(filename="datetime.csv", separator=",")
        self.assertEqual(json.dumps(exp), response)

    # @Test
    def test_determine_datatypesDateTimeWithTimezone_succeeds(self):
        exp = {
            "columns": {
                "Datum": "varchar",
                "Standort": "varchar",
                "Parameter": "varchar",
                "Intervall": "varchar",
                "Einheit": "varchar",
                "Wert": "decimal",
                "Status": "varchar",
            },
            "separator": ",",
        }

        # mock
        S3Client().upload_file("datetime_tz.csv", './data/test_dt/', 'dbrepo-upload')

        # test
        response = determine_datatypes(filename="datetime_tz.csv", separator=",")
        self.assertEqual(json.dumps(exp), response)

    # @Test
    def test_determine_datatypesDateTimeWithT_succeeds(self):
        exp = {
            "columns": {
                "Datum": "timestamp",
                "Standort": "varchar",
                "Parameter": "varchar",
                "Intervall": "varchar",
                "Einheit": "varchar",
                "Wert": "decimal",
                "Status": "varchar",
            },
            "separator": ",",
        }

        # mock
        S3Client().upload_file("datetime_t.csv", './data/test_dt/', 'dbrepo-upload')

        # test
        response = determine_datatypes(filename="datetime_t.csv", separator=",")
        self.assertEqual(json.dumps(exp), response)

    # @Test
    def test_determine_datatypes_succeeds(self):
        exp = {
            "columns": {
                "int": "bigint",
                "float": "decimal",
                "string": "varchar",
                "boolean": "bool",
                "date": "date",
                "time": "timestamp",
                "enum": "varchar",  # currently not used
            },
            "separator": ",",
        }

        # mock
        S3Client().upload_file("datatypes.csv", './data/test_dt/', 'dbrepo-upload')

        # test
        response = determine_datatypes(filename="datatypes.csv", separator=",")
        self.assertEqual(json.dumps(exp), response)

    # @Test
    def test_determine_datatypes_fileDoesNotExist_fails(self):
        # test
        try:
            response = determine_datatypes("i_do_not_exist.csv")
        except ClientError as e:
            pass  # s3.head operation
        except Exception:
            self.fail("unexpected exception raised")
        else:
            self.fail("ExpectedException not raised")

    # @Test
    def test_determine_datatypes_fileEmpty_succeeds(self):
        # mock
        S3Client().upload_file("empty.csv", './data/test_dt/', 'dbrepo-upload')

        # test
        response = determine_datatypes("empty.csv")
        data = json.loads(response)
        self.assertEqual(data["columns"], [])
        self.assertEqual(data["separator"], ",")

    # @Test
    def test_determine_datatypes_separatorSemicolon_succeeds(self):
        # mock
        S3Client().upload_file("separator.csv", './data/test_dt/', 'dbrepo-upload')

        # test
        response = determine_datatypes(filename="separator.csv", separator=";")
        data = json.loads(response)
        self.assertEqual(data["separator"], ";")

    # @Test
    def test_determine_datatypes_separatorGuess_succeeds(self):
        # mock
        S3Client().upload_file("separator.csv", './data/test_dt/', 'dbrepo-upload')

        # test
        response = determine_datatypes(filename="separator.csv")
        data = json.loads(response)
        self.assertEqual(data["separator"], ";")

    # @Test
    def test_determine_datatypes_separatorGuessLargeDataset_succeeds(self):
        # mock
        S3Client().upload_file("large.csv", './data/test_dt/', 'dbrepo-upload')

        # test
        response = determine_datatypes(filename="large.csv")
        data = json.loads(response)
        self.assertEqual(data["separator"], ",")


if __name__ == "__main__":
    unittest.main()

import unittest

from api.dto import AnalysisDto
from clients.s3_client import S3Client
from botocore.exceptions import ClientError
from determine_dt import determine_datatypes


class DetermineDatatypesTest(unittest.TestCase):

    def test_determine_datatypesDateTime_succeeds(self):
        exp = AnalysisDto(separator=",", line_termination="\n", columns={
            "Datum": {
                "type": "timestamp",
                "null_allowed": False,
            },
            "Standort": {
                "type": "varchar",
                "size": 255,
                "null_allowed": False,
            },
            "Parameter": {
                "type": "varchar",
                "size": 255,
                "null_allowed": False,
            },
            "Intervall": {
                "type": "varchar",
                "size": 255,
                "null_allowed": False,
            },
            "Einheit": {
                "type": "varchar",
                "size": 255,
                "null_allowed": False,
            },
            "Wert": {
                "type": "decimal",
                "size": 40,
                "d": 20,
                "null_allowed": False,
            },
            "Status": {
                "type": "varchar",
                "size": 255,
                "null_allowed": False,
            },
        })


        # mock
        S3Client().upload_file("datetime.csv", './data/test_dt/', 'dbrepo')

        # test
        response = determine_datatypes(filename="datetime.csv", separator=",")
        self.assertEqual(exp, response)

    def test_determine_datatypesDateTimeWithTimezone_succeeds(self):
        exp = AnalysisDto(separator=",", line_termination="\n", columns={
            "Datum": {
                "type": "timestamp",
                "null_allowed": False,
            },
            "Standort": {
                "type": "varchar",
                "size": 255,
                "null_allowed": False,
            },
            "Parameter": {
                "type": "varchar",
                "size": 255,
                "null_allowed": False,
            },
            "Intervall": {
                "type": "varchar",
                "size": 255,
                "null_allowed": False,
            },
            "Einheit": {
                "type": "varchar",
                "size": 255,
                "null_allowed": False,
            },
            "Wert": {
                "type": "decimal",
                "size": 40,
                "d": 20,
                "null_allowed": False,
            },
            "Status": {
                "type": "varchar",
                "size": 255,
                "null_allowed": False,
            },
        })

        # mock
        S3Client().upload_file("datetime_tz.csv", './data/test_dt/', 'dbrepo')

        # test
        response = determine_datatypes(filename="datetime_tz.csv", separator=",")
        self.assertEqual(exp, response)

    def test_determine_datatypesDateTimeWithT_succeeds(self):
        exp = AnalysisDto(separator=",", line_termination="\n", columns={
            "Datum": {
                "type": "timestamp",
                "null_allowed": False,
            },
            "Standort": {
                "type": "varchar",
                "size": 255,
                "null_allowed": False,
            },
            "Parameter": {
                "type": "varchar",
                "size": 255,
                "null_allowed": False,
            },
            "Intervall": {
                "type": "varchar",
                "size": 255,
                "null_allowed": False,
            },
            "Einheit": {
                "type": "varchar",
                "size": 255,
                "null_allowed": False,
            },
            "Wert": {
                "type": "decimal",
                "size": 40,
                "d": 20,
                "null_allowed": False,
            },
            "Status": {
                "type": "varchar",
                "size": 255,
                "null_allowed": False,
            },
        })

        # mock
        S3Client().upload_file("datetime_t.csv", './data/test_dt/', 'dbrepo')

        # test
        response = determine_datatypes(filename="datetime_t.csv", separator=",")
        self.assertEqual(exp, response)

    def test_determine_datatypes_succeeds(self):
        exp = AnalysisDto(separator=",", line_termination="\n", columns={
            "int": {
                "type": "bigint",
                "null_allowed": False,
            },
            "float": {
                "type": "decimal",
                "size": 40,
                "d": 20,
                "null_allowed": False,
            },
            "string": {
                "type": "varchar",
                "size": 255,
                "null_allowed": False,
            },
            "boolean": {
                "type": "bool",
                "null_allowed": False,
            },
            "bool": {
                "type": "bool",
                "null_allowed": False,
            },
            "date": {
                "type": "date",
                "null_allowed": False,
            },
            "time": {
                "type": "timestamp",
                "null_allowed": False,
            },
            "enum": {  # currently not used
                "type": "varchar",
                "size": 255,
                "null_allowed": False,
            },
        })

        # mock
        S3Client().upload_file("datatypes.csv", './data/test_dt/', 'dbrepo')

        # test
        response = determine_datatypes(filename="datatypes.csv", separator=",")
        self.assertEqual(exp, response)

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

    def test_determine_datatypes_fileEmpty_succeeds(self):

        # mock
        S3Client().upload_file("empty.csv", './data/test_dt/', 'dbrepo')

        # test
        response = determine_datatypes("empty.csv")
        self.assertEqual({}, response.columns)
        self.assertEqual(",", response.separator)

    def test_determine_datatypes_separatorSemicolon_succeeds(self):

        # mock
        S3Client().upload_file("separator.csv", './data/test_dt/', 'dbrepo')

        # test
        response = determine_datatypes(filename="separator.csv", separator=";")
        self.assertEqual(";", response.separator)

    def test_determine_datatypes_separatorGuess_succeeds(self):

        # mock
        S3Client().upload_file("separator.csv", './data/test_dt/', 'dbrepo')

        # test
        response = determine_datatypes(filename="separator.csv")
        self.assertEqual(";", response.separator)

    def test_determine_datatypes_separatorGuessText_succeeds(self):
        exp = AnalysisDto(separator=";", line_termination="\n", columns={
            "id": {
                "type": "bigint",
                "null_allowed": False
            },
            "author": {
                "type": "varchar",
                "size": 255,
                "null_allowed": False
            },
            "abstract": {
                "type": "text",
                "null_allowed": False
            },
        })

        # mock
        S3Client().upload_file("novel.csv", './data/test_dt/', 'dbrepo')

        # test
        response = determine_datatypes(filename="novel.csv", separator=";")
        self.assertEqual(exp, response)


if __name__ == "__main__":
    unittest.main()

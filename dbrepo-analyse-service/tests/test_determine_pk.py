import unittest

from dbrepo.core.client.storage import StorageServiceClient
from flask import current_app

from determine_pk import determine_pk


class DeterminePrimaryKeyTest(unittest.TestCase):
    def test_determine_pk_largeFileIdFirst_succeeds(self):
        # mock
        with current_app.app_context():
            StorageServiceClient(current_app.config['S3_ENDPOINT'], current_app.config['S3_ACCESS_KEY_ID'],
                                 current_app.config['S3_SECRET_ACCESS_KEY']).upload_file("largefile_idfirst.csv",
                                                                                         './data/test_pk/', 'dbrepo')

        # test
        response = determine_pk('largefile_idfirst.csv')
        self.assertEqual(1, int(response['id']))

    def test_determine_pk_largeFileIdInBetween_succeeds(self):
        # mock
        with current_app.app_context():
            StorageServiceClient(current_app.config['S3_ENDPOINT'], current_app.config['S3_ACCESS_KEY_ID'],
                                 current_app.config['S3_SECRET_ACCESS_KEY']).upload_file("largefile_idinbtw.csv",
                                                                                         './data/test_pk/', 'dbrepo')

        # test
        response = determine_pk('largefile_idinbtw.csv')
        self.assertEqual(1, int(response['id']))

    # @Test
    def test_determine_pk_largeFileNoPrimaryKey_fails(self):
        # mock
        with current_app.app_context():
            StorageServiceClient(current_app.config['S3_ENDPOINT'], current_app.config['S3_ACCESS_KEY_ID'],
                                 current_app.config['S3_SECRET_ACCESS_KEY']).upload_file("largefile_no_pk.csv",
                                                                                         './data/test_pk/', 'dbrepo')

        # test
        response = determine_pk('largefile_no_pk.csv')
        self.assertEqual({}, response)

    def test_determine_pk_largeFileNullInUnique_fails(self):
        # mock
        with current_app.app_context():
            StorageServiceClient(current_app.config['S3_ENDPOINT'], current_app.config['S3_ACCESS_KEY_ID'],
                                 current_app.config['S3_SECRET_ACCESS_KEY']).upload_file("largefile_nullinunique.csv",
                                                                                         './data/test_pk/', 'dbrepo')

        # test
        response = determine_pk('largefile_nullinunique.csv')
        self.assertFalse('uniquestr' in response)

    def test_determine_pk_smallFileIdFirst_fails(self):
        # mock
        with current_app.app_context():
            StorageServiceClient(current_app.config['S3_ENDPOINT'], current_app.config['S3_ACCESS_KEY_ID'],
                                 current_app.config['S3_SECRET_ACCESS_KEY']).upload_file("smallfile_idfirst.csv",
                                                                                         './data/test_pk/', 'dbrepo')

        # test
        response = determine_pk('smallfile_idfirst.csv')
        self.assertEqual(1, int(response['id']))

    def test_determine_pk_smallFileIdIntBetween_fails(self):
        # mock
        with current_app.app_context():
            StorageServiceClient(current_app.config['S3_ENDPOINT'], current_app.config['S3_ACCESS_KEY_ID'],
                                 current_app.config['S3_SECRET_ACCESS_KEY']).upload_file("smallfile_idinbtw.csv",
                                                                                         './data/test_pk/', 'dbrepo')

        # test
        response = determine_pk('smallfile_idinbtw.csv')
        self.assertEqual(1, int(response['id']))

    def test_determine_pk_smallFileNoPrimaryKey_fails(self):
        # mock
        with current_app.app_context():
            StorageServiceClient(current_app.config['S3_ENDPOINT'], current_app.config['S3_ACCESS_KEY_ID'],
                                 current_app.config['S3_SECRET_ACCESS_KEY']).upload_file("smallfile_no_pk.csv",
                                                                                         './data/test_pk/', 'dbrepo')

        # test
        response = determine_pk('smallfile_no_pk.csv')
        self.assertEqual({}, response)

    def test_determine_pk_smallFileNullInUnique_fails(self):
        # mock
        with current_app.app_context():
            StorageServiceClient(current_app.config['S3_ENDPOINT'], current_app.config['S3_ACCESS_KEY_ID'],
                                 current_app.config['S3_SECRET_ACCESS_KEY']).upload_file("smallfile_nullinunique.csv",
                                                                                         './data/test_pk/', 'dbrepo')

        # test
        response = determine_pk('smallfile_nullinunique.csv')
        self.assertFalse('uniquestr' in response)


if __name__ == '__main__':
    unittest.main()

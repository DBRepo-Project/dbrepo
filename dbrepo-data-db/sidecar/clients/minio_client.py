import os
import boto3
import logging
import sys

from botocore.exceptions import ClientError


class MinioClient:

    def __init__(self):
        endpoint_url = os.getenv('S3_STORAGE_ENDPOINT', 'http://localhost:9000')
        aws_access_key_id = os.getenv('S3_ACCESS_KEY_ID', 'minioadmin')
        aws_secret_access_key = os.getenv('S3_SECRET_ACCESS_KEY', 'minioadmin')
        logging.info("retrieve file from S3, endpoint_url=%s, aws_access_key_id=%s, aws_secret_access_key=(hidden)",
                     endpoint_url, aws_access_key_id)
        self.client = boto3.client(service_name='s3', endpoint_url=endpoint_url, aws_access_key_id=aws_access_key_id,
                                   aws_secret_access_key=aws_secret_access_key)
        self.bucket_exists_or_exit("dbrepo-upload")
        self.bucket_exists_or_exit("dbrepo-download")

    def upload_file(self, filename) -> bool:
        """
        Uploads a file to the blob storage.
        Follows the official API https://boto3.amazonaws.com/v1/documentation/api/latest/guide/s3-uploading-files.html.
        :param filename: The filename.
        :return: True if the file was uploaded.
        """
        filepath = os.path.join("/tmp/", filename)
        try:
            self.client.upload_file(filepath, "dbrepo-download", filename)
            logging.info("Uploaded .csv %s with key %s", filepath, filename)
            return True
        except ClientError as e:
            logging.error(e)
            return False

    def download_file(self, filename) -> bool:
        """
        Downloads a file from the blob storage.
        Follows the official API https://boto3.amazonaws.com/v1/documentation/api/latest/guide/s3-example-download-file.html
        :param filename: The filename.
        :return: True if the file was downloaded and saved.
        """
        self.file_exists("dbrepo-upload", filename)
        filepath = os.path.join("/tmp/", filename)
        try:
            self.client.download_file("dbrepo-upload", filename, filepath)
            logging.info("Downloaded .csv with key %s into %s", filename, filepath)
            return True
        except ClientError as e:
            logging.error(e)
            return False

    def file_exists(self, bucket, filename):
        try:
            self.client.head_object(Bucket=bucket, Key=filename)
        except ClientError as e:
            if e.response["Error"]["Code"] == "404":
                logging.error("Failed to find key %s in bucket %s", filename, bucket)
            else:
                logging.error("Unexpected error when finding key %s in bucket %s: %s", filename, bucket,
                              e.response["Error"]["Code"])
            raise e

    def bucket_exists_or_exit(self, bucket):
        try:
            self.client.head_bucket(Bucket=bucket)
        except ClientError as e:
            if e.response["Error"]["Code"] == "404":
                logging.error("Failed to find bucket %s", bucket)
            else:
                logging.error("Unexpected error when finding bucket %s: %s", bucket,
                              e.response["Error"]["Code"])
            sys.exit(1)

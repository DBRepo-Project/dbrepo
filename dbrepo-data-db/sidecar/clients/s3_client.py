import logging
import os
import sys

import boto3
from botocore.exceptions import ClientError


class S3Client:

    def __init__(self):
        self.endpoint_url = os.getenv('S3_ENDPOINT', 'http://localhost:9000')
        self.access_access_key_id = os.getenv('S3_ACCESS_KEY_ID', 'seaweedfsadmin')
        self.secret_secret_access_key = os.getenv('S3_SECRET_ACCESS_KEY', 'seaweedfsadmin')
        self.region = os.getenv('S3_REGION', 'default')
        self.bucket = os.getenv('S3_BUCKET', 'dbrepo')
        logging.info(
            f"retrieve file from S3, endpoint_url={self.endpoint_url}, access_access_key_id={self.access_access_key_id}, secret_secret_access_key=(hidden)")
        self.client = boto3.client(service_name='s3', endpoint_url=self.endpoint_url,
                                   aws_access_key_id=self.access_access_key_id,
                                   aws_secret_access_key=self.secret_secret_access_key, region_name=self.region)
        self.bucket_exists_or_exit(self.bucket)

    def upload_file(self, filename) -> bool:
        """
        Uploads a file to the blob storage.
        Follows the official API https://boto3.amazonaws.com/v1/documentation/api/latest/guide/s3-uploading-files.html.
        :param filename: The filename.
        :return: True if the file was uploaded.
        """
        filepath = os.path.join("/shared/", filename)
        try:
            self.client.upload_file(filepath, self.bucket, filename)
            logging.info(f"Uploaded .csv {filepath} with key {filename} into bucket {self.bucket}")
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
        self.file_exists(self.bucket, filename)
        dst = f"/shared/{filename}"
        try:
            self.client.download_file(self.bucket, filename, dst)
            logging.info(f"Downloaded .csv with key {filename} into {dst} from bucket {self.bucket}")
            return True
        except ClientError as e:
            logging.error(e)
            return False

    def file_exists(self, bucket, filename):
        try:
            self.client.head_object(Bucket=bucket, Key=filename)
            logging.debug(f"file with name {filename} exists in bucket with name {bucket}")
        except ClientError as e:
            if e.response["Error"]["Code"] == "404":
                logging.error(f"Failed to find key {filename} in bucket {bucket}")
            else:
                logging.error(
                    f"Unexpected error when finding key {filename} in bucket {bucket}: {e.response['Error']['Code']}")
            raise e

    def bucket_exists_or_exit(self, bucket):
        try:
            self.client.head_bucket(Bucket=bucket)
            logging.debug(f"bucket {bucket} exists.")
        except ClientError as e:
            if e.response["Error"]["Code"] == "404":
                logging.error(f"Failed to find bucket {bucket}")
            else:
                logging.error(f"Unexpected error when finding bucket {bucket}: {e.response['Error']['Code']}")
            sys.exit(1)

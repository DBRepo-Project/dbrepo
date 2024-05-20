import os
import boto3
import logging
import sys

from flask import current_app
from botocore.exceptions import ClientError


class S3Client:

    def __init__(self):
        endpoint_url = current_app.config['S3_ENDPOINT']
        aws_access_key_id = current_app.config['S3_ACCESS_KEY_ID']
        aws_secret_access_key = current_app.config['S3_SECRET_ACCESS_KEY']
        logging.info(
            f"retrieve file from S3, endpoint_url={endpoint_url}, aws_access_key_id={aws_access_key_id}, aws_secret_access_key=(hidden)")
        self.client = boto3.client(service_name='s3', endpoint_url=endpoint_url, aws_access_key_id=aws_access_key_id,
                                   aws_secret_access_key=aws_secret_access_key)
        self.bucket_exists_or_exit(current_app.config['S3_IMPORT_BUCKET'])
        self.bucket_exists_or_exit(current_app.config['S3_EXPORT_BUCKET'])

    def upload_file(self, filename, path, bucket) -> bool:
        """
        Uploads a file to the blob storage.
        Follows the official API https://boto3.amazonaws.com/v1/documentation/api/latest/guide/s3-uploading-files.html.
        :param filename: The filename.
        :param path: The path.
        :param bucket: The bucket.
        :return: True if the file was uploaded.
        """
        filepath = os.path.join(path, filename)
        try:
            self.client.upload_file(filepath, bucket, filename)
            logging.info(f"Uploaded .csv {filepath} with key {filename} into bucket {bucket}")
            return True
        except ClientError as e:
            logging.error(e)
            return False

    def download_file(self, filename, path, bucket) -> bool:
        """
        Downloads a file from the blob storage.
        Follows the official API https://boto3.amazonaws.com/v1/documentation/api/latest/guide/s3-example-download-file.html
        :param filename: The filename.
        :param path: The path.
        :param bucket: The bucket.
        :return: True if the file was downloaded and saved.
        """
        self.file_exists(bucket, filename)
        filepath = os.path.join(path, filename)
        try:
            self.client.download_file(bucket, filename, filepath)
            logging.info(f"Downloaded .csv with key {filename} into {filepath} from bucket {bucket}")
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

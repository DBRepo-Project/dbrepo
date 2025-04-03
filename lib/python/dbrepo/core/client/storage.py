import os
import boto3
import logging

from flask import current_app
from boto3.exceptions import S3UploadFailedError
from botocore.exceptions import ClientError


class StorageServiceClient:

    def __init__(self, endpoint: str, access_key_id: str, secret_access_key: str):
        endpoint = endpoint
        aws_access_key_id = access_key_id
        aws_secret_access_key = secret_access_key
        logging.info(
            f"retrieve file from S3, endpoint={current_app.config['S3_PROTO']}://{endpoint}, aws_access_key_id={aws_access_key_id}, aws_secret_access_key=(hidden)")
        self.client = boto3.client(service_name='s3', endpoint_url=f"{current_app.config['S3_PROTO']}://{endpoint}",
                                   aws_access_key_id=aws_access_key_id,
                                   aws_secret_access_key=aws_secret_access_key)
        self.bucket_exists_or_exit(current_app.config['S3_BUCKET'])

    def upload_file(self, filename: str, path: str = "/tmp", bucket: str = "dbrepo") -> bool:
        """
        Uploads a file to the blob storage.
        Follows the official API https://boto3.amazonaws.com/v1/documentation/api/latest/guide/s3-uploading-files.html.
        :param bucket: The bucket to upload the file into.
        :param path: The path the file is located.
        :param filename: The filename.
        :return: True if the file was uploaded.
        """
        filepath = os.path.join(path, filename)
        if os.path.isfile(filepath):
            logging.info(f'Found .csv at {filepath}')
        else:
            logging.error(f'Failed to find .csv at {filepath}')
            raise FileNotFoundError(f'Failed to find .csv at {filepath}')
        try:
            self.client.upload_file(filepath, bucket, filename)
            logging.info(f"Uploaded .csv {filepath} with key {filename}")
            return True
        except (ClientError, S3UploadFailedError) as e:
            logging.warning(f"Failed to upload file with key {filename}")
            raise ConnectionRefusedError(f"Failed to upload file with key {filename}", e)

    def download_file(self, filename: str, path: str = "/tmp", bucket: str = "dbrepo"):
        """
        Downloads a file from the blob storage.
        Follows the official API https://boto3.amazonaws.com/v1/documentation/api/latest/guide/s3-example-download-file.html
        :param filename: The filename.
        :param path: The path the file is located.
        :param bucket: The bucket to download the file from.
        """
        self.file_exists(bucket, filename)
        filepath = os.path.join(path, filename)
        self.client.download_file(bucket, filename, filepath)
        logging.info(f"Downloaded .csv with key {filename} into {filepath}")

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

    def get_file(self, bucket, filename):
        try:
            return self.client.get_object(Bucket=bucket, Key=filename)
        except ClientError as e:
            if e.response["Error"]["Code"] == "404":
                logging.error("Failed to get file with key %s in bucket %s", filename, bucket)
            else:
                logging.error("Unexpected error when get file with key %s in bucket %s: %s", filename, bucket,
                              e.response["Error"]["Code"])
            raise e

    def bucket_exists_or_exit(self, bucket):
        try:
            return self.client.head_bucket(Bucket=bucket)
        except ClientError as e:
            if e.response["Error"]["Code"] == "404":
                raise FileNotFoundError(f"Failed to find bucket: {bucket}")
            raise ConnectionError(f"Unexpected error when finding bucket {bucket}: %s", e.response["Error"]["Code"])

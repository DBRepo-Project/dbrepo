import os
import boto3
import logging
import sys

from botocore.exceptions import ClientError


class S3Client:

    def __init__(self):
        endpoint_url = os.getenv('S3_STORAGE_ENDPOINT', 'http://localhost:9000')
        aws_access_key_id = os.getenv('S3_ACCESS_KEY_ID', 'seaweedfsadmin')
        aws_secret_access_key = os.getenv('S3_SECRET_ACCESS_KEY', 'seaweedfsadmin')
        aws_region = os.getenv('S3_REGION', 'default')
        logging.info(
            f"retrieve file from S3, endpoint_url={endpoint_url}, aws_access_key_id={aws_access_key_id}, aws_secret_access_key=(hidden), aws_region={aws_region}")
        self.client = boto3.client(service_name='s3', endpoint_url=endpoint_url, aws_access_key_id=aws_access_key_id,
                                   region_name=aws_region, aws_secret_access_key=aws_secret_access_key)
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
            logging.info(f"Uploaded .csv {filepath} with key {filename} into bucket dbrepo-download")
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
            logging.info(f"Downloaded .csv with key {filename} into {filepath} from bucket dbrepo-upload")
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

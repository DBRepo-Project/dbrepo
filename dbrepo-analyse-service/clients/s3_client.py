import os
import boto3
import logging

from botocore.exceptions import ClientError


class S3Client:

    def __init__(self):
        endpoint_url = os.getenv('S3_STORAGE_ENDPOINT', 'http://localhost:9000')
        aws_access_key_id = os.getenv('S3_ACCESS_KEY_ID', 'seaweedfsadmin')
        aws_secret_access_key = os.getenv('S3_SECRET_ACCESS_KEY', 'seaweedfsadmin')
        logging.info("retrieve file from S3, endpoint_url=%s, aws_access_key_id=%s, aws_secret_access_key=(hidden)",
                     endpoint_url, aws_access_key_id)
        self.client = boto3.client(service_name='s3', endpoint_url=endpoint_url, aws_access_key_id=aws_access_key_id,
                                   aws_secret_access_key=aws_secret_access_key)
        self.bucket_exists_or_exit("dbrepo-upload")
        self.bucket_exists_or_exit("dbrepo-download")

    def upload_file(self, filename, path="/tmp/", bucket="dbrepo-download") -> bool:
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
            if self.client.upload_file(filepath, bucket, filename) is False:
                logging.warning(f"Failed to upload file with key {filename}")
                raise ConnectionRefusedError(f"Failed to upload file with key {filename}")
            logging.info(f"Uploaded .csv {filepath} with key {filename}")
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
            logging.info(f"Downloaded .csv with key {filename} into {filepath}")
            return True
        except ClientError:
            logging.error(f"Failed to download file with key {filename} into {filepath}")
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

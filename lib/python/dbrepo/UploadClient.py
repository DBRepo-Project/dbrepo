import logging
import os
import re
import sys
from io import BytesIO

from pandas import DataFrame
from tusclient import client

from dbrepo.api.exceptions import UploadError

logging.basicConfig(format='%(asctime)s %(name)-12s %(levelname)-6s %(message)s', level=logging.INFO,
                    stream=sys.stdout)


class UploadClient:
    """
    The UploadClient class for communicating with the DBRepo REST API. All parameters can be set also via environment \
    variables, e.g. set endpoint with DBREPO_ENDPOINT, username with DBREPO_USERNAME, etc. You can override the \
    constructor parameters with the environment variables.

    :param endpoint: The REST API endpoint. Optional. Default: "http://gateway-service/api/upload/files"
    """
    endpoint: str = None

    def __init__(self, endpoint: str = 'http://gateway-service/api/upload/files') -> None:
        self.endpoint = os.environ.get('REST_UPLOAD_ENDPOINT', endpoint)

    def upload(self, dataframe: DataFrame) -> str:
        logging.debug(f"upload to endpoint: {self.endpoint}")
        tus_client = client.TusClient(url=self.endpoint)
        buffer: BytesIO = BytesIO(dataframe.to_csv(index=False, header=False).encode('utf-8'))
        uploader = tus_client.uploader(file_stream=buffer)
        uploader.upload()
        m = re.search('\\/([a-f0-9]+)$', uploader.url)
        filename = m.group(0)[1:-1]
        if filename is None:
            raise UploadError('Failed to upload file: no filename')
        logging.info(f'Uploaded to storage service with key: {filename}')
        return filename

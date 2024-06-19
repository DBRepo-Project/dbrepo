import logging
import os
import re
import sys
from tusclient import client

logger = logging.getLogger("UploadClient")


class UploadClient:
    """
    The UploadClient class for communicating with the DBRepo REST API. All parameters can be set also via environment \
    variables, e.g. set endpoint with DBREPO_ENDPOINT, username with DBREPO_USERNAME, etc. You can override the \
    constructor parameters with the environment variables.

    :param endpoint: The REST API endpoint. Optional. Default: "http://gateway-service/api/upload/files"
    """
    endpoint: str = None

    def __init__(self, endpoint: str = 'http://gateway-service/api/upload/files') -> None:
        logging.basicConfig(format='%(asctime)s %(name)-12s %(levelname)-6s %(message)s', level=logging.DEBUG,
                            stream=sys.stdout)
        self.endpoint = os.environ.get('REST_UPLOAD_ENDPOINT', endpoint)

    def upload(self, file_path: str) -> str:
        """
        Imports a file through the Upload Service into the Storage Service.

        :param file_path: The file path on the local machine.

        :returns: Filename on the Storage Service, if successful.
        """
        tus_client = client.TusClient(url=self.endpoint)
        uploader = tus_client.uploader(file_path=file_path)
        uploader.upload()
        m = re.search('\\/([a-f0-9]+)\\+', uploader.url)
        filename = m.group(0)[1:-1]
        logger.debug(f'uploaded file {file_path} to storage service with key: {filename}')
        return filename

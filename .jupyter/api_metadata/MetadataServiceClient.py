import requests as rq


class MetadataServiceClient:

    def __init__(self, host):
        self.gateway = "http://" + host + ":9095/api/oai"

    def identify(self):
        response = rq.get(self.gateway)
        if response.status_code == 200:
            return response.text
        raise Exception("Failed to identify", response)

    def identify1(self):
        response = rq.get(self.gateway + "?verb=Identify")
        if response.status_code == 200:
            return response.text
        raise Exception("Failed to identify", response)

    def list_identifiers(self):
        response = rq.get(self.gateway + "?verb=ListIdentifiers")
        if response.status_code == 200:
            return response.text
        raise Exception("Failed to list identifiers", response)

    def get_record(self, identifier, metadata_prefix="oai_dc"):
        response = rq.get(
            self.gateway + "?verb=GetRecord&metadataPrefix=" + metadata_prefix + "&identifier=" + identifier)
        if response.status_code == 200:
            return response.text
        raise Exception("Failed to get record", response)

    def list_metadata_formats(self):
        response = rq.get(self.gateway + "?verb=ListMetadataFormats")
        if response.status_code == 200:
            return response.text
        raise Exception("Failed to get record", response)

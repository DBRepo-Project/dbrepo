#!/usr/bin/env python3
import os.path
from os import listdir
from os.path import isfile, join
from api_document.api.document_endpoint_api import DocumentEndpointApi
from api_document.api.file_endpoint_api import FileEndpointApi
from api_authentication.api.authentication_endpoint_api import AuthenticationEndpointApi

authentication = AuthenticationEndpointApi()
document = DocumentEndpointApi()
file = FileEndpointApi()

response = authentication.authenticate_user1({
    "username": "user",
    "password": "user"
})
headers = {"Authorization": "Bearer " + response.token}
document.api_client.default_headers = headers
file.api_client.default_headers = headers

# Create document
response = document.create({
    "access": {
        "record": "public",
        "files": "public"
    },
    "files": {
        "enabled": True
    },
    "metadata": {
        "creators": [
            {
                "affiliations": [
                    {
                        "name": "TU Wien"
                    }
                ],
                "person_or_org": {
                    "type": "personal",
                    "name": "M., Weise",
                    "identifiers": [
                        {
                            "scheme": "orcid",
                            "identifier": "0000-0003-4216-302X"
                        }
                    ],
                    "given_name": "Martin",
                    "family_name": "Weise"
                }
            }
        ],
        "title": "Jupyter Notebook Test",
        "resource_type": {
            "id": "other"
        },
        "publication_date": "2022-06-28"
    }
})
document_id = response.id
print(document_id)

# Upload files
files = [f for f in listdir("./audio") if isfile(join("./audio", f))]
for f in files:
    print("... upload file", "/tmp/" + f)
    response = file.upload_file({
        "location": os.path.curdir + "/tmp/" + f
    }, document_id)

# Publish
response = document.publish(document_id)
print(response)

import os.path
import uuid
import time
import re
import csv
import requests as rq
from api_authentication.api.authentication_endpoint_api import AuthenticationEndpointApi
from api_authentication.api.user_endpoint_api import UserEndpointApi
from api_container.api.container_endpoint_api import ContainerEndpointApi
from api_database.api.container_database_endpoint_api import ContainerDatabaseEndpointApi
from api_table.api.table_endpoint_api import TableEndpointApi

authentication = AuthenticationEndpointApi()
user = UserEndpointApi()
container = ContainerEndpointApi()
database = ContainerDatabaseEndpointApi()
table = TableEndpointApi()

url = "https://test.researchdata.tuwien.ac.at/records/vqpbr-5b889"

host = re.findall("^https?:\/\/([a-z0-9\.]+)", url)[0]
id = re.findall("/([a-z0-9-]+)$", url)[0]

response = rq.get("https://" + host + "/api/records/" + id + "/files")
record = response.json()

for file in record["entries"]:
    print("... save file contents from", file["links"]["content"])
    wav = rq.get(file["links"]["content"])
    filename = "/tmp/" + file["key"]
    open(filename, "wb").write(wav.content)
    print("... file saved in", filename)
    audio = "http://localhost:8000/v1/audio"
    with open(filename, "rb") as f:
        data = f.read()
        print("... feature extract to", audio)
        res = rq.post(audio, data=data, headers={"Content-Type": "audio/wav"})
        print("... extracted", res.json())
        payload = []
        for part in res.json()["track"]["parts"]:
            payload.append(part)
        print("... payload", payload)
print("Finished.")
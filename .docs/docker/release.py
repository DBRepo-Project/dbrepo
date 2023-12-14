#!/bin/env python3
from client.Dockerhub import Dockerhub
from dotenv import load_dotenv

load_dotenv()

dockerhub = Dockerhub()

components = [
    {
        "name": "Analyse Service",
        "doc": "system-services-analyse",
        "dir": "analyse-service"
    },
    {
        "name": "Authentication Service",
        "doc": "system-services-authentication",
        "dir": "authentication-service"
    },
    {
        "name": "Broker Service",
        "doc": "system-services-broker",
        "dir": "broker-service"
    },
    {
        "name": "Data Service",
        "doc": "system-services-data",
        "dir": "data-service"
    },
    {
        "name": "Metadata Service",
        "doc": "system-services-metadata",
        "dir": "metadata-service"
    },
    {
        "name": "Metadata Database",
        "doc": "system-metadata-db",
        "dir": "metadata-db"
    },
    {
        "name": "User Interface",
        "doc": "system-other-ui",
        "dir": "ui"
    }
]

if __name__ == "__main__":
    for component in components:
        response = dockerhub.modify_description(component)
        print(response)

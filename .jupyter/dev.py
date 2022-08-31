#!/bin/env python3
from api_broker.BrokerServiceClient import BrokerServiceClient

broker = BrokerServiceClient(exchange="airquality_6f28efee-2377-11ed-9491-8c8caada74c3", routing_key="airquality_6f510ee8-2377-11ed-9491-8c8caada74c3", host="localhost", username="test1",
                             password="test1")
payload = {"date": "2021-01-01", "location": "Stampfenbachstrasse", "parameter": "CO", "interval": "h1", "unit": "mg/m3", "value": 0.44, "status": "tentative"}
response = broker.send(payload)

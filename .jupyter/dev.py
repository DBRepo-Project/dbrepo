#!/bin/env python3
import time

from api_broker.BrokerServiceClient import BrokerServiceClient

broker = BrokerServiceClient(exchange="airquality2", routing_key="airquality2", host="localhost", username="user",
                             password="user")
payload = {'date': '2022-07-21', 'location': 'Kuala Lumpur', 'parameter': 'T', 'interval': 'h1', 'unit': 'deg-celsius',
           'value': 33.0, 'status': 'tentative'}
response = broker.send(payload)
print(payload)

# faulty date
payload = {'date': '2022-07', 'location': 'Kuala Lumpur', 'parameter': 'T', 'interval': 'h1', 'unit': 'deg-celsius',
           'value': 33.0, 'status': 'tentative'}
response = broker.send(payload)
print(payload)

# faulty number
payload = {'date': '2022-07-01', 'location': 'Kuala Lumpur', 'parameter': 'T', 'interval': 'h1', 'unit': 'deg-celsius',
           'value': 'hello', 'status': 'tentative'}
response = broker.send(payload)
print(payload)

# faulty string
payload = {'date': '2022-07-01', 'location': 'Kuala Lumpur', 'parameter': 'T', 'interval': 'h1', 'unit': 'deg-celsius',
           'value': 33.0, 'status': 88}
response = broker.send(payload)
print(payload)

while True:
    response = broker.send(payload)
    print(payload)
    time.sleep(1)

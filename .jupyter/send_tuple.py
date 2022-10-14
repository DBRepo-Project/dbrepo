#!/bin/env python3
from pika.exceptions import ProbableAuthenticationError

from api_broker.BrokerServiceClient import BrokerServiceClient

dbexchange = "airquality_c64fc6f4-3a87-11ed-a3d0-64bc58900b78"
ttopic = "airquality_c67d2b58-3a87-11ed-a3d0-64bc58900b78"


def send_tuple(exchange, routing_key, username, password, payload):
    broker = BrokerServiceClient(exchange=exchange, routing_key=routing_key, host="localhost", username=username,
                                 password=password)
    response = broker.send(payload)
    print("sent tuple to exchange with routing key %s" % routing_key)
    return response


def send_tuple_fails(exchange, routing_key, username, password, payload):
    try:
        broker = BrokerServiceClient(exchange=exchange, routing_key=routing_key, host="localhost", username=username,
                                 password=password)
        broker.send(payload)
    except ProbableAuthenticationError:
        print("... authentication successfully failed")
        return True
    raise Exception("Tuple successfully sent, should have failed")


if __name__ == '__main__':
    send_tuple(dbexchange, ttopic, "test1", "test1", {"primary": 1})
    send_tuple(dbexchange, ttopic, "test1", "test1", {"primary": 2})
    send_tuple(dbexchange, ttopic, "test1", "test1", {"primary": 3})
    send_tuple_fails(dbexchange, ttopic, "test2", "test2", {"primary": 4})

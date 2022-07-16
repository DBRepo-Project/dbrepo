#!/bin/env python3
import pika
import json


class BrokerServiceClient:

    def __init__(self, exchange=None, routing_key=None, host=None, username=None, password=None):
        self.exchange = exchange
        self.routing_key = routing_key
        self.host = host
        self.username = username
        self.password = password

    def send(self, data):
        creds = pika.credentials.PlainCredentials(self.username, self.password)
        connection = pika.BlockingConnection(pika.ConnectionParameters(host=self.host, credentials=creds))
        channel = connection.channel()
        dump = json.dumps(data)
        channel.basic_publish(exchange=self.exchange,
                              routing_key=self.routing_key,
                              body=bytes(json.dumps(data), encoding='utf8'))
        method, properties, body = channel.basic_get(queue=self.routing_key)
        connection.close()
        if method is not None:
            return False
        else:
            return True

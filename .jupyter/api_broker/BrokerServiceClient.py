import json
from pika import BlockingConnection, ConnectionParameters
from pika.credentials import PlainCredentials


class BrokerServiceClient:

    def __init__(self, exchange, routing_key, host, username, password):
        self.exchange = exchange
        self.routing_key = routing_key
        self.host = host
        self.username = username
        self.password = password
        self.connection = BlockingConnection(ConnectionParameters(self.host,
                                                                  credentials=PlainCredentials(
                                                                      username=self.username,
                                                                      password=self.password)))

    def send(self, message):
        channel = self.connection.channel()
        payload = json.dumps(message)
        print("... sending tuple %s" % payload)
        channel.basic_publish(exchange=self.exchange, routing_key=self.routing_key,
                              body=bytes(payload, encoding='utf8'))
        print("... sent tuple")
        channel.close()

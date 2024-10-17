import os
import pika
import sys
import json
import logging

from dbrepo.api.exceptions import AuthenticationError

logging.basicConfig(format='%(asctime)s %(name)-12s %(levelname)-6s %(message)s', level=logging.INFO,
                    stream=sys.stdout)


class AmqpClient:
    """
    The AmqpClient class for communicating with the DBRepo AMQP API to import data. All parameters can be set also \
    via environment variables, e.g. set endpoint with DBREPO_ENDPOINT. You can override the constructor parameters \
    with the environment variables.

    :param broker_host: The AMQP API host. Optional. Default: "localhost".
    :param broker_port: The AMQP API port. Optional. Default: 5672,
    :param broker_virtual_host: The AMQP API virtual host. Optional. Default: "dbrepo".
    :param username: The AMQP API username. Optional.
    :param password: The AMQP API password. Optional.
    """
    broker_host: str = None
    broker_port: int = 5672
    broker_virtual_host: str = None
    username: str = None
    password: str = None

    def __init__(self,
                 broker_host: str = 'localhost',
                 broker_port: int = 5672,
                 broker_virtual_host: str = 'dbrepo',
                 username: str = None,
                 password: str = None) -> None:
        self.broker_host = os.environ.get('AMQP_API_HOST', broker_host)
        self.broker_port = os.environ.get('AMQP_API_PORT', broker_port)
        if os.environ.get('AMQP_API_VIRTUAL_HOST') is not None:
            self.broker_virtual_host = os.environ.get('AMQP_API_VIRTUAL_HOST')
        else:
            self.broker_virtual_host = broker_virtual_host
        self.username = os.environ.get('AMQP_API_USERNAME', username)
        self.password = os.environ.get('AMQP_API_PASSWORD', password)

    def publish(self, routing_key: str, data=dict, exchange: str = 'dbrepo') -> None:
        """
        Publishes data to a given exchange with the given routing key with a blocking connection.

        :param routing_key: The routing key.
        :param data: The data.
        :param exchange: The exchange name. Default: "dbrepo".
        """
        if self.username is None or self.password is None:
            raise AuthenticationError(f"Failed to perform request: authentication required")
        parameters = pika.ConnectionParameters(host=self.broker_host, port=self.broker_port,
                                               virtual_host=self.broker_virtual_host,
                                               credentials=pika.credentials.PlainCredentials(self.username,
                                                                                             self.password))
        connection = pika.BlockingConnection(parameters)
        channel = connection.channel()
        channel.basic_publish(exchange=exchange, routing_key=routing_key, body=json.dumps(data))
        connection.close()

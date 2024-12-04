import os
import sys
import json
import logging
import paho.mqtt.client as mqtt

from dbrepo.api.exceptions import AuthenticationError

logging.basicConfig(format='%(asctime)s %(name)-12s %(levelname)-6s %(message)s', level=logging.INFO,
                    stream=sys.stdout)


class MqttClient:
    """
    The MqttClient class for communicating with the DBRepo MQTT API to import data. All parameters can be set also \
    via environment variables, e.g. set endpoint with DBREPO_ENDPOINT. You can override the constructor parameters \
    with the environment variables.

    :param broker_host: The MQTT API host. Optional. Default: "localhost".
    :param broker_port: The MQTT API port. Optional. Default: 1883,
    :param username: The MQTT API username. Optional.
    :param password: The MQTT API password. Optional.
    """
    broker_host: str = None
    broker_port: int = 1883
    username: str = None
    password: str = None

    def __init__(self,
                 broker_host: str = 'localhost',
                 broker_port: int = 1883,
                 username: str = None,
                 password: str = None) -> None:
        self.broker_host = os.environ.get('MQTT_API_HOST', broker_host)
        self.broker_port = os.environ.get('MQTT_API_PORT', broker_port)
        self.username = os.environ.get('MQTT_API_USERNAME', username)
        self.password = os.environ.get('MQTT_API_PASSWORD', password)

    def publish(self, routing_key: str, data=dict) -> None:
        """
        Publishes data to a given exchange with the given routing key with a blocking connection.

        :param routing_key: The routing key.
        :param data: The data.
        """
        if self.username is None or self.password is None:
            raise AuthenticationError(f"Failed to perform request: authentication required")
        client = mqtt.Client()
        client.username_pw_set(self.username, self.password)
        client.connect(self.broker_host, self.broker_port, 60)
        client.loop_start()
        client.publish(routing_key, json.dumps(data))
        client.loop_stop()

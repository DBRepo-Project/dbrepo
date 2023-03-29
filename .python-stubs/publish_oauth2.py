#!/bin/env python3
import os

import pika
from dotenv import load_dotenv

load_dotenv()

if __name__ == "__main__":
    token = os.getenv("TOKEN")
    credentials = pika.credentials.PlainCredentials("", token)
    parameters = pika.ConnectionParameters('localhost', 5672, '/', credentials)

    connection = pika.BlockingConnection(parameters)
    channel = connection.channel()
    channel.queue_declare(queue='test', durable=True)
    channel.basic_publish(exchange='',
                          routing_key='test',
                          body=b'Hello World!')
    print(" [x] Sent 'Hello World!'")
    connection.close()

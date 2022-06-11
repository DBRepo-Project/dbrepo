#!/usr/bin/env python3
import pika
from datetime import datetime as dt
import random
import json
from sys import argv


def send(exchange, routing_key):
    creds = pika.credentials.PlainCredentials('fda', 'fda')
    connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost', credentials=creds))
    channel = connection.channel()

    # 2022-02-01 09:09:09
    # data = {'timestamp': dt.now().strftime('%Y-%m-%d %H:%M:%d'), 'location': 'somelocation',
    #         'value': random.randint(20, 30)}
    data = {'status': 'provisorisch', 'datum': dt.now().strftime('%Y-%m-%d %H:00:00'), 'parameter': 'TEST',
            'intervall': 'h1', 'wert': random.randrange(0, 100, 1) / 10, 'einheit': 'test', 'standort': 'Vienna'}

    channel.basic_publish(exchange=exchange,
                          routing_key=routing_key,
                          body=bytes(json.dumps(data), encoding='utf8'))
    print('submitted', data)

    connection.close()


def usage():
    print('USAGE: ./amqp exchange routing_key')


if __name__ == "__main__":
    if len(argv) != 3:
        usage()
        exit(1)
    send(exchange=argv[1], routing_key=argv[2])

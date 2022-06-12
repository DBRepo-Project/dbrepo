#!/usr/bin/env python3
import pika
import json
import time
import datetime
from sys import argv, exit


def mock(channel, exchange, routing_key, size):
    data = {'status': 'provisorisch', 'datum': '2022-06-12 00:00:00', 'parameter': None,
            'intervall': 'h1', 'wert': 0, 'einheit': 'mg/m3', 'standort': 'Vienna'}
    dump = json.dumps(data)
    if len(dump) > size:
        print('ERROR: data skeleton is too big:', len(dump))
        exit(1)
    fill = 'A' * (size - len(dump))
    data['parameter'] = fill

    channel.basic_publish(exchange=exchange,
                          routing_key=routing_key,
                          body=bytes(json.dumps(data), encoding='utf8'))
    method, properties, body = channel.basic_get(queue=routing_key)
    if method is not None:
        return 1
    else:
        return 0


def send(exchange, routing_key):
    creds = pika.credentials.PlainCredentials('fda', 'fda')
    connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost', credentials=creds))
    channel = connection.channel()

    size = 200
    rate = 200  # tuple/s
    wait = 60  # seconds

    print('submit tuple size', size / 1000, '(kB) data at rate', rate, '(tuple/s) ...')
    now = datetime.datetime.now()
    stop = now + datetime.timedelta(0, wait)

    while datetime.datetime.now() < stop:
        res = mock(channel, exchange, routing_key, size)
        if res == 1:
            print('WARN: queue is not empty, exit after', (datetime.datetime.now() - now).total_seconds(), 'seconds.')
            exit(2)
        time.sleep(1 / rate)

    print('waited', wait, 'seconds. Goodbye!')

    connection.close()


def usage():
    print('USAGE: ./amqp exchange routing_key')


if __name__ == "__main__":
    if len(argv) != 3:
        usage()
        exit(1)
    send(exchange=argv[1], routing_key=argv[2])

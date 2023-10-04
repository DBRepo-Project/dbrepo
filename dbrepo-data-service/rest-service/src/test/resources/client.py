#!/usr/bin/env python3
import pika
import sys

if len(sys.argv) != 4:
    print("USAGE: ./client PORT ROUTING_KEY MESSAGE")
    sys.exit(1)

credentials = pika.PlainCredentials('fda', 'fda')
parameters = pika.ConnectionParameters('localhost', int(sys.argv[1]), 'dbrepo', credentials)
connection = pika.BlockingConnection(parameters)
channel = connection.channel()
channel.basic_publish('dbrepo', sys.argv[2], sys.argv[3],
                      pika.BasicProperties(content_type='text/plain',
                                           delivery_mode=pika.DeliveryMode.Transient))
print("Success.")
connection.close()
#!/usr/bin/env python3
import pika
import sys

if len(sys.argv) != 6:
    print("USAGE: ./client PORT ROUTING_KEY MESSAGE USERNAME PASSWORD")
    sys.exit(1)

credentials = pika.PlainCredentials(sys.argv[4], sys.argv[5])
parameters = pika.ConnectionParameters('localhost', int(sys.argv[1]), 'dbrepo', credentials)
connection = pika.BlockingConnection(parameters)
channel = connection.channel()
channel.basic_publish('dbrepo', sys.argv[2], sys.argv[3],
                      pika.BasicProperties(content_type='text/plain',
                                           delivery_mode=pika.DeliveryMode.Transient))
print("Success.")
connection.close()
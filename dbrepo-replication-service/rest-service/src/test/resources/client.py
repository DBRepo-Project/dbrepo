#!/usr/bin/env python3
import pika
import sys

if len(sys.argv) != 7:
    print("USAGE: ./client HOST PORT ROUTING_KEY MESSAGE USERNAME PASSWORD")
    sys.exit(1)

credentials = pika.PlainCredentials(sys.argv[5], sys.argv[6])
parameters = pika.ConnectionParameters(sys.argv[1], int(sys.argv[2]), 'dbrepo', credentials)
connection = pika.BlockingConnection(parameters)
channel = connection.channel()
channel.basic_publish('dbrepo', sys.argv[3], sys.argv[4],
                      pika.BasicProperties(content_type='text/plain',
                                           delivery_mode=pika.DeliveryMode.Transient))
print("Success.")
connection.close()

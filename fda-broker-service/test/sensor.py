#!/usr/bin/env python
import pika
from datetime import datetime as dt
import random
import json

creds = pika.credentials.PlainCredentials('fda', 'fda')
connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost', credentials=creds))
channel = connection.channel()

# 2022-02-01 09:09:09
data = {'timestamp': dt.now().strftime('%Y-%m-%d %H:%M:%d'), 'location': 'somelocation',
        'value': random.randint(20, 30)}

channel.basic_publish(exchange='sensor',
                      routing_key='temperature',
                      body=bytes(json.dumps(data), encoding='utf8'))
print(' [x] Sent %v', data)

connection.close()

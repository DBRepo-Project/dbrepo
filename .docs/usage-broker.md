---
author: Martin Weise
---

# Broker Service

## Authentication

The RabbitMQ client can be authenticated through plain (username, password) and OAuth2 mechanisms. Note that the access
token already contains a field `client_id=foo`, so the username is optional in `PlainCredentials()`.

=== "Plain"

    ``` py
    import pika

    credentials = pika.credentials.PlainCredentials("foo", "bar")
    parameters = pika.ConnectionParameters('localhost', 5672, '/', credentials)
    connection = pika.BlockingConnection(parameters)
    channel = connection.channel()
    channel.queue_declare(queue='test', durable=True)
    channel.basic_publish(exchange='',
    routing_key='test',
    body=b'Hello World!')
    print(" [x] Sent 'Hello World!'")
    connection.close()
    ```

=== "OAuth2"

    ``` py
    import pika
    
    credentials = pika.credentials.PlainCredentials("", "THE_ACCESS_TOKEN")
    parameters = pika.ConnectionParameters('localhost', 5672, '/', credentials)
    connection = pika.BlockingConnection(parameters)
    channel = connection.channel()
    channel.queue_declare(queue='test', durable=True)
    channel.basic_publish(exchange='',
    routing_key='test',
    body=b'Hello World!')
    print(" [x] Sent 'Hello World!'")
    connection.close()
    ```


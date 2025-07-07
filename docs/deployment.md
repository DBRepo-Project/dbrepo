---
author: Martin Weise
hide:
- navigation
---

# Deployment

!!! info "Abstract"

    We modified some services and exchanged them with reviewed, open-source implementations that extend the functionality
    even more from version 1.2 onwards. On this page, some of the configuration possible is summarized.

## Authentication Service

## Broker Service

### Authentication

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

## Identifier Service

From version 1.2 onwards there are two modes for the Identifier Service:

1. Persistent Identifier (PID)
2. Digital Object Identifier (DOI)

By default, the URI mode is used, creating a PID for databases or subsets. If starting the Identifier Service in DOI mode,
a DOI is minted for persistent identification of databases or subsets. Using the DOI system is entirely *optional* and
should not be done for test-deployments.

<figure markdown>
![](/infrastructures/dbrepo/1.10/images/identifier-doi.png)
</figure>

## Gateway Service

From version 1.2 onwards we use both HTTP and HTTPS to serve the API, especially for the Authentication Service. The Discovery
Service lists both the non-secure and secure ports.

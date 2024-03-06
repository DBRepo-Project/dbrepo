---
author: Martin Weise
---

# Broker Service

## Preliminary

The RabbitMQ client can be authenticated through Basic Authentication (username, password) and Bearer Authentication.

!!! example "Bearer Authentication"

    Note that the encoded/signed `ACCESS_TOKEN` already contains a field `client_id=username`, so the username is
    optional in `PlainCredentials` when using Bearer Authentication, but provided must match the username. 

=== "Bearer Authentication"

    ```python
    import pika
    
    # Configure client
    credentials = pika.credentials.PlainCredentials("", "ACCESS_TOKEN")
    parameters = pika.ConnectionParameters('localhost', 5672, '/', credentials)
    connection = pika.BlockingConnection(parameters)

    # Channel
    channel = connection.channel()
    channel.basic_publish(exchange='dbrepo',
        routing_key='dbrepo.database_name.table_name',
        body=b'Hello World!')
    print(" [x] Sent 'Hello World!'")
    connection.close()
    ```

=== "Basic Authentication"

    ```python
    import pika
    
    # Configure client
    credentials = pika.credentials.PlainCredentials("username", "password")
    parameters = pika.ConnectionParameters('localhost', 5672, '/', credentials)
    connection = pika.BlockingConnection(parameters)

    # Channel
    channel = connection.channel()
    channel.basic_publish(exchange='dbrepo',
        routing_key='dbrepo.database_name.table_name',
        body=b'Hello World!')
    print(" [x] Sent 'Hello World!'")
    connection.close()
    ```

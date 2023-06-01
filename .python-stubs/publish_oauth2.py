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

# Broker Service

Supports MQTT v3, v4 and v5 (https://www.rabbitmq.com/blog/2023/07/21/mqtt5)

## Advanced Config

https://www.rabbitmq.com/docs/ldap

## LDAP regression check

From the repository root, with Docker, the local Python library, `pika` and
`pandas` available:

```sh
PYTHONPATH=lib/python python3 dbrepo-broker-service/tests/test_integration_ldap.py
```

The test starts disposable services without changing the running deployment.
See [live-check contracts](../docs/developer-guide/live-check-contracts.md#amqp-authentication)
for account requirements and the distinction between RabbitMQ environment
interpolation and LDAP username substitution. Recreate the broker after upgrading
its mounted configuration; changing the file alone does not reload its settings.

# Broker Service

Supports MQTT v3, v4 and v5 (https://www.rabbitmq.com/blog/2023/07/21/mqtt5)

## Consumer Retention

Fresh-install definitions include a durable quorum parking queue. Existing installations must explicitly apply
[the consumer reliability rollout](CONSUMER-RELIABILITY.md) before starting the updated consumer.
The global installer does not perform this migration.

## Advanced Config

https://www.rabbitmq.com/docs/ldap

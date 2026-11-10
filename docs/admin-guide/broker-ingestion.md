# Broker publisher identity

The Consumer authenticates to Data Service with its configured internal identity.
For each tuple it forwards RabbitMQ's validated `user-id` as `X-DBRepo-Publisher`
to the system-only ingestion operation. Data Service checks the publisher's
current enabled account, `insert-table-data` realm role, database/table write grant
and upload ownership before inserting data.
The ordinary tuple API keeps its existing user permission requirements.

AMQP 0-9-1 publishers must set `user-id` to their connection username. The Python
client does this for single tuples and dataframes. Older clients must add this
property before upgrading the Consumer; unidentified messages are rejected without
requeue. Never grant an untrusted publisher RabbitMQ's `impersonator` tag. Treat
federated brokers as part of the trust boundary. See
https://www.rabbitmq.com/docs/validated-user-id for the broker contract.

MQTT deliveries without a broker-validated AMQP user identity cannot use this
path. They are rejected rather than attributed to a table owner. Configure a
trusted identity-preserving bridge before using MQTT with the upgraded Consumer.
Transient retry/acknowledgement behavior remains tracked separately in #145.

# Consumer retries and retained failures

The Consumer makes at most three HTTP attempts for a tuple after connection,
HTTP 5xx or HTTP 429 failures, waiting 500 ms and 1000 ms before the next attempt.
Malformed JSON, invalid routing/identity and permanent HTTP rejection are not
retried. A successful write is acknowledged only after Data Service confirms it.
Exhausted or invalid deliveries are rejected into the broker's durable
`dbrepo.consumer.errors` quorum queue, with the original body and publisher
properties. Its `x-death` header records the source queue and rejection reason.

This is at-least-once processing. If the database committed but the HTTP response
was lost, a retry can duplicate an operation or encounter a duplicate primary key.
The first-503-before-write regression control proves recovery for that case; it
does not establish exactly-once processing. An interrupted retry is requeued, and
the broker policy caps repeated redelivery at ten.

## Upgrade an existing installation

1. Back up the broker configuration and exported definitions. Pause publishers
   and stop Consumer while keeping RabbitMQ and its persistent volume running.
2. Install the updated `advanced.config` and restart the broker without deleting
   its volume or source queue. The LDAP resource query reserves the complete
   `dbrepo.consumer.errors` namespace for internal broker accounts. Do not grant
   an ordinary LDAP account access to the error queue or exchange. Verify this
   denial with an ordinary account before enabling dead lettering.
3. Ensure the source queue is already a quorum queue and the `stream_queue`
   feature flag is enabled. This upgrade does not change queue type or enable
   feature flags. Apply the additive topology with the helper below. It refuses
   unrelated queue policies and operator policies; merge these settings manually
   when an installation already has one.
4. Deploy Consumer and update AMQP publishers, then resume publishing. The SDK
   sets the verified user-id and persistent delivery mode. Verify a transient
   failure recovers and a permanent failure remains in the error queue.

Keep the management credentials in a protected file, for example mode 0600:

```json
{
  "endpoint": "http://127.0.0.1:15672",
  "username": "internal-broker-operator",
  "password": "READ_FROM_PROTECTED_CONFIGURATION",
  "vhost": "dbrepo",
  "queue": "dbrepo"
}
```

```sh
python3 .scripts/configure-consumer-errors.py --config /private/broker-management.json
```

The helper adds the error exchange, queue, binding and narrowly matched policy.
It does not import users, change passwords, replace source-queue arguments or
remove messages. Fresh Compose, Helm and live-image definitions include the same
topology. For Helm, deploy the updated broker Secret first and verify the live
policy before upgrading Consumer. Custom source queue/vhost names must be passed
to the helper or configured equivalently by the operator.

The policy uses quorum at-least-once dead lettering and `overflow=reject-publish`.
Do not disable that strategy while unconfirmed dead letters exist; RabbitMQ can
discard those pending transfers when switching back to at-most-once. See
[RabbitMQ's quorum queue documentation](https://www.rabbitmq.com/docs/3.13/quorum-queues#dead-lettering).

Only an internal broker operator should inspect retained failures. Repair the
cause and resubmit through the original authenticated publisher so that current
account, role and database/table grants are checked again. Do not blindly replay
an old identity or remove the error queue as rollback. Monitor error-queue depth,
age and broker disk capacity. Retained failures have no automatic expiry; review
and archive them under the installation retention policy. The compatible topology
can remain when rolling the application back, although an old Consumer still
swallows failures and is not protected by the new retry behavior.

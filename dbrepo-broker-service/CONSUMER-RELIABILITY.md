# Consumer Delivery Reliability

## Delivery Contract

The consumer accepts a JSON object (the raw column/value map, not a `data` wrapper) and the exact routing shape
`dbrepo.<database UUID>.<table UUID>`. UUIDs must use the full hyphenated form; hexadecimal case is insignificant.
Extra segments, wrong prefixes, missing IDs, abbreviated UUIDs, invalid JSON, trailing JSON values and JSON `null`
are rejected without calling the data service. The broad `dbrepo.#` binding is retained so malformed routes under
that prefix reach retention rather than becoming unroutable. Publishers must still use confirms and handle
unroutable publications; unrelated prefixes will not normally reach this queue.

Successful consumption means HTTP **201** from `POST /api/v1/database/{id}/table/{id}/data`.
This is the normal insert path, including the source SQL/outbox transaction, not the replication-only endpoint.

| Outcome | Consumer action |
| --- | --- |
| HTTP 201 | ACK after the gateway returns successfully |
| Network/timeout failure, HTTP 408/429, HTTP 5xx | At most 3 total attempts per broker delivery, waiting 1s then 2s |
| Other HTTP 4xx, including 401/403/404/409 | Reject immediately; fix authorization, metadata or data before manual replay |
| Malformed message, unexpected status or unexpected application exception | Reject immediately |
| Transient attempts exhausted | Reject, never acknowledge as success |

The Spring stateless retry interceptor and `RejectAndDontRequeueRecoverer` issue a NACK with `requeue=false`.
`REQUEUE_REJECTED` is no longer honored by this consumer. There is no automatic DLQ-to-source replay loop.
Prefetch is 1 per consumer; concurrency remains controlled by `MIN_CONCURRENT_CONSUMERS` / `MAX_CONCURRENT_CONSUMERS`.
`DATA_CONNECT_TIMEOUT` (default 5000ms) and `DATA_READ_TIMEOUT` (default 30000ms) must be positive.
The read timeout bounds stalled reads, not a transaction's lifetime. Keep the broker acknowledgement timeout above
the retry/read-timeout budget. Tune HTTP timeouts to actual insert latency to avoid unnecessary ambiguous retries.

## Broker Topology

- Existing source: durable quorum queue `dbrepo` in vhost `dbrepo`; no queue recreation or argument change.
- Policy `dbrepo-consumer-retention`, exact pattern `^dbrepo$`, priority 50: DLX `dbrepo.dead-letter`, routing key
  `rejected`, `dead-letter-strategy=at-least-once`, `overflow=reject-publish`, `delivery-limit=5`.
- New durable direct exchange and durable quorum queue, both named `dbrepo.dead-letter`, with binding `rejected`.
- Parking queue has no TTL, expiry, max length, dead-letter route or application consumer. Leave its delivery limit
  unset: this is unlimited on the shipped RabbitMQ 3.13.7. Do not set `x-delivery-limit=-1` on this version: it can
  discard a message on the first requeue, including a management-API inspection. Revalidate delivery-limit semantics
  before any broker major-version upgrade. Do not apply a broad expiry/drop policy to this queue.

At-least-once quorum dead lettering retains the source copy until the target confirms receipt, including during
target unavailability. Ordinary default dead lettering is at-most-once and is not an acceptable substitute.
The source delivery limit also bounds broker redelivery after consumer crashes/channel loss (Spring's in-memory
attempt count resets on redelivery). Broker redelivery limits do not deduplicate fresh publisher retries.

## Explicit Rollout

These are operator instructions, not actions performed by the build or global installer. Use staging first.
The shipped broker image is RabbitMQ 3.13.7; require quorum queues and the `stream_queue` feature flag needed for
at-least-once dead lettering. Fresh-install Compose and Helm definitions match `consumer-retention.json`.
Changing startup definitions alone is not sufficient evidence that an existing broker has applied the policy.

1. Pause/scale down **all** consumer instances. Keep publishers paused too when practical, or monitor source backlog
   and disk capacity. Record replica counts and preserve broker data volumes. Do not delete or purge either queue.
2. On the chosen broker, inspect `rabbitmqctl version`, `rabbitmqctl list_feature_flags`,
   `rabbitmqctl list_queues -p dbrepo name type durable arguments policy`,
   `rabbitmqctl list_policies -p dbrepo` and `rabbitmqctl list_operator_policies -p dbrepo`.
   Require the existing `dbrepo` queue to be quorum. A classic queue requires a separate planned migration, not deletion.
   Check for conflicting policies or queue arguments: only one ordinary policy wins. Merge any needed existing
   settings deliberately; remove conflicting queue arguments only via a separately planned migration. A higher/equal
   priority matching policy or operator policy can override retention. Custom vhosts/queue names require an adapted
   policy and matching consumer settings; the supplied file targets only `dbrepo`.
3. Enable `stream_queue` and import the **partial** definitions below. This file does not change users, permissions,
   vhosts or source queue arguments. Reimporting the same file is repeatable and does not purge messages.
   Do not import the full bootstrap `definitions.json` into an established deployment, because it includes credentials.
4. Verify effective topology and perform the staging acceptance checks below before resuming the updated consumer.
   Deploy the data-service version with source SQL/outbox atomicity before the consumer. Restore recorded consumer
   replicas only after the retention gate passes. Install the updated broker configuration/Helm template too so later
   restarts use the same definitions; no global installer modification is required.

If an earlier rollout created `dbrepo.dead-letter` with `x-delivery-limit=-1`, importing the corrected file does not
remove that immutable queue argument. Pause consumers and all other writers to the parking queue/exchange; inspect queue
metadata, not messages. RabbitMQ 3.13 quorum queues do not support `if-empty` or `if-unused` delete guards. For a known
empty, unused parking queue, remove its DLX binding to prevent new transfers, recheck both ready/unacknowledged counts
and consumer count are zero, then delete only that queue and immediately reimport the corrected partial file. If writers
cannot be quiesced, do not delete it. Never delete the source queue. A nonempty parking queue needs a separate migration:
do not inspect it with requeue semantics or delete it. Preserve each message in a confirmed, routed replacement before
acknowledging its original. Already discarded messages cannot be restored from broker retention.

### Compose Commands

Run from the checkout for the intended deployment after stopping its consumer:

```sh
docker compose stop dbrepo-consumer-service
docker compose exec -T dbrepo-broker-service rabbitmqctl enable_feature_flag stream_queue
docker compose cp dbrepo-broker-service/consumer-retention.json dbrepo-broker-service:/tmp/consumer-retention.json
docker compose exec -T dbrepo-broker-service rabbitmqctl import_definitions /tmp/consumer-retention.json
docker compose exec -T dbrepo-broker-service rabbitmqctl list_policies -p dbrepo
docker compose exec -T dbrepo-broker-service rabbitmqctl list_queues -p dbrepo name type durable arguments policy messages_ready messages_unacknowledged consumers
docker compose exec -T dbrepo-broker-service rabbitmqctl list_bindings -p dbrepo source_name destination_name routing_key
```

After verification, rebuild/select the intended consumer image and recreate that service using the deployment's
normal image/version procedure. `start` alone can restart the old image; do not use it as an upgrade.

### Kubernetes Commands

Select the namespace, actual broker pod/container and consumer deployment explicitly. First record and scale the
consumer deployment to zero. The commands below do not assume release-derived pod or deployment names:

```sh
export NAMESPACE='your-namespace' BROKER_POD='your-broker-pod' BROKER_CONTAINER='rabbitmq'
kubectl -n "$NAMESPACE" exec "$BROKER_POD" -c "$BROKER_CONTAINER" -- rabbitmqctl enable_feature_flag stream_queue
kubectl -n "$NAMESPACE" cp dbrepo-broker-service/consumer-retention.json "$BROKER_POD":/tmp/consumer-retention.json -c "$BROKER_CONTAINER"
kubectl -n "$NAMESPACE" exec "$BROKER_POD" -c "$BROKER_CONTAINER" -- rabbitmqctl import_definitions /tmp/consumer-retention.json
kubectl -n "$NAMESPACE" exec "$BROKER_POD" -c "$BROKER_CONTAINER" -- rabbitmqctl list_policies -p dbrepo
kubectl -n "$NAMESPACE" exec "$BROKER_POD" -c "$BROKER_CONTAINER" -- rabbitmqctl list_queues -p dbrepo name type durable arguments policy messages_ready messages_unacknowledged consumers
kubectl -n "$NAMESPACE" exec "$BROKER_POD" -c "$BROKER_CONTAINER" -- rabbitmqctl list_bindings -p dbrepo source_name destination_name routing_key
```

Apply the updated broker Secret through the normal chart upgrade with the deployment's existing values and secrets.
Keep consumers paused until verification, then roll out the consumer image and restore their previous replica count.
Do not rely on a Secret update alone to reimport definitions into an already-running broker.

### Verification Gate

Verify the policy is selected for `dbrepo`, all five policy settings are effective, both queues are durable quorum,
`stream_queue` is enabled, and the `rejected` binding reaches only the parking queue. Inspect queue details in the
management API as needed (`effective_policy_definition` and queue arguments). Verify no operator policy overrides
retention and that the parking queue has zero consumers and no TTL/length/drop policy.

On a disposable staging deployment and test table, use persistent messages with publisher confirms and mandatory
routing. Record original source/DLQ counts, routing keys, message IDs and test payloads:

1. Publish a valid tuple; verify exactly one test insert and its outbox handoff before the delivery disappears.
2. Publish malformed JSON and a malformed route under `dbrepo.`; verify no insert and one retained dead letter each.
   On an otherwise empty test DLQ, inspect only these marker messages with requeue semantics, wait at least 60 seconds,
   and verify both the original bodies and `x-death.reason=rejected` are still present. This catches broker-version
   delivery-limit behavior that static definitions and an initial arrival check cannot establish.
3. Return a permanent HTTP 4xx from a test data endpoint; verify one attempt and retention, not an ACK or hot loop.
4. Return HTTP 503 continuously; verify exactly three attempts separated by 1s/2s, then retention. Restore the endpoint
   before the third attempt in a separate test and verify eventual successful ACK instead.
5. In staging only, temporarily remove the parking binding, then reject a test message. Verify the broker retains the
   source dead letter; restore the binding by reimporting the partial file and verify eventual DLQ arrival. Test a
   broker restart and verify retained messages survive. Do not purge queues as cleanup on a shared broker.

The JVM tests cover real Spring container ACK/NACK behavior with a mocked channel, not broker persistence or quorum
failover. Broker acceptance tests require a separate staging broker and were not run where Docker was unavailable.

## Recovery and Limits

Alert on any parking backlog, growing source backlog, rejected publishes, broker dead-letter transfer warnings,
disk/memory alarms and lost quorum. Dead letters can remain retained in the source while the target is unavailable;
source counts and warnings matter even if the DLQ is empty. Retention has a disk-capacity cost and no automatic expiry.
Durability is not high availability: a single-node quorum queue still has only one copy. Use replicated quorum members
and backups according to the deployment's failure model. Publishers must retain/retry messages rejected under pressure.

Do not attach an automatic shovel/consumer to the parking queue. Inspect with non-destructive/requeue semantics;
export payload, properties and `x-death` history before recovery. Repair the cause, then check whether each tuple
already committed. For deliberate replay, publish the original raw map to exchange `dbrepo` with the original
`dbrepo.<database UUID>.<table UUID>` key from the source `x-death.routing-keys` entry (repair malformed keys explicitly),
not the parking key `rejected`. Preserve the message ID and history for audit. Use persistent publishing, confirms
and mandatory routing. Remove/ACK the parked copy only after confirmed, routed handoff, with a recorded replay outcome.
A replay that fails is parked again, including when it carries `x-death` headers. Never bulk purge to clear an alert.

This is **at-least-once**, not exactly-once. A timeout can occur after SQL/outbox commit; a consumer can crash after HTTP
201 but before RabbitMQ receives its ACK. Retrying or manually replaying can insert another tuple and another outbox
event. The consumer has no durable inbox/deduplication key and does not pass the AMQP message ID as an idempotency key.
SQL/outbox atomicity prevents a committed tuple from missing its replication handoff; it does not make broker ingestion
idempotent. Tables without a suitable unique business key may contain duplicates. A conflict is retained for review,
not treated as proof of prior success. Concurrent consumers/retries can reorder writes. Resolve ambiguous commits using
application/business-key evidence before replay; exactly-once ingestion needs a separate durable deduplication contract.

If application rollback is needed, pause consumers first and leave the DLX, DLQ, feature flag and retention policy in
place. An older consumer can silently ACK failed writes; rolling back the image alone does not preserve these guarantees.
Do not disable at-least-once dead lettering or switch overflow mode while source dead letters await target confirmation.

## Offline Checks

```sh
python3 dbrepo-broker-service/tests/test_consumer_retention.py
```

Build `lib/java/dbrepo-core` and `dbrepo-consumer-service` in one temporary Maven reactor to avoid using a stale locally
installed core artifact. With JDK 21, run `mvn -f /path/to/reactor.xml -Djacoco.skip=true -DargLine= test
-Dtest=DataServiceGatewayUnitTest,RabbitListenerUnitTest -Dsurefire.failIfNoSpecifiedTests=false`.
These tests require no Docker. The existing Spring-context tests, including `CredentialServiceUnitTest` and
`PrometheusEndpointMvcTest`, import the Redis test-container configuration and need a separate Docker-enabled run.

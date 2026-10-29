# Selectable replication transport

## Agreed scope

Choose REST or RabbitMQ per local database and target site. Existing connections
default to REST. The choice covers bootstrap, history chunks, live mutations,
timestamp evidence and subsets. Preserve transaction boundaries, idempotency,
history and existing retry jobs. Credentials remain server-side.

## Implementation

- [x] Add a persisted per-database/peer transport registry beside the existing
  outbox, with REST as the default and explicit remote database mappings.
  Test restart persistence, independent targets and invalid configuration.
- [x] Add a bounded RabbitMQ request/reply transport to the existing peer client.
  Reuse the current destination endpoints through an explicit method/path allowlist.
  Use durable requests, publisher confirms, manual acknowledgements, restricted
  per-peer queues and application replies after endpoint completion. A lost reply
  retries the same business operation; broker acceptance is not job completion.
  Test rejection of arbitrary paths, unavailable receivers, restricted broker
  permissions and application receipts. Never fall back silently to REST.
- [x] Route subset/evidence delivery through the replication service so the same
  selection applies. Keep its existing durable query-store outbox.
- [x] Add admin API and UI selection before bootstrap and for existing connections.
  Resolve database IDs on the server; never accept broker credentials from the UI.
  Test permission checks, defaults, saved selection and failed configuration.
- [x] Document Compose/Helm secrets, TLS, queue permissions, backup, switching,
  retries and monitoring.
- [x] Test the transport on an isolated VPS broker vhost: database-ID learning,
  mutation envelopes, 4-MiB payload, application completion, receiver outage and
  broker permissions. Check authenticated read-only API receipts on all three VPS.
- [ ] Perform a staged deployment and full database bootstrap, history and subset
  re-execution acceptance over RabbitMQ, including duplicate/redelivered events.
  The transport-level handler tests do not establish SQL/history correctness.
  Do not switch existing user databases or start local Docker/Kubernetes.

## Verification record (2026-10-29)

- Replication Maven package: 123 tests passed, including two real-broker tests.
- Data-service unit-test package: 377 tests passed; Docker-dependent credential
  and integration suites were not run in this transport check.
- UI: 24 Node tests passed; Nuxt production build passed.
- Helm rendering with an AMQP Secret enabled passed.
- Local replication-service package startup and admin endpoint authentication
  checked; arbitrary operations and untrusted destinations returned HTTP 400.
- No application deployment, commit or push. Official testing data was untouched.

## Boundaries

The first implementation keeps the existing bounded, sequential history chunks;
RabbitMQ is an alternative transport, not a claim of higher throughput. No new
tuple identity, subset result snapshots or schema changes. One replication-service
writer owns its persistent configuration, as it already owns the file outbox.
Transport changes affect subsequent attempts; already delivered/in-flight messages
cannot be recalled. Only trusted administrator-provisioned peers are selectable.

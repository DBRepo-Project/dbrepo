# REST and RabbitMQ replication

## What the choice changes

An administrator selects a transport **per local database and target site** on
the Replication page. Unconfigured connections use REST. A new replica saves the
selection before requesting bootstrap. Existing connections have their own
transport selector and Save button. Database creation through the ordinary create
form retains its REST default; alternatively create the database without peers
and add its targets on the Replication page.

The selection covers metadata creation, history chunks, tuple inserts/updates/
deletes (including CSV and multi-row changes), timestamp evidence and subsets.
RabbitMQ carries bounded replication requests to the target, where the existing
authenticated replication endpoints apply them. This is not a second importer.
No tuple keys, master timestamps, historical visibility or query semantics change.

Data, history, journal and the local outbox still commit atomically. The existing
after-commit dispatcher starts delivery; scheduled retries remain enabled. A
RabbitMQ publisher confirm means only that the broker accepted a request. A job
succeeds only after the target endpoint returns an application receipt. A lost
receipt can repeat a request, so the existing event IDs and idempotent inbox remain
essential. A broker outage **does not silently switch the connection to REST**.

History transfer retains its current 256-row / 4-MiB chunk bounds and sequential
application. RabbitMQ envelopes are limited to 16 MiB including JSON encoding.
This release does not claim a throughput improvement: it still waits for an
application receipt per request. Bootstrap's table-level reconciliation transaction
and individual journal catch-up events have not been redesigned.

## Provision the receiving broker

Use a dedicated replication vhost, TLS on port 5671, and a different publisher
account for every sending site. Do not use `guest`, `system_user`, the ingestion
account or a broker administrator. Network access should be limited to participating
sites; the management API need not be publicly reachable.

For a peer named `site_a`, administratively create these durable resources in the
receiving site's replication vhost:

| Resource | Type | Binding |
| --- | --- | --- |
| `dbrepo.replication.site_a` | direct exchange | |
| `dbrepo.replication.site_a` | durable queue | exchange above, routing key `request` |
| `dbrepo.replication.site_a.replies` | direct exchange | |

Use quorum queues on a suitably sized broker cluster; a single broker still has
single-node availability. Set appropriate disk/memory limits and a queue overflow
policy that rejects publishing rather than dropping old requests. Requests are
persistent but expire after the configured delivery timeout; the source outbox
retains the operation and retries it. Do not attach destructive policies to other
DBRepo queues.

Example resource creation using an already authenticated `rabbitmqadmin` profile:

```sh
rabbitmqadmin -V replication declare exchange name=dbrepo.replication.site_a type=direct durable=true
rabbitmqadmin -V replication declare exchange name=dbrepo.replication.site_a.replies type=direct durable=true
rabbitmqadmin -V replication declare queue name=dbrepo.replication.site_a durable=true
rabbitmqadmin -V replication declare binding source=dbrepo.replication.site_a destination_type=queue destination=dbrepo.replication.site_a routing_key=request
```

Publisher permissions on that vhost, in configure/write/read order:

```sh
rabbitmqctl set_permissions -p replication site_a_publisher '^dbrepo\.replication\.site_a\.reply\.[0-9a-f-]+$' '^dbrepo\.replication\.site_a(\.reply\.[0-9a-f-]+)?$' '^dbrepo\.replication\.site_a\.(replies|reply\.[0-9a-f-]+)$'
```

The publisher creates temporary reply queues and binds them to the replies
exchange. It cannot consume requests, publish replies, change the durable request
queue or publish into another site's request exchange. **Do not grant write access
to `amq.default`: that would bypass the per-peer exchange restriction.**

The local receiver account needs read access to the inbound request queues and
write access to their replies exchanges. It only passively checks the queues, so
it does not need configure permission. For this single peer:

```sh
rabbitmqctl set_permissions -p replication local_receiver '^$' '^dbrepo\.replication\.site_a\.replies$' '^dbrepo\.replication\.site_a$'
```

Repeat provisioning with a distinct exchange/queue name and publisher account for
each additional peer. The receiving application's replication credentials are used
only for local HTTP endpoint authorization; broker passwords are separate. Broker
admission is site-level, not a new per-database authorization system.

Resource permission details: [RabbitMQ access control](https://www.rabbitmq.com/docs/3.13/access-control).

## Configure each application site

Mount a private JSON file into the replication-service container:

```json
{
  "localUri": "amqps://local_receiver:URL_ENCODED_PASSWORD@broker-b.example:5671/replication",
  "inbound": {
    "https://site-a.example": "dbrepo.replication.site_a"
  },
  "outbound": {
    "https://site-a.example": {
      "uri": "amqps://site_b_publisher:URL_ENCODED_PASSWORD@broker-a.example:5671/replication",
      "queue": "dbrepo.replication.site_b"
    }
  },
  "allowPlaintext": false
}
```

The keys are canonical HTTPS site origins, without paths or trailing slashes.
Every inbound/outbound site must also appear in `REPLICATION_ALLOWED_SITES`.
`outbound` points to a peer's broker, not the local broker. An inbound-only site
can omit outbound entries; two-way subset/evidence delivery needs both directions.
Changing this file requires restarting the replication service.

TLS verifies both certificate trust and the broker hostname using the JVM trust
store. For a private CA, mount an appropriate Java trust store and configure
`-Djavax.net.ssl.trustStore=...` and its password through the deployment's private
Java configuration. Keep HTTPS trust roots in that store too. Never disable
verification to accommodate a hostname mismatch. See [RabbitMQ TLS setup](https://www.rabbitmq.com/docs/3.13/ssl).

`allowPlaintext: true` is only for isolated tests or explicitly secured local
tunnels. It must not be used for production cross-site connections.

### Docker Compose

Add a private override; the file must be readable by the container user:

```yaml
services:
  dbrepo-replication-service:
    environment:
      REPLICATION_AMQP_CONFIG: /run/secrets/replication-amqp.json
      REPLICATION_AMQP_TIMEOUT_MS: "120000"
    volumes:
      - ./secrets/replication-amqp.json:/run/secrets/replication-amqp.json:ro
```

Use the existing Compose files plus this override. Preserve all existing volumes,
environment overrides and image pins. Do not use `down -v`. No broker ports are
automatically exposed by this feature; provision the TLS listener deliberately.

### Helm

Create a Secret in the deployment namespace from the private file:

```sh
kubectl -n YOUR_NAMESPACE create secret generic replication-amqp --from-file=config.json=./secrets/replication-amqp.json
```

Set these values while preserving the site's existing values:

```yaml
replicationservice:
  amqp:
    existingSecret: replication-amqp
    timeoutMs: 120000
```

The chart mounts the Secret read-only. AMQP is disabled when `existingSecret` is
empty. Retain the existing persistent outbox volume and single replication-service
writer. Restart the deployment after secret rotation; mounts alone do not reload
open broker connections.

### Timeouts

The data service's internal replication HTTP client waits 270 seconds by default,
so it can receive the application receipt after the default 120-second broker
confirmation and 120-second reply waits. Metadata HTTP requests retain their
30-second timeout. If you increase `REPLICATION_AMQP_TIMEOUT_MS`, also set
`DBREPO_REPLICATION_REQUESTTIMEOUTMS` on the data service to at least twice that
value plus 30,000 milliseconds. A timeout leaves delivery uncertain; the existing
outbox retries the same operation using its original identity.

## API and UI

All configuration endpoints require the `system` role:

- `GET /api/replication/transports`: saved routes, trusted sites and sites with
  outbound RabbitMQ configuration. Configuration availability is not a live broker
  health result.
- `PUT /api/replication/transports/{localDatabaseId}` with
  `{"site":"https://site-b.example","transport":"RABBITMQ"}` (or `"REST"`).
  The server obtains the remote database ID from metadata; it learns newly created
  IDs from successful creation receipts. Credentials are never accepted by this API.
- Then use the existing `POST /api/v1/database/{id}/replicas` API to add a target.
  Repeating configuration after a failed bootstrap request is safe.

On the Replication page, select a database, approved target and transport before
adding a replica. For an existing connection select the new transport, click Save
and confirm. A switch affects subsequent attempts, not already delivered messages.
Pending work retains its event IDs; never delete and recreate the outbox to switch.
Subset/evidence senders now use the replication service's internal delivery endpoint
so that they obey the same selection. Deploy the replication service **before**
the updated data service.

Transport configuration is stored at `REPLICATION_OUTBOX_PATH` plus
`.transports.json`. Include it alongside the outbox in backups/restores. Broker
configuration and credentials are separate secrets and must be backed up securely.

## Acceptance checklist

- Keep one connection on REST and another on RabbitMQ; verify independent choices.
- Bootstrap a database with more than 256 historical rows; compare values, native
  history evidence, master timestamps and subsets on both sites.
- Insert, update, delete, CSV import and multi-row changes after bootstrap.
- Roll back a transaction: neither transport may publish its changes.
- Stop only the test receiver, commit changes, then restart it. Confirm retry and
  catch-up, with no silent REST delivery and no duplicate tuples/history.
- Lose a reply after application and redeliver the same event. Its inbox receipt
  must prevent a second mutation.
- Re-execute subsets created before and during delayed delivery; compare row
  counts, result hashes and streamed data.
- Reject an untrusted broker account, another peer's exchange, arbitrary HTTP paths
  and invalid TLS certificates. Confirm private databases remain private.

The real-broker Java test uses `REPLICATION_TEST_AMQP_URI` pointing to an isolated
test vhost. It creates/removes its own uniquely named resources. Never point it at
a production ingestion vhost. Run with:

```sh
mvn -f dbrepo-replication-service/pom.xml -Dtest=ReplicationAmqpIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

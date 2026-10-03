# Replication outbox recovery

`maxAttempts` is an alert threshold, not an outage expiration time. Recoverable
failures keep the entry `FAILED` after this threshold, with a persisted
`nextAttemptAt`. The scheduler selects these entries when due. A successful retry
changes the status to `SUCCEEDED`; failures and dependency waits must not reset
an existing `FAILED` alert to `PENDING`.

Transport failures and HTTP 408, 429, and 5xx responses are recoverable. Other
HTTP errors and invalid payloads stop automatic retries at the threshold. A 404
does not prove that a job is obsolete and never causes automatic cancellation.
Retry delays grow exponentially, capped by `maxRetryDelaySeconds` (900 seconds
by default). Batch size remains bounded, and due-time ordering prevents old
outages from permanently starving newer work.

Existing `FAILED` entries whose next-attempt time is null need one explicit
operator retry. Error strings are not a reliable basis for reclassifying them.
Subsequent recoverable failures receive the durable recovery schedule.

## Dispatcher integration

The metadata dispatcher already uses this contract. The orchestration dispatcher
in `ReplicationServiceImpl` must use the new overloads:

```java
} catch (ReplicaDependencyPendingException e) {
    outboxService.defer(entry.getId(), e.getMessage(),
            retryDelayFor(entry.getAttempts()), Math.max(1, maxAttempts));
    return false;
} catch (Exception e) {
    log.error("Failed to retry replication outbox entry {}: {}", id, e.getMessage(), e);
    outboxService.markFailed(entry.getId(), e.getMessage(),
            retryDelayFor(entry.getAttempts()), Math.max(1, maxAttempts), isRecoverable(e));
    return false;
}
```

Both dispatchers can use these methods; the parameter is the *previous* attempt
count, so callers must not add one before passing it:

```java
private boolean isRecoverable(Exception error) {
    if (error instanceof ResourceAccessException) {
        return true;
    }
    if (error instanceof RestClientResponseException response) {
        final int status = response.getStatusCode().value();
        return status == 408 || status == 429 || status >= 500 && status <= 599;
    }
    return false;
}

private Duration retryDelayFor(int previousAttempts) {
    final long base = Math.max(1, retryDelaySeconds);
    final long cap = Math.max(base, maxRetryDelaySeconds);
    final long multiplier = 1L << Math.min(Math.max(0, previousAttempts), 62);
    return Duration.ofSeconds(base > cap / multiplier ? cap : base * multiplier);
}
```

Preserve typed HTTP exceptions. If a client returns a non-2xx response instead
of throwing, throw `RestClientResponseException` with the response status rather
than an untyped exception. Do not classify error text.

## Tuple journal integration

`TupleReplicationNotificationDispatcher` and `TupleReplicationOutboxServiceMariaDbImpl`
implement the same recoverable-failure policy. Claims carry persisted `claimToken`
and `claimUntil` fields. Both completion methods require the current token and an
unexpired lease and return whether the transition was committed:

```java
outboxService.markSucceeded(database, entry.getId(), entry.getClaimToken());
outboxService.markFailed(database, entry.getId(), entry.getClaimToken(), e.getMessage(),
        retryDelayFor(entry.getAttempts()), Math.max(1, maxAttempts), isRecoverable(e));
```

There is no unfenced completion overload. The only production caller is the
dispatcher, which passes the token obtained from the claim. A stale completion
returns false and cannot change status, attempts, errors, or retry timing.
Success clears the lease and retry time but retains the journal event; operational
`findAll` excludes successful entries, while `readRange` still exposes them.

Exhausted jobs remain visibly `FAILED` while a worker owns the lease. Active
leases exclude both manual and scheduled claims. Due failed entries and expired
claims are automatically eligible. An interrupted manual retry of a permanent
failure is also recovered after its lease expires, even if its previous retry
time was null. An idle permanent failure still requires an operator retry.
Failure counts saturate at `Integer.MAX_VALUE` and failed alerts remain sticky
until success, even if the configured threshold is subsequently increased.

The scheduler is bounded by `batchSize` and claims one event immediately before
sending it. It visits databases regardless of their current replica URL map so
configuration changes do not hide already-persisted notifications. Candidate
selection and atomic claims use the same eligibility predicate, ordered by retry
time and event sequence. No network request occurs inside the claim transaction.

Enqueue scheduling, retry scheduling, lease creation and lease checks use the
database clock. Operational timestamps are read as server epochs so JDBC/session
timezone differences do not change their meaning. Event IDs, sequences, payloads
and other replay fields are never changed by delivery transitions.

Schema preparation adds nullable token and lease columns, including to an
already-initialized source journal. Legacy `PROCESSING` entries without tokens
become claimable after `processingTimeoutSeconds` from their last modification.
Quiesce old dispatchers before upgrading; their unfenced updates are not safe to
run alongside the new code. Set the processing timeout above the HTTP timeout
plus acknowledgement-write allowance (default lease: 300 seconds).

A peer response alone is not a durable local success. If its acknowledgement
cannot be persisted, the dispatcher leaves the lease for recovery. Delivery is
at least once; retries retain the event identity for receiver deduplication.

## Cancelling obsolete orchestration jobs

An authenticated operator with `system` authority can submit
`POST /api/replication/outbox/{id}/cancel` with a JSON body such as
`{"reason":"Replica site was permanently retired"}`. The reason must be nonblank
and at most 2000 characters. The actor is taken from the authenticated principal,
not the request body. Unknown IDs return 404; successful jobs cannot be cancelled
and return 409. Repeating cancellation is idempotent and preserves the first audit.

Cancellation keeps the operation, payload, last error, attempt count, actor,
timestamp and reason in the file outbox. It clears the next-attempt time and
changes the state to `CANCELLED`, never `SUCCEEDED`. Late status updates cannot
overwrite cancellation. The retry endpoint rejects cancelled jobs with 409.
No cascading cancellation is performed; cancel each obsolete job explicitly.

The orchestration dispatcher's `retryOutboxEntry` must also return false when
reading a cancelled entry, before making any network request. This guards other
callers and stale scheduler candidate lists:

```java
if (entry == null || entry.getStatus() == ReplicationOutboxStatus.CANCELLED) {
    return false;
}
```

Cancellation stops future retries; it cannot undo a request already sent to a
peer. Operators should quiesce dispatch while decommissioning a peer if an
in-flight side effect is unacceptable. This endpoint manages orchestration jobs;
metadata notification and tuple journal cancellation require their own retained
audit fields and fenced state changes before exposing matching endpoints.

## Verification

`FileReplicationOutboxServiceUnitTest` checks file restart recovery, dependency
waiting, permanent failures and preservation of successful records.
`ReplicationNotificationDispatcherUnitTest` checks HTTP classification and capped
backoff. `ReplicationOutboxRecoverySqlTest` runs against H2 by default. To run the
latter against the isolated MariaDB test instance, set
`DBREPO_RECOVERY_TEST_JDBC_URL` to
`jdbc:mariadb://127.0.0.1:13366/recovery_replication_test` and provide
`DBREPO_RECOVERY_TEST_PASSWORD`. It creates only that test database and recreates
only its metadata outbox table; other databases are not modified.

`TupleReplicationNotificationDispatcherUnitTest` covers HTTP classification,
backoff limits, claim-token propagation, bounded batches and acknowledgement
failures. `TupleReplicationRecoveryIntegrationTest` exercises real MariaDB with
25 failures, service recreation, expired/competing claims, terminal fencing,
schema upgrade, timezone differences and unchanged journal history. Enable it
with `RECOVERY_TUPLE_SQL_TEST_PORT=13366` and
`RECOVERY_TUPLE_SQL_TEST_PASSWORD`; it recreates only outbox/counter tables in
`recovery_tuple_test`. To run the existing journal regression suite in that same
isolated database, also set `JOURNAL_SQL_TEST_PORT=13366`,
`JOURNAL_SQL_TEST_PASSWORD` and `JOURNAL_SQL_TEST_SCHEMA=recovery_tuple_test`.

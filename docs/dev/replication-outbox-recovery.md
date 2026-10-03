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

The tuple storage implementation is maintained separately. Its matching change
must be applied before considering end-to-end outage recovery complete:

1. Add `boolean recoverable` to a `markFailed` overload on
   `TupleReplicationOutboxService` and its implementation. Keep the old overload
   delegating with `false` for compatibility. The dispatcher passes the typed
   classification above and computes backoff from the previous attempt count.
2. Saturate attempts at `Integer.MAX_VALUE`. When the threshold is reached or the
   job was already failed, persist `FAILED`; persist `now + retryDelay` for a
   recoverable error and null otherwise. Never update completed or cancelled
   rows, and fence completion with the journal's current claim token.
3. Include due `FAILED` rows only when `next_attempt_at IS NOT NULL`. Apply the
   same predicate during the atomic claim, not only in the candidate query.
   Sort by next attempt and creation time and preserve the configured batch limit.
4. Keep the failed alert visible during claims. Use the journal's claim token
   and lease independently of alert status; active leases must exclude concurrent
   scheduled and manual attempts. A crashed claim must become eligible when its
   lease expires. Do not set an exhausted job to `PENDING` on claim/retry.
5. Retain successful and cancelled journal rows as audit evidence, rather than
   deleting them. Manual retry must never claim cancelled or successful rows.

The durable eligibility predicate, combined with lease eligibility, is:

```sql
WHERE (status = 'PENDING' AND next_attempt_at <= ?)
   OR (status = 'FAILED' AND next_attempt_at IS NOT NULL AND next_attempt_at <= ?)
   OR (status = 'PROCESSING' AND last_modified <= ?)
ORDER BY next_attempt_at, created
LIMIT ?
```

Tests should cover 25 transport failures followed by recovery, a process restart,
a stale claim, concurrent claims, permanent HTTP errors, and dependency waits.
No receiver acknowledgement may substitute for a durable journal write.

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

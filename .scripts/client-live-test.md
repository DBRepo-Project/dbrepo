# Live client checks for any DBRepo instance

The runner has two profiles. `user` checks the workflow available to an ordinary account. `full` adds administrative metadata operations, internal Search/Dashboard synchronization and optional AMQP ingestion. Both create their own uniquely named database and synthetic data. They do not update existing customer databases, tables, images or container registrations.

## Install

Use Python 3.11 or later and the client version you intend to evaluate. From this checkout:

```sh
python3.11 -m venv .venv-live
.venv-live/bin/python -m pip install ./lib/python
```

For a comparison, use the same installed client and runner on both targets. Reports include the client source hash, advertised frontend build and runtime library versions. The frontend build alone does not prove every backend service's version.

## Ordinary user check

```sh
python .scripts/client-live-test.py \
  --endpoint https://YOUR-CUSTOMER-INSTANCE \
  --profile user --username YOUR-USERNAME
```

The password is prompted without echo. For CI, use `--password-env DBREPO_TEST_PASSWORD`. Add `--peer-username SECOND-USER` and optionally `--peer-password-env DBREPO_PEER_PASSWORD` to test grants, writes, revocation and ownership transfer. Without a second account, those multi-user cases are explicitly outside the selected profile.

The user profile does not call admin or internal system endpoints, even if such credentials happen to be present in the configuration. It reads the owner's profile but does not update it. With no owner Bearer token or OIDC client settings, Search is tested against the run's synthetic database while that database is temporarily public; it is then made private again. No existing customer dataset is made public.

## Generate two accounts instead

Where ordinary self-registration is enabled:

```sh
python .scripts/client-live-test.py \
  --endpoint https://YOUR-TEST-INSTANCE --profile user --register-users
```

This explicitly registers a random owner and peer using the normal public Keycloak registration form. It does not use an administrator, add roles, set hidden admin attributes or bypass verification. The accounts' passwords are saved in a local file under `.client-live-credentials/`, with mode 0600; they are not printed. `--credentials-output PATH` selects another new file, which must not already exist. Reuse that file with `--config PATH --profile user` on later runs to avoid registering additional accounts.

By default generated emails use `example.invalid`. Use `--email-domain YOUR-TEST-MAIL-DOMAIN` if your setup requires deliverable email. CAPTCHA, required email verification, external identity providers or disabled registration can require manual account setup. The runner reports the blocked setup; it does not bypass those controls. On customer instances, registration is never automatic unless `--register-users` is supplied.

Freshly generated accounts also attempt normal Grafana OAuth login, which may be needed before access synchronization. With existing accounts, enable that preparation explicitly with `--prepare-grafana`. Its confirmed or unconfirmed state is recorded in the report.

## Full check

Copy `client-live-full.example.json` to a protected location, set the target endpoint and connection details, and provide its password environment variables:

```sh
python .scripts/client-live-test.py \
  --config /private/path/customer.json --profile full --prepare-grafana
```

The owner and peer are normal accounts. The optional admin account needs the image, container and maintenance roles. The optional system credentials are the application's internal account. OIDC client settings permit authenticated private Search checks. AMQP uses the owner's credentials and tests the actual `AmqpClient` and Consumer. A missing privileged account or service connection produces skipped cases, not a successful complete check.

`test_container` registers a new metadata entry for the supplied engine connection, then deletes only that new registration. Supply both privileged and dedicated read-only credentials, as shown in the full example. Read-only and privileged usernames must differ; an existing read-only account needs its current plaintext password. Image and maintenance tests also mutate only objects created by this run. A maintenance message is scheduled for the future and deleted. These are additional checks of shared administration features; use the `user` profile when only normal account access is available.

Use one configuration per target. Overriding a different configured endpoint is rejected, so internal credentials cannot accidentally be sent to another instance. `password_env` is supported per role, and `client_secret_env` under `oidc`. A `token` per role, or a separate `search` token configuration, is also supported. A username/password is needed for account-profile and AMQP operations.

## Publication and external metadata

The full profile includes explicit opt-ins:

```sh
python .scripts/client-live-test.py --config /private/path/customer.json \
  --profile full --publish --external-doi YOUR_TEST_DOI
```

`--publish` creates a separate synthetic database identifier, temporarily makes only the synthetic test database public, publishes the local identifier and checks OAI GetRecord. The identifier is immutable and remains after the run. The editable subset draft receives its own deletion test. On a DOI-enabled server, earlier draft creation can already contact DataCite. Use a provider stub or an approved sandbox; `--publish` is not a switch that disables every external write.

`--external-doi` calls the application's external metadata retrieval endpoint. Its DOI handler supports Crossref funder identifiers, such as `10.13039/501100000780`; a journal article DOI is not a valid success fixture for that handler. It can contact the external providers configured by that application. Both flags are inactive by default. The user profile excludes these publication/provider cases.

## Three-site replication checks

The same runner can invoke `replication-acceptance.py` with a private three-site
configuration. See [replication acceptance](../dbrepo-replication-service/ACCEPTANCE.md)
for account roles, SQL observers and scoped fault hooks. Credentials are read from
the environment variables named in that configuration.

```sh
python .scripts/client-live-test.py \
  --replication-only --replication-config /private/path/three-sites.json \
  --replication-allow-faults --output replication-report.json
```

This creates synthetic databases and checks replica activation, tuple changes,
read-only enforcement, historical subsets, duplicate and reordered delivery,
archive behavior and recovery. Fault hooks must affect only the new fixtures and
have a bounded expiry. Missing required hooks or incomplete observations produce
`BLOCKED`, never `PASS`. Reports must use a new path and retain fixture IDs for
inspection; immutable historical records remain after the run.

To run both suites, add `--replication-config` to a normal `user` or `full` run.
Use `--table-deletion archive` against the replication branch. The client report
links to a separate `-replication.json` report. Exit code 1 means at least one
failure; 2 means incomplete coverage or a blocked check; 0 requires both suites
to pass. Run again with each site selected as primary to cover all directions.

## Compare instances

Run the same profile twice, with target-specific credentials or explicit registration:

```sh
python .scripts/client-live-test.py --endpoint https://BASELINE-INSTANCE \
  --profile user --username BASELINE-USER --output baseline.json
python .scripts/client-live-test.py --endpoint https://CANDIDATE-INSTANCE \
  --profile user --username CANDIDATE-USER --output candidate.json
python .scripts/client-live-compare.py baseline.json candidate.json --output client-live-comparison.json
```

The comparison separates matching failure signatures, different failures, candidate-only failures, baseline-only failures, passes and incomparable/skipped cases. It rejects a comparison based on a different client or profile. A skipped case is never counted as a pass. A failure on both targets is evidence of shared behavior, while code history and deployment configuration are still needed to establish its age and cause.

If the baseline cannot create its own database, dependent operations remain untested there. Their success or failure on the candidate does not establish a regression. A server can fail after partially performing a write; inspect the recorded run prefix if a setup operation failed before returning an ID.

## Reports and cleanup

The named cases `catalog.get_units`, `catalog.get_ontologies` and `catalog.get_concepts`
check the current SDK's removed-method contract. Each must raise `NotImplementedError`
with guidance to `get_table()` and `update_table_column()` for `concept_uri/unit_uri`,
without sending an HTTP request. Reports label these assertions with
`contract: removed_sdk_method`. A pass confirms the documented removal; it does
not count as a successful catalog read or HTTP coverage. An unexpected return,
another exception, missing guidance or an HTTP request fails the case. Other SDK
methods keep their normal success checks. Use the same client and runner for both
targets; old reports retain their original outcomes.

The supported replacement is checked on a column in the run's own synthetic table:
write nonempty concept and unit URIs, read both back, clear both and confirm the
cleared values through another table read. These checks use `example.invalid`
URIs and require no registry or external ontology service. The added readback and
clear cases increase the standard case count from 144 to 147.

The default report name includes hostname, profile and UTC timestamp. `--output PATH` selects a file. After each case the JSON file is replaced atomically. It contains case outcomes, HTTP statuses, durations, API error codes, validation field locations, coverage, resource IDs and setup status. Passwords, tokens and request/response bodies are not written to it.

Exit codes are `0` for a completed passing selected profile, `1` when a case fails, and `2` for blocked setup or incomplete selected coverage. `not_in_profile` distinguishes excluded profile scope from skipped required cases. `completed` distinguishes a finished run from a partial report. Multiple failed cases can share one underlying cause.

`--keep` retains the run's table, view and editable identifier. Normal cleanup deletes those objects and restores the database's private visibility. The REST API exposes no database deletion endpoint, so empty test databases remain. Uploads, query-store entries and dashboards without an applicable deletion endpoint may also remain. Interrupted runs can retain additional synthetic resources. The report identifies the run prefix and returned IDs.

TLS verification stays enabled. `--ca-bundle /path/company-ca.pem` supports private customer CAs. HTTP requests have connection/read timeouts and disable automatic redirects. The registration helper only follows same-origin authentication links. AMQP runs in a bounded child process. These checks do not establish complete input, load, concurrency or browser coverage.

## Endpoint inventory and offline tests

The main catalogue contains 99 operations, covering literal Spring/Flask API mappings, separate OAI verbs, gateway health and PID routes. Two non-routed internal Data operations are explicitly excluded. All 62 public RestClient methods are listed. Framework-generated OPTIONS/HEAD variants, third-party administration, UI routes, metrics and replication APIs absent from main are outside this HTTP catalogue. The user profile further excludes privileged lifecycles and optional connections that were not configured.

Existing client methods are called as shipped. Missing methods use the client's transport. A successful API write followed by a client DTO error stays failed; a returned new resource ID is retained so independent tests can continue. There is no client/server patch to turn a failed contract into a pass.

```sh
python .scripts/test-client-live-test.py
python .scripts/client-live-test.py --check-source .
```

These commands require neither a live instance nor Docker. Review newly introduced endpoints and their scenarios before regenerating the catalogue. The runtime report shows which operations actually received requests.


## Table deletion and archival

The runner defaults to main's physical-deletion contract. For an integration build
that archives tables, select the contract before running:

```sh
python .scripts/client-live-test.py --endpoint https://ARCHIVAL-INSTANCE \
  --profile user --username TEST-OWNER --table-deletion archive --output archive.json
```

The same setting is available as `"table_deletion": "archive"` in a protected JSON
configuration. The report records the selected contract. No response-based fallback
is used. Physical deletion requires HTTP 404 and absence from the active list.
Archival requires the same table ID, a valid archive timestamp and absence from the
active list. A second deletion must retain the timestamp, a new write must return
HTTP 403, and the saved historical subset and draft identifier must still return
their original rows. The draft is deleted after these checks. A missing historical
fixture remains skipped, so it cannot produce a clean acceptance result.

These checks exercise historical query and identifier reads. Published immutable
citations require the separately approved publication fixture. Selecting archival
does not enable publication. The comparison tool marks differing deletion contracts
explicitly instead of calling a contract difference a regression. Other cases remain
comparable when the client and profile match. Batch lookup compatibility is described
in [live-check contracts](../docs/developer-guide/live-check-contracts.md).

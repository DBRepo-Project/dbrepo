# Live-check contracts

Use the same client and runner on both targets. Record each backend image digest;
the version shown by the frontend does not identify the Metadata-Service build.
Keep unsupported operations visible in the report instead of counting them as
successful requests.

## Database batch lookup

`GET /api/v1/database/batch?id=UUID&id=UUID` is available in main builds containing
commit `e42d312e9` (PR #163). Earlier builds without that change do not support
this endpoint. A release version alone is insufficient for unreleased or mixed
deployments; verify the Metadata-Service source revision or image provenance.

The endpoint accepts 1 to 100 IDs, removes duplicates and returns the requested
databases that the caller may read. Missing and inaccessible databases are omitted.
The result contains full database metadata; its order follows the first occurrence
of each requested ID. Invalid UUIDs or an invalid raw number of IDs return HTTP 400.

Older builds can route the literal `batch` to `/database/{databaseId}` and reject
it as an invalid UUID with HTTP 400. This explains a comparison with HTTP 200 on
main without establishing a regression or an additional fix in that comparison.
For these older versions, use the supported individual database reads. A fallback
must not be reported as successful batch-endpoint coverage.

The live `database.batch` check must request its own synthetic database and assert
that the returned list contains that database. Do not accept an arbitrary HTTP 200
or silently switch to individual reads after an error. The server regression suite
also covers multiple IDs, duplicate IDs, missing IDs, current permissions and the
literal HTTP route.

## Identifier metadata updates

Metadata updates retain the identifier's existing DOI in both the default and
`doi` profiles. Omitting `doi` or sending JSON null leaves it unchanged. A supplied
DOI must equal the existing value; this endpoint cannot assign, replace or remove
DOIs. The body ID must match the URL and the identifier must remain in its database.

An existing local draft without a DOI remains local when its metadata is edited,
including with the `doi` profile enabled. No provider update is sent. DOI creation
and publication keep their separate workflows. Draft creation on a DOI-enabled
server can already contact DataCite, even without the runner's `--publish` flag.
Use a provider stub or an approved sandbox for those tests.

The DOI-profile persistence tests stub the provider transport and verify the exact
resource and request ID, local readback and rollback on a provider failure. They
must run alongside the default-profile tests; disabling DOI does not exercise the
provider update path.

## Table removal

The portable runner and its offline tests are in `.scripts/client-live-test.py`
and `.scripts/test-client-live-test.py`. Select `--table-deletion physical` for main
or `--table-deletion archive` for an integration build with commit `f39b2cbb9`.
The default is physical deletion. See the [runner guide](../../.scripts/client-live-test.md)
for required fixtures and historical-read assertions. The selected contract is
recorded, and a mismatch cannot silently change the expected outcome.

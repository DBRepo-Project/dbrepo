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

# Search Service

## Actuator

- Health: http://localhost:5000/api/search/health
- Prometheus: http://localhost:5000/metrics

## Swagger UI Endpoints

- Swagger UI: http://localhost:5000/swagger-ui/

## OpenAPI Endpoints

- OpenAPI v3 as .json: http://localhost:5000/api-search.json

## Rebuilding the database index

The setup job fetches the catalog from the Metadata Service, writes it to a new OpenSearch index, and checks the document count before switching the `database` alias. Existing installations can start with a concrete index named `database`. The first successful rebuild replaces it with the alias in one OpenSearch request. Allow enough disk space for both generations during a rebuild.

When upgrading, roll out the Search Service writers before running the new setup job. An older writer does not check for a rebuild in progress and can miss the first switch. If both versions must run at the same time, pause catalog writes until that rebuild finishes.

After the writers have been upgraded, a catalog write during import makes the job discard its snapshot. The next run fetches the catalog again. If OpenSearch cannot confirm a switch or cleanup, check which index the `database` alias points to before removing an unused generation. Each rebuild creates one empty, single-shard index named `dbrepo-search-guard-<generation>`. The setup job removes it in the same OpenSearch request that switches the alias. A catalog writer can cancel a stale rebuild by deleting its guard. Give writers permission to resolve the catalog and guard names and delete indices under this guard prefix.

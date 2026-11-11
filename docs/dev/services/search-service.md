---
author: Martin Weise
---

## tl;dr

!!! debug "Debug Information"

    Image: [`ghcr.io/dbrepo-project/dbrepo/search-service:1.11.0`](https://github.com/orgs/DBRepo-Project/packages)

    * Health: `http://<container_ip>:8080/api/search/health`
    * Prometheus: `http://<container_ip>:8080/metrics`
    * Swagger UI: `http://<container_ip>:8080/swagger-ui/`

    To directly access in Kubernetes (for e.g. debugging), forward the svc port to your local machine:

    ```shell
    kubectl [-n namespace] port-forward svc/search-service 8080:80
    ```

## Overview

This service communicates between the Search Database and the [User Interface](/infrastructures/dbrepo/1.13/dev/services/ui)
to allow structured search of databases, tables, columns, users, identifiers, views, semantic concepts &amp; units of 
measurements used in databases.

<figure markdown>
![Built-in search](/infrastructures/dbrepo/1.13/images/screenshots/feature-search.png)
</figure>

## Index

The
index [`database`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/dev/dbrepo-search-db/init/indices/database.json)
holds mirrored metadata from
the [Metadata Database](/infrastructures/dbrepo/1.13/dev/services/metadata-db).

<figure markdown>
![Mirroring statistical properties in Metadata Database and Search Database](/infrastructures/dbrepo/1.13/images/statistics-mirror.png)
</figure>

## Faceted Browsing

This service enables the frontend to search the `database` index with eight different *types* of desired results
(database, table, column, view, identifier, user, concept, unit) and their *facets*.

For example, the [User Interface](/infrastructures/dbrepo/1.13/dev/services/ui/) allows for the search of databases that contain a certain
semantic concept (provided as URI, e.g.
temperature [http://www.wikidata.org/entity/Q11466](http://www.wikidata.org/entity/Q11466)) and unit of measurement
(provided as URI, e.g. degree
Celsius [http://www.ontology-of-units-of-measure.org/resource/om-2/degreeCelsius](http://www.ontology-of-units-of-measure.org/resource/om-2/degreeCelsius)).

GET `/api/v1/search/{type}` and POST `/api/v1/search/{type}` return JSON arrays of the requested type.
Database results contain database objects; table, column and view results contain those objects. Identifier results include
records attached to databases, subsets, tables and views. User results contain database owners. Concept and unit results
contain a `uri`, with names and descriptions when Metadata supplies them.

For a unit-independent range search, send `tables.columns.concept.uri`, `tables.columns.unit.uri`, `t1` and `t2` in the POST
body. Bounds may also be query parameters; the shorter `concept.uri` and `unit.uri` keys remain accepted. Both bounds must
be finite numbers in ascending order. A column matches when its entire recorded minimum-to-maximum range falls within the
requested range after conversion. Concept, unit and statistics must belong to that same column.

The service supports compatible non-temperature OM unit conversions and absolute Celsius, Kelvin and Fahrenheit conversions for their
exact OM URIs. Missing statistics, incompatible units and unsupported conversions do not match. Concept and unit URI
values can also be compared directly without a conversion.

## Limitations

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](/infrastructures/dbrepo/1.13/contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Response permissions

Search result bodies use current Metadata Service responses with the caller's credentials. Anonymous requests send no
credentials. Nested schemas, access lists and identifier drafts follow the Metadata Service's visibility rules.

Each search lists the databases currently visible to the caller, then fetches their current metadata in batches of up to
100 IDs. Matching and result counts use those responses. The mirrored index does not select candidates or determine their
order. A grant or metadata change is reflected by the next request; the list and batches are separate reads, not a single
repository-wide snapshot. Missing or inaccessible databases are omitted by the batch endpoint.

Search sends the original Authorization header on both Metadata requests and sends none for anonymous callers.
Environment credentials and netrc are disabled for these requests. A Metadata endpoint URL containing user credentials
is rejected. Metadata timeouts, malformed responses and other backend failures return HTTP 503.

Upgrade the Metadata Service before the Search Service: the new Search Service requires `/api/v1/database/batch`.
During a mixed-version rollout, an older Metadata Service without this route causes search requests to return HTTP 503.
Search does not fall back to indexed permissions.

## Result order and pages

Results use a stable order by ID, or by URI for concepts and units, after permission checks and projection. User results
use their ID or username. This replaces relevance-score ordering. Repeated values remain repeated; ties use visible
result content. A page beyond the last result is empty.

* GET `/api/v1/search/{type}` defaults to page 1 with 50 results.
* POST `/api/v1/search/{type}` and fuzzy GET `/api/v1/search?q=...` return all matches when pagination is omitted.
* All three routes accept `page` and `results_per_page`. Page numbers start at 1; page sizes are 1 to 500. If only one
  parameter is supplied, the other defaults to page 1 or size 50. Invalid values return HTTP 400.
* `X-Count` reports the complete number of authorized projected results, including results outside the requested page.
  Paginated responses also set `X-Page`, `X-Page-Size` and `X-Total-Pages`. These headers are exposed to browser callers.

There is no fixed 1,000-candidate limit. Large requests can still exceed the processing budget and fail with HTTP 503;
results are not returned as a successful partial page.

## Native matching and temporary queries

OpenSearch evaluates the request against inline current Metadata bodies using a dedicated `dbrepo-search-queries`
percolator index. Database payloads are not written to that index. Search response objects keep their documented DTO
shape; current fields outside that shape can still match if their field mapping is available. Fuzzy search covers
source-mapped fields and supported Database DTO fields. Explicit fields absent from the prepared mapping do not match,
including fields supplied only as extra Metadata body properties. Arbitrary never-mapped fields are not automatically included in fuzzy search.

The query index copies source field mappings and fills missing DTO types. Established query-index types remain stable
when the source mapping later gains fields. Incompatible payloads or unsupported queries fail explicitly. The database
field-list route reads only the `database` index.

The query DSL itself is temporarily stored with a request UUID and a five-minute expiry. Each request deletes its query
in a `finally` block, including an uncertain write response. Later matching requests delete at most 100 expired query
documents before registering a new query. Active queries are isolated by UUID and expiry. Missing or expired queries,
partial shard responses and native timeouts are errors, not empty matches.

A crash, failed delete or prolonged outage can leave query documents until a later healthy request performs cleanup;
an idle service does not clean them automatically. Deletion does not promise physical erasure from storage or backups.
Operators should treat this index as containing temporary user search terms.

Matching has a 30-second monotonic budget checked between operations. Metadata calls have bounded connect/read timeouts;
OpenSearch calls have bounded request timeouts and no automatic retries. Final deletion can take up to five additional
seconds. These checks do not impose a strict total wall-clock limit on a slowly streaming HTTP body.

The Metadata batch route is GET `/api/v1/database/batch?id=<uuid>&id=<uuid>`. It accepts 1 to 100 IDs before deduplication,
uses the same permissions and response filtering as a single database read, and omits missing or forbidden IDs. A maximum
batch produces a URL of roughly 4 KB. Database brief responses include published identifiers and drafts owned by the
caller; database ownership or membership alone does not expose another user's draft. Trusted system behavior is unchanged.

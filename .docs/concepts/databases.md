---
author: Martin Weise
---

## Relational Database

DBRepo manages relational databases that store information relations in tables.

## Query

A query is the method to interact with a relational database and is used to read/write data or to create/change/delete
schema information e.g. tables. DBRepo uses a query store to store certain (important) queries that generate subsets
to restore the exact same subset at a later point.

## System Versioning

DBRepo uses a mechanism offered by SQL:2013 to version tables with the system (=server) time. When inserting a tuple
into a system-versioned table, the database engine maintains invisible `ROW_START` and `ROW_END` timestamp columns to
denote a tuple validity. When deleting a tuple, the database engine actually just marks the tuple as `ROW_END = NOW()`
and does not delete the tuple.

At a later point in time, the (historic) tuple can still be queried using system versioning.

## Data Ingest

<figure markdown>
![Data ingest](../images/data-ingest.svg)
<figcaption>Figure 1: Modes of data ingest</figcaption>
</figure>

More [usage examples](../usage-overview/) include how to ingest datasets, data dumps, live data, etc.

### Generation of Metadata in DBRepo

You can generate metadata e.g. UI tbd

!!! warning "Limitation"

    Only system-versioned tables are considered when generating metadata to tables. If your table is not system-versioned
    e.g. a base table, it will not be visible in the UI.
---
author: Martin Weise
---

## tl;dr

[:fontawesome-solid-database: &nbsp;Dataset](https://handle.test.datacite.org/10.82556/t5ay-3e73){ .md-button .md-button--primary target="_blank" }
[:material-file-document: &nbsp;Archive](https://doi.org/10.48436/yaecs-dgr27){ .md-button .md-button--secondary target="_blank" }

## Description

This data base contains concentrations of hazardous substances and other water quality parameters in different
environmental compartments:

* river water (water and suspended sediments)
* ground water
* waste water (treated and untreated) and sewage sludge
* storm water runoff from combined and separate sewer systems
* atmospheric deposition
* soil

Data from many different data sources were collected, checked and combined and meta data were harmonized to allow for
a combined data evaluation.

## Solution

We imported the database from the [archive](https://doi.org/10.48436/yaecs-dgr27) repository, converted the PostgreSQL
language code to MariaDB:

* `"current_user"()` to `current_user()`
* `SEQUENCE CACHE 1` to `NOCACHE` because of MariaDB Galera distribution not being able to cache sequences
* `(0)::double precision` to `0.0`
* `character varying` to `text` because MariaDB needs sizes, `text` data type can have arbitrary size
* `!~~` to `NOT LIKE`
* `ANY ARRAY` to `<array> OR <array2>` because of different MariaDB syntax

The complex nature of the views (i.e. `SELECT-FROM-SELECT` statements) required a logical overhaul of how DBRepo obtains
view metadata. As a consequence, we removed the SQL parser that tried to parse the metadata up to a depth of 10 to a
simple query to the `information_schema` that is maintained by the database engine.

## DBRepo Features

- [x] Complex database schema
- [x] Complex views
- [x] System versioning
- [x] Subset exploration

## Acknowledgement

This work was part of a cooperation with the [Institute of Water Quality and Resource Management](https://www.tuwien.at/cee/iwr).

<img src="/infrastructures/dbrepo/images/logos/iwr.png" width=100 />
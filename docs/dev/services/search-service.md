---
author: Martin Weise
---

## tl;dr

!!! debug "Debug Information"

    Image: [`registry.datalab.tuwien.ac.at/dbrepo/search-service:1.11.0`](https://hub.docker.com/r/dbrepo/search-service)

    * Health: `http://<container_ip>:8080/api/search/health`
    * Prometheus: `http://<container_ip>:8080/metrics`
    * Swagger UI: `http://<container_ip>:8080/swagger-ui/`

    To directly access in Kubernetes (for e.g. debugging), forward the svc port to your local machine:

    ```shell
    kubectl [-n namespace] port-forward svc/search-service 8080:80
    ```

## Overview

This service communicates between the Search Database and the [User Interface](/infrastructures/dbrepo/1.10/dev/services/ui)
to allow structured search of databases, tables, columns, users, identifiers, views, semantic concepts &amp; units of 
measurements used in databases.

<figure markdown>
![Built-in search](/infrastructures/dbrepo/1.10/images/screenshots/feature-search.png)
</figure>

## Index

There is only one
index [`database`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/dev/dbrepo-search-db/init/indices/database.json)
that holds all the metadata information which is mirrored from 
the [Metadata Database](/infrastructures/dbrepo/1.10/dev/services/metadata-db).

<figure markdown>
![Mirroring statistical properties in Metadata Database and Search Database](/infrastructures/dbrepo/1.10/images/statistics-mirror.png)
</figure>

## Faceted Browsing

This service enables the frontend to search the `database` index with eight different *types* of desired results
(database, table, column, view, identifier, user, concept, unit) and their *facets*.

For example, the [User Interface](/infrastructures/dbrepo/1.10/dev/services/ui/) allows for the search of databases that contain a certain
semantic concept (provided as URI, e.g.
temperature [http://www.wikidata.org/entity/Q11466](http://www.wikidata.org/entity/Q11466)) and unit of measurement
(provided as URI, e.g. degree
Celsius [http://www.ontology-of-units-of-measure.org/resource/om-2/degreeCelsius](http://www.ontology-of-units-of-measure.org/resource/om-2/degreeCelsius)).

## Limitations

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](/infrastructures/dbrepo/1.10/contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

(none)

---
author: Martin Weise
---

# Metadata Service

## tl;dr

!!! debug "Debug Information"

    Image: [`dbrepo/metadata-service:1.4`](https://hub.docker.com/r/dbrepo/metadata-service)

    * Ports: 9099/tcp
    * Info: `http://9093:9099/actuator/info`
    * Health: `http://9093:9099/actuator/health`
        - Readiness: `http://9093:9099/actuator/health/readiness`
        - Liveness: `http://9093:9099/actuator/health/liveness`
    * Prometheus: `http://9093:9099/actuator/prometheus`
    * Swagger UI: `http://9093:9099/swagger-ui/index.html` <a href="../swagger/metadata" target="_blank">:fontawesome-solid-square-up-right: view online</a>

## Overview

This service manages the following topics:

* Databases
* Identifiers (DataCite, OAI-PMH)
* Queries
* Semantics (Ontologies)
* Tables
* Users
* Views

### Databases

The service handles table operations inside a database. We use [Hibernate](https://hibernate.org/orm/) for schema and 
data ingest operations.

### Identifiers

The service is responsible for creating and resolving a *persistent identifier* (PID) attached to a query to
obtain the metadata attached to it and allow re-execution of a query. We store both the query and hashes of the query
and result set to allow equality checks of the originally obtained result set and the currently obtained result set. In
the reference implementation we currently only use a numerical id column and plan to integrate *digital object
identifier* (DOI) through our institutional library soon.

This service provides an OAI-PMH endpoint for metadata aggregators.

### Queries

It provides an interface to insert data into the tables. It also allows for view-only, paginated and versioned query 
execution to the raw data.

### Semantics

The service provides metadata to the table columns in the [Metadata Database](../system-databases-metadata) from
registered ontologies like Wikidata [`wd:`](https://wikidata.org), Ontology of Units of 
Measurement [`om2:`](https://www.ontology-of-units-of-measure.org/resource/om-2), Friend of a 
Friend [`foaf:`](http://xmlns.com/foaf/0.1/), the [`prov:`](http://www.w3.org/ns/prov#) namespace, etc.


### Tables

The service manages tables in the [Data Database](../system-databases-data) and manages the metadata of these tables
in the [Metadata Database](../system-databases-metadata).

### Users

The service manages users in the [Data Database](../system-databases-data) 
and [Metadata Database](../system-databases-metadata), as well as in the [Broker Service](../system-services-broker)
and the [Authentication Service](../system-services-authentication).

### Views

The service manages views in the [Data Database](../system-databases-data)
and [Metadata Database](../system-databases-metadata).

## Limitations

* No support for other databases than [MariaDB](https://mariadb.org/) because of system-versioning capabilities missing
  in other database engines.

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](../contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

(none)

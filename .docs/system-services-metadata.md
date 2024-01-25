---
author: Martin Weise
---

# Metadata Service

## tl;dr

!!! debug "Debug Information"

    Image: [`dbrepo/metadata-service:__APPVERSION__`](https://hub.docker.com/r/dbrepo/metadata-service)

    * Ports: 9099/tcp
    * Info: `http://<hostname>:9099/actuator/info`
    * Health: `http://<hostname>:9099/actuator/health`
        - Readiness: `http://<hostname>:9099/actuator/health/readiness`
        - Liveness: `http://<hostname>:9099/actuator/health/liveness`
    * Prometheus: `http://<hostname>:9099/actuator/prometheus`
    * Swagger UI: `http://<hostname>:9099/swagger-ui/index.html` <a href="../swagger/metadata" target="_blank">:fontawesome-solid-square-up-right: view online</a>

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

The service is responsible for creating and resolving a *persistent identifier* (PID) attached to a database, subset,
table or view to obtain the metadata attached to it and allow reproduction of the exact same result.

This service also provides an OAI-PMH endpoint for metadata aggregators 
(e.g. [OpenAIRE Graph](https://graph.openaire.eu/)). Through the User Interface, it also exposes metadata through
JSON-LD to metadata aggregators (e.g. [Google Datasets](https://datasetsearch.research.google.com/)). PID metadata
is always exposed, even for private databases.

The service generates internal PIDs, essentially representing internal URIs in 
the [DataCite Metadata Schema 4.4](https://doi.org/10.14454/3w3z-sa82). This can be enhanced with activating the 
external DataCite Fabrica system to generate DOIs, this is disabled by default. 

To activate DOI minting, pass your DataCite Fabrica credentials in the environment variables:

```yaml title="docker-compose.yml"
services:
  dbrepo-metadata-service:
    image: docker.io/dbrepo/metadata-service:1.4.0
    environment:
      spring_profiles_active: doi
      DATACITE_URL: https://api.datacite.org
      DATACITE_PREFIX: 10.12345
      DATACITE_USERNAME: username
      DATACITE_PASSWORD: password
  ...
```

### Queries

It provides an interface to insert data into the tables. It also allows for view-only, paginated and versioned query
execution to the raw data. Any stale queries (query that have been executed by users in DBRepo but were not saved) are
periodically being deleted from the query store based on the `DELETE_STALE_QUERIES_RATE` environment variable (defaults
to 60 seconds).

### Semantics

The service provides metadata to the table columns in the [Metadata Database](../system-databases-metadata) from
registered ontologies like Wikidata [`wd:`](https://wikidata.org), Ontology of Units of
Measurement [`om2:`](https://www.ontology-of-units-of-measure.org/resource/om-2), Friend of a
Friend [`foaf:`](http://xmlns.com/foaf/0.1/), the [`prov:`](http://www.w3.org/ns/prov#) namespace, etc.

### Tables

The service manages tables in the [Data Database](../system-databases-data) and manages the metadata of these tables
in the [Metadata Database](../system-databases-metadata). Any tables that are created outside of DBRepo (e.g. directly via the JDBC API) are
periodically fetched by this service (based on the `OBTAIN_METADATA_RATE` environment variable, default interval is 60
seconds).

### Users

The service manages users in the [Data Database](../system-databases-data)
and [Metadata Database](../system-databases-metadata), as well as in the [Broker Service](../system-services-broker)
and the [Authentication Service](../system-services-authentication).

The default configuration grants the users only very basic permissions on the databases:

* `SELECT`
* `CREATE`
* `CREATE VIEW`
* `CREATE ROUTINE`
* `CREATE TEMPORARY TABLES`
* `LOCK TABLES`
* `INDEX`
* `TRIGGER`
* `INSERT`
* `UPDATE`
* `DELETE`

This configuration is passed as environment variable `GRANT_PRIVILEGES` to the service as comma-separated string. You
can add/remove grants by setting this environment variable, e.g. allow the users to only select data and create
temporary tables:

```yaml title="docker-compose.yml"
services:
  dbrepo-metadata-service:
    environment:
      GRANT_PRIVILEGES=SELECT,CREATE TEMPORARY TABLES
      ...
```

A list of all grants is available in the MariaDB documentation for [`GRANT`](https://mariadb.com/kb/en/grant/)

### Views

The service manages views in the [Data Database](../system-databases-data)
and [Metadata Database](../system-databases-metadata). Any views that are created outside of DBRepo (e.g. directly via 
the JDBC API) are periodically fetched by this service (based on the `OBTAIN_METADATA_RATE` environment variable,
default interval is 60 seconds).

## Limitations

* No support for other databases than [MariaDB](https://mariadb.org/) because of system-versioning capabilities missing
  in other database engines.

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](../contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

(none)

---
author: Martin Weise
---

## tl;dr

!!! debug "Debug Information"

    Image: [`registry.datalab.tuwien.ac.at/dbrepo/metadata-service:1.4.4`](https://hub.docker.com/r/dbrepo/metadata-service)

    * Ports: 9099/tcp
    * Info: `http://<hostname>:9099/actuator/info`
    * Health: `http://<hostname>:9099/actuator/health`
        - Readiness: `http://<hostname>:9099/actuator/health/readiness`
        - Liveness: `http://<hostname>:9099/actuator/health/liveness`
    * Prometheus: `http://<hostname>:9099/actuator/prometheus`
    * Swagger UI: `http://<hostname>:9099/swagger-ui/index.html`

## Overview

The metadata service manages metadata of identities, the [Broker Service](../broker-service) (i.e. obtaining queue
types), semantic concepts (i.e. ontologies) and relational metadata (databases, tables, queries, views) and identifiers.

## Generation

Most of the metadata available in DBRepo is generated automatically, leveraging the available information and taking
the burden away from researchers, data stewards, etc. For example, the schema (names, constraints, data length) of
generated tables and views is obtained from the `information_schema` database maintained by MariaDB internally.

## Identifiers

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
    image: registry.datalab.tuwien.ac.at/dbrepo/metadata-service:1.4.4
    environment:
      spring_profiles_active: doi
      DATACITE_URL: https://api.datacite.org
      DATACITE_PREFIX: 10.12345
      DATACITE_USERNAME: username
      DATACITE_PASSWORD: password
  ...
```

## Semantics

The service provides metadata to the table columns in the [Metadata Database](../metadata-db) fromregistered ontologies
like Wikidata [`wd:`](https://wikidata.org), Ontology of Units of
Measurement [`om2:`](https://www.ontology-of-units-of-measure.org/resource/om-2), Friend of a
Friend [`foaf:`](http://xmlns.com/foaf/0.1/), the [`prov:`](http://www.w3.org/ns/prov#) namespace, etc.

## Limitations

* No support for other databases than [MariaDB](https://mariadb.org/) because of system-versioning capabilities missing
  in other database engines.

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](../../contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

(none)

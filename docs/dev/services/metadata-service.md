---
author: Martin Weise
---

## tl;dr

!!! debug "Debug Information"

    Image: [`registry.datalab.tuwien.ac.at/dbrepo/metadata-service:1.11.0`](https://hub.docker.com/r/dbrepo/metadata-service)

    * Info: `http://<container_ip>:8080/actuator/info`
    * Health: `http://<container_ip>:8080/actuator/health`
        - Readiness: `http://<container_ip>:8080/actuator/health/readiness`
        - Liveness: `http://<container_ip>:8080/actuator/health/liveness`
    * Prometheus: `http://<container_ip>:8080/actuator/prometheus`
    * Swagger UI: `http://<container_ip>:8080/swagger-ui/index.html`

    To directly access in Kubernetes (for e.g. debugging), forward the svc port to your local machine:

    ```shell
    kubectl [-n namespace] port-forward svc/metadata-service 8080:80
    ```

## Overview

The metadata service manages metadata of identities, the [Broker Service](/infrastructures/dbrepo/1.12/broker-service) (i.e. obtaining queue
types), semantic concepts (i.e. ontologies) and relational metadata (databases, tables, queries, views) and identifiers.

## Generation

DBRepo generates metadata for managed tables automatically by querying MariaDB's internal structures 
(e.g. `information_schema`). 

!!! info "Managed Tables"

    DBRepo only manages system-versioned tables, other tables are not supported. These other tables are ignored by
    DBRepo and thus can co-exist in the same database. If you want a non-system-versioned table `my_table` to be managed
    by DBRepo, make it system-versioned:

    ```sql
    ALTER TABLE `my_table` ADD SYSTEM VERSIONING;
    ```

    Then, refresh the managed table index by navigating to your database > Settings > Schema > Refresh. This action can
    only be performed by the database owner.

## Identifiers

The service is responsible for creating and resolving a *persistent identifier* (PID) attached to a database, subset,
table or view to obtain the metadata attached to it and allow reproduction of the exact same result.

The service generates internal PIDs, essentially representing internal URIs in
the [DataCite Metadata Schema 4.4](https://doi.org/10.14454/3w3z-sa82). This can be enhanced with activating the
external DataCite Fabrica system to generate DOIs, this is disabled by default.

To activate DOI minting, pass your DataCite Fabrica credentials in the environment variables:

```yaml title="docker-compose.yml"
services:
  dbrepo-metadata-service:
    image: registry.datalab.tuwien.ac.at/dbrepo/metadata-service:1.11.0
    environment:
      spring_profiles_active: doi
      DATACITE_URL: https://api.datacite.org
      DATACITE_PREFIX: 10.12345
      DATACITE_USERNAME: username
      DATACITE_PASSWORD: password
  ...
```

## Semantics

The service provides metadata to the table columns in 
the [Metadata Database](/infrastructures/dbrepo/1.12/dev/services/metadata-db) from registered ontologies
like Wikidata [`wd:`](https://wikidata.org), Ontology of Units of
Measurement [`om2:`](https://www.ontology-of-units-of-measure.org/resource/om-2), Friend of a
Friend [`foaf:`](http://xmlns.com/foaf/0.1/), the [`prov:`](http://www.w3.org/ns/prov#) namespace, etc.

## Limitations

* No support for other databases than [MariaDB](https://mariadb.org/) because of system-versioning capabilities missing
  in other database engines.

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](/infrastructures/dbrepo/1.12/contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

(none)

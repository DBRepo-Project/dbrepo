---
author: Martin Weise
---

# Mirror Service

## tl;dr

!!! debug "Debug Information"

    Image: [`dbrepo/mirror-service:1.4`](https://hub.docker.com/r/dbrepo/mirror-service)

    * Ports: 9050/tcp
    * Info: `http://<hostname>:9050/actuator/info`
    * Health: `http://<hostname>:9050/actuator/health`
        - Readiness: `http://<hostname>:9050/actuator/health/readiness`
        - Liveness: `http://<hostname>:9050/actuator/health/liveness`
    * Prometheus: `http://<hostname>:9050/actuator/prometheus`
    * Swagger UI: `http://<hostname>:9050/swagger-ui/index.html` <a href="../swagger/mirror" target="_blank">:fontawesome-solid-square-up-right: view online</a>

## Overview

This service is responsible for synchronizing the [Metadata Database](../system-databases-metadata) with 
the [Search Database](../system-databases-search) and the user permissions of databases, tables, etc. with 
the [Broker Service](../system-services-broker). 

| Metadata DB         | &#8614; | Search DB     |
|---------------------|:-------:|---------------|
| `mdb_users`         |         | `/user`       |
| `mdb_view`          |         | `/view`       |
| `mdb_databases`     |         | `/database`   |
| `mdb_identifiers`   |         | `/identifier` |
| `mdb_concepts`      |         | `/concept`    |
| `mdb_columns`       |         | `/column`     |
| `mdb_tables`        |         | `/table`      |
| `mdb_units`         |         | `/unit`       |

## Limitations

* No support for cron-job like execution.
* No support for conditional updates in the [Search Database](../system-databases-search), updates occur in defined
  intervals.

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](../contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

(none)

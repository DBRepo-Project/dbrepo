---
author: Martin Weise
---

## tl;dr

!!! debug "Debug Information"

    Image: [`registry.datalab.tuwien.ac.at/dbrepo/data-service:1.13.0`](https://hub.docker.com/r/dbrepo/data-service)

    * Info: `http://<container_ip>:8080/actuator/info`
    * Health: `http://<container_ip>:8080/actuator/health`
        - Readiness: `http://<container_ip>:8080/actuator/health/readiness`
        - Liveness: `http://<container_ip>:8080/actuator/health/liveness`
    * Prometheus: `http://<container_ip>:8080/actuator/prometheus`
    * Swagger UI: `http://<container_ip>:8080/swagger-ui/index.html`

    To directly access in Kubernetes (for e.g. debugging), forward the svc port to your local machine:

    ```shell
    kubectl [-n namespace] port-forward svc/data-service 8080:80
    ```

## Overview

The Data Service is responsible for inserting AMQP tuples from the Broker Service into the Data DB
via [Spring AMQP](https://docs.spring.io/spring-amqp/reference/html/). To increase the number of consumers, scale the
Data Service up.

## Data Analytics

The Data Service uses [Apache Spark](https://spark.apache.org/), a open-source data analytics engine to load data
from/into the [Data Database](/infrastructures/dbrepo/1.13/data-db) with a wide range of open-source connectors. Retrieving data from a subset
internally generates a view with the 64-character hash of the query. This view is not automatically deleted currently.

The Data Service also uses [DuckDB](), a open-source column-oriented database for large-data analytics. It is used as
in-memory data type analytics database to suggest data types from a (big) dataset using the larger-than-memory feature.

## Caching

The Data Service uses [Caffeine](https://github.com/ben-manes/caffeine), a caching solution that is used to temporarily
cache the connection details from the [Metadata Service](/infrastructures/dbrepo/1.13/metadata-service) such that they don't have to be queried
everytime e.g. a sensor measurement is inserted. By default, this information is stored for 60 minutes. System
administrators can disable this behavior by setting `CREDENTIAL_CACHE_TIMEOUT=0` (cache is deleted after 0 seconds).

## Storage

The Data Service also is capable to upload files to the S3 backend. The default limit
of [`Tomcat`](https://spring.io/guides/gs/uploading-files#_tuning_file_upload_limits) in Spring Boot is configured to be
`2GB`. You can provide your own limit with setting `MAX_UPLOAD_SIZE`.

By default, the Data Service removes datasets older than 24 hours on a regular basis every 60 minutes. You can set the
`MAX_AGE` (in seconds) and `S3_STALE_CRON` to fit your use-case. You can disable this feature by setting `S3_STALE_CRON`
to `-`, this may lead to storage issues as no space will be available inevitably. Note
that [Spring Boot uses its own flavor](https://spring.io/blog/2020/11/10/new-in-spring-5-3-improved-cron-expressions#usage)
of cron syntax.

## Limitations

* Views in DBRepo can only have 63-character length (it is assumed only internal views have the maximum length of 64
  characters).
* Local mode of embedded processing of Apache Spark directly in the service using
  a [`local[2]`](https://spark.apache.org/docs/latest/#running-the-examples-and-shell) configuration.

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](/infrastructures/dbrepo/1.13/contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

(none)

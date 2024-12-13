---
author: Martin Weise
---

## tl;dr

!!! debug "Debug Information"

    Image: [`registry.datalab.tuwien.ac.at/dbrepo/data-service:1.4.7`](https://hub.docker.com/r/dbrepo/data-service)

    * Ports: 9093/tcp
    * Info: `http://<hostname>:9093/actuator/info`
    * Health: `http://<hostname>:9093/actuator/health`
        - Readiness: `http://<hostname>:9093/actuator/health/readiness`
        - Liveness: `http://<hostname>:9093/actuator/health/liveness`
    * Prometheus: `http://<hostname>:9093/actuator/prometheus`
    * Swagger UI: `http://<hostname>:9093/swagger-ui/index.html` <a href="../../rest/" target="_blank">:fontawesome-solid-square-up-right: view online</a>

    To directly access in Kubernetes (for e.g. debugging), forward the svc port to your local machine:

    ```shell
    kubectl [-n namespace] port-forward svc/data-service 9093:80
    ```

## Overview

The Data Service is responsible for inserting AMQP tuples from the Broker Service into the Data DB 
via [Spring AMQP](https://docs.spring.io/spring-amqp/reference/html/). To increase the number of consumers, scale the
Data Service up.

## Data Processing

The Data Service uses [Apache Spark](https://spark.apache.org/), a data engine to load data from/into 
the [Data Database](../data-db) with a wide range of open-source connectors. The default deployment uses a local mode of
embedded processing directly in the service until there exists 
a [Bitnami Chart](https://artifacthub.io/packages/helm/bitnami/spark) for Spark 4.

Retrieving data from a subset internally generates a view with the 64-character hash of the query. This view is not
automatically deleted currently.

## Limitations

* Views in DBRepo can only have 63-character length (it is assumed only internal views have the maximum length of 64 
  characters).
* Local mode of embedded processing of Apache Spark directly in the service using 
  a [`local[2]`](https://spark.apache.org/docs/latest/#running-the-examples-and-shell) configuration.

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](../../contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

(none)

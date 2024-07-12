---
author: Martin Weise
---

## tl;dr

!!! debug "Debug Information"

    Image: [`registry.datalab.tuwien.ac.at/dbrepo/data-service:1.4.5`](https://hub.docker.com/r/dbrepo/data-service)

    * Ports: 9093/tcp
    * Info: `http://<hostname>:9093/actuator/info`
    * Health: `http://<hostname>:9093/actuator/health`
        - Readiness: `http://<hostname>:9093/actuator/health/readiness`
        - Liveness: `http://<hostname>:9093/actuator/health/liveness`
    * Prometheus: `http://<hostname>:9093/actuator/prometheus`
    * Swagger UI: `http://<hostname>:9093/swagger-ui/index.html` <a href="./swagger/data" target="_blank">:fontawesome-solid-square-up-right: view online</a>

## Overview

The Data Service is responsible for inserting AMQP tuples from the Broker Service into the Data DB 
via [Spring AMQP](https://docs.spring.io/spring-amqp/reference/html/). To increase the number of consumers, scale the
Data Service up.

## Limitations

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](../../contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

(none)

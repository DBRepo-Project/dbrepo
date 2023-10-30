---
author: Martin Weise
---

# Analyse Service

## tl;dr

!!! debug "Debug Information"

    Image: [`dbrepo/analyse-service:latest`](https://hub.docker.com/r/dbrepo/analyse-service)

    * Ports: 5000/tcp
    * Prometheus: `http://<hostname>:5000/metrics`
    * Health: `http://<hostname>:5000/health`
    * Swagger UI: `http://<hostname>:5000/swagger-ui/index.html` <a href="../swagger/analyse" target="_blank">:fontawesome-solid-square-up-right: view online</a>

## Overview

It suggests data types for the FAIR Portal when creating a table from a *comma separated values* (CSV) file. It
recommends enumerations for columns and returns e.g. a list of potential primary key candidates. The researcher is able
to confirm these suggestions manually. Moreover, the *Analyze Service* determines basic statistical properties of
numerical columns.

## Limitations

* No support for authentication

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](../contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

1. Since authentication is not supported, use IP-based ingress rules to limit access to the upload endpoint.

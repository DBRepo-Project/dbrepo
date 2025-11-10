---
author: Martin Weise
---

## tl;dr

!!! debug "Debug Information"

    Image: [`registry.datalab.tuwien.ac.at/dbrepo/dashboard-service:1.10.0`](https://hub.docker.com/r/dbrepo/dashboard-service)

    * Health: `http://<container_ip>:8080/api/dashboard/health`
    * Prometheus: `http://<container_ip>:8080/metrics`
    * Swagger UI: `http://<container_ip>:8080/swagger-ui/`

    To directly access in Kubernetes (for e.g. debugging), forward the svc port to your local machine:

    ```shell
    kubectl [-n namespace] port-forward svc/dashboard-service 8080:80
    ```

## Overview

tbd

!!! example "Example"

    More info on how to customize database dashboards can be found in
    the [User Guide](/infrastructures/dbrepo/1.12/user-guide/database-dashboard/).

## Limitations

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](/infrastructures/dbrepo/1.12/contact/) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

(none)

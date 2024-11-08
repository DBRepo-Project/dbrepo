---
author: Martin Weise
---

## tl;dr

!!! debug "Debug Information"

    Image: [`docker.io/bitnami/grafana:10.4.9-debian-12-r0`](https://hub.docker.com/r/bitnami/grafana)

    * Ports: 3000/tcp
    * UI: `http://<hostname>/dashboard`
        * Management UI: `http://<hostname>:3000` (see [Management](#management))
    * Prometheus: `http://<hostname>/dashboard/metrics`

    To directly access in Kubernetes (for e.g. debugging), forward the svc port to your local machine:

    ```shell
    kubectl [-n namespace] port-forward svc/dashboard-service 3000:3000
    ```

## Overview

The Dashboard Service is visualizing the status of DBRepo with charts. The default dashboard provisioner located in
`/etc/grafana/provisioning/dashboards/provider.yaml` checks for new `JSON` dashboard files in `/app/dashboards` every 10
seconds and makes the available in the Dashboard Service.

## Management

The Dashboard Service can be accessed with admin users (see [Identity Service](../../api/identity-service)). In this
case, access the UI via the port `3000` directly to avoid UI Redirects.

!!! bug "UI Redirects"

    It is a known bug [#460](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/460)
    that when being logged-in, the UI randomly redirects when being accessed from the Gateway Service, we therefore
    recommend to access port `3000` directly: `http://<hostname>:3000`. Anonmyous users are not affected.

## Limitations

* Unintended redirects when being logged-in (see above).

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](../../contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

(none)

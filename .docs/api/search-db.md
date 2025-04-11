---
author: Martin Weise
---

!!! debug "Debug Information"

    Image: [`docker.io/bitnami/opensearch:2.10.0`](https://hub.docker.com/r/bitnami/opensearch)

    * Ports: 9200/tcp

    To directly access in Kubernetes (for e.g. debugging), forward the svc port to your local machine:

    ```shell
    kubectl [-n namespace] port-forward svc/search-db 9200:9200
    ```

## Overview

The Search Database contains duplicate metadata from the Metadata Database for fast and efficient search.

## Configuration

This section will be expanded soon.

## Limitations

(none)

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](../contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

* By default, the security plugin is not used in the [Docker](../../installation) installation. This allows anyone to
  read/write and is considered not secure.

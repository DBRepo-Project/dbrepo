---
author: Martin Weise
---

!!! debug "Debug Information"

    Image: [`docker.io/bitnamilegacy/valkey:8.1.3`](https://hub.docker.com/r/bitnamilegacy/valkey)

    * Ports: 6379/tcp

    To directly access in Kubernetes (for e.g. debugging), forward the svc port to your local machine:

    ```shell
    kubectl [-n namespace] port-forward svc/cache-db 6379:6379
    ```

## Overview

The Cache Database contains cached metadata and credentials that automatically expire after a set time.

## Configuration

By default, all items stored have a time-to-live (TTL) of `60` seconds. This can be changed by setting `CACHE_TTL` in
the [Metadata Service](/infrastructures/dbrepo/1.12/metadata-service), 
[Data Service](/infrastructures/dbrepo/1.12/data-service)
and [Consumer Service](/infrastructures/dbrepo/1.12/consumer-service) according to 
the [Valkey documentation](https://valkey.io/commands/ttl/).

!!! warning "Disable Caching"

    Disable caching in a service by setting the environment variable `CACHE_TTL=-1`.

## Limitations

(none)

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](/infrastructures/dbrepo/1.12/contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

(none)

---
author: Martin Weise
---

# Upload Service

## tl;dr

!!! debug "Debug Information"

    Image: [tusproject/tusd:v1.12`](https://hub.docker.com/r/tusproject/tusd)

    * Ports: 1080/tcp
    * Prometheus: `http://<hostname>:1080/api/upload/metrics`
    * API: `http://<hostname>:1080/api/upload`

## Overview

We use the [TUS](https://tus.io/) open protocol for resumable file uploads which based entirely on HTTP.

### Examples

Upload a CSV-file into the `dbrepo-upload` bucket with the console 
via `http://<hostname>/admin/storage/browser/dbrepo-upload`.



We recommend using a TUS-compatible client:

* [tus-java-client](https://github.com/tus/tus-java-client) (Java)
* [tus-js-client](https://github.com/tus/tus-js-client) (JavaScript/Node.js)
* [tusd](https://github.com/tus/tusd) (Go)

## Limitations

* No support for authentication.

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](../contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

1. We strongly encourage to limit the clients allowed to upload by adding your subnet, e.g. `128.130.0.0/16` 
   (=TU Wien subnet) to the [Gateway Service](../system-services-gateway) configuration file like this:

       ```nginx title="dbrepo.conf"
       location /api/upload {
         allow 128.130.0.0/16;
         deny all;
         ...
       }
       ```

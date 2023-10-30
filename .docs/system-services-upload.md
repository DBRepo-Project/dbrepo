---
author: Martin Weise
---

# Upload Service

## tl;dr

!!! debug "Debug Information"

    Image: [`dbrepo/upload-service:latest`](https://hub.docker.com/r/dbrepo/upload-service)

    * Ports: 1080/tcp
    * TUS: `http://<hostname>:1080/api/upload/files`
    * Prometheus: `http://<hostname>:1080/metrics`
    * Swagger UI: <a href="../swagger/upload" target="_blank">:fontawesome-solid-square-up-right: view online</a>

## Overview

Upload files using one of the official the TUSd clients:

* [NodeJS / JavaScript](https://github.com/tus/tus-js-client)
* [Java](https://github.com/tus/tus-java-client)
* [Python](https://github.com/tus/tus-py-client)

The [TUS](https://tus.io/) protocol allows for flexible file uploads that, when interrupted, can be resumed at a later
point. It is based on the open HTTP protocol and uploading a new file is a sequence of `HEAD`, `POST` and `PATCH`
requests for large files.

For more information, see the [official Docker image](https://hub.docker.com/r/tusproject/tusd).

## Limitations

* No support for authentication

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](../contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

1. Since authentication is not supported, use IP-based ingress rules to limit access to the upload endpoint.

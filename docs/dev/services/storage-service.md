---
author: Martin Weise
---

## tl;dr

!!! debug "Debug Information"

    Image: [`docker.io/chrislusf/seaweedfs:3.71.0`](https://hub.docker.com/r/chrislusf/seaweedfs)

    * Ports: 8888/tcp, 9000/tcp
    * Prometheus: `http://<hostname>:9091/metrics`
    * Filer UI: `http://<hostname>:8888`
    * Cluster UI: `http://<hostname>:9333`

    To directly access in Kubernetes (for e.g. debugging), forward the svc port to your local machine:

    ```shell
    kubectl [-n namespace] port-forward svc/storage-service-s3 9000:8333
    ```

## Overview

We use [SeaweedFS](https://seaweedfs.github.io/) as a high-performance, S3 compatible object store for easy, cloud-ready
deployments that by default support replication and monitoring. No graphical user interface is provided out-of-the-box,
administrators can access the S3 storage via S3-compatible clients
e.g. [AWS CLI](https://docs.aws.amazon.com/cli/latest/reference/s3/) (see below).

The default configuration creates admin credentials `seaweedfsadmin:seaweedfsadmin`. By default, one bucket `dbrepo` is
created that holds uploads temporarily. It is recommended to delete the contents regularly.

The S3 endpoint of the Storage Service is available on port `9000`.

### Filer UI

The storage service comes with a simple UI that can be used to explore the uploaded files, rename them and delete them.
Please note that the Filer UI is not intended for production and should be turned off for security purposes.

<figure markdown>
![Filer UI with a list of uploaded files in the bucket dbrepo](../images/screenshots/storage-service-filer.png)
<figcaption>Figure 1: Filer UI</figcaption>
</figure>

## Limitations

* No support for multiple regions.

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](../../contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

1. For public deployments, change the default credentials.

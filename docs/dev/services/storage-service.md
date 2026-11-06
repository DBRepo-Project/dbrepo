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

### User uploads

`POST /api/v1/upload` returns `201` and an opaque `X-S3-Key` for every upload,
including repeated uploads of the same filename or content. Clients must keep the
returned key and pass it unchanged to import, schema analysis, BLOB tuple writes,
or preview-image requests. The original filename is stored separately as UTF-8
Base64 in S3 metadata; it is never used as an object path.

An upload belongs to the authenticated account's canonical Keycloak subject UUID.
Basic and Bearer authentication for the same account use the same owner. Changing
a username does not transfer uploads. Ordinary callers can consume their own
uploads; database ownership or a write grant does not grant access to another
account's upload. The existing trusted system identity retains internal access.
Missing objects, foreign uploads, and old uploads without ownership metadata
return `404`. Existing pending uploads must be uploaded again after upgrading;
this change does not delete or rewrite stored objects or imported database data.

Keys consist of a server-generated UUID and a fixed reader suffix. CSV is the
default, including filenames without an extension; TSV and recognized compression
suffixes are retained for the existing CSV readers. The suffix does not validate
content or add reader support for a compression format. Raw BLOB and image reads
preserve the original bytes.

Upgrade Data and Metadata together to apply the same ownership checks at all
upload consumers. During the upgrade, pause uploads and their consumption until
both services are updated.

### Filer UI

The storage service comes with a simple UI that can be used to explore the uploaded files, rename them and delete them.
Please note that the Filer UI is not intended for production and should be turned off for security purposes.

<figure markdown>
![Filer UI with a list of uploaded files in the bucket dbrepo](/infrastructures/dbrepo/1.13/images/screenshots/storage-service-filer.png)
</figure>

## Limitations

* No support for multiple regions.

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](/infrastructures/dbrepo/1.13/contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

1. For public deployments, change the default credentials.

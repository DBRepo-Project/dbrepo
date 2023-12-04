---
author: Martin Weise
---

# Storage Service

## tl;dr

!!! debug "Debug Information"

    Image: [`chrislusf/seaweedfs:3.59`](https://hub.docker.com/r/chrislusf/seaweedfs)

    * Ports: 9000/tcp
    * Prometheus: `http://<hostname>:9091/metrics`

## Overview

We use [SeaweedFS](https://seaweedfs.github.io/) as a high-performance, S3 compatible object store for easy, cloud-ready
deployments that by default support replication and monitoring. No graphical user interface is provided out-of-the-box,
administrators can access the S3 storage via S3-compatible clients 
e.g. [AWS CLI](https://docs.aws.amazon.com/cli/latest/reference/s3/) (see below).

### Users

The default configuration creates one user `seaweedfsadmin` with password `seaweedfsadmin`.

### Buckets

The default configuration creates two buckets `dbrepo-upload`, `dbrepo-download`:

* `dbrepo-upload` for CSV-file upload (for import of data, analysis, etc.) from the User Interface
* `dbrepo-download` for CSV-file download (exporting data, metadata, etc.)

### Examples

Upload a CSV-file into the `dbrepo-upload` bucket with the AWS CLI:

```console
$ aws --endpoint-url http://<hostname>:9000 \
    s3 \
    cp /path/to/file.csv \
    s3://dbrepo-upload/
upload: /path/to/file.csv to s3://dbrepo-upload/file.csv
```

You can list the buckets:

```console
$ aws --endpoint-url http://<hostname>:9000 \
    s3 \
    ls
2023-12-03 16:23:15 dbrepo-download
2023-12-03 16:28:05 dbrepo-upload
```

And list the files in the bucket `dbrepo-upload` with:

```console
$ aws --endpoint-url http://<hostname>:9000 \
    s3 \
    ls \
    dbrepo-upload
2023-12-03 16:28:05     535219 file.csv
```

Alternatively, you can use the middleware of the [User Interface](../system-other-ui/) to upload files.

Alternatively, you can use a S3-compatible client:

* [boto3](https://boto3.amazonaws.com/v1/documentation/api/latest/index.html) (generic Python implementation of S3)
* AWS SDK (tailored towards Amazon S3)

## Limitations

* No support for multiple regions.

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](../contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

1. For public deployments, change the default credentials.

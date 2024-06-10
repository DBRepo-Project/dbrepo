---
author: Martin Weise
---

# Storage Service

## Preliminary

Configure the credentials to access the S3 endpoint:

```shell
$ aws configure \
    --endpoint-url http://localhost:9000
AWS Access Key ID [None]: seaweedfsadmin
AWS Secret Access Key [None]: seaweedfsadmin
Default region name [None]: 
Default output format [None]:
```

## Upload

Upload a CSV-file into the `dbrepo-upload` bucket with the AWS CLI:

```shell
$ aws --endpoint-url http://localhost:9000 \
    s3 \
    cp /path/to/file.csv \
    s3://dbrepo-upload/
upload: /path/to/file.csv to s3://dbrepo-upload/file.csv
```

## List

You can list the buckets:

```shell
$ aws --endpoint-url http://localhost:9000 \
    s3 \
    ls
2023-12-03 16:23:15 dbrepo-download
2023-12-03 16:28:05 dbrepo-upload
```

And list the files in the bucket `dbrepo-upload` with:

```shell
$ aws --endpoint-url http://localhost:9000 \
    s3 \
    ls \
    dbrepo-upload
2023-12-03 16:28:05     535219 file.csv
```

## Other

Alternatively, you can use the middleware of the [User Interface](./system-other-ui/) to upload files.

Alternatively, you can use a S3-compatible client:

* [boto3](https://boto3.amazonaws.com/v1/documentation/api/latest/index.html) (generic Python implementation of S3)
* AWS SDK (tailored towards Amazon S3)

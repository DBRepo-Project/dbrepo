---
author: Martin Weise
---

## tl;dr

!!! debug "Debug Information"

    Image: [`registry.datalab.tuwien.ac.at/dbrepo/analyse-service:1.9.3`](https://hub.docker.com/r/dbrepo/analyse-service)

    * Ports: 5000/tcp
    * Prometheus: `http://<hostname>:5000/metrics`
    * Health: `http://<hostname>:5000/health`
    * Swagger UI: `http://<hostname>:5000/swagger-ui/`

    To directly access in Kubernetes (for e.g. debugging), forward the svc port to your local machine:

    ```shell
    kubectl [-n namespace] port-forward svc/analyse-service 5000:80
    ```

## Overview

It suggests data types for the [User Interface](../ui) when creating a table from a
*comma separated values* (CSV) -file. It recommends enumerations for columns and returns e.g. a list of potential
primary key candidates. The researcher is able to confirm these suggestions manually. Moreover, the Analyse Service
determines basic statistical properties of numerical columns.

### Analysis

After [uploading](../storage-service/#buckets) the CSV-file into the `dbrepo-upload` bucket of
the [Storage Service](../storage-service), analysis for data types and primary keys follows the flow:

1. Retrieve the CSV-file from the `dbrepo-upload` bucket of the Storage Service as data stream (=nothing is stored in
   the service) with the [`boto3`](https://boto3.amazonaws.com/v1/documentation/api/latest/index.html) client.
2. When no separator is known, the Analyse Service tries to guess the separator from the first line
   with [`csv.Sniff().sniff(...)`](https://docs.python.org/3/library/csv.html#csv.Sniffer). This step is optional when
   the separator was provided via HTTP-payload: `{"separator": ";", ...}`
3. With the separator known (either from step 2 or via HTTP-payload), the [`Pandas`](https://pypi.org/project/pandas/)
   guesses the headers and column types and enums by analysing the first 10.000 rows, if the HTTP-payload contains 
   `{"enum": true, ...}`. The data type is guessed by a combination of Pandas and heuristics.

If your datasets are larger than 10.000 rows, increase the number of lines analysed by setting the `ANALYSE_NROWS`
variable to the desired integer.

### Examples

See the [usage page](..) for examples.

## Limitations

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](../../contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

1. Credentials for the [Storage Service](../storage-service) are stored in plaintext environment variables.

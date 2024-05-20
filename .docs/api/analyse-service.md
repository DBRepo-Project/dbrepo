---
author: Martin Weise
---

## tl;dr

!!! debug "Debug Information"

    Image: [`dbrepo/analyse-service:__APPVERSION__`](https://hub.docker.com/r/dbrepo/analyse-service)

    * Ports: 5000/tcp
    * Prometheus: `http://<hostname>:5000/metrics`
    * Health: `http://<hostname>:5000/health`
    * Swagger UI: `http://<hostname>:5000/swagger-ui/` <a href="../swagger/analyse" target="_blank">:fontawesome-solid-square-up-right: view online</a>

## Overview

It suggests data types for the [User Interface](../system-other-ui) when creating a table from a 
*comma separated values* (CSV) -file. It recommends enumerations for columns and returns e.g. a list of potential 
primary key candidates. The researcher is able to confirm these suggestions manually. Moreover, the Analyse Service
determines basic statistical properties of numerical columns.

### Analysis

After [uploading](../system-services-storage/#buckets) the CSV-file into the `dbrepo-upload` bucket of 
the [Storage Service](../system-services-storage), analysis for data types and primary keys follows the flow:
 
1. Retrieve the CSV-file from the `dbrepo-upload` bucket of the Storage Service as data stream (=nothing is stored in 
   the service) with the [`boto3`](https://boto3.amazonaws.com/v1/documentation/api/latest/index.html) client.
2. When no separator is known, the Analyse Service tries to guess the separator from the first line 
   with [`csv.Sniff().sniff(...)`](https://docs.python.org/3/library/csv.html#csv.Sniffer). This step is optional when
   the separator was provided via HTTP-payload: `{"separator": ";", ...}`
3. With the separator known (either from step 2 or via HTTP-payload), 
   the [`messytables.CSVTableSet(...)`](https://messytables.readthedocs.io/en/latest/#csv-support) guesses the headers
   and column types and enums, if the HTTP-payload contains `{"enum": true, ...}`.

### Examples

See the [usage page](../usage-analyse/) for examples.

## Limitations

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](../contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

1. Credentials for the [Storage Service](../system-services-storage) are stored in plaintext environment variables.

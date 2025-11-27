---
author: Martin Weise
---

!!! debug "Debug Information"

    Image: [`docker.io/bitnamilegacy/mariadb:11.3.2`](https://hub.docker.com/r/bitnamilegacy/mariadb-galera)

    * Ports: 3306/tcp
    * JDBC: `jdbc://mariadb:<hostname>:3306`

    To directly access in Kubernetes (for e.g. debugging), forward the svc port to your local machine:

    ```shell
    kubectl [-n namespace] port-forward svc/data-db 3306:3306
    ```

## Overview

The Data Database contains the research data. In the default configuration, only one database of this type is deployed.
Any number of MariaDB ata databases can be integrated into DBRepo, even non-empty databases. The database needs to be
registered in the Metadata Database to be visible in the [User Interface](/infrastructures/dbrepo/1.13/dev/services/ui) and usable from e.g. the Python
Library.

## Data

The procedures require the in parameter of the `hash_table` stored procedure to have the same collation as the
`information_schema.columns` table. We observed this unexpected behavior for
the [MariaDB Galera chart](https://artifacthub.io/packages/helm/bitnami/mariadb-galera) powered by Bitnami and had to
set extra flags.

## Backup & Restore

Please refer to our detailed documentation to perform 
a [full backup](/infrastructures/dbrepo/1.13/maintainer-guide/backup-data/) 
and [restore](/infrastructures/dbrepo/1.13/maintainer-guide/restore-data/).

## Limitations

(none)

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](/infrastructures/dbrepo/1.13/contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

(none)

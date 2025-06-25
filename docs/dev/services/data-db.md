---
author: Martin Weise
---

!!! debug "Debug Information"

    Image: [`docker.io/bitnami/mariadb:11.3.2`](https://hub.docker.com/r/bitnami/mariadb-galera)

    * Ports: 3306/tcp
    * JDBC: `jdbc://mariadb:<hostname>:3306`

    To directly access in Kubernetes (for e.g. debugging), forward the svc port to your local machine:

    ```shell
    kubectl [-n namespace] port-forward svc/data-db 3306:3306
    ```

## Overview

The Data Database contains the research data. In the default configuration, only one database of this type is deployed.
Any number of MariaDB ata databases can be integrated into DBRepo, even non-empty databases. The database needs to be
registered in the Metadata Database to be visible in the [User Interface](/infrastructures/dbrepo/dev/services/ui) and usable from e.g. the Python
Library.

## Configuration

By default, the Data Database is configured as a cluster of three nodes where each node has a maximum of 2048 MiB RAM
available. As recommended by
[MariaDB](https://mariadb.com/kb/en/mariadb-memory-allocation/#allocating-ram-for-mariadb-the-short-answer), we set
`innodb_buffer_pool_size=1430M` (70% of the available RAM). If you have more RAM available, you should set the variable
accordingly to improve the performance.

## Data

The procedures requires the in parameter of the `hash_table` stored procedure to have the same collation as the
`information_schema.columns` table. We observed this unexpected behavior for
the [MariaDB Galera chart](https://artifacthub.io/packages/helm/bitnami/mariadb-galera) powered by Bitnami and had to
set extra flags.

### Backup

Export all databases with `--skip-lock-tables` option for MariaDB Galera clusters as it is not supported currently by
MariaDB Galera.

=== "Terminal"

    ```shell
    mariadb \
        -u <privilegedUsername> \
        -p<privilegedPassword> \
        --complete-insert \
        --skip-lock-tables \
        --skip-add-locks \
        --all-databases > dump.sql
    ```

### Restore

=== "Terminal"

    ```shell
    mariadb \
        -u <privilegedUsername> \
        -p<privilegedPassword> < dump.sql
    ```

## Limitations

(none)

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](/infrastructures/dbrepo/contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

(none)

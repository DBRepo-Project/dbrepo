---
author: Martin Weise
---

!!! danger "Security Disclaimer"

    This quick default backup guide should not be considered secure. Please always encrypt your backups using `openssl`
    or `gpg` as [recommended by MariaDB](https://mariadb.com/docs/server/server-usage/backup-and-restore/mariadb-backup/using-encryption-and-compression-tools-with-mariadb-backup).

## TL;DR

This page explains how to perform regular backups of MariaDB Galera (
i.e. [Data Database](/infrastructures/dbrepo/1.13/dev/services/data-db/)
and [Metadata Database](/infrastructures/dbrepo/1.13/dev/services/metadata-db/)) so they can be recovered at a later
point in time.

## Full Backup

We adhere to the 
official [MariaDB 11.3.2 documentation](https://mariadb.com/docs/server/server-usage/backup-and-restore/mariadb-backup/full-backup-and-restore-with-mariadb-backup)
in this section.

The following command creates a directory to store the backup and performs a full backup for node `0` (you need to do
this for all nodes e.g. nodes `0`, `1` and `2`). Beware that not all paths are writable so we use one that is.

=== "Data Database"

    ```shell
    kubectl -n $NAMESPACE exec pod/data-db-0 -c mariadb-galera -- \
        mkdir \
        /bitnami/mariadb/backup
    ```

    Then create the backup using the values from `values.yaml`. The credentials are located at 
    `datadb.galera.mariabackup.user` and `datadb.galera.mariabackup.password`.

    ```shell
    kubectl -n $NAMESPACE exec pod/data-db-0 -c mariadb-galera -- \
        mariadb-backup \
        --backup \
        --galera-info \
        --target-dir=/bitnami/mariadb/backup \
        --user=backup \
        --password=backup
    ```

    Compress the archive directory.

    ```shell
    kubectl -n $NAMESPACE exec pod/data-db-0 -c mariadb-galera -- \
        tar czfv \
        /bitnami/mariadb/backup.tar.gz \
        /bitnami/mariadb/backup && \
        rm -rf \
            /bitnami/mariadb/backup
    ```

    Move the backup from the nodes to your backup machine through e.g. copying it:
    
    ```shell
    kubectl -n $NAMESPACE \
        cp \
        pod/data-db-0:/bitnami/mariadb/backup.tar.gz \
        backup-data-db-0.tar.gz && \
        rm -f \
            /bitnami/mariadb/backup.tar.gz
    ```

=== "Metadata Database"

    ```shell
    kubectl -n $NAMESPACE exec pod/metadata-db-0 -c mariadb-galera -- \
        mkdir \
        /bitnami/mariadb/backup
    ```

    Then create the backup using the values from `values.yaml`. The credentials are located at 
    `metadatadb.galera.mariabackup.user` and `metadatadb.galera.mariabackup.password`.

    ```shell
    kubectl -n $NAMESPACE exec pod/metadata-db-0 -c mariadb-galera -- \
        mariadb-backup \
        --backup \
        --galera-info \
        --target-dir=/bitnami/mariadb/backup \
        --user=backup \
        --password=backup
    ```

    Compress the archive directory.

    ```shell
    kubectl -n $NAMESPACE exec pod/metadata-db-0 -c mariadb-galera -- \
        tar czfv \
        /bitnami/mariadb/backup.tar.gz \
        /bitnami/mariadb/backup && \
        rm -rf \
            /bitnami/mariadb/backup
    ```

    Move the backup from the nodes to your backup machine through e.g. copying it:
    
    ```shell
    kubectl -n $NAMESPACE \
        cp \
        pod/metadata-db-0:/bitnami/mariadb/backup.tar.gz \
        backup-metadata-db-0.tar.gz && \
        rm -f \
            /bitnami/mariadb/backup.tar.gz
    ```

Repeat this for nodes `1` and `2` (or more).
    
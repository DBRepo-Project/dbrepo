---
author: Martin Weise
---

!!! debug "Debug Information"

    Image: [`docker.io/bitnami/mariadb-galera:11.1.3-debian-11-r8`](https://hub.docker.com/r/bitnami/mariadb-galera)

    * Ports: 3306/tcp
    * JDBC: `jdbc://mariadb:<hostname>:3306`

    To directly access in Kubernetes (for e.g. debugging), forward the svc port to your local machine:

    ```shell
    kubectl [-n namespace] port-forward svc/data-db 3306:3306
    ```

## Overview

The Data Database contains the research data. In the default configuration, only one database of this type is deployed.
Any number of MariaDB ata databases can be integrated into DBRepo, even non-empty databases. The database needs to be
registered in the Metadata Database to be visible in the [User Interface](../ui) and usable from e.g. the Python 
Library.

## Data

The procedures require the user-generated databases to have the same collation (because of comparison operations).
Ensure that the Data Database has the character set `utf8mb4` and collation `utf8mb4_general_ci` in your `my.cfg`:

```ini
[mysqld]
character_set_server=utf8mb4
collation_server=utf8mb4_general_ci
```

We observed this unexpected behavior for
the [MariaDB Galera chart](https://artifacthub.io/packages/helm/bitnami/mariadb-galera) powered by Bitnami and had to
set extra flags. We could not observe this behavior with
the [MariaDB Galera container image](https://hub.docker.com/r/bitnami/mariadb-galera) itself.

```yaml
mariadb-galera:
  extraFlags: "--character-set-server=utf8mb4 --collation-server=utf8mb4_general_ci"
```

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
    in [contact](../contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

(none)

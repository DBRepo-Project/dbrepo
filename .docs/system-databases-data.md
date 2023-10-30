---
author: Martin Weise
---

# Data Database

## tl;dr

!!! debug "Debug Information"

    Image: [`bitnami/mariadb:10.5`](https://hub.docker.com/r/bitnami/mariadb)

    * Ports: 3306/tcp
    * JDBC: `jdbc://mariadb:<hostname>:3306`

## Overview

By default, only one Data Database is deployed. You can deploy multiple (different) Data Database instances and make
them available in the repository as follows:

```console
curl \
   -sSL \
   http://<hostname>:9093/api/container \
   -X POST \
   -d '{"name": "Data Database 2", "imageId": 1, "host": "example.com", "port": 3306, "privilegedUsername": "root", "privilegedPassword": "s3cr3t" }'
```

### Backup

Export all databases with `--skip-lock-tables` option for MariaDB Galera clusters as it is not supported currently by
MariaDB Galera.

```console
mysqldump \
    -u <privilegedUsername> \
    -p<privilegedPassword> \
    --complete-insert \
    --skip-lock-tables \
    --skip-add-locks \
    --all-databases > dump.sql
```

### Restore

```console
mysql \
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

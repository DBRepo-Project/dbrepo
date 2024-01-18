---
author: Martin Weise
---

# Auth Database

## tl;dr

!!! debug "Debug Information"

    Image: [`bitnami/mariadb:11.2.2-debian-11-r0`](https://hub.docker.com/r/bitnami/mariadb)

    * Ports: 3306/tcp
    * JDBC: `jdbc://mariadb:<hostname>:3306`

## Overview

This is the database in which the [Authentication Service](../system-services-authentication) writes into. In the
default configuration, only MariaDB is supported.

## Limitations

* No support for MariaDB Galera at the moment.

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](../contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

(none)

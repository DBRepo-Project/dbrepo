---
author: Martin Weise
---

# Metadata Database

## tl;dr

!!! debug "Debug Information"

    Image: [`dbrepo/metadata-db:__APPVERSION__`](https://hub.docker.com/r/dbrepo/metadata-db)

    * Ports: 3306/tcp
    * JDBC: `jdbc://mariadb:<hostname>:3306`

It is the core component of the project. It is a relational database that contains metadata about all researcher
database created in the database repository like column names, check expressions, value enumerations or key/value
constraints and relevant data for citing data sets. Additionally, the concept, e.g. URI of units of measurements of
numerical columns is stored in the Metadata Database in order to provide semantic knowledge context. We use MariaDB for
its rich capabilities in the reference implementation.

The default credentials are `root:dbrepo` for the database `fda`. Connect to the database via the JDBC connector on
port `3306`.

## Limitations

(none)

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](../contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

(none)

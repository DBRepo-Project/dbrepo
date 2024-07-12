---
author: Martin Weise
---

## tl;dr

!!! debug "Debug Information"

    Image: [`quay.io/keycloak/keycloak:24.0`](quay.io/keycloak/keycloak)

    * Ports: 8080/tcp
    * UI: `http://<hostname>/api/auth/`

## Overview

By default, users are created using the [User Interface](../ui) and the sign-up page in the User Interface.
This creates a new user in Keycloak. The user identity is then managed by the Auth Service. Only a very small subset
of immutable properties (id, username) is mirrored in the [Metadata Database](../metadata-db) for faster access.

## Identities

:octicons-tag-16:{ title="Minimum version" } 1.4.4

Identities can also be added in Keycloak directly. When requesting a JWT token from the `/api/user` endpoint, the
immutable properties mentioned in c.f. [Overview](#overview) are copied transparent to the user on first login.

## Groups

The authorization scheme follows a group-based access control (GBAC). Users are organized in three distinct
(non-overlapping) groups:

1. Researchers (*default*)
2. Developers
3. Data Stewards

Based on the membership in one of these groups, the user is assigned a set of roles that authorize specific actions. By
default, all users are assigned to the `researchers` group.

## Roles

We organize the roles into default- and escalated composite roles. There are three composite roles, one for each group.
Each of the composite role has a set of other associated composite roles.

<figure markdown>
![Grouped Roles](../images/groups-roles.png)
<figcaption>Three groups (Researchers, Developers, Data Stewards) and their composite roles associated.</figcaption>
</figure>

There is one role for one specific action in the services. For example: the `create-database` role authorizes a user to
create a database.

A full list of available roles can be obtained
from [`dbrepo-realm.json`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/blob/fb8d14ba02ee32b9a69a30905437b5c9e28adc21/dbrepo-auth-service/dbrepo-realm.json#L46)
which is imported into Keycloak on startup.

## Limitations

* No support for sending e-mails through Keycloak by default.
* No support for temporary passwords.
* No support for multi-factor authentication.

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](../contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

1. Keycloak should be configured to use TLS certificates, follow
   the [official documentation](https://www.keycloak.org/server/enabletls).

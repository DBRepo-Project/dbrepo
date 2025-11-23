---
author: Martin Weise
---

## tl;dr

!!! debug "Debug Information"

    Image: [`quay.io/keycloak/keycloak:26.4.0`](https://hub.docker.com/r/bitnamilegacy/keycloak)

    * Ports: 8080/tcp
    * UI: `http://<hostname>:8080/`

    To directly access in Kubernetes (for e.g. debugging), forward the svc port to your local machine:

    ```shell
    kubectl [-n namespace] port-forward svc/auth-service 8080:80
    ```

## Overview

By default, users are created using the [User Interface](/infrastructures/dbrepo/1.13/dev/services/ui) and the sign-up
page
in the User Interface. This creates a new user in Keycloak. The user identity is then managed by the Auth Service. Only
a very small subset of immutable properties (id, username) is mirrored in
the [Metadata Database](/infrastructures/dbrepo/1.13/dev/services/metadata-db) for faster access.

## Identities

:octicons-tag-16:{ title="Minimum version" } 1.4.5

Identities are managed via LDAP through
the [Identity Service](/infrastructures/dbrepo/1.13/dev/services/identity-service).
Users can register themselves through the [Auth service](/infrastructures/dbrepo/1.13/dev/services/auth-service) or via
the Auth Service admin user (`admin:admin` by default). The recommended workflow is:

1. Login to the Auth Service as **Admin** and in the dbrepo realm navigate to **Users**
2. Click the **Add user** button and fill out the Username field and assign the group `researchers` by clicking
   the **Join Groups** and selecting it. Click **Join** and **Create**.
3. Click the **Credentials** tab above and **Set password**. In the popup window assign a secure password to the user
   and set **Temporary** to `Off`.

The REST API supports three kinds of authentication:

* OAuth2.0 Bearer Authentication
* Basic Authentication
* Internal Authentication: limited to a local system user that is only used between services (i.e. *never* publicly)

<figure markdown>
![Auth](/infrastructures/dbrepo/1.13/images/authentication.svg)
<figcaption>Figure 1: Authentication Mechanisms in DBRepo</figcaption>
</figure>

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
Each of the composite role has a set of other associated composite roles. The roles in Keycloak are mapped to
authorities in the services and can be used to give fine-grained permissions to users. There is one role for one
specific action in the services. For example: the `create-database` role authorizes a user to create a database.

## Limitations

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](/infrastructures/dbrepo/1.13/contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

1. Keycloak should be configured to use TLS certificates, follow
   the [official documentation](https://www.keycloak.org/server/enabletls).

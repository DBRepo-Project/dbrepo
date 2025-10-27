---
author: Martin Weise
---

## tl;dr

!!! debug "Debug Information"

    Image: [`docker.io/bitnamilegacy/keycloak:26.0.4`](https://hub.docker.com/r/bitnami/keycloak)

    * Ports: 8080/tcp
    * UI: `http://<hostname>:8080/`

    To directly access in Kubernetes (for e.g. debugging), forward the svc port to your local machine:

    ```shell
    kubectl [-n namespace] port-forward svc/auth-service 8080:80
    ```

## Overview

By default, users are created using the [User Interface](/infrastructures/dbrepo/1.10/dev/services/ui) and the sign-up page
in the User Interface. This creates a new user in Keycloak. The user identity is then managed by the Auth Service. Only
a very small subset of immutable properties (id, username) is mirrored in
the [Metadata Database](/infrastructures/dbrepo/1.10/dev/services/metadata-db) for faster access.

## Identities

:octicons-tag-16:{ title="Minimum version" } 1.4.5

Identities are managed via LDAP through the [Identity Service](/infrastructures/dbrepo/1.10/dev/services/identity-service).
The normal workflow is that the [Metadata Service](/infrastructures/dbrepo/1.10/dev/services/metadata-service) adds
identities when user register. In some cases, where this is not possible (e.g. in workshop-scenarios where accounts are
created before the workshop starts), identities need to be created manually in Keycloak. The recommended workflow is:

1. Login to the Auth Service as **Admin** and in the dbrepo realm navigate to **Users**
2. Click the **Add user** button and fill out the Username field and assign the group `researchers` by clicking
   the **Join Groups** and selecting it. Click **Join** and **Create**.
3. Click the **Credentials** tab above and **Set password**. In the popup window assign a secure password to the user
   and set **Temporary** to `Off`.

    !!! example "Create user with specific id"

        The user id is created automatically. In case you need to create a user with specific id such as in migration
        scenarios, you need to change the `entryUUID` in the [Identity Service](/infrastructures/dbrepo/1.10/dev/services/identity-service) by modifying this
        protected attribute in `relax` mode:

        ```bash
        echo "dn: uid=<username>,ou=users,dc=dbrepo,dc=at
        changetype: modify
        replace: entryUUID
        entryUUID: 506ae590-11a2-4d2d-82b8-45121c6b4dab" | \
        ldapmodify -h localhost -p 1389 -D cn=admin,dc=dbrepo,dc=at -c -x -e relax \
        -w<adminpassword> 
        ```

4. Finally you need to query the user info once by navigating again to **Users**
   and search for the **Username** and click :arrow_right: to search. Click the username and ensure that the
   **User metadata** contains the entry **LDAP_ID**.

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
![Grouped Roles](/infrastructures/dbrepo/1.10/images/groups-roles.png)
</figure>

There is one role for one specific action in the services. For example: the `create-database` role authorizes a user to
create a database.

A full list of available roles can be obtained
from [`dbrepo-realm.json`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/blob/fb8d14ba02ee32b9a69a30905437b5c9e28adc21/dbrepo-auth-service/dbrepo-realm.json#L46)
which is imported into Keycloak on startup.

## Limitations

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](/infrastructures/dbrepo/1.10/contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

1. Keycloak should be configured to use TLS certificates, follow
   the [official documentation](https://www.keycloak.org/server/enabletls).

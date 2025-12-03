---
author: Martin Weise
---

This guide assumes you have already performed the
quick [Install on Docker](/infrastructures/dbrepo/1.13/maintainer-guide/install-docker/). You can (re-)configure most
components by executing:

```shell
make gen-secrets
```

Note that this script overwrites (previously generated) secrets, use with caution.

### Identity Service

Add the admin password to the `.env` file:

```shell
echo "IDENTITY_SERVICE_ADMIN_PASSWORD=$(openssl rand -hex 16) >> .env"
```

### Auth Service

We need to configure three components:

1. Environment
2. Client `dbrepo-client`
3. User federation `Identity Service`

First, add the admin password to the `.env` file:

```shell
echo "AUTH_SERVICE_ADMIN_PASSWORD=$(openssl rand -hex 16) >> .env"
```

First, access the Admin UI at [http://localhost:8080](http://localhost:8080) and change the client secret of the 
default `dbrepo-client` client:

<figure markdown>
![Change the Client Secret](/infrastructures/dbrepo/1.13/images/screenshots/auth-service-client-secret.png)
</figure>

Next, log into the Auth Service with the default credentials `admin` and the value of `AUTH_SERVICE_ADMIN_PASSWORD` 
and select the "dbrepo" realm :material-numeric-1-circle-outline:. In the sidebar, select the 
"User federation" :material-numeric-2-circle-outline: and from the provider list, select the "Identity Service" provider
:material-numeric-3-circle-outline:.

<figure markdown>
![Keycloak identitiy provider list](/infrastructures/dbrepo/1.13/images/screenshots/auth-service-ldap-1.png){ .img-border }
</figure>

Second, modify the Bind DN :material-numeric-1-circle-outline:. Change the **Bind credentials** to the desired 
password :material-numeric-2-circle-outline: from the variable `IDENTITY_SERVICE_ADMIN_PASSWORD` in `.env`.

<figure markdown>
![Keycloak identity provider settings](/infrastructures/dbrepo/1.13/images/screenshots/auth-service-ldap-2.png){ .img-border }
</figure>

### Apply

You need to remove all containers and volumes to apply the changes and then restart:

```shell
docker container stop $(docker container ls -aq -f "name=dbrepo-*")
docker container rm $(docker container ls -aq -f "name=dbrepo-*")
docker volume rm $(docker volume ls -q -f "name=dbrepo_*")
docker compose up -d
```

## Next Steps

You should now be able to view the front end at [https://localhost](https://localhost).

Please be warned that the default configuration is not intended for public deployments. It is only intended to have a
running system within minutes to play around within the system and explore features. It is strongly advised to change 
the default `.env` environment variables.

Next, create a [user account](/infrastructures/dbrepo/1.13/api/#create-user-account) and 
then [create a database](/infrastructures/dbrepo/1.13/api/#create-database) 
to [import a dataset](/infrastructures/dbrepo/1.13/api/#import-dataset).


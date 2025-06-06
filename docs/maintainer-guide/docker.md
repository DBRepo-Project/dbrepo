---
author: Martin Weise
---

!!! danger "Security Disclaimer"

    This quick default installation should **not be considered secure**. It is intended for local testing and
    demonstration and should not be used in public deployments or in production. It is a quick installation method and
    is intended for a quick look at DBRepo.

## TL;DR

Install DBRepo in one line:

```shell
curl -sSL https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-1.9/install.sh | bash
```

Then start DBRepo and visit [`http://localhost`](http://localhost) in your browser:

```shell
docker compose up -d
```

## Prerequisites

!!! info

    * min. 8 vCPU cores
    * min. 20GB free RAM memory

Since DBRepo is intended to be a publicly available repository, an optional fixed/static IP-address with optional
SSL/TLS certificate is recommended. Follow the [secure installation](#secure-installation) guide.

## Secure Installation

Execute the installation script to download only the environment and save it to `dist`.

```shell
curl -sSL https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-1.9/install.sh | DOWNLOAD_ONLY=1 bash
```

### Static Configuration

Call the helper script to regenerate the client secret of the `dbrepo-client` and set it as value of the 
`AUTH_SERVICE_CLIENT_SECRET` variable in the `.env` file.

Update the rest of the default secrets in the `.env` file to secure passwords. You can use `openssl` for that, e.g. 
`openssl rand -hex 16`. Set `auth_ldap.dn_lookup_bind.password` in `dist/rabbitmq.conf` to the value of
`SYSTEM_PASSWORD`.

Only set the `BASE_URL` environment variable in `.env` when your hostname is **not** `localhost`.

### Runtime Configuration

The [Auth Service](../api/auth-service) can be configured easily when DBRepo is running. Start DBRepo temporarily:

```shell
docker compose up -d
```

Log into the Auth Service with the default credentials `admin` and the value of `AUTH_SERVICE_ADMIN_PASSWORD` 
(c.f. Figure 1) and select the "dbrepo" realm :material-numeric-1-circle-outline:. In the sidebar, select the 
"User federation" :material-numeric-2-circle-outline: and from the provider list, select the "Identity Service" provider
:material-numeric-3-circle-outline:.

<figure markdown>
![Keycloka identitiy provider list](images/screenshots/auth-service-ldap-1.png){ .img-border }
<figcaption>Figure 1: Select the Identity Service provider.</figcaption>
</figure>

If you plan to change the default admin username (c.f. Figure 2), modify the Bind DN :material-numeric-1-circle-outline:
but this is optional. Change the Bind credentials to the desired password :material-numeric-2-circle-outline: from
the variable `IDENTITY_SERVICE_ADMIN_PASSWORD` in `.env`.

<figure markdown>
![Keycloak identity provider settings](images/screenshots/auth-service-ldap-2.png){ .img-border }
<figcaption>Figure 2: Update the Identity Service admin user credentials.</figcaption>
</figure>

Update the client secret of the `dbrepo-client`:

```bash
curl -sSL "https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-1.9/.scripts/reg-client-secret.sh" | bash
```
   
Also, update the JWT key according to the 
[Keycloak documentation](https://www.keycloak.org/docs/24.0.1/server_admin/index.html#rotating-keys). To secure your
deployment traffic with SSL/TLS, tell the Gateway Service to use your certificate secret (e.g. from Let's Encrypt):

```yaml title="docker-compose.yml"
services:
  ...
  dbrepo-gateway-service:
    ...
    volumes:
      - /path/to/cert.crt:/app/cert.crt
      - /path/to/cert.key:/app/cert.key
    ...
```

Now redirect all non-HTTPS routes to HTTPS in the Gateway Service:

```config title="dist/dbrepo.conf"
server {
    listen 80 default_server;
    server_name _;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl default_server;
    server_name my_hostname;
    ssl_certificate /app/cert.crt;
    ssl_certificate_key /app/cert.key;
    ...
}
```

### Apply the Configuration

Restart the configured DBRepo system to apply the static and runtime configuration:

```shell
docker compose down
docker compose up -d
```

The secure installation is now finished!

## Next Steps

You should now be able to view the front end at [http://localhost](http://localhost).

Please be warned that the default configuration is not intended for public deployments. It is only intended to have a
running system within minutes to play around within the system and explore features. It is strongly advised to change 
the default `.env` environment variables.

Next, create a [user account](../api/#create-user-account) and 
then [create a database](../api/#create-database) to [import a dataset](../api/#import-dataset).


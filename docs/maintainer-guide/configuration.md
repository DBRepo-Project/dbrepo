---
author: Martin Weise
---

## Prepare

Execute the installation script to download only the environment and save it to `dist`.

```shell
curl -sSL https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-1.10/install.sh | DOWNLOAD_ONLY=1 bash
```

### Static Configuration

Call the helper script to regenerate the client secret of the `dbrepo-client` and set it as value of the 
`AUTH_SERVICE_CLIENT_SECRET` variable in the `.env` file.

Update the rest of the default secrets in the `.env` file to secure passwords. You can use `openssl` for that, e.g. 
`openssl rand -hex 16`. Set `auth_ldap.dn_lookup_bind.password` in `dist/rabbitmq.conf` to the value of
`SYSTEM_PASSWORD`.

Only set the `BASE_URL` environment variable in `.env` when your hostname is **not** `localhost`.

## Runtime Configuration

### Auth Service

The [Auth Service](/infrastructures/dbrepo/1.10/api/auth-service/) can be configured easily when DBRepo is running. Start 
DBRepo temporarily:

```shell
docker compose up -d
```

Log into the Auth Service with the default credentials `admin` and the value of `AUTH_SERVICE_ADMIN_PASSWORD` 
(c.f. Figure 1) and select the "dbrepo" realm :material-numeric-1-circle-outline:. In the sidebar, select the 
"User federation" :material-numeric-2-circle-outline: and from the provider list, select the "Identity Service" provider
:material-numeric-3-circle-outline:.

<figure markdown>
![Keycloak identitiy provider list](/infrastructures/dbrepo/1.10/images/screenshots/auth-service-ldap-1.png){ .img-border }
</figure>

If you plan to change the default admin username (c.f. Figure 2), modify the Bind DN :material-numeric-1-circle-outline:
but this is optional. Change the Bind credentials to the desired password :material-numeric-2-circle-outline: from
the variable `IDENTITY_SERVICE_ADMIN_PASSWORD` in `.env`.

<figure markdown>
![Keycloak identity provider settings](/infrastructures/dbrepo/1.10/images/screenshots/auth-service-ldap-2.png){ .img-border }
</figure>

Update the client secret of the `dbrepo-client`:

```bash
curl -sSL "https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-1.10/.scripts/reg-client-secret.sh" | bash
```

### Gateway Service
   
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

## Apply the Configuration

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

Next, create a [user account](/infrastructures/dbrepo/1.10/api/#create-user-account) and 
then [create a database](/infrastructures/dbrepo/1.10/api/#create-database) to [import a dataset](/infrastructures/dbrepo/1.10/api/#import-dataset).


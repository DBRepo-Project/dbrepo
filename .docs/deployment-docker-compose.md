---
author: Martin Weise
---

# Docker Compose

## TL;DR

If you have [Docker](https://docs.docker.com/engine/install/) already installed on your system, you can install DBRepo with:

```shell
curl -sSL https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/dev/install.sh | bash
```

## Architecture

The repository is designed as a microservice architecture to ensure scalability and the utilization of various
technologies. The conceptualized microservices operate the basic database operations, data versioning as well as
*findability*, *accessability*, *interoperability* and *reuseability* (FAIR).

<figure markdown>
![DBRepo architecture](images/architecture-docker-compose.svg)
<figcaption>Architecture of the services deployed via Docker Compose</figcaption>
</figure>

Alternatively, you can also deploy DBRepo with [Helm](../deployment-helm/) in your virtual machine instead.

## Environment Values

| Key                    | Type   | Default                                                                                                                                                                                                                                                                                                                                                                                                    | Description                                                                                                                                                                                                                                                                   |
|------------------------|--------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `DBREPO_CLIENT_ID`     | string | `dbrepo-client`                                                                                                                                                                                                                                                                                                                                                                                            | Client ID of the keycloak client for API communication.                                                                                                                                                                                                                       |
| `DBREPO_CLIENT_SECRET` | string | `MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG`                                                                                                                                                                                                                                                                                                                                                                         | Client secret of the keycloak client, this should be changed in the admin console of keycloak.                                                                                                                                                                                |
| `JWT_ISSUER`           | string | `http://localhost/api/auth/realms/dbrepo`                                                                                                                                                                                                                                                                                                                                                                  | The issuer in the JWT `iss` field of the (decoded) token. Public deployments with hostnames other than localhost need to change that. The issuer always has the form `<PROTOCOL>://<HOSTNAME>/api/auth/realms/dbrepo`, e.g. change PROTOCOL to https for SSL/TLS deployments. |
| `JWT_PUBKEY`           | string | `MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqqnHQ2BWWW9vDNLRCcxD++xZg/16oqMo/c1l+lcFEjjAIJjJp/HqrPYU/U9GvquGE6PbVFtTzW1KcKawOW+FJNOA3CGo8Q1TFEfz43B8rZpKsFbJKvQGVv1Z4HaKPvLUm7iMm8Hv91cLduuoWx6Q3DPe2vg13GKKEZe7UFghF+0T9u8EKzA/XqQ0OiICmsmYPbwvf9N3bCKsB/Y10EYmZRb8IhCoV9mmO5TxgWgiuNeCTtNCv2ePYqL/U0WvyGFW0reasIK8eg3KrAUj8DpyOgPOVBn3lBGf+3KFSYi+0bwZbJZWqbC/Xlk20Go1YfeJPRIt7ImxD27R/lNjgDO/MwIDAQAB` | Public key that can verify the JWT signature, this should be changed.                                                                                                                                                                                                         |
| `JWT_CERT`             | string | `MIICmzCCAYMCBgGG3GWyBTANBgkqhkiG9w0BAQsFADARMQ8wDQYDVQQDDAZkYnJlcG8wHhcNMjMwMzEzMTkxMzE3WhcNMzMwMzEzMTkxNDU3WjARMQ8wDQYDVQQDDAZkYnJlcG8wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCqqcdDYFZZb28M0tEJzEP77FmD/Xqioyj9zWX6VwUSOMAgmMmn8eqs9hT9T0a+q4YTo9tUW1PNbUpwprA5b4Uk04DcIajxDVMUR/PjcHytmkqwVskq9AZW/Vngdoo+8tSbuIybwe/3Vwt266hbHpDcM97a+DXcYooRl7tQWCEX7RP27wQrMD9epDQ6IgKayZg9vC9/03dsIqwH9jXQRiZlFvwiEKhX2aY7lPGBaCK414JO00K/Z49iov9TRa/IYVbSt5qwgrx6DcqsBSPwOnI6A85UGfeUEZ/7coVJiL7RvBlsllapsL9eWTbQajVh94k9Ei3sibEPbtH+U2OAM78zAgMBAAEwDQYJKoZIhvcNAQELBQADggEBAASnN1Cuif1sdfEK2kWAURSXGJCohCROLWdKFjaeHPRaEfpbFJsgxW0Yj3nwX5O3bUlOWoTyENwnXSsXMQsqnNi+At32CKaKO8+AkhAbgQL9F0B+KeJwmYv3cUj5N/LYkJjBvZBzUZ4Ugu5dcxH0k7AktLAIwimkyEnxTNolOA3UyrGGpREr8MCKWVr10RFuOpF/0CsJNNwbHXzalO9D756EUcRWZ9VSg6QVNso0YYRKTnILWDn9hcTRnqGy3SHo3anFTqQZ+BB57YbgFWy6udC0LYRB3zdp6zNti87eu/VEymiDY/mmo1AB8Tm0b6vxFz4AKcL3ax5qS6YnZ9efSzk=IJjJp/HqrPYU/U9GvquGE6PbVFtTzW1KcKawOW+FJNOA3CGo8Q1TFEfz43B8rZpKsFbJKvQGVv1Z4HaKPvLUm7iMm8Hv91cLduuoWx6Q3DPe2vg13GKKEZe7UFghF+0T9u8EKzA/XqQ0OiICmsmYPbwvf9N3bCKsB/Y10EYmZRb8IhCoV9mmO5TxgWgiuNeCTtNCv2ePYqL/U0WvyGFW0reasIK8eg3KrAUj8DpyOgPOVBn3lBGf+3KFSYi+0bwZbJZWqbC/Xlk20Go1YfeJPRIt7ImxD27R/lNjgDO/MwIDAQAB` | Public key that can verify the JWT signature, this should be changed.                                                                                                                                                                                                         |

## Requirements

### Hardware

For this small, local, test deployment any modern hardware would suffice, we recommend a dedicated virtual machine with
the following settings. Note that most of the vCPU and RAM resources will be needed for starting the infrastructure,
this is because of Docker. During idle times, the deployment will use significantly less resources.

- 4 vCPU cores
- 16GB RAM memory
- 100GB SSD storage

### Software

Install Docker Engine for your operating system. There are excellent guides available for Linux, we highly recommend
to use a stable distribution such as [:simple-debian: Debian](https://www.debian.org/download). In the following guide
we only consider Debian.

## Deployment

We maintain a rapid prototype deployment option through Docker Compose (v2.17.0 and newer). This deployment creates the
core infrastructure and a single Docker container for all user-generated databases.

    curl -sSL https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/dev/install.sh | sudo bash

View the logs:

    docker compose logs -f

You should now be able to view the front end at [http://localhost:80](http://localhost:80).

Please be warned that the default configuration is not intended for public deployments. It is only intended to have a
running system within minutes to play around within the system and explore features. It is strongly advised to change 
the default `.env` environment variables.

!!! warning "Known security issues with the default configuration"

    The system is auto-configured for a small, local, test deployment and is *not* secure! You need to make modifications
    in various places to make it secure:

    * **Authentication Service**:

        a. You need to use your own instance or configure a secure instance using a (self-signed) certificate.
           Additionally, when serving from a non-default Authentication Service, you need to put it into the 
           `JWT_ISSUER` environment variable (`.env`).

        b. You need to change the default admin user `fda` password in Realm
           master > Users > fda > Credentials > Reset password.

        c. You need to change the client secrets for the clients `dbrepo-client` and `broker-client`. Do this in Realm
           dbrepo > Clients > dbrepo-client > Credentials > Client secret > Regenerate. Do the same for the
           broker-client.

        d. You need to regenerate the public key of the `RS256` algorithm which is shared with all services to verify 
           the signature of JWT tokens. Add your securely generated private key in Realm 
           dbrepo > Realm settings > Keys > Providers > Add provider > rsa.

    * **Broker Service**: by default, this service is configured with an administrative user that has major privileges.
      You need to change the password of the user *fda* in Admin > Update this user > Password. We found this
      [simple guide](https://onlinehelp.coveo.com/en/ces/7.0/administrator/changing_the_rabbitmq_administrator_password.htm)
      to be very useful.

    * **Search Database**: by default, this service is configured to require authentication with an administrative user
      that is allowed to write into the indizes. Following
      this [simple guide](https://www.elastic.co/guide/en/elasticsearch/reference/8.7/reset-password.html), this can be
      achieved using the command line.

    * **Gateway Service**: by default, no HTTPS is used that protects the services behind. You need to provide a trusted
      SSL/TLS certificate in the configuration file or use your own proxy in front of the Gateway Service. See this
      [simple guide](http://nginx.org/en/docs/http/configuring_https_servers.html) on how to install a SSL/TLS
      certificate on NGINX.

## Upgrade Guide

### 1.2 to 1.3

In case you have a previous deployment from version 1.2, shut down the containers and back them up manually. You can do
this by using the `busybox` image. Replace `deadbeef` with your container name or hash:

```console
export NAME=dbrepo-userdb-xyz
docker run --rm --volumes-from $NAME -v /home/$USER/backup:/backup busybox tar pcvfz /backup/$NAME.tar.gz /var/lib/mysql
```

!!! danger "Wipe all traces of DBRepo from your system"

    To erase all traces of DBRepo from your computer or virtual machine, the following commands delete all containers,
    volumes and networks that are present, execute the following **dangerous** command. It will **wipe** all information
    about DBRepo from your system (excluding the images).
    
    ```console
    docker container stop $(docker container ls -aq -f name=^/dbrepo-.*) || true
    docker container rm $(docker container ls -aq -f name=^/dbrepo-.*) || true
    docker volume rm $(docker volume ls -q -f name=^dbrepo-.*) || true
    docker network rm $(docker network ls -q -f name=^dbrepo-.*) || true
    ```

You can restore the volume *after* downloading the new 1.3 images and creating the infrastructure:

```console
export NAME=dbrepo-userdb-xyz
export PORT=12345
docker container create -h $NAME --name $NAME -p $PORT:3306 -e MARIADB_ROOT_PASSWORD=mariadb --network userdb -v /backup mariadb:10.5
docker run --rm --volumes-from $NAME -v /home/$USER/backup/.tar.gz:/backup/$NAME.tar.gz busybox sh -c 'cd /backup && tar xvfz /backup/$NAME.tar.gz && cp -r /backup/var/lib/mysql/* /var/lib/mysql'
```

Future releases will be backwards compatible and will come with migration scripts.

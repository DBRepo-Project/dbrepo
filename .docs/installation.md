---
author: Martin Weise
---

# Installation

[![Image Pulls](https://img.shields.io/docker/pulls/dbrepo/data-service?style=flat&cacheSeconds=3600)](https://hub.docker.com/u/dbrepo){ tabindex=-1 }

## TL;DR

If you have [Docker](https://docs.docker.com/engine/install/) already installed on your system, you can install DBRepo with:

```shell
curl -sSL https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-1.4.4/install.sh | bash
```

Or perform a [custom install](#custom-install).

## Requirements

### Hardware

For this small, local, test deployment any modern hardware would suffice, we recommend a dedicated virtual machine with
the following settings.

- min. 8 vCPU cores
- min. 8GB free RAM memory
- min. 200GB free SSD storage
- min. 100Mbit/s connection

*Optional*: public IP-address if you want to secure the deployment with a (free) TLS-certificate from Let's Encrypt.

!!! info "Resource Consumption"

    Note that most of the vCPU and RAM resources will be needed for starting the infrastructure, this is because of
    Docker. During operation and especially idle times, the deployment will use significantly less resources.

### Software

We only test the Docker Compose deployment with the 
official [Docker Engine](https://docs.docker.com/engine/install/debian/) installed on 
a [Debian](https://www.debian.org/)-based operating system. Other software deployments (e.g. Docker Desktop on Windows)
are *not* recommended and not tested.

## Custom Install

In case you prefer a customized install, start by downloading the `docker-compose.yml` file used to define the services:

```bash
curl -O docker-compose.yml -sSL https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-1.4.4/.docker/docker-compose.yml
```

Create the folder `dist/` that hold necessary configuration files and download the Metadata Database schema and initial 
data to display the created Data Database container:

```bash
mkdir -p dist
curl -O dist/setup-schema.sql -sSL https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-1.4.4/dbrepo-metadata-db/setup-schema.sql
curl -O dist/setup-data.sql -sSL https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-1.4.4/dbrepo-metadata-db/setup-data.sql
```

Download the Broker Service configuration files:

```bash
curl -O dist/rabbitmq.conf -sSL https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-1.4.4/dbrepo-broker-service/rabbitmq.conf
curl -O dist/enabled_plugins -sSL https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-1.4.4/dbrepo-broker-service/enabled_plugins
curl -O dist/definitions.json -sSL https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-1.4.4/dbrepo-broker-service/definitions.json
```

!!! warning "Default admin user credentials"

    Note that you need to change the default user credentials `fda:fda` of the Broker Service by setting `users.0.name`
    and `users.0.password_hash` of the `definitions.json` file. The `password_hash` can be created by executing 
    `./helm/dbrepo/hack/generate-rabbitmq-pw.sh <your_password>`.

Download the Gateway Service configuration file (or integrate it into your existing NGINX reverse proxy config):

```bash
curl -O dist/dbrepo.conf -sSL https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-1.4.4/dbrepo-gateway-service/dbrepo.conf
```

Download the S3 configuration for the Storage Service:

```bash
curl -O dist/s3_config.conf -sSL https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-1.4.4/dbrepo-storage-service/s3_config.conf
```

Continue the custom install by customizing the [User Interface](../api/ui).

## Architecture

The repository is designed as a service-based architecture to ensure scalability and the utilization of various
technologies. The conceptualized microservices operate the basic database operations, data versioning as well as
*findability*, *accessability*, *interoperability* and *reuseability* (FAIR).

<figure markdown>
![DBRepo architecture](images/architecture-docker-compose.svg)
<figcaption>Architecture of the services deployed via Docker Compose</figcaption>
</figure>

Please note that we only save the state of the databases as well as the [Broker Service](../broker-service)
since RabbitMQ maintains state inside the container.

## Deployment

We maintain a rapid prototype deployment option through Docker Compose (v2.17.0 and newer). This deployment creates the
core infrastructure and a single Docker container for all user-generated databases.

View the logs:

    docker compose logs -f

You should now be able to view the front end at [http://localhost](http://localhost).

Please be warned that the default configuration is not intended for public deployments. It is only intended to have a
running system within minutes to play around within the system and explore features. It is strongly advised to change 
the default `.env` environment variables.

### Troubleshooting

In case the deployment is unsuccessful, we have explanations on their origin and solutions to the most common errors:

**Are you trying to mount a directory onto a file (or vice-versa)?**

:   *Origin*:   Docker Compose does not find all files referenced in the `volumes` section of your `docker-compose.yml`
                file.
:   *Solution*: Ensure all mounted files in the `volumes` section of your `docker-compose.yml` exist and have correct
                file permissions (`0644`) to be found in the filesystem. Note that paths containing directories may not
                work when using Windows instead of the supported Linux.

**The Docker images have been updated but my deployment is not receiving the updates**

:   *Origin*:   Your local Docker image cache is not up-to-date and needs to fetch the remote changes.
:   *Solution*: Update your local Docker image cache by executing `docker compose pull`, it automatically downloads
                all Docker images that have updates. Then apply the new images with `docker compose up -d`.

**Error response from daemon: Error starting userland proxy: listen tcp4 0.0.0.0:xyz: bind: address already in use**

:   *Origin*:   Your deployment machine (e.g. laptop, virtual machine) has the port `xyz` already assigned. Some service
                or application is already listening to this port.
:   *Solution*: This service or application needs to be stopped. You can find out the service or application via
                `sudo netstat -tulpn` (sudo is necessary for the process id) and then stop the service or application
                gracefully or force a stop via `kill -15 PID` (not recommended).

**IllegalArgumentException values less than -1 bytes are not supported**

:   *Origin*:   Your deployment machine (e.g. laptop, virtual machine) appears to not have enough RAM assigned.
:   *Solution*: Assign more RAM to the deployment machine (e.g. add vRAM to the virtual machine).

**HTTP access denied: user 'admin' - invalid credentials**

:   *Origin*:   The broker service cannot bind to the identity service due to wrong configuration.
:   *Solution*: This is very likely due to a wrong `auth_ldap.dn_lookup_bind.password` in `rabbitmq.conf`. The error
                indicates that LDAP check is not even attempted.

## Next Steps

You should now be able to view the front end at [http://localhost](http://localhost).

Please be warned that the default configuration is not intended for public deployments. It is only intended to have a
running system within minutes to play around within the system and explore features. It is strongly advised to change 
the default `.env` environment variables.

Next, create a [user account](../api/#create-user-account) and 
then [create a database](../api/#create-database) to [import a dataset](../api/#import-dataset).

## Limitations

!!! info "Alternative Deployments"

    Alternatively, you can also deploy DBRepo with [Kubernetes](../deployment-helm) in your virtual machine instead.

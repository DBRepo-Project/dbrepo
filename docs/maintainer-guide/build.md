---
author: Martin Weise
---

!!! danger "Security Disclaimer"

    This quick default installation should **not be considered secure**. It is intended for local testing and
    demonstration and should not be used in public deployments or in production. It is a quick installation method and
    is intended for a quick look at DBRepo.

## TL;DR

Clone the source code repository and build the container images:

```shell
git clone git@gitlab.phaidra.org:fair-data-austria-db-repository/fda-services.git && \
git checkout release-1.10 && \
make build-java-lib build-auth-event-listener build-images
```

Then start DBRepo and visit [`http://localhost`](http://localhost) in the VM browser:

```shell
docker compose up -d
```

## Prerequisites

### Buildtime

!!! info

    * Docker 28.x

Utility tools to run the build commands:

=== "Debian"

    ```shell
    apt install git make bash
    ```

### Runtime

!!! info

    * min. 8 vCPU cores
    * min. 20GB free RAM memory

## Build

Clone the source code repository:

```shell
git clone git@gitlab.phaidra.org:fair-data-austria-db-repository/fda-services.git && \
git checkout release-1.10
```

Build the Docker container images and auxiliary files needed for the Auth Service:

```shell
make build-java-lib build-auth-event-listener build-images
```

## Run

Then start DBRepo and visit [`http://localhost`](http://localhost) in the VM browser:

```shell
docker compose up -d
```

!!! debug "Shorthand Dev Build"

    You can build everything and (re-)start the necessary services between each build conveniently:

    ```shell
    make start-dev
    ```
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
curl -sSL https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-1.12/install.sh | bash
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
SSL/TLS certificate is recommended. Follow the [Secure Installation](#secure-installation) guide.

## Uninstall

Remove DBRepo by stopping and removing all containers and volumes:

```shell
docker container stop $(docker container ls -f "name=dbrepo-*" -aq)
docker container rm $(docker container ls -f "name=dbrepo-*" -aq)
docker volume rm $(docker volume ls -f "name=dbrepo_*" -q)
```

!!! danger "Further configuration is highly recommended"

    See "Security Disclaimer" above, this quick &amp; non-secure installation needs to be configured further to achieve
    basic security guarantees. Please visit 
    the [configuration](/infrastructures/dbrepo/1.13/maintainer-guide/configuration/) page in the next step to complete
    the installation.
---
author: Martin Weise
---

!!! warning "Deprecated from v2+"

    Starting v2 we will no longer support Docker Compose as deployment option. Instead, a lightweight cluster deployment
    based on the CNCF-sandboxed [K3S](https://k3s.io/) project will be used.

!!! danger "Security Disclaimer"

    This quick default installation should **not be considered secure**. It is intended for local testing and
    demonstration and should not be used in public deployments or in production. It is a quick installation method and
    is intended for a quick look at DBRepo.

## TL;DR

Install DBRepo in one line:

```shell
curl -sSL https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-1.11/install.sh | bash
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

!!! danger "Further configuration is highly recommended"

    See "Security Disclaimer" above, this quick &amp; non-secure installation needs to be configured further to achieve
    basic security guarantees. Please visit 
    the [configuration](/infrastructures/dbrepo/1.12/maintainer-guide/configuration/) page in the next step to complete
    the installation.
---
author: Martin Weise
---

!!! danger "Security Disclaimer"

    This quick default installation should **not be considered secure**. It is intended for local testing and
    demonstration and should not be used in public deployments or in production. It is a quick installation method and
    is intended for a quick look at DBRepo.

## TL;DR

Download the KVM configuration and virtual disk:

```shell
wget https://www.ifs.tuwien.ac.at/infrastructures/dbrepo/1.10/dbrepo.qcow2.gz
gunzip dbrepo.qcow2.gz
```

Then start DBRepo and visit [`http://localhost`](http://localhost) in the VM browser:

```shell
docker compose up -d
```

## Prerequisites

!!! info

    * min. 8 vCPU cores
    * min. 20GB free RAM memory

## Configuration

The image is based on Debian 12 and has the following user credentials pre-configured:

* `root:changeme`
* `dbrepo:dbrepo`

The installation is located in the default home directory of the `dbrepo` user:

```shell
/home/dbrepo/
  |- config/             # config directory
  |- docker-compose.yaml # services definition
```
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
curl -sSL https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-1.13/.scripts/install.sh | bash
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

## Upgrade: preserve SeaweedFS uploads

The Compose configuration introduced in [#135](https://github.com/DBRepo-Project/dbrepo/pull/135)
stores SeaweedFS data in the named volume `storage-service-data`. Older installations use an
anonymous volume. Switching to the new configuration without copying that volume makes existing
S3 objects unavailable. This affects uploads that have not been imported yet; imported database
rows are stored separately.

For the first upgrade to this configuration, copy the data before starting the new storage service.
Run the following commands in the directory containing your installation's `docker-compose.yml`,
using the same shell throughout. Keep your existing `.env` and Compose project name.

First, record the old volume and stop the stack. Do this before replacing the Compose file or
removing any containers:

```shell
set -eu
OLD=$(docker inspect "$(docker compose ps -a -q dbrepo-storage-service)" \
  --format '{{range .Mounts}}{{if eq .Destination "/data"}}{{.Name}}{{end}}{{end}}')
test -n "$OLD"
docker compose stop
```

Update `docker-compose.yml` to the new version. Then create the replacement container without
starting it, copy the old data, and start the stack:

```shell
docker compose create --no-deps dbrepo-storage-service
NEW=$(docker inspect "$(docker compose ps -a -q dbrepo-storage-service)" \
  --format '{{range .Mounts}}{{if eq .Destination "/data"}}{{.Name}}{{end}}{{end}}')
test -n "$NEW"
test "$OLD" != "$NEW"
docker run --rm -v "$OLD":/from:ro -v "$NEW":/to alpine cp -a /from/. /to/
docker compose up -d
```

Verify that pending uploads can still be imported before removing the old volume. Keep it until
verification is complete; do not run volume pruning or `docker compose down -v` during migration.
The new configuration disables disk preallocation and uses 64 MB SeaweedFS volumes. This is a
per-volume setting, not an upload-size or total-storage limit. Helm installations are unaffected.

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

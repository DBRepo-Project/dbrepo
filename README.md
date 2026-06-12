[![CI/CD Status](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/badges/master/pipeline.svg)](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services)
[![CI/CD Coverage](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/badges/master/coverage.svg)](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services)
[![Latest Release](https://img.shields.io/gitlab/v/release/fair-data-austria-db-repository%2Ffda-services?gitlab_url=https%3A%2F%2Fgitlab.phaidra.org&display_name=release&style=flat)](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services)
[![PyPI Library version](https://img.shields.io/pypi/v/dbrepo)](https://pypi.org/project/dbrepo/)
[![Image Pulls](https://img.shields.io/docker/pulls/dbrepo/data-service?style=flat)](https://hub.docker.com/u/dbrepo)
[![Helm Chart version](https://img.shields.io/endpoint?url=https://artifacthub.io/badge/repository/dbrepo)](https://artifacthub.io/packages/helm/dbrepo/dbrepo)
[![GitLab License](https://img.shields.io/gitlab/license/fair-data-austria-db-repository%2Ffda-services?gitlab_url=https%3A%2F%2Fgitlab.phaidra.org%2F&style=flat&cacheSeconds=3600)](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services)

<img src="./dbrepo-ui/public/logo.png" width="200" alt="DBRepo &mdash; Repository for Data in Databases" />

## tl;dr

If you have [Docker](https://docs.docker.com/engine/install/) already installed on your system, you can install DBRepo
with:

```bash
curl -sSL curl -sSL https://raw.githubusercontent.com/DBRepo-Project/dbrepo/refs/heads/main/.scripts/install.sh | bash
```

## Documentation

Find a system description, component documentation and endpoint documentation
online: [https://dbrepo.github.io](https://dbrepo.github.io).

## Development

Contributions are always welcome and encouraged, please read the [contribution overview](./CONTRIBUTING.md).

### Build

Build the java library from scratch (you have to do this only once unless something has changed in `./lib/java/`):

```shell
make build-java-lib
```

Build the python library from scratch (you have to do this only once unless something has changed in `./lib/python/`):

```shell
make build-python-lib
```

Build the docker images:

```shell
docker compose build
```

### Run

After building, run the docker containers:

```shell
docker compose up -d
```

When updating the configuration, you need to recreate the Gateway Service
additionally: `docker compose restart dbrepo-gateway-service`.

```shell
docker compose logs -f
```

## Release Checklist

Build the Helm Chart schema:

```shell
make build-helm
```

Build the OpenAPI docs:

```shell
make gen-openapi-doc
```

## Acknowledgements

We want to thank the following organizations:

* [ARI&amp;Snet](https://forschungsdaten.at/en/arisnet/) for their continuous support in project work and funding.
* [TU.it &amp; .digital office](https://www.it.tuwien.ac.at/en/) for their continuous support in project
  work, [funding](https://www.tuwien.at/tu-wien/organisation/zentrale-bereiche/digital-office/projekte/dcall-2023-projekte)
  and compute resources provided in-kind.
* Bundesministerium für Bildung, Wissenschaft und Forschung (BMBWF) for funding during
  the [call](https://www.bmbwf.gv.at/Themen/HS-Uni/Aktuelles/Ausschreibung--Digitale-und-soziale-Transformation-in-der-Hochschulbildung-.html)
  "Digitale und soziale Transformation in der Hochschulbildung".
* [Deutsche Forschungsgemeinschaft (DFG)](https://www.dfg.de/) for funding the [DBRepo aaS project](https://gepris.dfg.de/gepris/projekt/562333837).

## License

The source code is licensed under [Apache 2.0](https://opensource.org/licenses/Apache-2.0).

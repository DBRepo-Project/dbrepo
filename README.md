[![pipeline status](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/badges/master/pipeline.svg)](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/commits/master)
[![coverage report](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/badges/master/coverage.svg)](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/commits/master)
[![license](.gitlab/license.svg)](https://opensource.org/licenses/Apache-2.0)
[![release](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/badges/release.svg)](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags)
[![Artifact Hub](https://img.shields.io/endpoint?url=https://artifacthub.io/badge/repository/dbrepo)](https://artifacthub.io/packages/helm/dbrepo/dbrepo-core)

![DBRepo &mdash; Repository for Data in Databases](./.gitlab/logo.png)

## tl;dr

If you have [Docker](https://docs.docker.com/engine/install/) already installed on your system, you can install DBRepo
with:

```bash
curl -sSL https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/master/install.sh | bash
```

## Documentation

Find a system description, component documentation and endpoint documentation 
online: https://www.ifs.tuwien.ac.at/infrastructures/dbrepo/.

## Development

Contributions are always welcome and encouraged, please read the [contribution overview](./CONTRIBUTING.md) and
contact [Prof. Andreas Rauber](http://www.ifs.tuwien.ac.at/~andi/) or [Martin Weise](https://ec.tuwien.ac.at/~weise/).

### Build

Install the build dependencies under Debian 
12 ([Instructions for Docker Engine](https://docs.docker.com/engine/install/debian/#install-using-the-repository)):

```console
$ apt install -y bash maven openjdk-17-jdk nodejs && npm install --global yarn
$ node --version
v18.19.0
```

Build the Docker containers:

```console
./bin/build-docker.sh
```

### Test

Install the [build dependencies](#build) as they also cover the test dependencies.

Test the backend and frontend:

```console
./bin/test.sh
```

## Run

After [building the docker containers](#build) you can run them using the default `docker-compose.yml` in the root of
the sourcecode directory. This starts all services in the background (as daemons hence the `-d` flag).

```console
$ docker compose up -d
```

Optionally view all logs in real-time:

```console
$ docker compose logs -f
```

## Acknowledgements

We want to thank the following organizations:

* Bundesministerium für Bildung, Wissenschaft und Forschung (BMBWF) for funding during
  the [call](https://www.bmbwf.gv.at/Themen/HS-Uni/Aktuelles/Ausschreibung--Digitale-und-soziale-Transformation-in-der-Hochschulbildung-.html)
  "Digitale und soziale Transformation in der Hochschulbildung".
* [TU.it &amp; .digital office](https://www.it.tuwien.ac.at/en/) for their continuous support in project 
  work, [funding](https://www.tuwien.at/tu-wien/organisation/zentrale-bereiche/digital-office/projekte/dcall-2023-projekte)
  and compute resources provided in-kind.

## License

The source code is licensed under [Apache 2.0](https://opensource.org/licenses/Apache-2.0).
[![pipeline status](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/badges/master/pipeline.svg)](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/commits/master)
[![coverage report](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/badges/master/coverage.svg)](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/commits/master)
[![license](.gitlab/license.svg)](https://opensource.org/licenses/Apache-2.0)

# FAIR Data Austria Database Repository

## Deployment

Download the Docker Compose template and the environment file:

```console
$ curl -o docker-compose.yml https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/master/docker-compose.prod.yml
$ curl -o .env https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/master/.env.unix.example
```

Start the Docker containers:

```console
$ docker compose up -d
$ docker compose logs -f
```

## Development

### Build

Local development minimum requirements:

- Ubuntu 18.04 LTS (Rocky Linux is also supported)
- Apache Maven 3.0.0
- OpenJDK 11.0.0
- Docker Engine 20.10.0
- Docker Compose 1.28.0

Everything is handled by compose, just build it by running:

```console
$ docker-compose build --parallel
```

A more detailed description on how
to get started is available at our documentation
website: [https://dbrepo-docs.ossdip.at/getting-started/](https://dbrepo-docs.ossdip.at/getting-started/)

### Run

Copy and *optionally* edit the environment:

```console
$ cp .env.unix .env
$ docker compose -f ./docker-compose.prod.yml up -d
$ docker compose -f ./docker-compose.prod.yml logs -f 
```

Once the services are started, open [http://localhost:3000](http://localhost:3000).

## Contribute

Contributions are always welcome and encouraged, simply fork the repository and
contact [Andreas Rauber](http://www.ifs.tuwien.ac.at/~andi/).

# License

This work is licensed under
a [Creative Commons Attribution 4.0 International License](http://creativecommons.org/licenses/by/4.0/)
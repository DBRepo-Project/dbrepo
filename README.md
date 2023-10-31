[![pipeline status](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/badges/master/pipeline.svg)](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/commits/master)
[![coverage report](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/badges/master/coverage.svg)](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/commits/master)
[![license](.gitlab/license.svg)](https://opensource.org/licenses/Apache-2.0)

# DBRepo &mdash; A Repository for Databases

## tl;dr

```shell
docker compose up -d
docker compose logs -f
```

## Build

Build DBRepo from scratch:

```shell
make build
```

## Documentation

More documentation can be found online: https://www.ifs.tuwien.ac.at/infrastructures/dbrepo/

## Development

### CI/CD

We get compute resources in-kind from [dataLAB](https://www.it.tuwien.ac.at/en/services/network-and-servers/datalab)
to run our pipeline:

<p align="center">
<img src="./.gitlab/gitlab-runner.png" alt="Gitlab runner configuration in the cluster" width="732" height="262" /><br/>
<i><strong>Figure 1.</strong> Gitlab runner configuration in the cluster.</i>
</p>

Minikube cluster with 6vCPU and 28GB RAM. The CI pipeline is configured as follows in the `config.toml`:

```toml
concurrent = 10
[[runners]]
  executor = "kubernetes"
  environment = [
    "FF_USE_LEGACY_KUBERNETES_EXECUTION_STRATEGY=false"
  ]
  [runners.kubernetes]
    namespace = "{{.Release.Namespace}}"
    privileged = true
    allowed_services = ["docker:24-dind"]
    [[runners.kubernetes.services]]
      name = "docker:24-dind"
      command = [ "--insecure-registry=registry-mirror:80" ]
      alias = "docker"
    [[runners.kubernetes.volumes.empty_dir]]
      name = "rundind"
      mount_path = "/var/run/dind"
      medium = "Memory"
    [[runners.kubernetes.volumes.empty_dir]]
      name = "tmp"
      mount_path = "/tmp"
      medium = "Memory"
```

For each job in the CI/CD pipeline, a pod with three containers is started:

1. `build` the main build container, you can *freely* specify any image with `image: xyz` as base
2. `helper` the default helper container.
3. `svc-0` the Docker-in-Docker sidecar (rootless executed as user `rootless`/`1000`) exposing the Docker socket to the
   `build` container under `

*Note.* Only Docker-in-Docker (dind) is allowed as service in the pipeline currently. For each job, a 
dind-sidecar container `svc-0` is started that exposes the Docker socket at `/var/run/dind/docker.sock` in the `build` 
container you can freely configure how you want. We are aware this is not optimal as it exposes *root* privileges in the
cluster.

The full CI/CD pipeline Helm chart is documented in 
the [`fda-deployment`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-deployment/-/tree/master/charts/dbrepo-devops)
repository.

## Contribute

Contributions are always welcome and encouraged, simply fork the repository and
contact [Andreas Rauber](http://www.ifs.tuwien.ac.at/~andi/).

## Acknowledgements

We want to thank the following organizations:

* Bundesministerium für Bildung, Wissenschaft und Forschung (BMBWF) for funding during 
  the [call](https://www.bmbwf.gv.at/Themen/HS-Uni/Aktuelles/Ausschreibung--Digitale-und-soziale-Transformation-in-der-Hochschulbildung-.html)
  "Digitale und soziale Transformation in der Hochschulbildung".
* [TU.it](https://www.it.tuwien.ac.at/en/) for their continuous support in project work, funding and compute resources 
  provided in-kind.

## License

This work is licensed under
a [Creative Commons Attribution 4.0 International License](http://creativecommons.org/licenses/by/4.0/)
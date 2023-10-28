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

## Development

### CI/CD

Minikube cluster with 6vCPU and 28GB RAM. The CI pipeline is configured as follows in the CD:

```toml
[[runners]]
  executor = "kubernetes"
  environment = [
    "FF_USE_LEGACY_KUBERNETES_EXECUTION_STRATEGY=false"
  ]
  [runners.kubernetes]
    namespace = "{{.Release.Namespace}}"
    privileged = true
    allowed_services = ["docker:24-dind-rootless"]
    [[runners.kubernetes.services]]
      name = "docker:24-dind-rootless"
      alias = "docker"
    [[runners.kubernetes.volumes.empty_dir]]
      name = "rundind"
      mount_path = "/var/run/dind"
      medium = "Memory"
```

**Note** that only rootless Docker-in-Docker (dind) is allowed as service in the pipeline currently. For each job,
a dind-sidecar `svc-0` is started that exposes the Docker socket at `/var/run/dind/docker.sock` in the `build` container
you can freely configure how you want.

## Contribute

Contributions are always welcome and encouraged, simply fork the repository and
contact [Andreas Rauber](http://www.ifs.tuwien.ac.at/~andi/).

# License

This work is licensed under
a [Creative Commons Attribution 4.0 International License](http://creativecommons.org/licenses/by/4.0/)
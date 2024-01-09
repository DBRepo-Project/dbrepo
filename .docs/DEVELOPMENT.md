# Development Guide

## Dependencies

Local development depends on the following packages:

* [Apache Maven](https://maven.apache.org/) 3+
* [Java JDK](https://openjdk.org/) 17 (LTS)
* [Docker Engine](https://docs.docker.com/engine/install/) 24+

Optional but recommended:

* [GNU Make](https://www.gnu.org/software/make/) 4+

## Getting Started

The Java-based services share some classes from the `metadata-service`. You need to install them locally as Maven
library via:

```shell
mvn -f ./dbrepo-metadata-service/pom.xml clean install -DskipTests
```

## Testing

We practice test-driven development and require contributors to test their code with at least 90% code coverage.

## Code Versioning

### Branching Strategy

<p align="center">
<img src="../.gitlab/branching-strategy.png" alt="Branching strategy from the master-dev-feature branches and release branches." width="732" height="391" /><br/>
<i><strong>Figure 1.</strong> Branching strategy of the source code development.</i>
</p>

### CI/CD

We get compute resources in-kind from [dataLAB](https://www.it.tuwien.ac.at/en/services/network-and-servers/datalab)
to run our pipeline:

<p align="center">
<img src="../.gitlab/gitlab-runner.png" alt="Gitlab runner configuration in the cluster" width="732" height="262" /><br/>
<i><strong>Figure 2.</strong> Gitlab runner configuration in the cluster.</i>
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
      command = [ "--registry-mirror=http://docker-io-mirror:80", "--insecure-registry=docker-io-mirror:80", "--registry-mirror=http://gcr-io-mirror:80", "--insecure-registry=gcr-io-mirror:80", "--registry-mirror=http://ghcr-io-mirror:80", "--insecure-registry=ghcr-io-mirror:80", "--registry-mirror=http://k8s-io-mirror:80", "--insecure-registry=k8s-io-mirror:80" ]
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
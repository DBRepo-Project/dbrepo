---
author: Martin Weise
---

[![Helm Chart version](https://img.shields.io/endpoint?url=https://artifacthub.io/badge/repository/dbrepo)](https://artifacthub.io/packages/helm/dbrepo/dbrepo){ tabindex=-1 }

## TL;DR

To install DBRepo in your existing cluster, download the
sample [`values.yaml`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/blob/release-1.4.5/helm/dbrepo/values.yaml)
for your deployment and update the variables, especially `hostname`.

```shell
helm upgrade --install dbrepo \
  -n dbrepo \
  "oci://registry.datalab.tuwien.ac.at/dbrepo/helm/dbrepo" \
  --values ./values.yaml \
  --version "1.4.5" \
  --create-namespace \
  --cleanup-on-fail
```

This chart is also on [Artifact Hub](https://artifacthub.io/packages/helm/dbrepo/dbrepo) with a full documentation
about values, etc. Before installing, you need to change credentials, e.g. the Broker Service administrator user
password:

```yaml title="values.yaml"
brokerservice:
  ...
  auth:
    ...
    username: broker
    password: broker
    passwordHash: 1gwjNNTBPKLgyzbsUykfR0JIFC6nNqbNJaxzZ14uPT8JGcTZ
```

The `brokerservice.auth.passwordHash` field is the RabbitMQ SHA512-hash of the `brokerservice.auth.password` field and
can be obtained with
the [`generate-rabbitmq-pw.sh`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/blob/release-1.4.5/helm/dbrepo/hack/generate-rabbitmq-pw.sh)
script:

```console
$ ./generate-rabbitmq-pw.sh my_password
klPdmv4dgnRH64czHolIHAfXvc0G9hc24FQmPlI6eeI1NOf9
```

The script needs the package `xxd` for generation of the random salt. If you don't have `xxd` installed, install it:

* Debian/Ubuntu: `apt install xxd`
* Windows: `choco install xxd`
* MacOS: `brew install coreutils`

## Prerequisites

* Kubernetes 1.24+
* Kubernetes 3.8.0+
* PV provisioner support in the underlying infrastructure

## Limitations

1. MariaDB Galera does not (yet) support XA-transactions required by the authentication service (=Keycloak). Therefore
   only a single MariaDB pod can be deployed at once for the Auth database.
2. The entire Helm deployment is rootless (=`runAsNonRoot=true`) except for
   the [Storage Service](../api/storage-service) which still requires a root user.

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](../../contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

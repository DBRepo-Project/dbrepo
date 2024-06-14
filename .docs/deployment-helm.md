---
author: Martin Weise
---

[![Helm Chart version](https://img.shields.io/endpoint?url=https://artifacthub.io/badge/repository/dbrepo)](https://artifacthub.io/packages/helm/dbrepo/dbrepo){ tabindex=-1 }

## TL;DR

To install DBRepo in your existing cluster, download the
sample [`values.yaml`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/blob/release-1.4.4/helm/dbrepo/values.yaml)
for your deployment and update the variables, especially `hostname`.

```shell
helm upgrade --install dbrepo \
  -n dbrepo \
  "oci://registry.datalab.tuwien.ac.at/dbrepo/helm/dbrepo" \
  --values ./values.yaml \
  --version "1.4.4" \
  --create-namespace \
  --cleanup-on-fail
```

This chart is also on [Artifact Hub](https://artifacthub.io/packages/helm/dbrepo/dbrepo) with a full documentation
about values, etc.

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

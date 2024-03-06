---
author: Martin Weise
---

## TL;DR

To install DBRepo in your existing cluster, download the
sample [`values.yaml`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-deployment/-/raw/master/charts/dbrepo-core/values.yaml?inline=false)
for your deployment and update the variables, especially `hostname`.

```shell
helm upgrade --install dbrepo \
  -n dbrepo \
  "oci://s210.dl.hpc.tuwien.ac.at/dbrepo/helm/dbrepo" \
  --values ./values.yaml \
  --version "__CHARTVERSION__" \
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
   only a single MariaDB pod can be deployed at once for the [auth database](../system-databases-authentication).
2. The entire Helm deployment is rootless (=`runAsNonRoot=true`) except for
   the [Storage Service](../system-services-storage/) which still requires a root user.

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](../contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

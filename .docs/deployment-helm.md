---
author: Martin Weise
---

## TL;DR

To install DBRepo in your existing cluster, download the sample [`values.yaml`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-deployment/-/raw/dev/charts/dbrepo-core/values.yaml?inline=false)
for your deployment and update the variables, especially `hostname`.

```shell
helm upgrade --install dbrepo \
  -n dbrepo \
  "oci://dbrepo.azurecr.io/helm/dbrepo-core" \
  --values ./values.yaml \
  --version "0.1.4" \
  --create-namespace \
  --cleanup-on-fail
```

## Dependencies

Our chart depends on seven other charts which will be automatically resolved when installing our `dbrepo-core` chart:

* Keycloak (Bitnami, v17.3.3) for [Authentication Service](../system-services-authentication)
* MariaDB Galera (Bitnami, v11.0.1) for [Data Database](../system-databases-data) &amp; [Metadata Database](../system-databases-metadata)
* SeaweedFS (SeaweedFS, v3.59.4) for [Storage Service](../system-services-storage)
* OpenSearch (OpenSearch Project, v2.16.0) for [Search Database](../system-databases-search)
* OpenSearch Dashboards (OpenSearch Project, v2.14.0) for [Search Dashboard](../system-other-search-dashboard)
* PostgreSQL HA (Bitnami, v12.1.7) for [Auth Database](../system-databases-auth)
* RabbitMQ (Bitnami, v12.5.1) for [Broker Service](../system-services-broker)
* FluentBit (FluentBit, v0.40.0) for logging in the cluster.

## Limitations

1. MariaDB Galera does not (yet) support XA-transactions required by the authentication service (=Keycloak). Therefore
   only a single MariaDB pod can be deployed at once for the [auth database](../system-databases-auth).

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](../contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

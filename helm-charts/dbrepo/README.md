# DBRepo Helm chart

[DBRepo](https://www.ifs.tuwien.ac.at/infrastructures/dbrepo/__CHARTVERSION__/) is a database repository system that
allows researchers to ingest data into a central, versioned repository through common interfaces.

## TL;DR

Download the
sample [`values.yaml`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/master/helm-charts/dbrepo/values.yaml?inline=true)
for your deployment and update the variables, especially `hostname`.

```bash
helm install my-release "oci://s210.dl.hpc.tuwien.ac.at/dbrepo/helm/dbrepo" --values ./values.yaml --version "__CHARTVERSION__"
```

## Prerequisites

* Kubernetes 1.24+
* Kubernetes 3.8.0+
* PV provisioner support in the underlying infrastructure

## Installing the Chart

To install the chart with the release name `my-release`:

```bash
helm install my-release "oci://s210.dl.hpc.tuwien.ac.at/dbrepo/helm/dbrepo" --values ./values.yaml --version "__CHARTVERSION__"
```

The command deploys DBRepo on the Kubernetes cluster in the default configuration. The Parameters section lists the
parameters that can be configured during installation.

## Uninstalling the Chart

To uninstall/delete the `my-release` deployment:

```bash
helm delete my-release
```

The command removes all the Kubernetes components associated with the chart and deletes the release.

## Parameters

### Common parameters

| Name            | Description                           | Value           |
|-----------------|---------------------------------------|-----------------|
| `namespace`     | Namespace which DBRepo is running in. | `""`            |
| `hostname`      | The hostname for ingress rules.       | `""`            |
| `strategyType`  | Deployments update strategy.          | `RollingUpdate` |
| `clusterDomain` | Internal cluster domain.              | `cluster.local` |

### Metadata Database

The Metadata Database uses the [Bitnami MariaDB Galera](https://artifacthub.io/packages/helm/bitnami/mariadb-galera)
Helm chart. See their documentation for the remaining overridden values.

| Name                       | Description                               | Value         |
|----------------------------|-------------------------------------------|---------------|
| `metadataDb.host`          | Hostname.                                 | `metadata-db` |
| `metadataDb.jdbcExtraArgs` | Extra arguments for the JDBC connections. | `""`          |

### Authentication Service

The Auth Service uses the [Bitnami Keycloak](https://artifacthub.io/packages/helm/bitnami/keycloak) Helm chart. See
their documentation for the remaining overridden values.

| Name                        | Description                                                     | Value                              |
|-----------------------------|-----------------------------------------------------------------|------------------------------------|
| `authService.client.id`     | Client id. This value is publicly known.                        | `dbrepo-client`                    |
| `authService.client.secret` | Client secret. This value should never be known outside DBRepo. | `MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG` |

### Auth Database

The Auth Database uses the [Bitnami PostgreSQL HA](https://artifacthub.io/packages/helm/bitnami/postgresql-ha) Helm
chart. See their documentation for the remaining overridden values.

| Name          | Description                          | Value            |
|---------------|--------------------------------------|------------------|
| `authDb.host` | Hostname. Needed for other services. | `auth-db-pgpool` |
| `authDB.port` | Port. Needed for other services.     | `5432`           |

### Data Database

The Data Database uses the [Bitnami MariaDB Galera](https://artifacthub.io/packages/helm/bitnami/mariadb-galera)
Helm chart. See their documentation for the remaining overridden values. It is important to note that the Data Database 
uses a sidecar to import/export files from the Storage Service.
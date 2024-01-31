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
* Ingress support in the underlying infrastructure
* TLS certificate provisioner support in the underlying infrastructure, e.g. [cert-manager](https://cert-manager.io/)

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

| Name                        | Description                               | Value         |
|-----------------------------|-------------------------------------------|---------------|
| `metadata-db.host`          | Hostname.                                 | `metadata-db` |
| `metadata-db.jdbcExtraArgs` | Extra arguments for the JDBC connections. | `""`          |

### Authentication Service

The Auth Service uses the [Bitnami Keycloak](https://artifacthub.io/packages/helm/bitnami/keycloak) Helm chart. See
their documentation for the remaining overridden values.

| Name                         | Description                                                     | Value                              |
|------------------------------|-----------------------------------------------------------------|------------------------------------|
| `auth-service.client.id`     | Client id. This value is publicly known.                        | `dbrepo-client`                    |
| `auth-service.client.secret` | Client secret. This value should never be known outside DBRepo. | `MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG` |

### Auth Database

The Auth Database uses the [Bitnami PostgreSQL HA](https://artifacthub.io/packages/helm/bitnami/postgresql-ha) Helm
chart. See their documentation for the remaining overridden values.

| Name           | Description                          | Value            |
|----------------|--------------------------------------|------------------|
| `auth-db.host` | Hostname. Needed for other services. | `auth-db-pgpool` |
| `auth-db.port` | Port. Needed for other services.     | `5432`           |

### Data Database

The Data Database uses the [Bitnami MariaDB Galera](https://artifacthub.io/packages/helm/bitnami/mariadb-galera)
Helm chart. See their documentation for the remaining overridden values. It is important to note that the Data Database
uses a sidecar to import/export files from the Storage Service.

### Search Database

The Search Database uses
the [OpenSearch](https://artifacthub.io/packages/helm/opensearch-project-helm-charts/opensearch) Helm
chart. See their documentation for the remaining overridden values.

| Name                 | Description                          | Value       |
|----------------------|--------------------------------------|-------------|
| `search-db.host`     | Hostname. Needed for other services. | `search-db` |
| `search-db.port`     | Port. Needed for other services.     | `9200`      |
| `search-db.username` | Username. Needed for other services. | `admin`     |
| `search-db.password` | Password. Needed for other services. | `admin`     |

### Search Database Dashboard

The Search Database Dashboard uses
the [OpenSearch](https://artifacthub.io/packages/helm/opensearch-project-helm-charts/opensearch-dashboards) Helm
chart. See their documentation for the remaining overridden values.

### Upload Service

| Name                              | Description                            | Value             |
|-----------------------------------|----------------------------------------|-------------------|
| `upload-service.enabled`          | Enables/disabled the deployment.       | `true`            |
| `upload-service.image.registry`   | Registry to pull the image             | `docker.io`       |
| `upload-service.image.repository` | Repository to pull the image           | `tusproject/tusd` |
| `upload-service.image.tag`        | Tag of the image.                      | `v1.12`           |
| `upload-service.replicaCount`     | Number of replicas for the deployment. | `2`               |

### Broker Service

The Broker Service uses the [Bitnami RabbitMQ](https://artifacthub.io/packages/helm/bitnami/rabbitmq)
Helm chart. See their documentation for the remaining overridden values.

| Name                               | Description                                                             | Value                         |
|------------------------------------|-------------------------------------------------------------------------|-------------------------------|
| `broker-service.url`               | Admin API endpoint. Needed for other services.                          | `http://broker-service:15672` |
| `broker-service.host`              | Service hostname. Needed for other services.                            | `broker-service`              |
| `broker-service.port`              | Service port. Needed for other services.                                | `5672`                        |
| `broker-service.virtualHost`       | Virtual host on RabbitMQ. Needed for other services.                    | `dbrepo`                      |
| `broker-service.queueName`         | Queue name on RabbitMQ. Needed for other services.                      | `dbrepo`                      |
| `broker-service.exchangeName`      | Exchange name on RabbitMQ. Needed for other services.                   | `dbrepo`                      |
| `broker-service.routingKey`        | Route binding for queue to exchange defined. Needed for other services. | `dbrepo.#`                    |
| `broker-service.connectionTimeout` | Connection timeout. Needed for other services.                          | `60000`                       |

### Analyse Service

| Name                              | Description                            | Value                      |
|-----------------------------------|----------------------------------------|----------------------------|
| `analyseService.enabled`          | Enables/disabled the deployment.       | `true`                     |
| `analyseService.image.registry`   | Registry to pull the image             | `s210.dl.hpc.tuwien.ac.at` |
| `analyseService.image.repository` | Repository to pull the image           | `dbrepo/analyse-service`   |
| `analyseService.image.tag`        | Tag of the image.                      | `1.4.1`                    |
| `analyseService.image.pullPolicy` | Image pull policy on deployments       | `Always`                   |
| `analyseService.image.debug`      | Enables/disabled the debug logging.    | `false`                    |
| `analyseService.replicaCount`     | Number of replicas for the deployment. | `2`                        |

### Metadata Service

| Name                                       | Description                                                                      | Value                      |
|--------------------------------------------|----------------------------------------------------------------------------------|----------------------------|
| `metadataService.enabled`                  | Enables/disabled the deployment.                                                 | `true`                     |
| `metadataService.image.registry`           | Registry to pull the image                                                       | `s210.dl.hpc.tuwien.ac.at` |
| `metadataService.image.repository`         | Repository to pull the image                                                     | `dbrepo/metadata-service`  |
| `metadataService.image.tag`                | Tag of the image.                                                                | `1.4.1`                    |
| `metadataService.image.pullPolicy`         | Image pull policy on deployments                                                 | `Always`                   |
| `metadataService.image.debug`              | Enables/disabled the debug logging.                                              | `false`                    |
| `metadataService.adminEmail`               | E-Mail address of the administrator displayed for OAI-PMH.                       | `noreply@example.com`      |
| `metadataService.authService.url`          | Url to the Auth Service.                                                         | `http://auth-service`      |
| `metadataService.website`                  | Url to redirect PIDs to.                                                         | `http://example.com`       |
| `metadataService.repositoryName`           | Repository name for OAI-PMH.                                                     | `Database Repository`      |
| `metadataService.datacite.enabled`         | Enable/disable DataCite Fabrica DOI minting.                                     | `false`                    |
| `metadataService.datacite.url`             | DataCite Fabrica API endpoint.                                                   | `https://api.datacite.org` |
| `metadataService.datacite.prefix`          | DataCite Fabrica DOI prefix.                                                     | `""`                       |
| `metadataService.datacite.username`        | DataCite Fabrica API username.                                                   | `""`                       |
| `metadataService.datacite.password`        | DataCite Fabrica API password.                                                   | `""`                       |
| `metadataService.rates.deleteStaleFiles`   | Interval rate to delete stale files in the Storage Service.                      | `60`                       |
| `metadataService.rates.mirror`             | Interval rate to mirror to the Search Database.                                  | `60`                       |
| `metadataService.rates.obtainMetadata`     | Interval rate to obtain metadata from the Data Database.                         | `60`                       |
| `metadataService.rates.deleteStaleQueries` | Interval rate to delete stale queries from the Query Store in the Data Database. | `60`                       |
| `metadataService.replicaCount`             | Number of replicas for the deployment.                                           | `2`                        |

### Data Service

| Name                                | Description                                      | Value                                                                                                                                                                                                                                                                                                                                                                                                      |
|-------------------------------------|--------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `dataService.enabled`               | Enables/disabled the deployment.                 | `true`                                                                                                                                                                                                                                                                                                                                                                                                     |
| `dataService.image.registry`        | Registry to pull the image                       | `s210.dl.hpc.tuwien.ac.at`                                                                                                                                                                                                                                                                                                                                                                                 |
| `dataService.image.repository`      | Repository to pull the image                     | `dbrepo/data-service`                                                                                                                                                                                                                                                                                                                                                                                      |
| `dataService.image.tag`             | Tag of the image.                                | `1.4.1`                                                                                                                                                                                                                                                                                                                                                                                                    |
| `dataService.image.pullPolicy`      | Image pull policy on deployments                 | `Always`                                                                                                                                                                                                                                                                                                                                                                                                   |
| `dataService.image.debug`           | Enables/disabled the debug logging.              | `false`                                                                                                                                                                                                                                                                                                                                                                                                    |
| `dataService.jwt.pubkey`            | The JWT pubkey to verify JWT signature.          | `MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqqnHQ2BWWW9vDNLRCcxD++xZg/16oqMo/c1l+lcFEjjAIJjJp/HqrPYU/U9GvquGE6PbVFtTzW1KcKawOW+FJNOA3CGo8Q1TFEfz43B8rZpKsFbJKvQGVv1Z4HaKPvLUm7iMm8Hv91cLduuoWx6Q3DPe2vg13GKKEZe7UFghF+0T9u8EKzA/XqQ0OiICmsmYPbwvf9N3bCKsB/Y10EYmZRb8IhCoV9mmO5TxgWgiuNeCTtNCv2ePYqL/U0WvyGFW0reasIK8eg3KrAUj8DpyOgPOVBn3lBGf+3KFSYi+0bwZbJZWqbC/Xlk20Go1YfeJPRIt7ImxD27R/lNjgDO/MwIDAQAB` |
| `dataService.consumerConcurrentMin` | The number of concurrent consumers (minimum).    | `1`                                                                                                                                                                                                                                                                                                                                                                                                        |
| `dataService.consumerConcurrentMax` | The number of concurrent consumers (maximum).    | `5`                                                                                                                                                                                                                                                                                                                                                                                                        |
| `dataService.requeueRejected`       | Requeue rejected tuples into the Broker Service. | `false`                                                                                                                                                                                                                                                                                                                                                                                                    |
| `dataService.replicaCount`          | Number of replicas for the deployment.           | `2`                                                                                                                                                                                                                                                                                                                                                                                                        |

### Search Service

| Name                             | Description                            | Value                      |
|----------------------------------|----------------------------------------|----------------------------|
| `searchService.enabled`          | Enables/disabled the deployment.       | `true`                     |
| `searchService.image.registry`   | Registry to pull the image             | `s210.dl.hpc.tuwien.ac.at` |
| `searchService.image.repository` | Repository to pull the image           | `dbrepo/search-service`    |
| `searchService.image.tag`        | Tag of the image.                      | `1.4.1`                    |
| `searchService.image.pullPolicy` | Image pull policy on deployments       | `Always`                   |
| `searchService.image.debug`      | Enables/disabled the debug logging.    | `false`                    |
| `searchService.replicaCount`     | Number of replicas for the deployment. | `2`                        |

### Storage Service

The Storage Service uses the [SeaweedFS](https://artifacthub.io/packages/helm/seaweedfs/seaweedfs)
Helm chart. See their documentation for the remaining overridden values.

| Name                            | Description                                 | Value            |
|---------------------------------|---------------------------------------------|------------------|
| `storage-service.auth.username` | Username for S3. Needed for other services. | `seaweedfsadmin` |
| `storage-service.auth.password` | Password for S3. Needed for other services. | `seaweedfsadmin` |

### User Interface

To replace e.g. the default logo with your organization's logo `my_logo.png`, encode it to
base64 `cat my_logo.png | base64` and create a [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/)
under a handy name `my-config`.

```yaml
# my-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: my-config
binaryData:
  my_logo.png: |
    <output from `cat my_logo.png | base64`>
```

Then mount it into the container:

```yaml
# values.yaml
ui:
  extraVolumes:
    - name: config-map
      configMap:
        name: my-config
  extraVolumeMounts:
    - name: config-map
      mountPath: /app/my_logo.png
      subPath: my_logo.png
      readOnly: true
  ...
```

| Name                   | Description                                                                                                                                                                                             | Value                      |
|------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| `ui.enabled`           | Enables/disabled the deployment.                                                                                                                                                                        | `enabled`                  |
| `ui.image.registry`    | Registry to pull the image                                                                                                                                                                              | `s210.dl.hpc.tuwien.ac.at` |
| `ui.image.repository`  | Repository to pull the image                                                                                                                                                                            | `dbrepo/ui`                |
| `ui.image.tag`         | Tag of the image.                                                                                                                                                                                       | `1.4.1`                    |
| `ui.image.pullPolicy`  | Image pull policy on deployments                                                                                                                                                                        | `Always`                   |
| `ui.replicaCount`      | Number of replicas for the deployment.                                                                                                                                                                  | `2`                        |
| `ui.config`            | JSON file containting the configuration of the UI. See [`dbrepo.config.json`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/blob/release-v1.4/dbrepo-ui/dbrepo.config.json) | `2`                        |
| `ui.extraVolumes`      | List of extra volumes.                                                                                                                                                                                  | `[]`                       |
| `ui.extraVolumeMounts` | List of extra volume mounts.                                                                                                                                                                            | `[]`                       |

## Ingress

The deployment depends on ingress, by default ingress is configured
for [NGINX Ingress Controller](https://github.com/kubernetes/ingress-nginx) with annotations.
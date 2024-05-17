# DBRepo Helm chart

[DBRepo](https://www.ifs.tuwien.ac.at/infrastructures/dbrepo/__CHARTVERSION__/) is a database repository system that
allows researchers to ingest data into a central, versioned repository through common interfaces.

## TL;DR

Download the
sample [`values.yaml`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/master/helm-charts/dbrepo/values.yaml?inline=true)
for your deployment and update the variables, especially `hostname`.

```bash
helm install my-release "oci://s210.dl.hpc.tuwien.ac.at/dbrepo/helm" --values ./values.yaml --version "1.4.3"
```

## Prerequisites

* Kubernetes 1.24+
* Optional PV provisioner support in the underlying infrastructure (for persistence).
* Optional ingress support in the underlying infrastructure:
  e.g. [NGINX](https://docs.nginx.com/nginx-ingress-controller/) (for the UI).
* Optional certificate provisioner support in the underlying infrastructure:
  e.g. [cert-manager](https://cert-manager.io/) (for production use).

## Installing the Chart

To install the chart with the release name `my-release`:

```bash
helm install my-release "oci://s210.dl.hpc.tuwien.ac.at/dbrepo/helm" --values ./values.yaml --version "1.4.3"
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

| Name            | Description                        | Value                 |
| --------------- | ---------------------------------- | --------------------- |
| `namespace`     | The namespace to install the chart | `dbrepo`              |
| `hostname`      | The hostname.                      | `example.com`         |
| `gateway`       | The gateway endpoint.              | `https://example.com` |
| `strategyType`  | The image pull                     | `RollingUpdate`       |
| `clusterDomain` | The cluster domain.                | `cluster.local`       |

### Internal Admin User

| Name             | Description                  | Value   |
| ---------------- | ---------------------------- | ------- |
| `admin.username` | The internal admin username. | `admin` |
| `admin.password` | The internal admin password. | `admin` |

### Metadata Database

| Name                             | Description                                                    | Value         |
| -------------------------------- | -------------------------------------------------------------- | ------------- |
| `metadatadb.enabled`             | Enable the Metadata Database.                                  | `true`        |
| `metadatadb.image.debug`         | Set the logging level to `trace`. Otherwise, set to `info`.    | `false`       |
| `metadatadb.host`                | The hostname for the microservices.                            | `metadata-db` |
| `metadatadb.rootUser.user`       | The root username.                                             | `root`        |
| `metadatadb.rootUser.password`   | The root user password.                                        | `dbrepo`      |
| `metadatadb.jdbcExtraArgs`       | The extra arguments for JDBC connections in the microservices. | `""`          |
| `metadatadb.db.name`             | The database name.                                             | `fda`         |
| `metadatadb.persistence.enabled` | Enable persistent storage. Requires PV-provisioner.            | `false`       |
| `metadatadb.replicaCount`        | The number of replicas, should be uneven (2n+1).               | `3`           |

### Auth Service

| Name                             | Description                                                  | Value                                                                                                                                                                                                                                                                                                                                                                                                      |
| -------------------------------- | ------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `authservice.enabled`            | Enable the Auth Service.                                     | `true`                                                                                                                                                                                                                                                                                                                                                                                                     |
| `authservice.image.debug`        | Set the logging level to `trace`. Otherwise, set to `info`.  | `false`                                                                                                                                                                                                                                                                                                                                                                                                    |
| `authservice.endpoint`           | The hostname for the microservices.                          | `http://auth-service`                                                                                                                                                                                                                                                                                                                                                                                      |
| `authservice.auth.adminUser`     | The admin username.                                          | `fda`                                                                                                                                                                                                                                                                                                                                                                                                      |
| `authservice.auth.adminPassword` | The admin user password.                                     | `fda`                                                                                                                                                                                                                                                                                                                                                                                                      |
| `authservice.jwt.pubkey`         | The JWT public key from the `dbrepo-client`.                 | `MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqqnHQ2BWWW9vDNLRCcxD++xZg/16oqMo/c1l+lcFEjjAIJjJp/HqrPYU/U9GvquGE6PbVFtTzW1KcKawOW+FJNOA3CGo8Q1TFEfz43B8rZpKsFbJKvQGVv1Z4HaKPvLUm7iMm8Hv91cLduuoWx6Q3DPe2vg13GKKEZe7UFghF+0T9u8EKzA/XqQ0OiICmsmYPbwvf9N3bCKsB/Y10EYmZRb8IhCoV9mmO5TxgWgiuNeCTtNCv2ePYqL/U0WvyGFW0reasIK8eg3KrAUj8DpyOgPOVBn3lBGf+3KFSYi+0bwZbJZWqbC/Xlk20Go1YfeJPRIt7ImxD27R/lNjgDO/MwIDAQAB` |
| `authservice.tls.enabled`        | Enable TLS/SSL communication. Required for HTTPS.            | `true`                                                                                                                                                                                                                                                                                                                                                                                                     |
| `authservice.tls.existingSecret` | The secret containing the `tls.crt`, `tls.key` and `ca.crt`. | `ingress-cert`                                                                                                                                                                                                                                                                                                                                                                                             |
| `authservice.tls.usePem`         | Use PEM certificates as input instead of PKS12/JKS stores.   | `true`                                                                                                                                                                                                                                                                                                                                                                                                     |
| `authservice.metrics.enabled`    | Enable the Prometheus metrics export sidecar container.      | `false`                                                                                                                                                                                                                                                                                                                                                                                                    |
| `authservice.client.id`          | The client id for the microservices.                         | `dbrepo-client`                                                                                                                                                                                                                                                                                                                                                                                            |
| `authservice.client.secret`      | The client secret for the microservices.                     | `MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG`                                                                                                                                                                                                                                                                                                                                                                         |

### Data Database

| Name                       | Description                                                 | Value    |
| -------------------------- | ----------------------------------------------------------- | -------- |
| `datadb.enabled`           | Enable the Data Database.                                   | `true`   |
| `datadb.image.debug`       | Set the logging level to `trace`. Otherwise, set to `info`. | `false`  |
| `datadb.rootUser.user`     | The root username.                                          | `root`   |
| `datadb.rootUser.password` | The root user password.                                     | `dbrepo` |
| `datadb.replicaCount`      | The number of replicas, should be uneven (2n+1).            | `3`      |

### Search Database

| Name                           | Description                                         | Value       |
| ------------------------------ | --------------------------------------------------- | ----------- |
| `searchdb.enabled`             | Enable the Search Database.                         | `true`      |
| `searchdb.host`                | The hostname for the microservices.                 | `search-db` |
| `searchdb.port`                | The port for the microservices.                     | `9200`      |
| `searchdb.username`            | The admin username.                                 | `admin`     |
| `searchdb.password`            | The admin user password.                            | `admin`     |
| `searchdb.replicas`            | The number of replicas.                             | `3`         |
| `searchdb.persistence.enabled` | Enable persistent storage. Requires PV-provisioner. | `true`      |

### Upload Service

| Name                         | Description                | Value  |
| ---------------------------- | -------------------------- | ------ |
| `uploadservice.enabled`      | Enable the Upload Service. | `true` |
| `uploadservice.replicaCount` | The number of replicas.    | `2`    |

### Broker Service

| Name                                | Description                                                                     | Value                         |
| ----------------------------------- | ------------------------------------------------------------------------------- | ----------------------------- |
| `brokerservice.enabled`             | Enable the Broker Service.                                                      | `true`                        |
| `brokerservice.endpoint`            | The management api endpoint for the microservices.                              | `http://broker-service:15672` |
| `brokerservice.host`                | The hostname for the microservices.                                             | `broker-service`              |
| `brokerservice.port`                | The port for the microservices.                                                 | `5672`                        |
| `brokerservice.virtualHost`         | The default virtual host name.                                                  | `dbrepo`                      |
| `brokerservice.queueName`           | The default queue name.                                                         | `dbrepo`                      |
| `brokerservice.exchangeName`        | The default exchange name.                                                      | `dbrepo`                      |
| `brokerservice.routingKey`          | The default routing key binding from the default queue to the default exchange. | `dbrepo.#`                    |
| `brokerservice.connectionTimeout`   | The connection timeout in ms.                                                   | `60000`                       |
| `brokerservice.persistence.enabled` | Enable persistent storage. Requires PV-provisioner.                             | `false`                       |
| `brokerservice.replicaCount`        | The number of replicas.                                                         | `2`                           |

### Analyse Service

| Name                          | Description                                           | Value                           |
| ----------------------------- | ----------------------------------------------------- | ------------------------------- |
| `analyseservice.enabled`      | Enable the Broker Service.                            | `true`                          |
| `analyseservice.s3.endpoint`  | The S3-capable endpoint the microservice connects to. | `http://storageservice-s3:9000` |
| `analyseservice.replicaCount` | The number of replicas.                               | `2`                             |

### Metadata Service

| Name                                       | Description                                                           | Value                           |
| ------------------------------------------ | --------------------------------------------------------------------- | ------------------------------- |
| `metadataservice.enabled`                  | Enable the Metadata Service.                                          | `true`                          |
| `metadataservice.admin.email`              | The OAI-PMH exposed admin e-mail.                                     | `noreply@example.com`           |
| `metadataservice.deletedRecord`            | The OAI-PMH exposed delete policy.                                    | `permanent`                     |
| `metadataservice.repositoryName`           | The OAI-PMH exposed repository name.                                  | `Database Repository`           |
| `metadataservice.granularity`              | The OAI-PMH exposed record granularity.                               | `YYYY-MM-DDThh:mm:ssZ`          |
| `metadataservice.datacite.enabled`         | Enable the DataCite account for minting DOIs.                         | `false`                         |
| `metadataservice.datacite.url`             | The DataCite api endpoint url.                                        | `https://api.datacite.org`      |
| `metadataservice.datacite.prefix`          | The DataCite prefix.                                                  | `""`                            |
| `metadataservice.datacite.username`        | The DataCite api username.                                            | `""`                            |
| `metadataservice.datacite.password`        | The DataCite api user password.                                       | `""`                            |
| `metadataservice.sparql.connectionTimeout` | The connection timeout for sparql queries fetching remote data in ms. | `10000`                         |
| `metadataservice.s3.endpoint`              | The S3-capable endpoint the microservice connects to.                 | `http://storageservice-s3:9000` |
| `metadataservice.s3.auth.username`         | The S3-capable endpoint username (or access key id).                  | `seaweedfsadmin`                |
| `metadataservice.s3.auth.password`         | The S3-capable endpoint user password (or access key secret).         | `seaweedfsadmin`                |
| `metadataservice.replicaCount`             | The number of replicas.                                               | `1`                             |

### Data Service

| Name                                | Description                                                    | Value                                                                                                                       |
| ----------------------------------- | -------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| `dataservice.enabled`               | Enable the Metadata Service.                                   | `true`                                                                                                                      |
| `dataservice.endpoint`              | The endpoint for the microservices.                            | `http://data-service`                                                                                                       |
| `dataservice.grant.read`            | The default database permissions for users with read access.   | `SELECT`                                                                                                                    |
| `dataservice.grant.write`           | The default database permissions for users with write access.  | `SELECT, CREATE, CREATE VIEW, CREATE ROUTINE, CREATE TEMPORARY TABLES, LOCK TABLES, INDEX, TRIGGER, INSERT, UPDATE, DELETE` |
| `dataservice.s3.endpoint`           | The S3-capable endpoint the microservice connects to.          | `http://storageservice-s3:9000`                                                                                             |
| `dataservice.s3.auth.username`      | The S3-capable endpoint username (or access key id).           | `seaweedfsadmin`                                                                                                            |
| `dataservice.s3.auth.password`      | The S3-capable endpoint user password (or access key secret).  | `seaweedfsadmin`                                                                                                            |
| `dataservice.consumerConcurrentMin` | The minimum broker service consumer number.                    | `1`                                                                                                                         |
| `dataservice.consumerConcurrentMax` | The maximum broker service consumer number.                    | `5`                                                                                                                         |
| `dataservice.requeueRejected`       | Enable re-queueing of rejected messages to the broker service. | `false`                                                                                                                     |
| `dataservice.replicaCount`          | The number of replicas.                                        | `2`                                                                                                                         |

### Search Service

| Name                         | Description                         | Value                   |
| ---------------------------- | ----------------------------------- | ----------------------- |
| `searchservice.enabled`      | Enable the Search Service.          | `true`                  |
| `searchservice.endpoint`     | The endpoint for the microservices. | `http://search-service` |
| `searchservice.replicaCount` | The number of replicas.             | `2`                     |

### Storage Service

| Name                     | Description                 | Value  |
| ------------------------ | --------------------------- | ------ |
| `storageservice.enabled` | Enable the Storage Service. | `true` |

### User Interface

| Name                              | Description                                                                  | Value                   |
| --------------------------------- | ---------------------------------------------------------------------------- | ----------------------- |
| `ui.enabled`                      | Enable the User Interface.                                                   | `true`                  |
| `ui.public.api.client`            | The endpoint for the client api.                                             | `""`                    |
| `ui.public.api.server`            | The endpoint for the server api.                                             | `""`                    |
| `ui.public.title`                 | The user interface title.                                                    | `Database Repository`   |
| `ui.public.logo`                  | The user interface logo.                                                     | `/logo.svg`             |
| `ui.public.icon`                  | The user interface icon.                                                     | `/favicon.ico`          |
| `ui.public.touch`                 | The user interface apple touch icon.                                         | `/apple-touch-icon.png` |
| `ui.public.broker.host`           | The displayed broker hostname.                                               | `example.com`           |
| `ui.public.broker.port.5671`      | Enable display of the broker 5671 port and mark it as secure (SSL/TLS).      | `true`                  |
| `ui.public.broker.port.5672`      | Enable display of the broker 5672 port and mark it as insecure (no SSL/TLS). | `false`                 |
| `ui.public.broker.extra`          | Extra metadata displayed.                                                    | `""`                    |
| `ui.public.database.extra`        | Extra metadata displayed.                                                    | `128.130.0.0/15`        |
| `ui.public.pid.default.publisher` | The default dataset publisher for persisted identifiers.                     | `Example University`    |
| `ui.public.doi.enabled`           | Enable the display that DOIs are minted.                                     | `false`                 |
| `ui.public.doi.endpoint`          | The DOI proxy.                                                               | `https://doi.org`       |
| `ui.replicaCount`                 | The number of replicas.                                                      | `2`                     |

### Ingress

| Name              | Description         | Value   |
| ----------------- | ------------------- | ------- |
| `ingress.enabled` | Enable the ingress. | `false` |

# DBRepo Helm chart

[DBRepo](https://www.ifs.tuwien.ac.at/infrastructures/dbrepo/1.4.4/) is a database repository system that
allows researchers to ingest data into a central, versioned repository through common interfaces.

## TL;DR

Download the
sample [`values.yaml`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-1.4.4/helm-charts/dbrepo/values.yaml?inline=true)
for your deployment and update the variables, especially `hostname`.

```bash
helm install my-release "oci://registry.datalab.tuwien.ac.at/dbrepo/helm" --values ./values.yaml --version "1.4.4"
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
helm install my-release "oci://oci://registry.datalab.tuwien.ac.at/dbrepo/helm" --values ./values.yaml --version "1.4.4"
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

### Metadata Database

| Name                                  | Description                                                      | Value         |
| ------------------------------------- | ---------------------------------------------------------------- | ------------- |
| `metadatadb.enabled`                  | Enable the Metadata Database.                                    | `true`        |
| `metadatadb.host`                     | The hostname for the microservices.                              | `metadata-db` |
| `metadatadb.auth.root`                | The root username.                                               | `root`        |
| `metadatadb.auth.rootPassword`        | The root user password.                                          | `dbrepo`      |
| `metadatadb.auth.database`            | The database name.                                               | `dbrepo`      |
| `metadatadb.auth.replicationUser`     | The database replication username.                               | `replication` |
| `metadatadb.auth.replicationPassword` | The database replication user password                           | `replication` |
| `metadatadb.jdbcExtraArgs`            | The extra arguments for JDBC connections in the microservices.   | `""`          |
| `metadatadb.extraInitDbScripts`       | Additional init.db scripts that are executed on the first start. | `{}`          |
| `metadatadb.secondary.replicaCount`   | The number of replicas of the secondary database pods.           | `2`           |

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
| `authservice.metrics.enabled`    | Enable the Prometheus metrics export sidecar container.      | `false`                                                                                                                                                                                                                                                                                                                                                                                                    |
| `authservice.client.id`          | The client id for the microservices.                         | `dbrepo-client`                                                                                                                                                                                                                                                                                                                                                                                            |
| `authservice.client.secret`      | The client secret for the microservices.                     | `MUwRc7yfXSJwX8AdRMWaQC3Nep1VjwgG`                                                                                                                                                                                                                                                                                                                                                                         |

### Data Database

| Name                              | Description                                                 | Value         |
| --------------------------------- | ----------------------------------------------------------- | ------------- |
| `datadb.enabled`                  | Enable the Data Database.                                   | `true`        |
| `datadb.image.debug`              | Set the logging level to `trace`. Otherwise, set to `info`. | `false`       |
| `datadb.auth.rootPassword`        | The root user password.                                     | `dbrepo`      |
| `datadb.auth.replicationUser`     | The database replication user password                      | `replication` |
| `datadb.auth.replicationPassword` | The database replication user password                      | `replication` |

### Search Database

| Name                   | Description                         | Value       |
| ---------------------- | ----------------------------------- | ----------- |
| `searchdb.enabled`     | Enable the Data Database.           | `true`      |
| `searchdb.host`        | The hostname for the microservices. | `search-db` |
| `searchdb.port`        | The port for the microservices.     | `9200`      |
| `searchdb.clusterName` | The cluster name.                   | `search-db` |

### Upload Service

| Name                         | Description                | Value  |
| ---------------------------- | -------------------------- | ------ |
| `uploadservice.enabled`      | Enable the Upload Service. | `true` |
| `uploadservice.replicaCount` | The number of replicas.    | `2`    |

### Broker Service

| Name                                | Description                                                                                                                      | Value                                                                        |
| ----------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| `brokerservice.enabled`             | Enable the Broker Service.                                                                                                       | `true`                                                                       |
| `brokerservice.image.debug`         | Set the logging level to `trace`. Otherwise, set to `info`.                                                                      | `true`                                                                       |
| `brokerservice.endpoint`            | The management api endpoint for the microservices.                                                                               | `http://broker-service:15672`                                                |
| `brokerservice.host`                | The hostname for the microservices.                                                                                              | `broker-service`                                                             |
| `brokerservice.port`                | The port for the microservices.                                                                                                  | `5672`                                                                       |
| `brokerservice.virtualHost`         | The default virtual host name.                                                                                                   | `dbrepo`                                                                     |
| `brokerservice.queueName`           | The default queue name.                                                                                                          | `dbrepo`                                                                     |
| `brokerservice.exchangeName`        | The default exchange name.                                                                                                       | `dbrepo`                                                                     |
| `brokerservice.routingKey`          | The default routing key binding from the default queue to the default exchange.                                                  | `dbrepo.#`                                                                   |
| `brokerservice.connectionTimeout`   | The connection timeout in ms.                                                                                                    | `60000`                                                                      |
| `brokerservice.ldap.binddn`         | The domain name the broker service should bind to. In many cases this is the admin user from `identityservice.global.adminUser`. | `cn=admin,dc=dbrepo,dc=at`                                                   |
| `brokerservice.ldap.bindpw`         | The password to bind on the identity service. In many cases this value is equal to `identityservice.global.adminPassword`.       | `admin`                                                                      |
| `brokerservice.ldap.uidField`       | The field containing the user id.                                                                                                | `uid`                                                                        |
| `brokerservice.ldap.basedn`         | The base domain name containing the users.                                                                                       | `ou=users,dc=dbrepo,dc=at`                                                   |
| `brokerservice.ldap.userDnPattern`  | The pattern to determine the user.                                                                                               | `${username}`                                                                |
| `brokerservice.extraPlugins`        | The list of plugins to be activated.                                                                                             | `rabbitmq_prometheus rabbitmq_auth_backend_ldap rabbitmq_auth_mechanism_ssl` |
| `brokerservice.persistence.enabled` | If set to true, a PVC will be created.                                                                                           | `false`                                                                      |
| `brokerservice.replicaCount`        | The number of replicas.                                                                                                          | `1`                                                                          |

### Analyse Service

| Name                          | Description                                                 | Value                           |
| ----------------------------- | ----------------------------------------------------------- | ------------------------------- |
| `analyseservice.enabled`      | Enable the Broker Service.                                  | `true`                          |
| `analyseservice.image.debug`  | Set the logging level to `trace`. Otherwise, set to `info`. | `false`                         |
| `analyseservice.endpoint`     | The url of the endpoint.                                    | `http://analyse-service`        |
| `analyseservice.s3.endpoint`  | The S3-capable endpoint the microservice connects to.       | `http://storageservice-s3:9000` |
| `analyseservice.replicaCount` | The number of replicas.                                     | `2`                             |

### Metadata Service

| Name                                       | Description                                                                        | Value                           |
| ------------------------------------------ | ---------------------------------------------------------------------------------- | ------------------------------- |
| `metadataservice.enabled`                  | Enable the Metadata Service.                                                       | `true`                          |
| `metadataservice.image.debug`              | Set the logging level to `trace`. Otherwise, set to `info`.                        | `false`                         |
| `metadataservice.endpoint`                 | The Metadata Service endpoint.                                                     | `http://metadata-service`       |
| `metadataservice.admin.email`              | The OAI-PMH exposed e-mail for contacting the metadata records responsible person. | `noreply@example.com`           |
| `metadataservice.deletedRecord`            | The OAI-PMH exposed delete policy.                                                 | `permanent`                     |
| `metadataservice.repositoryName`           | The OAI-PMH exposed repository name.                                               | `Database Repository`           |
| `metadataservice.granularity`              | The OAI-PMH exposed record granularity.                                            | `YYYY-MM-DDThh:mm:ssZ`          |
| `metadataservice.datacite.enabled`         | If set to true, the service mints DOIs instead of local PIDs.                      | `false`                         |
| `metadataservice.datacite.url`             | The DataCite api endpoint url.                                                     | `https://api.datacite.org`      |
| `metadataservice.datacite.prefix`          | The DataCite prefix.                                                               | `""`                            |
| `metadataservice.datacite.username`        | The DataCite api username.                                                         | `""`                            |
| `metadataservice.datacite.password`        | The DataCite api user password.                                                    | `""`                            |
| `metadataservice.sparql.connectionTimeout` | The connection timeout for sparql queries fetching remote data in ms.              | `10000`                         |
| `metadataservice.s3.endpoint`              | The S3-capable endpoint the microservice connects to.                              | `http://storageservice-s3:9000` |
| `metadataservice.s3.auth.username`         | The S3-capable endpoint username (or access key id).                               | `seaweedfsadmin`                |
| `metadataservice.s3.auth.password`         | The S3-capable endpoint user password (or access key secret).                      | `seaweedfsadmin`                |
| `metadataservice.replicaCount`             | The number of replicas.                                                            | `2`                             |

### Data Service

| Name                                         | Description                                                                                                                                      | Value                                                                                                                       |
| -------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------- |
| `dataservice.enabled`                        | Enable the Metadata Service.                                                                                                                     | `true`                                                                                                                      |
| `dataservice.endpoint`                       | The endpoint for the microservices.                                                                                                              | `http://data-service`                                                                                                       |
| `dataservice.image.debug`                    | Set the logging level to `trace`. Otherwise, set to `info`.                                                                                      | `false`                                                                                                                     |
| `dataservice.grant.read`                     | The default database permissions for users with read access.                                                                                     | `SELECT`                                                                                                                    |
| `dataservice.grant.write`                    | The default database permissions for users with write access.                                                                                    | `SELECT, CREATE, CREATE VIEW, CREATE ROUTINE, CREATE TEMPORARY TABLES, LOCK TABLES, INDEX, TRIGGER, INSERT, UPDATE, DELETE` |
| `dataservice.default.date`                   | The default date format id for dates. Default: YYYY-MM-dd (e.g. 2024-06-15).                                                                     | `3`                                                                                                                         |
| `dataservice.default.time`                   | The default date format id for times. Default: HH:mm:ss (e.g. 14:23:42).                                                                         | `4`                                                                                                                         |
| `dataservice.default.timestamp`              | The default date format id for timestamps. Default: YYYY-MM-dd HH:mm:ss (e.g. 2024-06-15 14:23:42).                                              | `1`                                                                                                                         |
| `dataservice.rabbitmq.consumerConcurrentMin` | The minimal number of RabbitMQ consumers.                                                                                                        | `2`                                                                                                                         |
| `dataservice.rabbitmq.consumerConcurrentMax` | The maximal number of RabbitMQ consumers.                                                                                                        | `6`                                                                                                                         |
| `dataservice.rabbitmq.requeueRejected`       | If set to true, rejected tuples will be re-queued.                                                                                               | `false`                                                                                                                     |
| `dataservice.rabbitmq.consumer.username`     | The username for the consumer to read tuples from the broker service. In many cases this value is equal to `identityservice.users`.              | `admin`                                                                                                                     |
| `dataservice.rabbitmq.consumer.password`     | The user password for the consumer to read tuples from the broker service. In many cases this value is equal to `identityservice.userPasswords`. | `admin`                                                                                                                     |
| `dataservice.s3.endpoint`                    | The S3-capable endpoint the microservice connects to.                                                                                            | `http://storageservice-s3:9000`                                                                                             |
| `dataservice.s3.auth.username`               | The S3-capable endpoint username (or access key id).                                                                                             | `seaweedfsadmin`                                                                                                            |
| `dataservice.s3.auth.password`               | The S3-capable endpoint user password (or access key secret).                                                                                    | `seaweedfsadmin`                                                                                                            |
| `dataservice.s3.filePath`                    | The local location to download/upload files from/to S3-capable endpoint.                                                                         | `/s3`                                                                                                                       |
| `dataservice.replicaCount`                   | The number of replicas.                                                                                                                          | `2`                                                                                                                         |

### Search Service

| Name                         | Description                                                 | Value                   |
| ---------------------------- | ----------------------------------------------------------- | ----------------------- |
| `searchservice.enabled`      | Enable the Search Service.                                  | `true`                  |
| `searchservice.endpoint`     | The endpoint for the microservices.                         | `http://search-service` |
| `searchservice.image.debug`  | Set the logging level to `trace`. Otherwise, set to `info`. | `false`                 |
| `searchservice.replicaCount` | The number of replicas.                                     | `2`                     |

### Storage Service

| Name                     | Description                 | Value  |
| ------------------------ | --------------------------- | ------ |
| `storageservice.enabled` | Enable the Storage Service. | `true` |

### Identity Service

| Name                                   | Description                                                                                                   | Value             |
| -------------------------------------- | ------------------------------------------------------------------------------------------------------------- | ----------------- |
| `identityservice.enabled`              | Enable the Identity Service.                                                                                  | `true`            |
| `identityservice.global.ldapDomain`    | The LDAP domain name in domain "dbrepo.at" form or explicit in "dc=dbrepo,dc=at" form.                        | `dc=dbrepo,dc=at` |
| `identityservice.global.adminUser`     | The admin username that is used to bind.                                                                      | `admin`           |
| `identityservice.global.adminPassword` | The admin user password that is used to bind.                                                                 | `admin`           |
| `identityservice.users`                | The admin username for internal authentication.                                                               | `admin`           |
| `identityservice.userPasswords`        | The admin user password for internal authentication.                                                          | `admin`           |
| `identityservice.group`                | The group that contains the administrators for the broker service.                                            | `system`          |
| `identityservice.persistence.enabled`  | If set to true, a PVC will be created.                                                                        | `true`            |
| `identityservice.replication.enabled`  | If set to true, the pods required a cluster. Needs `replicaCount` to be `3` or higher (of 2n+1).              | `false`           |
| `identityservice.replicaCount`         | The number of replicas. If `replicaCount` is set to more than 1, requires `replication.enabled` to be `true`. | `1`               |

### User Interface

| Name                              | Description                                                                  | Value                   |
| --------------------------------- | ---------------------------------------------------------------------------- | ----------------------- |
| `ui.enabled`                      | Enable the User Interface.                                                   | `true`                  |
| `ui.image.debug`                  | Set the logging level to `trace`. Otherwise, set to `info`.                  | `false`                 |
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

| Name                     | Description                                                                                                     | Value          |
| ------------------------ | --------------------------------------------------------------------------------------------------------------- | -------------- |
| `ingress.enabled`        | Enable the ingress.                                                                                             | `false`        |
| `ingress.className`      | The ingress class name.                                                                                         | `nginx`        |
| `ingress.tls.enabled`    | Enable the ingress.                                                                                             | `true`         |
| `ingress.tls.secretName` | The secret holding the SSL/TLS certificate. Needs to have keys `tls.crt` and `tls.key` and optionally `ca.crt`. | `ingress-cert` |

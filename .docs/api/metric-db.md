---
author: Martin Weise
---

## tl;dr

!!! debug "Debug Information"

    Image: [`bitnami/prometheus:2.54.1`](https://hub.docker.com/r/bitnami/prometheus)

    * Ports: 8080/tcp

    To directly access in Kubernetes (for e.g. debugging), forward the svc port to your local machine on port `8080`:

    ```shell
    kubectl [-n namespace] port-forward svc/data-db 8080:80
    ```

## Overview

The Metric Database is responsible for saving time-series data for the [Dashboard Service](../../api/dashboard-service).

## Metrics

## Analyse Service

| **Metric**                  | **Description**                             |
|-----------------------------|---------------------------------------------|
| `dbrepo_analyse_datatypes`  | Time needed to analyse datatypes of dataset |
| `dbrepo_analyse_keys`       | Time needed to analyse keys of dataset      |
| `dbrepo_analyse_table_stat` | Time needed to analyse table statistics     |
| `dbrepo_analyse_table_stat` | Time needed to analyse table statistics     |

## Auth Service

See [Keycloak documentation](https://www.keycloak.org/server/configuration-metrics).

## Broker Service

See [RabbitMQ documentation](https://www.rabbitmq.com/docs/prometheus).

## Databases

See [MariaDB Galera documentation](https://galeracluster.com/documentation/html_docs_20210213-1355-master/documentation/galera-manager-monitoring-clusters.html).

The [Data Database Sidecar](#) metrics are:

| **Metric**                      | **Description**                       |
|---------------------------------|---------------------------------------|
| `dbrepo_sidecar_import_dataset` | Time needed to import dataset from S3 |
| `dbrepo_sidecar_export_dataset` | Time needed to export dataset to S3   |

## Data Service

| **Metric**                  | **Description**                           |
|-----------------------------|-------------------------------------------|
| `dbrepo_message_receive`    | Received AMQP message from Broker Service |
| `dbrepo_subset_create`      | Create subset                             |
| `dbrepo_subset_data`        | Retrieved subset data                     |
| `dbrepo_subset_find`        | Find subset                               |
| `dbrepo_subset_list`        | Find subsets                              |
| `dbrepo_subset_persist`     | Persist subset                            |
| `dbrepo_table_data_create`  | Create table data                         |
| `dbrepo_table_data_delete`  | Delete table data                         |
| `dbrepo_table_data_export`  | Export table data                         |
| `dbrepo_table_data_history` | Find table history                        |
| `dbrepo_table_data_import`  | Import dataset                            |
| `dbrepo_table_data_list`    | Retrieve table data                       |
| `dbrepo_table_data_update`  | Update table data                         |
| `dbrepo_view_data`          | Retrieve view data                        |
| `dbrepo_view_schema_list`   | Find view schemas                         |

## Metadata Service

| **Metric**                         | **Description**                                   |
|------------------------------------|---------------------------------------------------|
| `dbrepo_database_count`            | The total number of managed research databases    |
| `dbrepo_view_count`                | The total number of available view data sources   |
| `dbrepo_subset_count`              | The total number of available subset data sources |
| `dbrepo_table_count`               | The total number of available table data sources  |
| `dbrepo_volume_sum`                | The total volume of available research data       |
| `dbrepo_user_refresh_token`        | Refresh user token                                |
| `dbrepo_identifier_save`           | Save identifier                                   |
| `dbrepo_oai_record_get`            | Get the record                                    |
| `dbrepo_access_give`               | Give access to some database                      |
| `dbrepo_ontologies_find`           | Find one ontology                                 |
| `dbrepo_database_findall`          | List databases                                    |
| `dbrepo_tables_refresh`            | Refresh database tables metadata                  |
| `dbrepo_license_findall`           | Get all licenses                                  |
| `dbrepo_user_modify`               | Modify user information                           |
| `dbrepo_container_findall`         | Find all containers                               |
| `dbrepo_maintenance_delete`        | Delete maintenance message                        |
| `dbrepo_maintenance_update`        | Update maintenance message                        |
| `dbrepo_ontologies_create`         | Register a new ontology                           |
| `dbrepo_identifier_delete`         | Delete some identifier                            |
| `dbrepo_oai_identify`              | Identify the repository                           |
| `dbrepo_database_create`           | Create database                                   |
| `dbrepo_oai_metadataformats_list`  | List the metadata formats                         |
| `dbrepo_user_password_modify`      | Modify user password                              |
| `dbrepo_semantic_concepts_findall` | List semantic concepts                            |
| `dbrepo_identifier_retrieve`       | Retrieve metadata from identifier                 |
| `dbrepo_identifier_list`           | Find all identifiers                              |
| `dbrepo_views_findall`             | Find all views                                    |
| `dbrepo_identifier_create`         | Draft identifier                                  |
| `dbrepo_oai_identifiers_list`      | List the identifiers                              |
| `dbrepo_image_findall`             | Find all images                                   |
| `dbrepo_database_visibility`       | Update database visibility                        |
| `dbrepo_container_create`          | Create container                                  |
| `dbrepo_views_refresh`             | Refresh database views metadata                   |
| `dbrepo_database_find`             | Find some database                                |
| `dbrepo_access_get`                | Check access to some database                     |
| `dbrepo_identifier_find`           | Find some identifier                              |
| `dbrepo_maintenance_create`        | Create maintenance message                        |
| `dbrepo_container_delete`          | Delete some container                             |
| `dbrepo_ontologies_delete`         | Delete an ontology                                |
| `dbrepo_ontologies_findall`        | List all ontologies                               |
| `dbrepo_user_token`                | Obtain user token                                 |
| `dbrepo_view_find`                 | Find one view                                     |
| `dbrepo_user_create`               | Create user                                       |
| `dbrepo_ontologies_update`         | Update an ontology                                |
| `dbrepo_maintenance_findall`       | Find maintenance messages                         |
| `dbrepo_users_list`                | Find all users                                    |
| `dbrepo_image_find`                | Find some image                                   |
| `dbrepo_user_find`                 | Get a user info                                   |
| `dbrepo_image_delete`              | Delete some image                                 |
| `dbrepo_identifier_publish`        | Publish identifier                                |
| `dbrepo_image_update`              | Update some image                                 |
| `dbrepo_view_create`               | Create a view                                     |
| `dbrepo_semantic_units_findall`    | List semantic units                               |
| `dbrepo_image_create`              | Create image                                      |
| `dbrepo_database_image`            | Update database image                             |
| `dbrepo_view_delete`               | Delete one view                                   |
| `dbrepo_database_transfer`         | Update database owner                             |
| `dbrepo_maintenance_find`          | Find one maintenance message                      |
| `dbrepo_access_modify`             | Modify access to some database                    |
| `dbrepo_ontologies_entities_find`  | Find entities                                     |
| `dbrepo_access_delete`             | Revoke access to some database                    |
| `dbrepo_container_find`            | Find some container                               |

## Search Service

| **Metric**                      | **Description**                                         |
|---------------------------------|---------------------------------------------------------|
| `dbrepo_search_index_list`      | Time needed to list search index                        |
| `dbrepo_search_type_list`       | Time needed to list search types                        |
| `dbrepo_search_fuzzy`           | Time needed to search fuzzy                             |
| `dbrepo_search_type`            | Time needed to search by type                           |
| `dbrepo_search_update_database` | Time needed to update a database in the search database |
| `dbrepo_search_delete_database` | Time needed to delete a database in the search database |

## Limitations

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](../../contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!

## Security

(none)

---
author: Martin Weise
---

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v1.10.2](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.10.2) - 2025-07-12

#### Fixes

tbd

## [v1.10.1](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.10.1) - 2025-07-10

#### Fixes

* Fixed a bug where PID presentation in the UI did not select the newest PID.
* Fixed a bug where a user could get a different id from the Identity Service when using a different authentication
  provider (i.e. SAML)

#### Changes

* Changed the mechanism to obtain the subset columns to use DuckDB and now create views anymore.
* Changed the internal user identifier from using the user id (i.e. `java.util.UUID`) to the username (i.e.
  `java.lang.String`) for all interfaces including the query store due to continued instability from SSO-integrations
  relying on the `EntryUUID` in the Identity Service.

## [v1.10.0](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.10.0) - 2025-07-05

#### Removals

* Removed the Analyse Service in favor of DuckDB in the Data Service covering the same functionality.

#### Fixes

* Fixed a bug where saving a PID multiple times broke the entity linking within Hibernate
  in [#545](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/545).

## [v1.9.3](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.9.3) - 2025-06-06

### Features

* Added a Jupyter Starter notebook for data scientists that is pre-configured to use DBRepo
  in [#529](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/529).

### Changes

* Gave the documentation website a major overhaul with detailed user guide, maintainer guide, animated step-by-step
  manuals and flowcharts for better overview
  in [#514](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/514).

### Fixes

* Fixed a bug where identifiers could not be created because of missing `ordinal_position` field for creators.
* Fixed multiple bugs where access grants were mapped wrongly.
* Fixed a bug where subsets could not be created because the wrong data source (table) was used in the data service.
* Fixed a bug in the UI where grants were not displayed once created/update/revoked.

## [v1.9.2](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.9.2) - 2025-06-05

#### Features

* Added an endpoint to check for database grants and display them in the UI (Database > Settings) for the owner
  in [#536](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/536).

## [v1.9.1](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.9.1) - 2025-05-31

#### Changes

* Release a new patch since version 1.9.0 was accidentally released on PyPI.

## [v1.9.0](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.9.0) - 2025-05-30

#### Fixes

* Fixed a bug where another PID was registered when using the DOI profile in the Metadata Service.
* Fixed a bug where titles, descriptions, creators, etc. were not sorted to the user-specified ordering
  in [#531](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/531).
* Fixed a design issue where the `get_identifier_data` method in the Python library only fetched the first 10.000 rows
  and could not paginate
  in [#527](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/527).
* Fixed a bug where Spark did not map the column headers correct when importing a dataset
  in [#518](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/518).

#### Changes

* Improved S3-related mechanisms to de-duplicate uploaded datasets and remove them on successful import, various
  structured logging improvements
  in [#528](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/528).

## [v1.8.2](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.8.2) - 2025-05-15

#### Fixes

* Fixed a bug in the UI where the resource status was displayed as identifier when a draft identifier has been created.

#### Features

* Added structured logging through the `fluentd` protocol via the lightweight fluentbit in a
  separate Logging Service.
  in [#524](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/524).
* Added a separate database for the Dashboard Service for high-availability deployment of Grafana
  in [#526](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/526).

#### Changes

* Improved internal packaging mechanism that is compatible with multiarch deployments
  in [#523](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/523).

## [v1.8.1](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.8.1) - 2025-04-13

#### Changes

* Improved default mask for PIDs
  in [#521](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/521).
* Added a visual filter for displaying starred/unstarred/all subsets in the UI.
* Specified image platform as `linux/amd64` in `docker-compose.yaml` deployment to enable host platform (e.g. ARM) to
  emulate it.
* Specified resource limits in the `docker-compose.yaml` deployment
  in [#517](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/517).

#### Fixes

* Fixed a bug in the UI that displays the "Create View" button only when the user has at least read access.

#### Removals

* Removed the stale objects scheduler from the Data Service and pushed it to next release.

## [v1.8.0](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.8.0) - 2025-04-04

#### Features

* Refactored internal Java-based testing data that improves test consistency
  in [#510](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/510).
* Added automated dashboard generation for all public databases where each view has an overview of its data and
  schema in [#460](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/460).

#### Changes

* Removed OpenSearch security plugin from the Docker test deployment and changed to the `bitnami/opensearch` image
  of the same version in [#515](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/515).

#### Fixes

* Fixed a bug where validation of missing `Principal` object in Java services caused a 400 error instead of a 401 error
  in [#512](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/512).

## [v1.7.3](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.7.3) - 2025-03-17

#### Fixes

* Fixed a wrong configuration where assets were not considered in the Kubernetes deployment.
* Fixed a wrong configuration in the Docker deployment where the OIDC provider did not consider other URLs than
  `http://localhost`.

## [v1.7.2](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.7.2) - 2025-03-13

#### Fixes

* Fixed a wrong configuration of `caffeine` in the Data Service that did not find views/subsets after table creation
  within the cache period of 60 seconds
  in [#506](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/506).

## [v1.7.1](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.7.1) - 2025-03-06

#### Features

* Added support to download `pandas` DataFrame by PID
  in [#503](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/503).
* Added the possibility to create and fill a table from a `pandas` DataFrame (or optionally just create the schema)
  in [#496](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/496).

#### Fixes

* Fixed a bug where quick interaction with the UI caused the user to trigger the brute-force login detection
  in [#501](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/501).

## [v1.7.0](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.7.0) - 2025-03-03

!!! warning "Contains Breaking Changes"

    This release updates the Metadata Database schema which is incompatible to v1.6.3! Follow the steps:

    1. Make a backup of the database with `mariadb-dump`.
    2. Apply the schema changes script: [`schema.sql`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/blob/release-1.7/dbrepo-metadata-db/migration/16/schema.sql):
       ```shell
       mariadb -h 127.0.0.1 -p3306 -u root --password=<password> -D dbrepo < schema.sql
       ```
    3. Install the dependencies from the [`requirements.txt`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/blob/release-1.7/dbrepo-metadata-db/migration/16/requirements.txt)
       file or use your local environment:
       ```shell
       pip install dbrepo==1.6.5rc15
       ```
    4. Run the data migration script [`data.py`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/blob/release-1.7/dbrepo-metadata-db/migration/16/data.py):
       ```shell
       python data.py > data.sql
       ```
       It generates the SQL statements used for migrating to the new schema.
    5. Run the generated `data.sql` script:
       ```shell
       mariadb -h 127.0.0.1 -p3306 -u root --password=<password> -D dbrepo < data.sql
       ```

#### Features

* Implemented a basic brute-force security defense strategy in the Auth Service that increments the wait time on wrong
  logins in [#494](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/494).
* Implemented a password policy
  in [#495](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/495).

#### Changes

* Replaced sequential numerical ids with non-guessable random ids in the Metadata Database
  in [#491](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/491).
* Changed the interface for executing query in subsets/views
  in [#493](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/493).

#### Removals

* Removed the Upload Service in favor of an internal stable upload endpoint in the Data Service
  in [#492](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/492).

## [v1.6.5](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.6.5) - 2025-02-18

#### Fixes

* Fixed a bug where listing the views in the Python library did not work.
* Fixed a wrong MariaDB configuration where the `innodb_buffer_pool_size` variable was not configured to 70% of the
  available memory in the Helm chart.

## [v1.6.4](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.6.4) - 2025-02-14

#### Fixes

* Fixed a bug where the users were not synced with the Metadata Database
  in [#489](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/489).

## [v1.6.3](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.6.3) - 2025-02-05

#### Changes

* Refactored the UI to support OIDC and added an event listener to the Auth Service that syncs users on creation to the
  Metadata DB in [#488](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/488).

## [v1.6.2](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.6.2) - 2025-01-24

#### Changes

* Added interface tests for the Python library in Gitlab CI/CD pipeline
  in [#486](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/486).

#### Fixes

* Fixed a bug where no pagination was possible
  in [#487](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/487).

## [v1.6.1](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.6.1) - 2025-01-21

#### Changes

* Added privacy feature for hidden databases (and optionally tables, views, subsets) that hides them completely from
  e.g. the search in [#482](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/482).

#### Fixes

* Added init container that adds the admin user to the Metadata Database
  in [#480](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/480).

## [v1.6.0](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.6.0) - 2025-01-07

#### Features

* Added possibility to modify table description and privacy mode that hides metadata of databases, tables, subsets and
  views in [#472](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/472).
* Added Auth Service init container to sync internal system ("admin") user into Metadata Database
  in [#470](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/470.)

#### Changes

* Drop MariaDB Galera chart version from [
  `14.0.12`](https://artifacthub.io/packages/helm/bitnami/mariadb-galera/14.0.12)
  to [`13.2.7`](https://artifacthub.io/packages/helm/bitnami/mariadb-galera/13.2.7) because of stability issues with the
  newer versions on the OpenShift Kubernetes cluster.
* Bumped Grafana chart version from [`10.1.1`](https://artifacthub.io/packages/helm/bitnami/grafana/10.1.1)
  to [`11.4.2`](https://artifacthub.io/packages/helm/bitnami/grafana/11.4.2).
* Bumped NGINX chart version from [`18.2.6`](https://artifacthub.io/packages/helm/bitnami/nginx/18.2.6)
  to [`18.3.1`](https://artifacthub.io/packages/helm/bitnami/nginx/18.3.1).
* Bumped SeaweedFS chart version from [`1.0.2`](https://artifacthub.io/packages/helm/bitnami/seaweedfs/1.0.2)
  to [`4.2.1`](https://github.com/MartinWeise/charts/tree/main/bitnami/spark) (created a pull request
  [#31192](https://github.com/bitnami/charts/pull/31192)).
* Improve RESTfulness of HTTP API to e.g. not include the database image due to size
  in [#463](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/463).

#### Fixes

* Fixed a wrong JPA entity configuration in Hibernate that in some cases causes the `SERIAL` increment to not trigger
  in MariaDB Galera. Removed the
  deprecated [
  `@GenericGenerator`](https://docs.jboss.org/hibernate/orm/6.6/javadocs/org/hibernate/annotations/GenericGenerator.html)
  annotation on JPA entities.
* Fixed a bug where the dataset separator was being ignored for imports
  in [#478](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/478).

## [v1.5.3](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.5.3) - 2024-12-13

#### Fixes

* Fixed a bug where subsets containing sub-queries are not able to retrieve data
  in [#476](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/476).

## [v1.5.2](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.5.2) - 2024-12-03

#### Changes

* Adapt Helm chart to support `runAsNonRoot` throughout and specify `resource` presets for the highly-constrained
  OpenShift Kubernetes environment
  in [#467](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/467).
* Require authentication for uploading files
  in [#466](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/466).

#### Fixes

* Fixed a validation problem failing to validate UUIDs
  in [#471](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/471).
* Fixed the `dist.tar.gz` file not being found in the CI/CD pipeline on `release-` branches
  in [#465](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/465).

## [v1.5.1](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.5.1) - 2024-11-09

#### Fixes

* Bug where the data volume could not be calculated when the data length column in the Metadata Database is `null`
  in [#462](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/462).
* Bug where the schema could not be created manually
  in [#461](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/461).

## [v1.5.0](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.5.0) - 2024-11-06

!!! warning "Contains Breaking Changes"

    This release updates the Metadata Database schema which is incompatible to v1.4.6! Use the migration
    script [`schema_1.4.5-to-1.5.0.sql`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/blob/release-1.5/dbrepo-metadata-db/migration/schema_1.4.5-to-1.5.0.sql)
    to apply the changes manually.

#### Features

* Added `SERIAL` data type to create incrementing key
  in [#454](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/454)

#### Changes

* Remove the Data Database Sidecar and replace it with Apache Spark
  in [#458](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/458).
* Allow anonymous users to create subsets for public databases
  in [#449](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/449).
* Show file upload progress
  in [#448](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/448).
* Change the Docker image of the Auth Service to Bitnami-maintained similar to Kubernetes deployment with accompanying
  Auth Database change to PostgreSQL
  in [#455](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/455)

#### Fixes

* Preventing the semicolon `;` to be used in UI and fixed cryptic subset execution error messages
  in [#456](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/456).
* Multiple UI errors in [#453](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/453).
* Fixed install script.sh
  in [#444](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/444)
* No hardcoded data type metadata in UI but instead added it hardcoded (associated with `image_id`) Metadata Database.

## [v1.4.6](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.4.6) - 2024-10-11

!!! warning "Contains Breaking Changes"

    This release updates the Metadata Database schema which is incompatible to v1.4.5!

#### Features

* Added [Dashboard Service](/infrastructures/dbrepo/1.10/api/dashboard-service/) and monitoring in default setup.

#### Changes

* Show the progress of dataset uploads in the UI
  in [#448](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/448)
* Anonymous users are allowed to create (non-persistent) subsets
  in [#449](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/449)
* Removed logic that maps `True`, `False` and `null`

#### Fixes

* Import of datasets stabilized in the UI
  in [#442](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/442)
* Install script in [#444](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/444)

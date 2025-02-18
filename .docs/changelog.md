---
author: Martin Weise
---

## v1.6.5 (2025-02-18)

[:simple-gitlab: GitLab Release](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.6.5)

### What's Changed

#### Fixes

* Fixed a bug where listing the views in the Python library did not work.

## v1.6.4 (2025-02-14)

[:simple-gitlab: GitLab Release](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.6.4)

### What's Changed

#### Fixes

* Fixed a bug where the users were not synced with the Metadata Database
  in [#489](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/489).

## v1.6.3 (2025-02-05)

[:simple-gitlab: GitLab Release](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.6.3)

### What's Changed

#### Changes

* Refactored the UI to support OIDC and added an event listener to the Auth Service that syncs users on creation to the
  Metadata DB in [#488](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/488).

## v1.6.2 (2025-01-24)

[:simple-gitlab: GitLab Release](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.6.2)

### What's Changed

#### Changes

* Added interface tests for the Python library in Gitlab CI/CD pipeline
  in [#486](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/486).

#### Fixes

* Fixed a bug where no pagination was possible
  in [#487](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/487).

## v1.6.1 (2025-01-21)

[:simple-gitlab: GitLab Release](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.6.1)

### What's Changed

#### Changes

* Added privacy feature for hidden databases (and optionally tables, views, subsets) that hides them completely from
  e.g. the search in [#482](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/482).

#### Fixes

* Added init container that adds the admin user to the Metadata Database
  in [#480](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/480).

## v1.6.0 (2025-01-07)

[:simple-gitlab: GitLab Release](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.6.0)

### What's Changed

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

## v1.5.3 (2024-12-13)

[:simple-gitlab: GitLab Release](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.5.3)

### What's Changed

#### Fixes

* Fixed a bug where subsets containing sub-queries are not able to retrieve data
  in [#476](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/476).

## v1.5.2 (2024-12-03)

[:simple-gitlab: GitLab Release](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.5.2)

### What's Changed

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

## v1.5.1 (2024-11-09)

[:simple-gitlab: GitLab Release](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.5.1)

### What's Changed

#### Fixes

* Bug where the data volume could not be calculated when the data length column in the Metadata Database is `null`
  in [#462](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/462).
* Bug where the schema could not be created manually
  in [#461](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/461).

## v1.5.0 (2024-11-06)

[:simple-gitlab: GitLab Release](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.5.0)

!!! warning "Contains Breaking Changes"

    This release updates the Metadata Database schema which is incompatible to v1.4.6! Use the migration
    script [`schema_1.4.5-to-1.5.0.sql`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/blob/release-1.5/dbrepo-metadata-db/migration/schema_1.4.5-to-1.5.0.sql)
    to apply the changes manually.

### What's Changed

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

## v1.4.6 (2024-10-11)

[:simple-gitlab: GitLab Release](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.4.6)

!!! warning "Contains Breaking Changes"

    This release updates the Metadata Database schema which is incompatible to v1.4.5!

### What's Changed

#### Features

* Added [Dashboard Service](../api/dashboard-service/) and monitoring in default setup.

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
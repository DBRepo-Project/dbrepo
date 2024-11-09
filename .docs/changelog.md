---
author: Martin Weise
---

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
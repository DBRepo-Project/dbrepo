# Changelog

All notable changes to this project will be documented in this file.

Changes from 1.4.0 onwards are available in [.docs/changelog.md](.docs/changelog.md).

## [[1.4.0]()] - 2024-01-19

### Added

- Increased DOI system support: tables, views, subsets and databases. Also allowing multiple identifiers per object.
- Extensive documentation directly in the repository with 
  usage [examples](https://www.ifs.tuwien.ac.at/infrastructures/dbrepo/1.4.0/usage-overview/).
- Identifier can now be crawled in the UI via JSON-LD (Google Datasets).
- OpenAIRE Graph protocol for OAI-PMH.
- Added a data service that inserts data from the Broker Service.
- Added search service for searching the search-db.
- Added storage service for S3-compatible file uploads and downloads.

### Changed

- Changed the model to one isolated database within a single database container (by default), additional containers
  can still be created and registered.
- Refactored the Hibernate mappings extensively, simplifying the save operations.
- Using only one OpenSearch index `database` containing all metadata mirrored from the metadata database.
- Broker service has only one exchange `dbrepo` (topic) and one queue `dbrepo` (quorum), the delivery of tuples is
  managed via routing keys according to the hierarchy, e.g. `dbrepo.database_name_rand.table_name`.

### Fixed

- Runtime dependencies for creating the OpenSearch index.
- Using Testcontainer fixtures where possible for integration tests.
- Fixed the wildcard search with own search service that can do faceted browsing.

### Removed

- The container service, merged into metadata service.
- The database service, merged into metadata service.
- The identifier service, merged into metadata service.
- The query service, merged into metadata service.
- The table service, merged into metadata service.

## [[1.3.0](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/tags/v1.3.0)] - 2023-06-01

### Added

- Added automatic vulnerability scanner `trivy` in the CI/CD pipeline for detecting critical vulnerabilities faster
- Added DataCite DOI system
- Semantic service contains multiple ontologies and improved SPARQL queries to get label and description
- The data steward can now actually curate databases (e.g. assigning semantic information to table schema)
- The metadata of identifiers can now be downloaded with button
- Documented all endpoint codes in the backend
- Multi-column foreign key relationships

### Changed

- Replaced the custom authentication service with a production-grade Keycloak system
- Queries issued via HTTP no longer count the result, there is a separate endpoint for that to decrease response time

### Fixed

- Fixed ~60% vulnerabilities coming from insecure Docker base images by changing to more secure ones
- Actuator and swagger endpoints to be reachable, added MVC tests to ensure their correct responses in the future
- Fixed issues around date formats
- Query aliases and query versioning issues including views and tables, cross-product and joins
- Bugs related to data insert in the frontend
- Bugs related to data viewing in the frontend
- Bugs related to primary key tuple modifications
- Various frontend bugfixes for new role management
- Refactored the constraint DTO for table schema creation

### Removed

- The discovery service image will no longer be maintained, it has been replaced with the reverse proxy

## [[1.2](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/commit/125db74af6d2e96345b92bb96f115422f7194f65)] - 2022-11-01

### Added

- Wikidata ontology and proxy the ontology of units of measure for using only units
- Mapping from concepts to table column
- Alerting to Prometheus (the registration part on the services side)
- Access model to give more than one user access to a database
- Default role flag for new users and administrator usernames
- Citation suggestions IEEE/BibTeX/APA
- Automatic deployment for dev pipeline to dbrepo2.ec.tuwien.ac.at
- MQTT plugin for broker service
- More logging and implemented minimum logging level flag
- PID for databases
- Public code wiki (documentation website)
- Analyse service tests to pipeline

### Changed

- Metadata database to use a system-versioned MariaDB 11.1.3 database
- Query Store to use trigger for query result count and query hash as well as result hash calculation
- Query service to allow XML/CSV export for PIDs
- Query service to support subsets of views

### Fixed

- Container creation in frontend prevents invalid state where no database can be created anymore
- RabbitMQ exchange/queue/binds when starting the system from a previous version or without data for RabbitMQ
- RabbitMQ non-uniqueness of exchanges/queues/routing keys
- A python loop that used too much CPU resources for registering at the discovery service
- Select `*` that does not return proper error message as well as vendor-specific aggregation functions like `min`, `max`, `avg`, `count`
- Various frontend error messages
- A bug where a invalid table creation and subsequent valid table creation is blocked

### Removed

- The name Unit Service will no longer be maintained, the Docker image is discontinued and is replaced by the Semantics Service

## [[1.1]](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/commit/2a1f1bd53e3445aafa881e1209703dfa7de9c918) - 2022-08-11

### Added

- DataCite mandatory fields on databases and queries
- User management (info, password forgotten, verification, email, RabbitMQ)
- Re-worked interfaces of database info, database tables, database queries, persisted queries (landing page)
- Password forgotten / change user password management
- ORCiD in the PID
- Time travel selector with chart
- Persistent dark mode
- Export query button again
- Export table button again
- Download of metadata in .xml
- Mail server configuration to send emails
- Arbitrary settings for importing .csv on existing tables, added double/single quote support, export subset and table as .csv
- Time-stamped JOIN that supports arbitrary regular JOINS
- Table history and time travelling view + frontend view
- REST-interface to Broker Service for managing RabbitMQ queues and (user-)permissions
- DataCite schema and .xml download
- Only-me database + only-me result set on persistent identification
- Raw-query interface in the frontend
- Creator-field to nearly all metadata-DB entities
- Update of values through .csv import
- CRUD on single entry in the backend + frontend
- .tsv file import support
- Replacement of TRUE, FALSE and NULL in one transaction
- Issuance of new java web token on valid tokens
- Error codes on exceptions in the backend services (only Java-based)
- More Analyse Service suggestions on data types and the separator
- Database volumes for each researcher database

### Changed

- Frontend routing to reflect the backend better
- More logging information to all actions
- Extent of the endpoint Swagger documentation


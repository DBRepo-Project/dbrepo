---
author: Martin Weise
---

[![CI/CD Pipeline](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/badges/master/pipeline.svg)](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services){ tabindex=-1 }
[![Code Coverage](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/badges/master/coverage.svg)](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services){ tabindex=-1 }
[![GitLab Release](https://img.shields.io/gitlab/v/release/fair-data-austria-db-repository%2Ffda-services?gitlab_url=https%3A%2F%2Fgitlab.phaidra.org&display_name=release&style=flat)](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services){ tabindex=-1 }
[![GitLab License](https://img.shields.io/gitlab/license/fair-data-austria-db-repository%2Ffda-services?gitlab_url=https%3A%2F%2Fgitlab.phaidra.org%2F&style=flat)](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services){ tabindex=-1 }
[![Artifact Hub](https://img.shields.io/endpoint?url=https://artifacthub.io/badge/repository/dbrepo)](https://artifacthub.io/packages/helm/dbrepo/dbrepo){ tabindex=-1 }


## Features

### Built-in search

DBRepo makes your dataset searchable without extra effort: most metadata is generated automatically for data in your 
databases. The fast and powerful OpenSearch database allows a fast retrieval of any information. Adding semantic mapping
through a suggestion-feature, allows machines to properly understand the context of your data. [Learn more.](../system-services-search/)

### Citable datasets

Adopting the recommendations of the RDA-WGDC, arbitrary subsets can be precisely, persistently identified using
system-versioned tables of MariaDB and the DataCite schema for minting DOIs. External systems i.e. metadata harvesters
(OpenAIRE, Google Datasets) can access these datasets through OAI-PMH, JSON-LD and FAIR Signposting protocols.
[Learn more.](../system-services-metadata/)

### Powerful API for Data Scientists

With our strongly typed Python Library, Data Scientists can import, export and work with data from Jupyter Notebook or
Python script, optionally using Pandas DataFrames. For example: the AMQP API Client can collect continuous data from
edge devices like sensors and store them asynchronous in DBRepo. [Learn more.](../usage-python/)

### Cloud Native

Our lightweight Helm chart allows for installations on any cloud provider or private-cloud setting that has an
underlying PV storage provider. DBRepo can be installed from 
the [Artifact Hub](https://artifacthub.io/packages/helm/dbrepo/dbrepo) repository. Databases are managed as MariaDB
Galera Cluster with high degree of availability ensuring your data is always accessible.
[Learn more.](../deployment-helm/)

## Demo Site

We run a small demonstration instance so you can see the latest version of DBRepo in action. The demonstration instance
is updated with new releases and should be considered ephemeral.

[:fontawesome-solid-flask: Demonstration Instance](https://test.dbrepo.tuwien.ac.at){ .md-button .md-button--primary }
---
author: Martin Weise
hide:
- navigation
social:
  cards_layout_options:
    title: Documentation that simply works
---

## Problem Statement

Digital repositories see themselves more frequently encountered with the problem of making databases accessible in their
collection. Challenges revolve around organizing, searching and retrieving content stored within databases and
constitute a major technical burden as their internal representation greatly differs from static documents most digital
repositories are designed for.

## Application Areas

We present a database repository system that allows researchers to ingest data into a central, versioned repository
through common interfaces, provides efficient access to arbitrary subsets of data even when the underlying data store is
evolving, allows reproducing of query results and supports findable-, accessible-, interoperable- and reusable data.

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
underlying PV storage provider. DBRepo can be installed from the Artifacthub repository. Databases are managed as 
MariaDB Galera Cluster with high degree of availability ensuring your data is always accessible.
[Learn more.](../deployment-helm/)

## More Information

- Demonstration instance [https://dbrepo1.ec.tuwien.ac.at](https://dbrepo1.ec.tuwien.ac.at)
- Test instance [https://test.dbrepo.tuwien.ac.at](https://test.dbrepo.tuwien.ac.at)
- System description [https://doi.org/10.2218/ijdc.v17i1.825](https://doi.org/10.2218/ijdc.v17i1.825)

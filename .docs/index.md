---
author: Martin Weise
---

[![CI/CD Pipeline](../images/pipeline.svg)](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services){ tabindex=-1 }
[![Code Coverage](../images/coverage.svg)](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services){ tabindex=-1 }
[![GitLab Release](https://img.shields.io/gitlab/v/release/fair-data-austria-db-repository%2Ffda-services?gitlab_url=https%3A%2F%2Fgitlab.phaidra.org&display_name=release&style=flat&cacheSeconds=3600)](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services){ tabindex=-1 }
[![Image Pulls](https://img.shields.io/docker/pulls/dbrepo/data-service?style=flat&cacheSeconds=3600)](https://hub.docker.com/u/dbrepo){ tabindex=-1 }
[![GitLab License](https://img.shields.io/gitlab/license/fair-data-austria-db-repository%2Ffda-services?gitlab_url=https%3A%2F%2Fgitlab.phaidra.org%2F&style=flat&cacheSeconds=3600)](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services){ tabindex=-1 }

Documentation for version: [v1.4.6](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/releases).

DBRepo is a repository for data in databases that are used from the beginning until the end of a research 
project supporting data evolution, -citation and -versioning. It implements the query store of the 
[RDA WGDC](https://doi.org/10.1162/99608f92.be565013) on precisely identifying arbitrary subsets of data.

## Why use DBRepo?

* **Built-in search** makes your dataset searchable without extra effort: most metadata is generated 
  automatically for data in your databases.
* **Citable datasets** adopting the recommendations of the RDA-WGDC, arbitrary subsets can be precisely, persistently 
  identified using system-versioned tables of MariaDB and the DataCite schema for minting DOIs.
* **Powerful API for Data Scientists** with our strongly typed Python Library, Data Scientists can import, export and
  work with data from Jupyter Notebook or Python script, optionally using Pandas DataFrames.
* **Cloud Native** our lightweight Helm chart allows for installations on any cloud provider or private-cloud setting 
  that has an underlying PV storage provider.

Installing DBRepo is very easy or
[give it a try online](https://test.dbrepo.tuwien.ac.at){ target="_blank" }.

## Who is using DBRepo?

- [TU Wien](https://dbrepo1.ec.tuwien.ac.at)
- TU Darmstadt
- [Universität Hamburg](https://dbrepo.fdm.uni-hamburg.de/)
- [Universiti Teknikal Malaysia Melaka](https://dbrepo.utem.edu.my/)
- University of the Philippines
- [Universiti Sains Malaysia](https://dbrepo.wrfexpress.com/)

## How can I try DBRepo

[:fontawesome-solid-flask: &nbsp;Demonstration Instance](https://test.dbrepo.tuwien.ac.at){ .md-button .md-button--primary target="_blank" }
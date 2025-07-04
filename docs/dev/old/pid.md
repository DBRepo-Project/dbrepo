---
author: Martin Weise
---

Data in DBRepo always has attached metadata (stored in the [Metadata Database](/infrastructures/dbrepo/1.10/dev/services//metadata-db/)). This metadata
is provided as machine-understandable context in various open-source formats that is available, even when the original
data is not available anymore due to e.g. a retracted dataset (hence the name **persistent**). A persistent identifier
globally, uniquely identifies a data record such as:

* Database,
* Table whose data is continously changed in the background (nature of databases),
* View who show a denomination, joined schema or aggregated result of tables,
* Subset which precisely identifies a data record to reproduce the same dataset.

Combining [data versioning](#data-versioning) and queries, subsets can be precisely identified by storing the query
that creates them and the time when the query was executed. We store both in a table inside the database we call the
query store.

## DOI

DBRepo uses the DOI system minted by DataCite, this is optional and not required to function for e.g. test deployments.
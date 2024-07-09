---
author: Martin Weise
---

## Data Versioning

Data is getting bigger and so are expectations of data provisioning in regards to data availability (i.e. immediately
after quality check and not in snapshot intervals), cost-effectiveness (i.e. no duplication of data), transparent,
precise citation and many more.

[System-versioned](https://mariadb.com/kb/en/system-versioned-tables/) tables in MariaDB are improved data structures
that keep track of historical data. For each entry in a system-versioned table, a time period is maintained that denotes
the validity time span of this tuple from its start to end. Tuples in system-versioned tables are not *actually*
modified, they are marked as (in-)valid in time periods.

<figure markdown>

| ID | Sensor | Temp | Start | End |
|----|--------|------|-------|-----|
| 1  | A      | 23.1 | t1    |     |
| 2  | B      | 25.8 | t2    |     |

</figure>

Assuming that Sensor A was calibrated wrong and an updated measurement is passed to the system-versioned table, the
table contents show that the old row with Temp 23.1 is not deleted, but marked as valid in time span (t1, t3). The
updated row with Temp 22.1 is marked as valid from time span t3 onwards.

<figure markdown>

| ID | Sensor | Temp | Start | End |
|----|--------|------|-------|-----|
| 1  | A      | 23.1 | t1    | t3  |
| 2  | B      | 25.8 | t2    |     |
| 1  | A      | 22.1 | t3    |     |

</figure>

System-versioned tables are part of the SQL:2011 standard and have been adopted by many database management system
vendors: MariaDB (10.5 and higher), Google BigQuery, IBM DB2 (12 and higher), SQL Server (2016 and higher), Azure SQL,
PostgreSQL with [temporal tables extension](https://github.com/nearform/temporal_tables), etc.

## Persistent Identifier

Data in DBRepo always has attached metadata (stored in the [Metadata Database](../../api/metadata-db)). This metadata
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
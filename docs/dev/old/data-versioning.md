---
author: Martin Weise
---

Data is getting bigger and so are expectations of data provisioning in regards to data availability (i.e. immediately
after quality check and not in snapshot intervals), cost-effectiveness (i.e. no duplication of data), transparent,
precise citation and many more.

System-versioned tables in MariaDB are improved data structures that keep track of historical data. For each entry in a
system-versioned table, a time period is maintained that denotes the validity time span of this tuple from its start to
end. Tuples in system-versioned tables are not *actually* modified, they are marked as (in-)valid in time periods
(c.f. [Fig. 1](#fig1)).

<figure id="fig1" markdown>
![](/infrastructures/dbrepo/images/data-versioning.png)
</figure>

Assuming that Sensor A was calibrated wrong and an updated measurement is passed to the system-versioned table, the
table contents show that the old row with Temp 23.1 is not deleted, but marked as valid in time span (t1, t3). The
updated row with Temp 22.1 is marked as valid from time span t3 onwards.

## Further Reading

System-versioned tables are part of the current ISO/IEC 9075-2:2023 ("SQL") standard and have been adopted by many
database management system vendors, e.g. MariaDB (10.5 and higher), Google BigQuery, IBM DB2 (12 and higher),
Cockroach DB, SAP HANA, SQL Server (2016 and higher), Azure SQL, PostgreSQL
with [temporal tables extension](https://github.com/nearform/temporal_tables), etc.
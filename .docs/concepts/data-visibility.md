---
author: Martin Weise
---

There are several ways to set the visibility of (meta-)data in DBRepo. It is possible to set the data visibility to
visible/hidden and the schema to be visible/hidden for each database and separately for each table, each view and each
subset of a database.

## Visibility

In total there are four possible visibility settings that can be applied on database level and then at the subsequent
levels (table, view, subset). We give two examples for better understanding:

!!! example "*Example*: Database that is hidden but certain views and subsets are visible"

    Database Airquality has the settings to hide all data and schema by default.

    * Table `sensor_measurements`

#### Visible

!!! info "Possible use-case: data publication supplement to an open-access publication"

Where the database's data and metadata is set to be *visible*. This means everything in the database (tables, views,
subsets) are visible by anyone from the public.

#### Data-only

!!! info "Possible use-case: private sensor measurements with timed embargo"

Where the database's data set to be *hidden* but the schema to be *visible*. This means everything in the database
(tables, views, subsets) are by default not visible by anyone from the public. You can however make specific views that
join tables and/or filter certain columns and apply a 14-day delay-embargo.

<figure markdown>
![Mirroring statistical properties in Metadata Database and Search Database](../images/private-embargo.svg)
<figcaption>Figure 1: Public view that joins two private tables and applies a time-embargo</figcaption>
</figure>

#### Schema-only

#### Draft

!!! info "Possible use-case: project data storage before publication"
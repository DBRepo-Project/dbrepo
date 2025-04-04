---
author: Martin Weise
---

## Overview

There are several ways to set the visibility of (meta-)data in DBRepo. It is possible to set the data visibility to
visible/hidden and the schema to be visible/hidden for each database and separately for each table, each view and each
subset of a database.

<figure markdown>

| Name                        | `is_public` | `is_schema_public` | UI?                | Search?            | Dashboard?         |
|-----------------------------|-------------|--------------------|--------------------|--------------------|--------------------|
| [Visible](#visibility)      | `True`      | `True`             | :white_check_mark: | :white_check_mark: | :white_check_mark: | 
| [Data-only](#data-only)     | `True`      | `False`            | :white_check_mark: | :white_check_mark: | :white_check_mark: | 
| [Schema-only](#schema-only) | `False`     | `True`             | :white_check_mark: | :white_check_mark: | :white_check_mark: | 
| [Hidden](#hidden)           | `False`     | `False`            | :x:                | :x:                | :x:                | 

</figure>

## Visibility

In total there are four possible visibility settings that can be applied on database level and then at the subsequent
levels (table, view, subset). We give two examples for better understanding:

!!! example "Example: Database that is hidden but certain views are visible"

    Database *Airquality* has the settings to hide all data and schema by default.

    * Table `sensor` inherits the settings from the database by default and therefore is also **hidden**. Nobody can
      read/write to this database by default. Only designated users that the database owner allows to read/write can do
      so.
    * View `v_sensor` inherits the settings from the database by default and therefore is also **hidden**. The database
      owner wants the data to be visible to the public (anonymously), so he changes the settings to data=visible,
      schema=hidden. Now everybody can see the data but not the table(s) that contain the data.

In any case, a Grafana dashboard will be created.

#### Visible

!!! info "Possible use-case: data publication supplement to an open-access publication"

Where the resource's data and schema is set to be visible.

#### Data-only

!!! info "Possible use-case: private sensor measurements with timed embargo"

Where the resource's schema visibility is hidden but the data is visible.

#### Schema-only

!!! info "Possible use-case: publish data for reviewers before the final publication"

Where the resource's data visibility is hidden but the schema is visible.

#### Hidden

!!! info "Possible use-case: project data storage before publication"

Where the resource's data and schema visibility is hidden. It will not be findable even in the search. The automatically
generated Dashboard cannot be viewed anonymously (only users with read-access can view).
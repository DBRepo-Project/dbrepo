---
author: Martin Weise
---

A user wants a datasource, e.g. a database, to be private and only give specific users access.

This can be applied to any datasource in DBRepo: tables, views, subsets and whole databases
There are two attributes that define a datasource's visibility:

* **Transparency**

    Can hide the datasource in listings and search results or make it visible.

* **Insights**

    Can hide additional info of the datasource associated with schema and structure, e.g. can hide the SQL query used to
    create a view to not expose the database schema; or make that info visible.

A datasource therefore can have four possible visibility states:

* **Visible**

    When set to visible Transparency and visible Insights, everything is visible to the world.

    !!! abstract "Note"

        Visible data and a PID with license to reuse (e.g. CC-BY 4.0) is equivalent to Open Data.

* **Data-only**

    When set to visible Transparency and hidden Insights, this affects data sources. Associated schema metadata is 
    hidden.
    
    * Database: removes the list of tables, views and subsets.
    * Table: removes the list of columns.
    * View: removes the SQL query.
    * Subset: removes the SQL query.

* **Schema-only**

    When set to hidden Transparency and visible Insights, this affects data sources. Associated data information is 
    hidden and the data preview is disabled for anyone without at least `read` access.

* **Hidden**

    When set to hidden Transparency and hidden Insights, this affects data sources, everything is hidden to anyone (even
    the existence). Only the owner and selected user accounts that have at least `read` access can see the datasource.
    No [database dashboard](../database-dashboard) is visible to anyone.

### UI

<video autoplay loop>
  <source src="../../videos/update-visibility.webm" type="video/webm" />
  <source src="../../videos/update-visibility.mp4" type="video/mp4" />
</video>

### Python

To change the visibility of a database where you are the owner (this is the case for self-created databases), send
a request to the HTTP API:

```python
from dbrepo.RestClient import RestClient

client = RestClient(endpoint="http://<hostname>", username="foo",
                    password="bar")
client.update_database_visibility(<database_id>,
                                  is_public=True,
                                  is_schema_public=False,
                                  is_dashboard_enabled=True)
```
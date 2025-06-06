---
author: Martin Weise
---

A database dashboard is a managed collection of available data sources automatically provisioned by DBRepo. Every
database dashboard always consists of exactly two areas:

* **Unmanaged**

    This area can be modified by the database owner and is not affected by provisioning. It is the area above the 
    **Generated Dashboard** row.

* **Managed**

    This area is affected by provisioning, any changes to this area will be overridden and deleted without notice.
    Note that the dashboard links are also affected by provisioning.

Everytime the views of the database change (e.g. a new view is added, a view is deleted) then the database dashboard for
this database is provisioned.

<video autoplay loop>
  <source src="/infrastructures/dbrepo/videos/database-dashboard.webm" type="video/webm" />
  <source src="/infrastructures/dbrepo/videos/database-dashboard.mp4" type="video/mp4" />
</video>

!!! abstract "Note"

    Database dashboards are only available for non-hidden databases, see [Update Visibility](../update-visibility).

A user wants to create a dashboard of available data sources and add personal customization.

### UI

As a database owner, you can disable database dashboards. They are enabled by default.

<video autoplay loop>
  <source src="/infrastructures/dbrepo/videos/disable-dashboard.webm" type="video/webm" />
  <source src="/infrastructures/dbrepo/videos/disable-dashboard.mp4" type="video/mp4" />
</video>

### Python

```python
from dbrepo.RestClient import RestClient
from python.dbrepo.api.dto import AccessType

client = RestClient(endpoint="http://<hostname>", username="foo",
                    password="bar")
client.update_database_visibility(<database_id>,
                                  is_public=True,
                                  is_schema_public=False,
                                  is_dashboard_enabled=True)
```

---
author: Martin Weise
---

### UI

A subset can be created by specifying a source view (e.g. My View), select the columns that are included in the subset
and optionally filter by values (e.g. `lot_shape` = `Reg`).

<video autoplay loop>
  <source src="/infrastructures/dbrepo/1.10/videos/create-subset.webm" type="video/webm" />
  <source src="/infrastructures/dbrepo/1.10/videos/create-subset.mp4" type="video/mp4" />
</video>

### Python

```python
from dbrepo.RestClient import RestClient
from dbrepo.api.dto import QueryDefinition, FilterDefinition, FilterType

client = RestClient("http://<hostname>", username="foo", password="bar")
subset = client.create_subset(<database_id>,
                              QueryDefinition("my_view",
                                              ["order", ...],
                                              filter=FilterDefinition(FilterType.WHERE, "lot_shape", "=", "Reg"),
                                              page=0,
                                              size=1000000))
printf(f"subset id: {subset.id}")
```
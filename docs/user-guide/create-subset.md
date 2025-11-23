---
author: Martin Weise
---

A subset is defined by a query that specifies which part of a dataset (=view) is included. By default, the DBRepo
restricts certain operations (e.g. computing functions) to ensure i) long-term stability, ii) -compatibility and iii)
cross-database vendor compatibility for future possible migration scenarios.

:octicons-tag-16:{ title="Minimum version" } 1.13.0

!!! info "Supported MariaDB features as of 1.13.0"

    Starting version 1.13.0, the following subset-queries are supported:

      * select queries
        - `WHERE` conditions (optional)
        - `GROUP BY` columns (optional)
        - `ORDER BY` columns (optional)
      * join queries
        - `INNER JOIN` (default for MariaDB)
        - `LEFT JOIN`
        - `LEFT INNER JOIN`
        - `RIGHT JOIN`
        - `RIGHT INNER JOIN`
        - `CROSS JOIN`

    The join conditions are connected via conjuctions (i.e. `SELECT ... FROM tbl JOIN other_tbl ON c1 = c2 AND ...`).

Currently `HAVING` conditions, `LIMIT` (and by extension `OFFSET`) statements are not supported.

### UI

A subset can be created by specifying a source view (e.g. My View), select the columns that are included in the subset
and optionally filter by values (e.g. `lot_shape` = `Reg`).

<video autoplay loop>
  <source src="/infrastructures/dbrepo/1.13/videos/create-subset.webm" type="video/webm" />
  <source src="/infrastructures/dbrepo/1.13/videos/create-subset.mp4" type="video/mp4" />
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

## Limitations

* The Python library currently does not support custom aliases when creating subsets with two columns with the same
  name. Instead, a suffix of `_a[n]` is added with `n` being an incremental counter starting at `0`.

!!! question "Do you miss functionality? Do these limitations affect you?"

    We strongly encourage you to help us implement it as we are welcoming contributors to open-source software and get
    in [contact](/infrastructures/dbrepo/1.13/contact) with us, we happily answer requests for collaboration with attached CV and your programming 
    experience!
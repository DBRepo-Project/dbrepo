---
author: Martin Weise
---

Need to start using DBRepo ASAP? This page contains only a brief introduction. You can find in-depth information about
these topics in the corresponding documentation pages on the left.

## How to Login

The following instances have different login mechanisms:

* Open for anyone:

    !!! warning "Ephemeral Data"
    
        This instance is intended for testing-purposes only and data may be deleted at any time.
        
        [test.dbrepo.tuwien.ac.at](https://test.dbrepo.tuwien.ac.at) - Create an account and login via username and
        password.

* By affiliation:

    * [dbrepo1.ec.tuwien.ac.at](https://dbrepo1.ec.tuwien.ac.at) - Login via TU SSO

## Create Database

### UI

A database can be created by choosing a name (e.g. My Database), selecting an engine (e.g. MariaDB) and defining the
visibility, this can be changed later.

<video autoplay loop>
  <source src="/infrastructures/dbrepo/1.10/videos/create-database.webm" type="video/webm" />
  <source src="/infrastructures/dbrepo/1.10/videos/create-database.mp4" type="video/mp4" />
</video>

### Python

!!! info "Python Compatibility"

    Ensure that you use the same Python library version as the target instance. For example: if you see `1.9.2` in the
    bottom left, you need to use the `1.9.2` Python library.

```python
from dbrepo.RestClient import RestClient

client = RestClient("http://<hostname>", username="foo", password="bar")
database = client.create_database("My Database", 
                                  "6cfb3b8e-1792-4e46-871a-f3d103527203",
                                  is_public=False,
                                  is_schema_public=False)
print(f"database id: {database.id}")
```

## Import Data

### UI

Create a table from a dataset by choosing a name (e.g. My Table), giving info on the CSV structure, uploading the CSV,
selecting a primary key column and then importing the dataset.

<video autoplay loop>
  <source src="/infrastructures/dbrepo/1.10/videos/import-data.webm" type="video/webm" />
  <source src="/infrastructures/dbrepo/1.10/videos/import-data.mp4" type="video/mp4" />
</video>

### Python

```python
from dbrepo.RestClient import RestClient
from pandas import DataFrame

df = DataFrame({'some_col': 123})

client = RestClient("http://<hostname>", username="foo", password="bar")
table = client.create_table(<database_id>,
                            "My Table",
                            is_public=False,
                            is_schema_public=False,
                            dataframe=df)
print(f"table id: {table.id}")
```

## Create View

### UI

A view can be created by specifying a source table (e.g. My Table), select the columns that are included in the view
and optionally filter by values (e.g. `street` = `Pave`).

<video autoplay loop>
  <source src="/infrastructures/dbrepo/1.10/videos/create-view.webm" type="video/webm" />
  <source src="/infrastructures/dbrepo/1.10/videos/create-view.mp4" type="video/mp4" />
</video>

### Python

```python
from dbrepo.RestClient import RestClient
from dbrepo.api.dto import QueryDefinition, FilterDefinition, FilterType

client = RestClient("http://<hostname>", username="foo", password="bar")
view = client.create_view(<database_id>,
                          "My View",
                          QueryDefinition("my_table",
                                          ["order", ...],
                                          filter=FilterDefinition(FilterType.WHERE, "street", "=", "Pave"),
                                          is_public=False,
                                          is_schema_public=False))
printf(f"view id: {view.id}")
```

## Create Subset

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
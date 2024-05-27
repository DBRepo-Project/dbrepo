---
author: Martin Weise
---

[![PyPI - Version](https://img.shields.io/pypi/v/dbrepo)](https://pypi.org/project/dbrepo/){ tabindex=-1 }

## tl;dr

[:simple-python: &nbsp;Python Library](../../sphinx/){ .md-button .md-button--primary tabindex=-1 }

## Installing

:octicons-tag-16:{ title="Minimum version" } 1.4.2

```console
$ python -m pip install dbrepo
```

To use DBRepo in your Jupyter notebook, install the `dbrepo` library` directly in a code cell and type:

```jupyter
!pip install dbrepo
```

This package supports Python 3.11+.

## Quickstart

Create a table and import a .csv file from your computer.

```python
from dbrepo.RestClient import RestClient
from dbrepo.api.dto import CreateTableColumn, ColumnType, CreateTableConstraints

client = RestClient(endpoint='https://test.dbrepo.tuwien.ac.at', username="foo",
                    password="bar")

# analyse csv
analysis = client.analyse_datatypes(file_path="sensor.csv", separator=",")
print(f"Analysis result: {analysis}")
# -> columns=(date=date, precipitation=decimal, lat=decimal, lng=decimal), separator=,
#    line_termination=\n

# create table
table = client.create_table(database_id=1,
                            name="Sensor Data",
                            constraints=CreateTableConstraints(checks=['precipitation >= 0'],
                                                               uniques=[['precipitation']]),
                            columns=[CreateTableColumn(name="date",
                                                       type=ColumnType.DATE,
                                                       dfid=3,  # YYYY-MM-dd
                                                       primary_key=True,
                                                       null_allowed=False),
                                     CreateTableColumn(name="precipitation",
                                                       type=ColumnType.DECIMAL,
                                                       size=10,
                                                       d=4,
                                                       primary_key=False,
                                                       null_allowed=True),
                                     CreateTableColumn(name="lat",
                                                       type=ColumnType.DECIMAL,
                                                       size=10,
                                                       d=4,
                                                       primary_key=False,
                                                       null_allowed=True),
                                     CreateTableColumn(name="lng",
                                                       type=ColumnType.DECIMAL,
                                                       size=10,
                                                       d=4,
                                                       primary_key=False,
                                                       null_allowed=True)])
print(f"Create table result {table}")
# -> (id=1, internal_name=sensor_data, ...)

client.import_table_data(database_id=1, table_id=1, file_path="sensor.csv", separator=",",
                         skip_lines=1, line_encoding="\n")
print(f"Finished.")
```

The library is well-documented, please see the [full documentation](./sphinx) or
the [PyPI page](https://pypi.org/project/dbrepo/).

## Supported Features & Best-Practices

- Manage user account ([docs](./usage-overview/#create-user-account))
- Manage databases ([docs](./usage-overview/#create-database))
- Manage database access & visibility ([docs](./usage-overview/#private-database-access))
- Import dataset ([docs](./usage-overview/#private-database-access))
- Create persistent identifiers ([docs](./usage-overview/#assign-database-pid))
- Execute queries ([docs](./usage-overview/#export-subset))
- Get data from tables/views/subsets

## Secrets

It is not recommended to store credentials directly in the notebook as they will be versioned with git, etc. Use
environment variables instead:

```properties title=".env"
DBREPO_ENDPOINT=https://test.dbrepo.tuwien.ac.at
DBREPO_USERNAME=foo
DBREPO_PASSWORD=bar
DBREPO_SECURE=True
```

Then use the default constructor of the `RestClient` to e.g. analyse a CSV. Your secrets are automatically passed:

```python title="analysis.py"
from dbrepo.RestClient import RestClient

client = RestClient()
analysis = client.analyse_datatypes(file_path="sensor.csv", separator=",")
```

## Future

- Searching

## Links

This information is also mirrored on [PyPI](https://pypi.org/project/dbrepo/).
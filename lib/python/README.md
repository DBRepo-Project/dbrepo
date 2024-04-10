# DBRepo Python Library

Official client library for [DBRepo](https://www.ifs.tuwien.ac.at/infrastructures/dbrepo/__APPVERSION__/), a database
repository to support research based
on [requests](https://pypi.org/project/requests/), [pydantic](https://pypi.org/project/pydantic/), [tuspy](https://pypi.org/project/tuspy/)
and [pika](https://pypi.org/project/pika/).

## Installing

```console
$ python -m pip install dbrepo
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
                            constraints=CreateTableConstraints(
                                checks=['precipitation >= 0'],
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

## Supported Features & Best-Practices

- Manage user
  account ([docs](https://www.ifs.tuwien.ac.at/infrastructures/dbrepo//usage-overview/#create-user-account))
- Manage
  databases ([docs](https://www.ifs.tuwien.ac.at/infrastructures/dbrepo//usage-overview/#create-database))
- Manage database access &
  visibility ([docs](https://www.ifs.tuwien.ac.at/infrastructures/dbrepo//usage-overview/#private-database-access))
- Import
  dataset ([docs](https://www.ifs.tuwien.ac.at/infrastructures/dbrepo//usage-overview/#private-database-access))
- Create persistent
  identifiers ([docs](https://www.ifs.tuwien.ac.at/infrastructures/dbrepo//usage-overview/#assign-database-pid))
- Execute
  queries ([docs](https://www.ifs.tuwien.ac.at/infrastructures/dbrepo//usage-overview/#export-subset))
- Get data from tables/views/subsets

## Configure

All credentials can optionally be set/overridden with environment variables. This is especially useful when sharing 
Jupyter Notebooks by creating an invisible `.env` file and loading it:

```
REST_API_ENDPOINT="https://test.dbrepo.tuwien.ac.at"
REST_API_USERNAME="foo"
REST_API_PASSWORD="bar"
REST_API_SECURE="True"
AMQP_API_HOST="https://test.dbrepo.tuwien.ac.at"
AMQP_API_PORT="5672"
AMQP_API_USERNAME="foo"
AMQP_API_PASSWORD="bar"
AMQP_API_VIRTUAL_HOST="/"
REST_UPLOAD_ENDPOINT="https://test.dbrepo.tuwien.ac.at/api/upload/files"
```

## Roadmap

- Searching

## Contact

* Prof. [Andreas Rauber](https://tiss.tuwien.ac.at/person/39608.html)<sup>TU Wien</sup>
* DI. [Martin Weise](https://tiss.tuwien.ac.at/person/287722.html)<sup>TU Wien</sup>
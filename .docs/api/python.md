---
author: Martin Weise
---

[![PyPI - Version](https://img.shields.io/pypi/v/dbrepo)](https://pypi.org/project/dbrepo/){ tabindex=-1 }

## tl;dr

[:fontawesome-solid-cube: &nbsp;View Docs](../../python){ .md-button .md-button--primary }

## Overview

The DBRepo Python library is using some of the most pupular and maintained Python packages for Data Scientists under the
hood. For example: [`requests`](https://requests.readthedocs.io/) to interact with the HTTP API
endpoints, [`pandas`](https://pandas.pydata.org/) for data operations and [`pydantic`](https://docs.pydantic.dev/) for
information representation from/to the HTTP API.

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

Get public data from a table as pandas `DataFrame`:

```python
from dbrepo.RestClient import RestClient

client = RestClient(endpoint="https://dbrepo1.ec.tuwien.ac.at")
# Get a small data slice of just three rows
df = client.get_table_data(database_id=7, table_id=13, page=0, size=3, df=True)
print(df)
#     x_coord         component   unit  ... value stationid meantype
# 0  16.52617  Feinstaub (PM10)  µg/m³  ...  21.0   01:0001      HMW
# 1  16.52617  Feinstaub (PM10)  µg/m³  ...  23.0   01:0001      HMW
# 2  16.52617  Feinstaub (PM10)  µg/m³  ...  26.0   01:0001      HMW
#
# [3 rows x 12 columns]
```

Import data into a table:

```python
import pandas as pd
from dbrepo.RestClient import RestClient

client = RestClient(endpoint="https://dbrepo1.ec.tuwien.ac.at", username="foo",
                    password="bar")
df = pd.DataFrame(data={'x_coord': 16.52617, 'component': 'Feinstaub (PM10)',
                        'unit': 'µg/m³', ...})
client.import_table_data(database_id=7, table_id=13, file_name_or_data_frame=df)
```

## Supported Features & Best-Practices

- Manage user
  account ([docs](https://www.ifs.tuwien.ac.at/infrastructures/dbrepo/1.4.5/api/#create-user-account))
- Manage
  databases ([docs](https://www.ifs.tuwien.ac.at/infrastructures/dbrepo//usage-overview/#create-database))
- Manage database access &
  visibility ([docs](https://www.ifs.tuwien.ac.at/infrastructures/dbrepo/1.4.5/api/#create-database))
- Import
  dataset ([docs](https://www.ifs.tuwien.ac.at/infrastructures/dbrepo/1.4.5/api/#import-dataset))
- Create persistent
  identifiers ([docs](https://www.ifs.tuwien.ac.at/infrastructures/dbrepo/1.4.5/api/#assign-database-pid))
- Execute
  queries ([docs](https://www.ifs.tuwien.ac.at/infrastructures/dbrepo/1.4.5/api/#export-subset))
- Get data from tables/views/subsets

## Configure

All credentials can optionally be set/overridden with environment variables. This is especially useful when sharing 
Jupyter Notebooks by creating an invisible `.env` file and loading it:

``` title=".env"
REST_API_ENDPOINT="https://dbrepo1.ec.tuwien.ac.at"
REST_API_USERNAME="foo"
REST_API_PASSWORD="bar"
REST_API_SECURE="True"
AMQP_API_HOST="https://dbrepo1.ec.tuwien.ac.at"
AMQP_API_PORT="5672"
AMQP_API_USERNAME="foo"
AMQP_API_PASSWORD="bar"
AMQP_API_VIRTUAL_HOST="dbrepo"
REST_UPLOAD_ENDPOINT="https://dbrepo1.ec.tuwien.ac.at/api/upload/files"
```

You can disable logging by setting the log level to e.g. `INFO`:

```python
from dbrepo.RestClient import RestClient
import logging
logging.getLogger().setLevel(logging.INFO)
...
client = RestClient(...)
```

## Future

- Searching

## Links

This information is also mirrored on [PyPI](https://pypi.org/project/dbrepo/).
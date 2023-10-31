# Search service

🚧 WIP 🚧

The dbrepo search service is used to enable searching for
entries in the opensearch databse.

## Running the app
Test the app locally:
```shell
pipenv install && pipenv run flask run --debug --port 4000
```

## Overview
Here's an overview about the different endpoints available at this service:
(`<index>` has to be one of the following indices:
table, user, database, column, identifier, concept, unit, view)

---
`/api/search/<index>` :
returns all entries for a given index

---
`/api/search/<index>/fields`:
returns all the fields that are saved in a given entry

---
`/api/search`:
this is the main endpoint for searching entries in the opensearch db. 
You can specify a search term, a time period
and certain fields that should match a certain value.

ToDo: Continue
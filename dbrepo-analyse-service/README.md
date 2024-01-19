# Analyse Service

Service to analyse datatypes, update statistical properties of databases in the metadata database, add metadata, e.g., 
data provenance, db description ... to the metadata database. Remark: if you use swagger-ui, you can switch between
/api-analyze.json and /api-mdb.json

## Endpoints

* Prometheus metrics [`/metrics`](http://localhost:5000/metrics)
* Health check [`/health`](http://localhost:5000/health)
* API 

## Development

Install all dev dependencies from the `Pipfile`:

```shell
pipenv install --dev
```

## Test

Run all tests in `test/`:

```shell
coverage run -m pytest test/test_determine_dt.py test/test_determine_pk.py test/test_s3_client.py --junitxml=report.xml
coverage html --omit="test/*" # (optional html report)
```

## Other

Potential issues when upgrading to Python 3.10+ as `messytables` requires `collections` and the interface changed for
Python 3.10 onwards, see
the [StackOverflow](https://stackoverflow.com/questions/69381312/importerror-cannot-import-name-mapping-from-collections-using-python-3-10)
post.
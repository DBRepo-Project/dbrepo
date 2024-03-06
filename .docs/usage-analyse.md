---
author: Martin Weise
---

# Analyse Service

Given a [CSV-file](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-datasets/-/raw/master/gps.csv)
containing GPS-data `gps.csv` already uploaded in the `dbrepo-upload` bucket of the Storage Service with key `gps.csv`:

=== "Terminal"

    ```shell
    curl -X POST \
      -d '{"filename":"gps.csv","separator":","}'
      http://<hostname>/api/analyse/determinedt
    ```
    
    This results in the response:
    
    ```json
    {
        "columns": {
            "ID": "bigint",
            "KEY": "varchar",
            "OBJECTID": "bigint",
            "LBEZEICHNUNG": "varchar",
            "LTYP": "bigint",
            "LTYPTXT": "varchar",
            "LAT": "decimal",
            "LNG": "decimal"
        },
        "separator": ","
    }
    ```
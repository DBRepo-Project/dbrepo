#!/bin/env python3
from api_table.api.table_endpoint_api import TableEndpointApi
from api_query.api.table_data_endpoint_api import TableDataEndpointApi
import time
import os
import shutil
import uuid

table = TableEndpointApi()
data = TableDataEndpointApi()

def create_table(container_id, database_id, columns=None):
    if columns is None:
        columns = [
            {"name": "UUID", "type": "string", dfid: None, "unique": True, "primary_key": True, "null_allowed": False},
            {"name": "Point", "type": "string", dfid: None, "unique": False, "primary_key": False, "null_allowed": True},
            {"name": "Value", "type": "decimal", dfid: None, "unique": False, "primary_key": False, "null_allowed": True},
            {"name": "Unit", "type": "string", dfid: None, "unique": False, "primary_key": False, "null_allowed": True},
            {"name": "Timestamp", "type": "timestamp", dfid: 1, "unique": False, "primary_key": False, "null_allowed": True}
        ]
    response = table.create({
        "name": "Power",
        "description": "Power consumption in the Pilot Factory",
        "columns": columns
    }, "Bearer " + token, container_id, database_id)
    print("created table with id %d" % response.id)
    return response


def fill_table(container_id, database_id, table_id):
    response = data.import_csv({
        "location": "/path/to/data.csv",
        "quote": "\"",
        "null_element": "NA"
        "separator": ",",
    }, container_id, database_id, table_id)
    print("filled table with id %d" % table_id)
    return response


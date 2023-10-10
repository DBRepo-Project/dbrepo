# -*- coding: utf-8 -*-
"""
Created on Fri Sep 25 21:25:09 2020
From: 
https://messytables.readthedocs.io/en/latest/

https://github.com/okfn/messytables/

@author: Co
"""

import json
import csv
import logging
import os

import messytables, pandas as pd
from messytables import CSVTableSet, type_guess, \
    headers_guess, headers_processor, offset_processor


def determine_datatypes(filename, enum=False, enum_tol=0.0001, separator=None) -> {}:
    # Use option enum=True for searching Postgres ENUM Types in CSV file. Remark
    # Enum is not SQL standard, hence, it might not be supported by all db-engines.
    # However, it can be used in Postgres and MySQL.
    path = os.path.join(os.getenv('SHARED_FILESYSTEM', '/tmp'), filename)
    if separator is None:
        with open(path) as csvfile:
            dialect = csv.Sniffer().sniff(csvfile.readline())
        separator = dialect.delimiter
        logging.debug('determined separator: %s', separator)

    # Load a file object:
    with open(path, 'rb') as fh:
        table_set = CSVTableSet(fh, delimiter=separator)

        # A table set is a collection of tables:
        row_set = table_set.tables[0]

        # guess header names and the offset of the header:
        offset, headers = headers_guess(row_set.sample)
        row_set.register_processor(headers_processor(headers))

        # add one to begin with content, not the header:
        row_set.register_processor(offset_processor(offset + 1))

        # guess column types:
        types = type_guess(row_set.sample, strict=True)

        r = {}

        # list of rows
        if enum == True:
            rows = pd.read_csv(path, sep=separator, header=offset)
            n = len(rows)

        for i in range(0, (len(types))):
            if type(types[i]) == messytables.types.BoolType:
                r[headers[i]] = "bool"
            elif type(types[i]) == messytables.types.IntegerType:
                r[headers[i]] = "bigint"
            elif type(types[i]) == messytables.types.FloatType:
                r[headers[i]] = "float"
            elif type(types[i]) == messytables.types.DateType:
                if ("S" in str(types[i])):
                    r[headers[i]] = "timestamp"
                else:
                    r[headers[i]] = "date"
            elif type(types[i]) == messytables.types.DecimalType:
                r[headers[i]] = "decimal"
            elif type(types[i]) == messytables.types.StringType:
                r[headers[i]] = "varchar"
            elif type(types[i]) == messytables.types.PercentageType:
                r[headers[i]] = "double"
            elif type(types[i]) == messytables.types.CurrencyType:
                r[headers[i]] = "double"
            elif type(types[i]) == messytables.types.TimeType:
                r[headers[i]] = "time"
            else:
                if enum == True:
                    enum_set = set()
                    m = 0
                    is_enum = True
                    for elem in range(0, n):
                        if (m < enum_tol * n):
                            enum_set.add(rows.iloc[elem, i])
                        else:
                            is_enum = False
                            break
                        m = len(enum_set)
                    if is_enum:
                        enum_set.discard(None)
                        r[headers[i]] = {"enums": list(enum_set)}
                    else:
                        r[headers[i]] = "text"
                else:
                    r[headers[i]] = "text"
        fh.close()
        s = {'columns': r, 'separator': separator}
        logging.info('Determined data types %s', s)
    return json.dumps(s)


"""
Example output:
{
  "columns": {
    "col1": "integer",
    "col2": "string",
    "col3": "string"
  }
}
"""

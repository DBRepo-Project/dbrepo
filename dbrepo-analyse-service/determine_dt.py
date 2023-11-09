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
import io

import boto3
import messytables, pandas as pd
from messytables import CSVTableSet, type_guess, \
    headers_guess, headers_processor, offset_processor


def determine_datatypes(filename, enum=False, enum_tol=0.0001, separator=None) -> {}:
    # Use option enum=True for searching Postgres ENUM Types in CSV file. Remark
    # Enum is not SQL standard, hence, it might not be supported by all db-engines.
    # However, it can be used in Postgres and MySQL.
    endpoint_url = os.getenv('S3_STORAGE_ENDPOINT', 'http://localhost:9000')
    aws_access_key_id = os.getenv('S3_ACCESS_KEY_ID', 'minioadmin')
    aws_secret_access_key = os.getenv('S3_SECRET_ACCESS_KEY', 'minioadmin')
    s3_client = boto3.client(service_name='s3', endpoint_url=endpoint_url, aws_access_key_id=aws_access_key_id,
                             aws_secret_access_key=aws_secret_access_key)
    logging.info("retrieve file from S3, endpoint_url=%s, aws_access_key_id=%s, aws_secret_access_key=(hidden)",
                 endpoint_url, aws_access_key_id)
    response = s3_client.get_object(Bucket='dbrepo-upload', Key=filename)
    stream = response['Body']
    if separator is None:
        logging.info('Attempt to guess separator for from first line')
        with io.BytesIO(stream.read()) as fh:
            line = fh.readline().decode('utf-8')
            dialect = csv.Sniffer().sniff(line)
            separator = dialect.delimiter
            logging.info('determined separator: %s', separator)

    # Load a file object:
    with io.BytesIO(stream.read()) as fh:
        logging.info('Analysing corpus with separator: %s', separator)
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
            rows = pd.read_csv(fh, sep=separator, header=offset)
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

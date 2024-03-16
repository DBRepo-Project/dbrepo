# -*- coding: utf-8 -*-
"""
@author: Martin Weise
"""
import json
import csv
import logging
import io
from clients.s3_client import S3Client

import messytables, pandas as pd
from messytables import (
    CSVTableSet,
    type_guess,
    headers_guess,
    headers_processor,
    offset_processor,
)


def determine_datatypes(filename, enum=False, enum_tol=0.0001, separator=None) -> {}:
    # Use option enum=True for searching Postgres ENUM Types in CSV file. Remark
    # Enum is not SQL standard, hence, it might not be supported by all db-engines.
    # However, it can be used in Postgres and MySQL.
    s3_client = S3Client()
    s3_client.file_exists('dbrepo-upload', filename)
    response = s3_client.get_file('dbrepo-upload', filename)
    stream = response['Body']
    if response['ContentLength'] == 0:
        logging.warning(f'Failed to determine data types: file {filename} has empty body')
        return json.dumps({'columns': [], 'separator': ','})
    if separator is None:
        logging.info("Attempt to guess separator for from first line")
        with io.BytesIO(stream.read()) as fh:
            line = next(fh)
            dialect = csv.Sniffer().sniff(line.decode("utf-8"))
            separator = dialect.delimiter
            logging.info("determined separator: %s", separator)

    # Load a file object:
    with io.BytesIO(stream.read()) as fh:
        line_terminator = None
        if b"\n" in fh.readline():
            line_terminator = "\n"
        elif b"\r" in fh.readline():
            line_terminator = "\r"
        elif b"\r\n" in fh.readline():
            line_terminator = "\r\n"
        logging.info("Analysing corpus with separator: %s", separator)
        table_set = CSVTableSet(fh, delimiter=separator, lineterminator=line_terminator)

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

        for i in range(0, (len(types))):
            if type(types[i]) == messytables.types.BoolType:
                r[headers[i]] = "bool"
            elif type(types[i]) == messytables.types.IntegerType:
                r[headers[i]] = "bigint"
            elif type(types[i]) == messytables.types.DateType:
                if (
                    "%H" in types[i].format
                    or "%M" in types[i].format
                    or "%S" in types[i].format
                    or "%Z" in types[i].format
                ):
                    r[
                        headers[i]
                    ] = "timestamp"  # todo: guesses date format too, return it
                else:
                    r[headers[i]] = "date"
            elif (
                type(types[i]) == messytables.types.DecimalType
                or type(types[i]) == messytables.types.FloatType
            ):
                r[headers[i]] = "decimal"
            elif type(types[i]) == messytables.types.StringType:
                r[headers[i]] = "varchar"
            else:
                r[headers[i]] = "text"
        s = {"columns": r, "separator": separator, "line_termination": line_terminator}
        logging.info("Determined data types %s", s)
    return json.dumps(s)

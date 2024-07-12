# -*- coding: utf-8 -*-
"""
@author: Martin Weise
"""
import json
import logging
import io
import pandas

from numpy import dtype, max, min
from flask import current_app

from clients.s3_client import S3Client


def determine_datatypes(filename, enum=False, enum_tol=0.0001, separator=None) -> {}:
    # Use option enum=True for searching Postgres ENUM Types in CSV file. Remark
    # Enum is not SQL standard, hence, it might not be supported by all db-engines.
    # However, it can be used in Postgres and MySQL.
    s3_client = S3Client()
    s3_client.file_exists(current_app.config['S3_BUCKET'], filename)
    response = s3_client.get_file(current_app.config['S3_BUCKET'], filename)
    stream = response['Body']
    if response['ContentLength'] == 0:
        logging.warning(f'Failed to determine data types: file {filename} has empty body')
        return json.dumps({'columns': [], 'separator': ','})

    with io.BytesIO(stream.read()) as fh:
        line_terminator = None

        line = peek_line(fh)
        if b"\n" in line:
            line_terminator = "\n"
        elif b"\r" in line:
            line_terminator = "\r"
        elif b"\r\n" in line:
            line_terminator = "\r\n"
        logging.info("Analysing corpus with separator: %s", separator)

        # index_col=False -> prevent shared index & count length correct
        df = pandas.read_csv(fh, delimiter=separator, nrows=100, lineterminator=line_terminator, index_col=False)

        if b"," in line:
            separator = ","
        elif b";" in line:
            separator = ";"
        elif b"\t" in line:
            separator = "\t"

        r = {}

        for name, dataType in df.dtypes.items():
            if dataType == dtype('float64'):
                r[name] = 'decimal'
            elif dataType == dtype('int64'):
                min_val = min(df[name])
                max_val = max(df[name])
                if 0 <= min_val <= 1 and 0 <= max_val <= 1:
                    r[name] = 'bool'
                    continue
                r[name] = 'bigint'
            elif dataType == dtype('O'):
                try:
                    pandas.to_datetime(df[name], format='mixed')
                    r[name] = 'timestamp'
                    continue
                except ValueError:
                    pass
                max_size = max(df[name].astype(str).map(len))
                if max_size <= 1:
                    r[name] = 'char'
                if 0 <= max_size <= 255:
                    r[name] = 'varchar'
                else:
                    r[name] = 'text'
            elif dataType == dtype('bool'):
                r[name] = 'bool'
            elif dataType == dtype('datetime64'):
                r[name] = 'datetime'
            else:
                logging.warning(f'default to \'text\' for column {name} and type {dtype}')
                r[name] = 'text'
        s = {"columns": r, "separator": separator, "line_termination": line_terminator}
        logging.info("Determined data types %s", s)
    return json.dumps(s)


def peek_line(f) -> bytes:
    pos = f.tell()
    line: bytes = f.readline()
    f.seek(pos)
    return line

# -*- coding: utf-8 -*-
"""
@author: Martin Weise
"""
import logging
import io
import pandas

from numpy import dtype, max, min
from flask import current_app
from pandas import DataFrame, NaT
from pandas.errors import EmptyDataError, ParserError

from api.dto import ColumnAnalysisDto, DataTypeDto, AnalysisDto
from clients.s3_client import S3Client


def determine_datatypes(filename, enum=False, enum_tol=0.0001, separator=',') -> AnalysisDto:
    # Use option enum=True for searching Postgres ENUM Types in CSV file. Remark
    # Enum is not SQL standard, hence, it might not be supported by all db-engines.
    # However, it can be used in Postgres and MySQL.
    s3_client = S3Client()
    s3_client.file_exists(current_app.config['S3_BUCKET'], filename)
    response = s3_client.get_file(current_app.config['S3_BUCKET'], filename)
    stream = response['Body']
    if response['ContentLength'] == 0:
        logging.warning(f'Failed to determine data types: file {filename} has empty body')
        return AnalysisDto(columns=dict(), separator=",", line_termination="\n")

    with io.BytesIO(stream.read()) as fh:
        line_terminator = None

        line = peek_line(fh)
        if b"\n" in line:
            line_terminator = "\n"
        elif b"\r" in line:
            line_terminator = "\r"
        elif b"\r\n" in line:
            line_terminator = "\r\n"
        logging.info(f"Analysing corpus with separator: {separator}")

        # index_col=False -> prevent shared index & count length correct
        df = None
        for encoding in ['utf-8', 'cp1252', 'latin1', 'iso-8859-1']:
            try:
                logging.debug(f"attempt parsing .csv using encoding {encoding}")
                df = pandas.read_csv(fh, delimiter=separator, nrows=current_app.config['ANALYSE_NROWS'],
                                     lineterminator=line_terminator, index_col=False, encoding=encoding)
                logging.debug(f"parsing .csv using encoding {encoding} was successful")
                break
            except ParserError as error:
                raise IOError(f"Failed to parse .csv using separator {separator}: {error}")
            except (UnicodeDecodeError, EmptyDataError) as error:
                raise IOError(f"Failed to parse .csv using encoding {encoding}: {error}")
        if df is None:
            raise IOError(
                f"Failed to parse .csv: no supported encoding found (one of: utf-8, cp1252, latin1, iso-8859-1)")

        if b"," in line:
            separator = ","
        elif b";" in line:
            separator = ";"
        elif b"\t" in line:
            separator = "\t"

        r = {}

        for name, dataType in df.dtypes.items():
            # trim leading/trailing whitespaces
            column_name = name.strip()
            col = ColumnAnalysisDto(type=DataTypeDto.TEXT, null_allowed=contains_null(df[name]))
            r[column_name] = col
            if dataType == dtype('float64'):
                if pandas.to_numeric(df[name], errors='coerce').notnull().all():
                    logging.debug(f"mapped column {name} from float64 to decimal")
                    col.type = DataTypeDto.DECIMAL
                else:
                    logging.debug(f"mapped column {name} from float64 to text")
                    col.type = DataTypeDto.TEXT
            elif dataType == dtype('int64'):
                min_val = min(df[name])
                max_val = max(df[name])
                if 0 <= min_val <= 1 and 0 <= max_val <= 1:
                    logging.debug(f"mapped column {name} from int64 to bool")
                    col.type = DataTypeDto.BOOL
                    continue
                logging.debug(f"mapped column {name} from int64 to bigint")
                col.type = DataTypeDto.BIGINT
            elif dataType == dtype('O'):
                try:
                    pandas.to_datetime(df[name], format='mixed')
                    if df[name].str.contains(':').any():
                        logging.debug(f"mapped column {name} from O to timestamp")
                        col.type = DataTypeDto.TIMESTAMP
                        continue
                    logging.debug(f"mapped column {name} from O to date")
                    col.type = DataTypeDto.DATE
                    continue
                except ValueError:
                    pass
                max_size = max(df[name].astype(str).map(len))
                if max_size <= 1:
                    logging.debug(f"mapped column {name} from O to char")
                    col.type = DataTypeDto.CHAR
                    col.size = 1
                if 0 <= max_size <= 255:
                    logging.debug(f"mapped column {name} from O to varchar")
                    col.type = DataTypeDto.VARCHAR
                    col.size = 255
                else:
                    logging.debug(f"mapped column {name} from O to text")
                    col.type = DataTypeDto.TEXT
            elif dataType == dtype('bool'):
                logging.debug(f"mapped column {name} from bool to bool")
                col.type = DataTypeDto.BOOL
            elif dataType == dtype('datetime64'):
                logging.debug(f"mapped column {name} from datetime64 to datetime")
                col.type = DataTypeDto.DATETIME
            else:
                logging.warning(f'default to \'text\' for column {name} and type {dtype}')
        s = AnalysisDto(columns=r, separator=separator, line_termination=line_terminator)
        logging.info("Determined data types %s", s)
    return s


def peek_line(f) -> bytes:
    pos = f.tell()
    line: bytes = f.readline()
    f.seek(pos)
    return line


def contains_null(df: DataFrame) -> bool:
    if '\\N' in df.values:
        return True
    return df.isnull().values.any()

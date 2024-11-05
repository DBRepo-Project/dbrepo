from enum import Enum
from typing import Optional, List

from pydantic import BaseModel


class DataTypeDto(str, Enum):
    """
    Enumeration of languages.
    """
    BIGINT = "bigint"
    BINARY = "binary"
    BIT = "bit"
    BLOB = "blob"
    BOOL = "bool"
    CHAR = "char"
    DATE = "date"
    DATETIME = "datetime"
    DECIMAL = "decimal"
    DOUBLE = "double"
    ENUM = "enum"
    FLOAT = "float"
    INT = "int"
    LONGBLOB = "longblob"
    LONGTEXT = "longtext"
    MEDIUMBLOB = "mediumblob"
    MEDIUMINT = "mediumint"
    MEDIUMTEXT = "mediumtext"
    SET = "set"
    SERIAL = "serial"
    SMALLINT = "smallint"
    TEXT = "text"
    TIMESTAMP = "timestamp"
    TINYBLOB = "tinyblob"
    TINYINT = "tinyint"
    TINYTEXT = "tinytext"
    YEAR = "year"
    VARBINARY = "varbinary"
    VARCHAR = "varchar"


class ColumnAnalysisDto(BaseModel):
    type: DataTypeDto
    null_allowed: bool
    size: Optional[int] = None
    d: Optional[int] = None
    dfid: Optional[int] = None
    enums: Optional[list] = None
    sets: Optional[list] = None


class AnalysisDto(BaseModel):
    columns: dict[str, ColumnAnalysisDto]
    separator: str
    line_termination: str


class ColumnStatDto(BaseModel):
    val_min: Optional[float] = None
    val_max: Optional[float] = None
    mean: Optional[float] = None
    median: Optional[float] = None
    std_dev: Optional[float] = None


class TableStatDto(BaseModel):
    columns: dict[str, ColumnStatDto]

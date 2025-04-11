import logging

import pandas
from numpy import dtype
from pandas import DataFrame, Series

from dbrepo.api.dto import Subset, QueryDefinition, Database, Table, Image, Filter, Order, CreateTableColumn, \
    CreateTableConstraints, ColumnType, DatasourceType
from dbrepo.api.exceptions import MalformedError


def query_to_subset(database: Database, image: Image, query: QueryDefinition) -> Subset:
    if len(query.columns) < 1:
        raise MalformedError(f'Failed to create view: no columns selected')
    tables: [Table] = [table for table in database.tables if table.internal_name == query.table]
    if len(tables) != 1:
        raise MalformedError(f'Failed to create view: table name not found in database')
    filtered_column_ids: [str] = [column.id for column in tables[0].columns if
                                  column.internal_name in query.columns]
    if len(filtered_column_ids) != len(query.columns):
        raise MalformedError(f'Failed to create view: not all columns found in database')
    filters = []
    if query.filter is not None:
        for filter in query.filter:
            # column_id
            filter_column_ids: [str] = [column.id for column in tables[0].columns if
                                        column.internal_name == filter.column]
            if len(filter_column_ids) != 1:
                raise MalformedError(f'Failed to create view: filtered column name not found in database')
            # operator_id
            filter_ops_ids: [str] = [op.id for op in image.operators if op.value == filter.operator]
            if len(filter_ops_ids) != 1:
                raise MalformedError(f'Failed to create view: filter operator not found in image')
            filters.append(Filter(type=filter.type,
                                  column_id=filter_column_ids[0],
                                  operator_id=filter_ops_ids[0],
                                  value=filter.value))
    orders = []
    if query.order is not None:
        for order in query.order:
            # column_id
            order_column_ids: [str] = [column.id for column in tables[0].columns if
                                       column.internal_name == order.column]
            if len(order_column_ids) != 1:
                raise MalformedError(f'Failed to create view: order column name not found in database')
            orders.append(Order(column_id=order_column_ids[0], direction=order.direction))
    return Subset(datasource_id=tables[0].id, datasource_type=DatasourceType.TABLE, columns=filtered_column_ids,
                  filter=filters, order=orders)


def dataframe_to_table_definition(dataframe: DataFrame) -> ([CreateTableColumn], CreateTableConstraints):
    if dataframe.index.name is None:
        raise MalformedError(f'Failed to map dataframe: index not set')
    constraints = CreateTableConstraints(uniques=[],
                                         checks=[],
                                         foreign_keys=[],
                                         primary_key=dataframe.index.names)
    dataframe = dataframe.reset_index()
    columns = []
    for name, series in dataframe.items():
        column = CreateTableColumn(name=str(name),
                                   type=ColumnType.TEXT,
                                   null_allowed=contains_null(dataframe[name]))
        if series.dtype == dtype('float64'):
            if pandas.to_numeric(dataframe[name], errors='coerce').notnull().all():
                logging.debug(f"mapped column {name} from float64 to decimal")
                column.type = ColumnType.DECIMAL
                column.size = 40
                column.d = 20
            else:
                logging.debug(f"mapped column {name} from float64 to text")
                column.type = ColumnType.TEXT
        elif series.dtype == dtype('int64'):
            min_val = min(dataframe[name])
            max_val = max(dataframe[name])
            if 0 <= min_val <= 1 and 0 <= max_val <= 1 and 'id' not in name:
                logging.debug(f"mapped column {name} from int64 to bool")
                column.type = ColumnType.BOOL
                columns.append(column)
                continue
            logging.debug(f"mapped column {name} from int64 to bigint")
            column.type = ColumnType.BIGINT
        elif series.dtype == dtype('O'):
            try:
                pandas.to_datetime(dataframe[name], format='mixed')
                if dataframe[name].str.contains(':').any():
                    logging.debug(f"mapped column {name} from O to timestamp")
                    column.type = ColumnType.TIMESTAMP
                    columns.append(column)
                    continue
                logging.debug(f"mapped column {name} from O to date")
                column.type = ColumnType.DATE
                columns.append(column)
                continue
            except ValueError:
                pass
            max_size = max(dataframe[name].astype(str).map(len))
            if max_size <= 1:
                logging.debug(f"mapped column {name} from O to char")
                column.type = ColumnType.CHAR
                column.size = 1
            if 0 <= max_size <= 255:
                logging.debug(f"mapped column {name} from O to varchar")
                column.type = ColumnType.VARCHAR
                column.size = 255
            else:
                logging.debug(f"mapped column {name} from O to text")
                column.type = ColumnType.TEXT
        elif series.dtype == dtype('bool'):
            logging.debug(f"mapped column {name} from bool to bool")
            column.type = ColumnType.BOOL
        elif series.dtype == dtype('datetime64'):
            logging.debug(f"mapped column {name} from datetime64 to datetime")
            column.type = ColumnType.DATETIME
        else:
            logging.warning(f'default to \'text\' for column {name} and type {dtype}')
        columns.append(column)
    return columns, constraints


def contains_null(dataframe: DataFrame) -> bool:
    if '\\N' in dataframe.values:
        return True
    return dataframe.isnull().values.any()

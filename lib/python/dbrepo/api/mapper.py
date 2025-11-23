import logging
from itertools import groupby
from typing import List

import pandas
from numpy import dtype, array
from pandas import DataFrame

from dbrepo.api.dto import Subset, QueryDefinition, Database, Image, Filter, Order, CreateTableColumn, \
    CreateTableConstraints, ColumnType, SubsetColumn, FilterType, Join, Conditional
from dbrepo.api.exceptions import MalformedError


def query_to_subset(database: Database, image: Image, query: QueryDefinition) -> Subset:
    if len(query.columns) < 1:
        raise MalformedError(f'Failed to map subset: no column(s) selected')
    wrong_columns = [column for column in query.columns if len(column.split('.')) != 2]
    if query.joins is not None:
        wrong_columns += [conditional for conditional in array([join.conditionals for join in query.joins]).flatten() if
                          len(conditional.column.split('.')) != 2 or len(conditional.foreign_column.split('.')) != 2]
    if query.filters is not None:
        wrong_columns += [filter.column for filter in query.filters if
                          filter.type == FilterType.WHERE and len(filter.column.split('.')) != 2]
    if query.orders is not None:
        wrong_columns += [order.column for order in query.orders if len(order.column.split('.')) != 2]
    if len(wrong_columns) > 0:
        raise MalformedError(f'Failed to map subset: column(s) are not in table.column notion: {wrong_columns}')
    if len(query.datasources) < 1:
        raise MalformedError(f'Failed to map subset: no datasource(s) selected')
    alias_columns = [key for (key, group) in groupby(query.columns, lambda x: x) if len(list(group)) > 1]
    # columns
    select_columns = [SubsetColumn(id=column.id) for column in array(
        [table.columns for table in database.tables if
         table.internal_name in [c.split('.')[0] for c in query.columns]]).flatten() if
                      column.internal_name in [c.split('.')[1] for c in query.columns]]
    select_columns += [SubsetColumn(id=column.id) for column in array(
        [view.columns for view in database.views if
         view.internal_name in [c.split('.')[0] for c in query.columns]]).flatten() if
                       column.internal_name in [c.split('.')[1] for c in query.columns]]
    if len(select_columns) != len(query.columns):
        raise MalformedError(
            f'Failed to map subset: column(s) not found in database {len(select_columns)} != {len(query.columns)}')
    # datasources
    datasource_ids = [table.id for table in database.tables if table.internal_name in query.datasources]
    datasource_ids += [view.id for view in database.views if view.internal_name in query.datasources]
    if len(datasource_ids) != len(query.datasources):
        raise MalformedError(f'Failed to map subset: datasource(s) not found in database')
    # joins
    joins = []
    if query.joins is not None:
        for join in query.joins:
            # datasource_id
            datasource_id = [table.id for table in database.tables if table.internal_name == join.datasource]
            datasource_id += [view.id for view in database.views if view.internal_name == join.datasource]
            if len(datasource_id) != 1:
                raise MalformedError(f'Failed to map subset: join datasource {join.datasource} not found in database')
            # conditionals
            conditionals = []
            for conditional in join.conditionals:
                # column_id
                column_id = [column.id for column in array(
                    [table.columns for table in database.tables if table.internal_name == conditional.column.split('.')[
                        0]]).flatten() if column.internal_name == conditional.column.split('.')[1]]
                column_id += [column.id for column in array(
                    [view.columns for view in database.views if view.internal_name == conditional.column.split('.')[
                        0]]).flatten() if column.internal_name == conditional.column.split('.')[1]]
                if len(column_id) != 1:
                    raise MalformedError(
                        f'Failed to map subset: conditional column {conditional.column} not found in database')
                # foreign_column_id
                foreign_column_id = [column.id for column in array(
                    [table.columns for table in database.tables if table.internal_name == conditional.foreign_column.split('.')[
                        0]]).flatten() if column.internal_name == conditional.foreign_column.split('.')[1]]
                foreign_column_id += [column.id for column in array(
                    [view.columns for view in database.views if view.internal_name == conditional.foreign_column.split('.')[
                        0]]).flatten() if column.internal_name == conditional.foreign_column.split('.')[1]]
                if len(foreign_column_id) != 1:
                    raise MalformedError(
                        f'Failed to map subset: conditional column {conditional.foreign_column} not found in database')
                conditionals.append(Conditional(column_id=column_id[0], foreign_column_id=foreign_column_id[0]))
            joins.append(Join(type=join.type, datasource_id=datasource_id[0], conditionals=conditionals))
    # filters
    filters = []
    if query.filters is not None:
        for filter in query.filters:
            if filter.type != FilterType.WHERE:
                filters.append(Filter(type=filter.type))
                continue
            # column_id
            filter_column_ids = [column.id for column in array(
                [table.columns for table in database.tables if
                 table.internal_name == filter.column.split('.')[0]]).flatten()
                                 if column.internal_name == filter.column.split('.')[1]]
            filter_column_ids += [column.id for column in array(
                [view.columns for view in database.views if
                 view.internal_name == filter.column.split('.')[0]]).flatten()
                                  if column.internal_name == filter.column.split('.')[1]]
            if len(filter_column_ids) != 1:
                raise MalformedError(
                    f'Failed to map subset: filtered column name {filter.column} not found in database: {database.internal_name}')
            # operator_id
            filter_ops_ids: List[str] = [op.id for op in image.operators if op.value == filter.operator]
            if len(filter_ops_ids) != 1:
                raise MalformedError(f'Failed to map subset: filter operator {filter.operator} not found in image')
            filters.append(Filter(type=filter.type,
                                  column_id=filter_column_ids[0],
                                  operator_id=filter_ops_ids[0],
                                  value=filter.value))
    orders = []
    if query.orders is not None:
        for order in query.orders:
            # column_id
            order_column_ids = [column.id for column in array(
                [table.columns for table in database.tables if
                 table.internal_name == order.column.split('.')[0]]).flatten()
                                if column.internal_name == order.column.split('.')[1]]
            order_column_ids += [column.id for column in array(
                [view.columns for view in database.views if
                 view.internal_name == order.column.split('.')[0]]).flatten()
                                 if column.internal_name == order.column.split('.')[1]]
            if len(order_column_ids) != 1:
                raise MalformedError(
                    f'Failed to map subset: order column name {order.column} not found in database: {database.internal_name}')
            orders.append(Order(column_id=order_column_ids[0], direction=order.direction))
    return Subset(columns=select_columns, datasource_ids=datasource_ids, joins=joins, filters=filters, orders=orders)


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

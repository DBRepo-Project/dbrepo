from dbrepo.api.dto import Subset, QueryDefinition, Database, Table, Image, Filter, Order
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
    order = None
    if query.order is not None:
        for order in query.order:
            # column_id
            order_column_ids: [str] = [column.id for column in tables[0].columns if
                                       column.internal_name == order.column]
            if len(order_column_ids) != 1:
                raise MalformedError(f'Failed to create view: order column name not found in database')
            order.append(Order(column_id=order_column_ids[0].id, direction=order.direction))
    return Subset(table_id=tables[0].id, columns=filtered_column_ids, filter=filters, order=order)

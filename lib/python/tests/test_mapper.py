import unittest

from dbrepo.api.dto import QueryDefinition, JoinDefinition, JoinType, ConditionalDefinition, FilterDefinition, \
    FilterType, OrderType, OrderDefinition, Database, UserBrief, Table, Constraints, PrimaryKey, TableBrief, \
    ColumnBrief, ColumnType, Column, ContainerBrief, ImageBrief, Image, Operator, DataType, SubsetColumn, Join, \
    Conditional, Filter, Order
from dbrepo.api.exceptions import MalformedError
from dbrepo.api.mapper import query_to_subset


class MapperUnitTest(unittest.TestCase):
    database = Database(
        id="6bd39359-b154-456d-b9c2-caa516a45732",
        name='test',
        owner=UserBrief(id='abdbf897-e599-4e5a-a3f0-7529884ea011', username='mweise'),
        contact=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'),
        exchange_name='dbrepo',
        internal_name='test_abcd',
        is_public=True,
        is_schema_public=True,
        is_dashboard_enabled=True,
        tables=[Table(id="029d773f-f98b-40c0-ab22-b8b1635d4fbc",
                      name="Some Table",
                      description="Test Table",
                      database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                      internal_name="some_table",
                      owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'),
                      is_versioned=True,
                      queue_name='dbrepo',
                      routing_key='dbrepo.test_database_1234.some_table',
                      is_public=True,
                      is_schema_public=True,
                      constraints=Constraints(uniques=[],
                                              foreign_keys=[],
                                              checks=[],
                                              primary_key=[PrimaryKey(id="1516310f-ecb5-4614-abe2-3b96114e1484",
                                                                      table=TableBrief(
                                                                          id="029d773f-f98b-40c0-ab22-b8b1635d4fbc",
                                                                          database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                                          name='Some Table',
                                                                          internal_name='some_table',
                                                                          is_versioned=True,
                                                                          is_public=True,
                                                                          is_schema_public=True,
                                                                          owned_by='8638c043-5145-4be8-a3e4-4b79991b0a16'),
                                                                      column=ColumnBrief(
                                                                          id="31a533b6-8ddf-43d6-ac6a-b9da597cb976",
                                                                          table_id="029d773f-f98b-40c0-ab22-b8b1635d4fbc",
                                                                          database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                                          name='id',
                                                                          alias=None,
                                                                          internal_name='id',
                                                                          type=ColumnType.BIGINT))]),
                      columns=[Column(id="31a533b6-8ddf-43d6-ac6a-b9da597cb976",
                                      name="ID",
                                      ord=0,
                                      database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                      table_id="029d773f-f98b-40c0-ab22-b8b1635d4fbc",
                                      internal_name="id",
                                      type=ColumnType.BIGINT,
                                      is_null_allowed=False),
                               Column(id="85de93a8-834c-4cf4-9d34-f80ebd97e606",
                                      name="Username",
                                      ord=1,
                                      database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                      table_id="029d773f-f98b-40c0-ab22-b8b1635d4fbc",
                                      internal_name="username",
                                      type=ColumnType.VARCHAR,
                                      is_null_allowed=False)
                               ]),
                Table(id="585d421a-ad1a-4543-b661-5e32a78dd3e1",
                      name="Other Table",
                      database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                      internal_name="other_table",
                      owner=UserBrief(id='8638c043-5145-4be8-a3e4-4b79991b0a16', username='mweise'),
                      is_versioned=True,
                      queue_name='dbrepo',
                      routing_key='dbrepo.test_database_1234.other_table',
                      is_public=True,
                      is_schema_public=True,
                      constraints=Constraints(uniques=[],
                                              foreign_keys=[],
                                              checks=[],
                                              primary_key=[PrimaryKey(id="3b060596-38f9-4055-8fb0-5526918f31a0",
                                                                      table=TableBrief(
                                                                          id="585d421a-ad1a-4543-b661-5e32a78dd3e1",
                                                                          database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                                          name='Other Table',
                                                                          internal_name='other_table',
                                                                          is_versioned=True,
                                                                          is_public=True,
                                                                          is_schema_public=True,
                                                                          owned_by='8638c043-5145-4be8-a3e4-4b79991b0a16'),
                                                                      column=ColumnBrief(
                                                                          id="4a2f20c3-9efd-4788-a38d-3079d198125c",
                                                                          table_id="585d421a-ad1a-4543-b661-5e32a78dd3e1",
                                                                          database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                                                          name='id',
                                                                          alias=None,
                                                                          internal_name='id',
                                                                          type=ColumnType.BIGINT))]),
                      columns=[Column(id="4a2f20c3-9efd-4788-a38d-3079d198125c",
                                      name="ID",
                                      ord=0,
                                      database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                      table_id="585d421a-ad1a-4543-b661-5e32a78dd3e1",
                                      internal_name="id",
                                      type=ColumnType.BIGINT,
                                      is_null_allowed=False),
                               Column(id="85de93a8-834c-4cf4-9d34-f80ebd97e606",
                                      name="City",
                                      ord=1,
                                      database_id="6bd39359-b154-456d-b9c2-caa516a45732",
                                      table_id="585d421a-ad1a-4543-b661-5e32a78dd3e1",
                                      internal_name="city",
                                      type=ColumnType.VARCHAR,
                                      is_null_allowed=False)
                               ])
                ],
        container=ContainerBrief(id="44d811a8-4019-46ba-bd57-ea10a2eb0c74",
                                 name='MariaDB Galera 11.1.3',
                                 internal_name='mariadb',
                                 image=ImageBrief(id="b104648b-54d2-4d72-9834-8e0e6d428b39",
                                                  name='mariadb',
                                                  version='11.2.2',
                                                  default=True)))
    image = Image(id="b104648b-54d2-4d72-9834-8e0e6d428b39",
                  name="mariadb",
                  version="10.11.3",
                  default=True,
                  operators=[
                      Operator(id="0917b17e-5d68-4ddf-94f6-f178f74a0dae",
                               display_name=">",
                               value=">",
                               documentation="https://mariadb.com/kb/en/greater-than/"),
                      Operator(id="e21b7b74-e7a3-4cde-9e57-b313f59c0c16",
                               display_name="<",
                               value="<",
                               documentation="https://mariadb.com/kb/en/less-than/")],
                  data_types=[
                      DataType(id="22975809-5496-4d67-9fd4-6689f0030f82",
                               display_name="SERIAL",
                               value="serial",
                               documentation="https://mariadb.com/kb/en/bigint/",
                               is_quoted=False,
                               is_buildable=True)])
    query = QueryDefinition(datasources=["some_table"],
                            columns=["other_table.city", "some_table.username"],
                            joins=[JoinDefinition(type=JoinType.INNER,
                                                  datasource="other_table",
                                                  conditionals=[ConditionalDefinition(
                                                      column="other_table.id",
                                                      foreign_column="some_table.id")])],
                            filters=[FilterDefinition(type=FilterType.WHERE,
                                                      column="some_table.id",
                                                      operator=">",
                                                      value="10"),
                                     FilterDefinition(type=FilterType.AND),
                                     FilterDefinition(type=FilterType.WHERE,
                                                      column="some_table.id",
                                                      operator="<",
                                                      value="20")],
                            orders=[OrderDefinition(column="some_table.id", direction=OrderType.DESC)])

    def test_query_to_subset_succeeds(self):
        # test
        response = query_to_subset(self.database, self.image, self.query)
        self.assertEqual([SubsetColumn(id='85de93a8-834c-4cf4-9d34-f80ebd97e606'),
                          SubsetColumn(id='85de93a8-834c-4cf4-9d34-f80ebd97e606')], response.columns)
        self.assertEqual(['029d773f-f98b-40c0-ab22-b8b1635d4fbc'], response.datasource_ids)
        self.assertEqual([Join(type=JoinType.INNER, datasource_id='585d421a-ad1a-4543-b661-5e32a78dd3e1',
                               conditionals=[Conditional(column_id='4a2f20c3-9efd-4788-a38d-3079d198125c',
                                                         foreign_column_id='31a533b6-8ddf-43d6-ac6a-b9da597cb976')])],
                         response.joins)
        self.assertEqual([Filter(type=FilterType.WHERE, column_id='31a533b6-8ddf-43d6-ac6a-b9da597cb976',
                                 operator_id='0917b17e-5d68-4ddf-94f6-f178f74a0dae', value='10'),
                          Filter(type=FilterType.AND),
                          Filter(type=FilterType.WHERE, column_id='31a533b6-8ddf-43d6-ac6a-b9da597cb976',
                                 operator_id='e21b7b74-e7a3-4cde-9e57-b313f59c0c16', value='20')], response.filters)
        self.assertEqual([Order(column_id='31a533b6-8ddf-43d6-ac6a-b9da597cb976', direction=OrderType.DESC)],
                         response.orders)

    def test_query_to_subset_column_malformed_fails(self):
        query = QueryDefinition(datasources=["other_table"],
                                columns=["other_table.city", "id"])  # <<<
        # test
        try:
            query_to_subset(self.database, self.image, query)
        except MalformedError:
            pass

    def test_query_to_subset_datasource_fails(self):
        query = QueryDefinition(datasources=["i_do_not_exist"],  # <<<
                                columns=["i_do_not_exist.city", "i_do_not_exist.id"])
        # test
        try:
            query_to_subset(self.database, self.image, query)
        except MalformedError:
            pass

    def test_query_to_subset_datasources_fails(self):
        query = QueryDefinition(datasources=["other_table", "i_do_not_exist"],  # <<<
                                columns=["other_table.city", "other_table.id"])
        # test
        try:
            query_to_subset(self.database, self.image, query)
        except MalformedError:
            pass

    def test_query_to_subset_join_datasource_fails(self):
        query = QueryDefinition(datasources=["some_table"],
                                columns=["other_table.city", "some_table.username"],
                                joins=[JoinDefinition(type=JoinType.INNER,
                                                      datasource="i_do_not_exist",  # <<<
                                                      conditionals=[ConditionalDefinition(
                                                          column="other_table.id",
                                                          foreign_column="some_table.id")])])
        # test
        try:
            query_to_subset(self.database, self.image, query)
        except MalformedError:
            pass

    def test_query_to_subset_join_conditional_column_fails(self):
        query = QueryDefinition(datasources=["some_table"],
                                columns=["other_table.city", "some_table.username"],
                                joins=[JoinDefinition(type=JoinType.INNER,
                                                      datasource="other_table",
                                                      conditionals=[ConditionalDefinition(
                                                          column="i_do_not_exist.id",  # <<<
                                                          foreign_column="some_table.id")])])
        # test
        try:
            query_to_subset(self.database, self.image, query)
        except MalformedError:
            pass

    def test_query_to_subset_join_conditional_column_malformed_fails(self):
        query = QueryDefinition(datasources=["some_table"],
                                columns=["other_table.city", "some_table.username"],
                                joins=[JoinDefinition(type=JoinType.INNER,
                                                      datasource="other_table",
                                                      conditionals=[ConditionalDefinition(
                                                          column="id",  # <<<
                                                          foreign_column="some_table.id")])])
        # test
        try:
            query_to_subset(self.database, self.image, query)
        except MalformedError:
            pass

    def test_query_to_subset_join_conditional_foreign_column_fails(self):
        query = QueryDefinition(datasources=["some_table"],
                                columns=["other_table.city", "some_table.username"],
                                joins=[JoinDefinition(type=JoinType.INNER,
                                                      datasource="other_table",
                                                      conditionals=[ConditionalDefinition(
                                                          column="other_table.id",
                                                          foreign_column="i_do_not_exist.id")])])  # <<<
        # test
        try:
            query_to_subset(self.database, self.image, query)
        except MalformedError:
            pass

    def test_query_to_subset_join_conditional_foreign_column_malformed_fails(self):
        query = QueryDefinition(datasources=["some_table"],
                                columns=["other_table.city", "some_table.username"],
                                joins=[JoinDefinition(type=JoinType.INNER,
                                                      datasource="other_table",
                                                      conditionals=[ConditionalDefinition(
                                                          column="other_table.id",
                                                          foreign_column="id")])])  # <<<
        # test
        try:
            query_to_subset(self.database, self.image, query)
        except MalformedError:
            pass

    def test_query_to_subset_join_filter_column_fails(self):
        query = QueryDefinition(datasources=["some_table"],
                                columns=["other_table.city", "some_table.username"],
                                joins=[JoinDefinition(type=JoinType.INNER,
                                                      datasource="other_table",
                                                      conditionals=[ConditionalDefinition(
                                                          column="other_table.id",
                                                          foreign_column="some_table.id")])],
                                filters=[FilterDefinition(type=FilterType.WHERE,
                                                          column="i_do_not_exist.id",  # <<<
                                                          operator=">",
                                                          value="10")])
        # test
        try:
            query_to_subset(self.database, self.image, query)
        except MalformedError:
            pass

    def test_query_to_subset_join_filter_column_malformed_fails(self):
        query = QueryDefinition(datasources=["some_table"],
                                columns=["other_table.city", "some_table.username"],
                                joins=[JoinDefinition(type=JoinType.INNER,
                                                      datasource="other_table",
                                                      conditionals=[ConditionalDefinition(
                                                          column="other_table.id",
                                                          foreign_column="some_table.id")])],
                                filters=[FilterDefinition(type=FilterType.WHERE,
                                                          column="id",  # <<<
                                                          operator=">",
                                                          value="10")])
        # test
        try:
            query_to_subset(self.database, self.image, query)
        except MalformedError:
            pass

    def test_query_to_subset_join_filter_operator_fails(self):
        query = QueryDefinition(datasources=["some_table"],
                                columns=["other_table.city", "some_table.username"],
                                joins=[JoinDefinition(type=JoinType.INNER,
                                                      datasource="other_table",
                                                      conditionals=[ConditionalDefinition(
                                                          column="other_table.id",
                                                          foreign_column="some_table.id")])],
                                filters=[FilterDefinition(type=FilterType.WHERE,
                                                          column="other_table.id",
                                                          operator="=",  # <<<
                                                          value="10")])
        # test
        try:
            query_to_subset(self.database, self.image, query)
        except MalformedError:
            pass

    def test_query_to_subset_join_filter_type_fails(self):
        query = QueryDefinition(datasources=["some_table"],
                                columns=["other_table.city", "some_table.username"],
                                joins=[JoinDefinition(type=JoinType.INNER,
                                                      datasource="other_table",
                                                      conditionals=[ConditionalDefinition(
                                                          column="other_table.id",
                                                          foreign_column="some_table.id")])],
                                filters=[FilterDefinition(type=FilterType.WHERE,
                                                          column="other_table.id",
                                                          operator="<",
                                                          value="10"),
                                         FilterDefinition(type=FilterType.AND)])  # <<<
        # test
        try:
            query_to_subset(self.database, self.image, query)
        except MalformedError:
            pass

    def test_query_to_subset_order_column_fails(self):
        query = QueryDefinition(datasources=["some_table"],
                                columns=["some_table.username"],
                                orders=[OrderDefinition(column="some_table.id")])  # <<<
        # test
        try:
            query_to_subset(self.database, self.image, query)
        except MalformedError:
            pass

    def test_query_to_subset_order_column_malformed_fails(self):
        query = QueryDefinition(datasources=["some_table"],
                                columns=["some_table.username"],
                                orders=[OrderDefinition(column="id")])  # <<<
        # test
        try:
            query_to_subset(self.database, self.image, query)
        except MalformedError:
            pass

#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Mon Jan 17 17:13:25 2024

@author: Sotiris Tsepelakis
"""

from opensearchpy import OpenSearch
from sqlalchemy import create_engine

from determine_stats import determine_stats


class TestDetermineStatisticalProperties:
    # @Test
    def test_determine_statistical_properties_succeeds(self, all_containers):
        # mock request
        payload = {
            "database_id": 1,
            "table_id": 1,
            "data_db_host": "localhost",
            "data_db_port": 33061,
        }

        os_host = all_containers[0].get_container_host_ip()
        os_port = all_containers[0].get_exposed_port("9200/tcp")
        os = OpenSearch([{"host": os_host, "port": os_port}])

        metadata_db_host = all_containers[1].get_container_host_ip()
        metadata_db_port = all_containers[1].get_exposed_port(3306)
        db = create_engine(
            f"mysql+pymysql://root:dbrepo@{metadata_db_host}:{metadata_db_port}/fda"
        )

        # index a test document
        all_containers[0].get_client().index(
            index="database",
            id="1",
            body={
                "id": 1,
                "name": "testdb",
                "exchange_name": "dbrepo",
                "internal_name": "testdb_ygvq",
                "tables": [
                    {
                        "id": 1,
                        "database_id": 1,
                        "name": "mytable",
                        "internal_name": "mytable",
                        "queue_name": "dbrepo",
                        "routing_key": "dbrepo.testdb_ygvq.mytable",
                        "columns": [
                            {
                                "id": 1,
                                "database_id": 1,
                                "table_id": 1,
                                "name": "id",
                                "internal_name": "id",
                                "auto_generated": True,
                                "is_primary_key": True,
                                "column_type": "BIGINT",
                                "is_public": True,
                                "is_null_allowed": False,
                                "enums": [],
                                "sets": [],
                            },
                            {
                                "id": 2,
                                "database_id": 1,
                                "table_id": 1,
                                "name": "col1",
                                "internal_name": "col1",
                                "date_format": {
                                    "id": 2,
                                    "database_format": "%Y-%c-%d %H:%i:%S",
                                    "unix_format": "yyyy-MM-dd HH:mm:ss",
                                    "has_time": True,
                                    "created_at": "2024-01-17T13:22:26.000Z",
                                },
                                "auto_generated": False,
                                "is_primary_key": False,
                                "column_type": "DATETIME",
                                "is_public": True,
                                "is_null_allowed": True,
                                "enums": [],
                                "sets": [],
                            },
                            {
                                "id": 3,
                                "database_id": 1,
                                "table_id": 1,
                                "name": "col2",
                                "internal_name": "col2",
                                "auto_generated": False,
                                "is_primary_key": False,
                                "column_type": "VARCHAR",
                                "size": 255,
                                "is_public": True,
                                "is_null_allowed": True,
                                "enums": [],
                                "sets": [],
                            },
                            {
                                "id": 4,
                                "database_id": 1,
                                "table_id": 1,
                                "name": "col3",
                                "internal_name": "col3",
                                "auto_generated": True,
                                "is_primary_key": True,
                                "column_type": "FLOAT",
                                "size": 24,
                                "is_public": True,
                                "is_null_allowed": True,
                                "enums": [],
                                "sets": [],
                            },
                            {
                                "id": 5,
                                "database_id": 1,
                                "table_id": 1,
                                "name": "col4",
                                "internal_name": "col4",
                                "auto_generated": False,
                                "is_primary_key": False,
                                "column_type": "FLOAT",
                                "size": 24,
                                "is_public": True,
                                "is_null_allowed": True,
                                "enums": [],
                                "sets": [],
                            },
                            {
                                "id": 6,
                                "database_id": 1,
                                "table_id": 1,
                                "name": "col5",
                                "internal_name": "col5",
                                "auto_generated": False,
                                "is_primary_key": False,
                                "column_type": "INT",
                                "size": 255,
                                "is_public": True,
                                "is_null_allowed": True,
                                "enums": [],
                                "sets": [],
                            },
                        ],
                        "constraints": {"uniques": [], "foreign_keys": []},
                    }
                ],
            },
        )
        # test
        response = determine_stats(db, os, **payload)
        assert response

#!/bin/env python3
from api_query.api.query_endpoint_api import QueryEndpointApi

query = QueryEndpointApi()


def create_query(container_id, database_id, statement, page=0, size=3):
    response = query.execute({
        "statement": statement
    }, container_id, database_id, page=page, size=size)
    print("executed query with id %d" % response.id)
    return response

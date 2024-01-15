# -*- coding: utf-8 -*-
"""
This file defines the endpoints for the dbrepo-search-service.
"""
import os
from ast import literal_eval

from flask import request

from app.api import api_bp
from flasgger.utils import swag_from
from app.opensearch_client import *
import math

available_types = literal_eval(
    os.getenv("COLLECTION", "['database','table','column','identifier','unit','concept','user','view']"))

logging.info(f"Available collection loaded as: {available_types}")


def general_filter(index, results):
    """
    Applies filtering to the result of opensearch queries.

    we only want to return specific entries of the result dict to the user, depending on the queried index.
    the keys for the entries per index that shouldn't be deleted are specified in the important_keys dict.

    :param index: the search index the query results are about
    :param results: the raw response of the query_index_by_term_opensearch function.
    :return:
    """
    important_keys = {
        "column": ["id", "name", "column_type"],
        "table": ["id", "name", "description"],
        "identifier": ["id", "title", "type"],
        "user": ["id", "username"],
        "database": ["id", "name", "is_public", "details"],
        "concept": ["uri", "name"],
        "unit": [],
        "view": ["id", "name", "creator", " created"],
    }
    if index not in important_keys.keys():
        error_msg = "the keys to be returned to the user for your index aren't specified in the important Keys dict"
        raise KeyError(error_msg)
    for result in results:
        result_keys_copy = tuple(result.keys())
        for key in result_keys_copy:
            if key not in important_keys[index]:
                del result[key]
    logging.debug('general filter results: %s', results)
    return results


@api_bp.route("/health", methods=["GET"], endpoint="actuator_health")
@swag_from("us-yml/get_health")  # ToDo: get the SWAG right
def health():
    return {"status": "UP"}


@api_bp.route("/api/search/<string:index>", methods=["GET"], endpoint="search_get_index")
@swag_from("us-yml")  # ToDo: get the SWAG right
def get_index(index):
    """
    returns all entries in a specific index
    :param index: desired index
    :return: list of the results
    """
    logging.info(f'Searching for index: {index}')
    if index not in available_types:
        return {
            "results": {},
        }, 404  # ToDo: replace with better error handling
    results = query_index_by_term_opensearch("*", "contains")
    results = general_filter(index, results)

    results_per_page = min(request.args.get("results_per_page", 50, type=int), 500)
    max_pages = math.ceil(len(results) / results_per_page)
    page = min(request.args.get("page", 1, type=int), max_pages)
    results = results[(results_per_page * (page - 1)): (results_per_page * page)]
    return {"results": results}, 200


@api_bp.route("/api/search/<string:type>/fields", methods=["GET"], endpoint="search_get_index_fields")
def get_fields(type):
    """
    returns a list of attributes of the data for a specific index.
    :param type: The search type
    :return:
    """
    logging.info(f'Searching in index database for type: {type}')
    if type not in available_types:
        return {
            "results": {},
        }, 404
    fields = get_fields_for_index(type)
    logging.debug(f'get fields for type {type} resulted in {len(fields)} field(s)')
    return fields, 200


@api_bp.route("/api/search", methods=["POST"], endpoint="search_fuzzy_search")
def post_fuzzy_search():
    """
    Main endpoint for fuzzy searching.
    :return:
    """
    if request.content_type != "application/json":
        return {
            "message": "Unsupported Media Type",
            "suggested_content_types": ["application/json"],
        }, 415
    req_body = request.json
    logging.debug(f"search request body: {req_body}")
    search_term = req_body.get("search_term")
    results = general_search(None, search_term, None, None, None)
    if "hits" in results and "hits" in results["hits"]:
        results = [hit["_source"] for hit in results["hits"]["hits"]]
    return {"results": results}, 200


@api_bp.route("/api/search/<string:type>", methods=["POST"], endpoint="search_general_search")
def post_general_search(type):
    """
    Main endpoint for fuzzy searching.
    :return:
    """
    if request.content_type != "application/json":
        return {
            "message": "Unsupported Media Type",
            "suggested_content_types": ["application/json"],
        }, 415
    req_body = request.json
    logging.info(f'Searching in index database for type: {type}')
    logging.debug(f"search request body: {req_body}")
    search_term = req_body.get("search_term")
    if type is not None and type not in available_types:
        logging.error(f"Type {type} is not in collection: {available_types}")
        return {
            "results": {},
        }, 404
    t1 = req_body.get("t1")
    if not str(t1).isdigit():
        t1 = None
    t2 = req_body.get("t2")
    if not str(t2).isdigit():
        t2 = None
    field_value_pairs = req_body.get("field_value_pairs")
    if t1 is not None and t2 is not None and "unit.uri" in field_value_pairs and "concept.uri" in field_value_pairs:
        response = unit_independent_search(t1, t2, field_value_pairs)
    else:
        response = general_search(type, search_term, t1, t2, field_value_pairs)
    # filter by type
    if type == 'table':
        tmp = []
        for database in response:
            if database["tables"] is not None:
                for table in database["tables"]:
                    table["is_public"] = database["is_public"]
                    tmp.append(table)
        response = tmp
    if type == 'identifier':
        tmp = []
        for database in response:
            if database["identifiers"] is not None:
                for identifier in database['identifiers']:
                    tmp.append(identifier)
            if database["subsets"] is not None:
                for identifier in database['subsets']:
                    tmp.append(identifier)
            if database["tables"] is not None:
                for table in database['tables']:
                    if database["identifiers"] is not None:
                        for identifier in table['identifiers']:
                            tmp.append(identifier)
        for view in [x for xs in response for x in xs["views"]]:
            if 'identifier' in view:
                tmp.append(view['identifier'])
        response = tmp
    elif type == 'column':
        response = [x for xs in response for x in xs["tables"]]
        for table in response:
            for column in table["columns"]:
                column["table_id"] = table["id"]
                column["database_id"] = table["database_id"]
        response = [x for xs in response for x in xs["columns"]]
    elif type == 'concept':
        tmp = []
        tables = [x for xs in response for x in xs["tables"]]
        for column in [x for xs in tables for x in xs["columns"]]:
            if 'concept' in column and column["concept"] is not None:
                tmp.append(column["concept"])
        response = tmp
    elif type == 'unit':
        tmp = []
        tables = [x for xs in response for x in xs["tables"]]
        for column in [x for xs in tables for x in xs["columns"]]:
            if 'unit' in column and column["unit"] is not None:
                tmp.append(column["unit"])
        response = tmp
    elif type == 'view':
        response = [x for xs in response for x in xs["views"]]
    return {'results': response, 'type': type}, 200

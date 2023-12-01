# -*- coding: utf-8 -*-
"""
This file defines the endpoints for the dbrepo-search-service.
"""
import os
from ast import literal_eval

from flask import request

# ToDo: make import recognisable by PyCharm
from app.api import api_bp
from flasgger.utils import swag_from
from app.opensearch_client import *
import math

available_indices = literal_eval(
    os.getenv("COLLECTION", "['database','table','column','identifier','unit','concept','user','view']"))

logging.info(f"Available collection loaded as: {available_indices}")


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
    if index not in available_indices:
        return {
            "results": {},
        }, 404  # ToDo: replace with better error handling
    results = query_index_by_term_opensearch(index, "*", "contains")
    results = general_filter(index, results)
    total_number_of_results = len(results)

    results_per_page = min(request.args.get("results_per_page", 50, type=int), 500)
    max_pages = math.ceil(len(results) / results_per_page)
    page = min(request.args.get("page", 1, type=int), max_pages)
    results = results[(results_per_page * (page - 1)): (results_per_page * page)]
    return {"results": results, "total": total_number_of_results, "status": 200}


@api_bp.route("/api/search/<string:index>/fields", methods=["GET"], endpoint="search_get_index_fields")
def get_fields(index):
    """
    returns a list of attributes of the data for a specific index.
    :param index:
    :return:
    """
    logging.info(f'Searching for index: {index}')
    if index not in available_indices:
        return {
            "results": {},
        }, 404
    fields = get_fields_for_index(index)
    logging.debug(f'get fields for index {index} resulted in {len(fields)} field(s)')
    return {"fields": fields, "status": 200}


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
    response = general_search(None, available_indices, search_term, None, None, None)
    return response, 200


@api_bp.route("/api/search/<string:index>", methods=["POST"], endpoint="search_general_search")
def post_general_search(index):
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
    logging.info(f'Searching for index: {index}')
    logging.debug(f"search request body: {req_body}")
    search_term = req_body.get("search_term")
    if index is not None and index not in available_indices:
        logging.error(f"Index {index} is not in list of searchable indices: {available_indices}")
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
        response = general_search(index, available_indices, search_term, t1, t2, field_value_pairs)
    return response, 200

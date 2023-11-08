# -*- coding: utf-8 -*-
"""
This file defines the endpoints for the dbrepo-search-service.
"""
import logging

from flask import request

# ToDo: make import recognisable by PyCharm
from app.api import api_bp
from flasgger.utils import swag_from
from app.opensearch_client import *
import math
from opensearchpy import OpenSearch

host = "localhost"
port = 9200
auth = ("admin", "admin")
client = OpenSearch(
    hosts=[{"host": host, "port": port}],
    http_compress=True,  # enables gzip compression for request bodies
    http_auth=auth,
)


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
    logging.info('Searching for index: %s', index)
    available_indices = [
        "table",
        "user",
        "database",
        "column",
        "identifier",
        "concept",
        "unit",
        "view",
    ]
    if index not in available_indices:
        return {
            "results": {},
            "status": 404,
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
    logging.info('Getting fields for index: %s', index)
    available_indices = [
        "table",
        "user",
        "database",
        "column",
        "identifier",
        "concept",
        "unit",
        "view",
    ]
    if index not in available_indices:
        return {
            "results": {},
            "status": 404,
        }, 404  # ToDo: replace with better error handling
    fields = []
    fields = get_fields_for_index(index)
    logging.debug('get fields for index %s resulted in fields: %s', index, fields)
    return {"fields": fields, "status": 200}


@api_bp.route("/api/search", methods=["POST"], endpoint="search_fuzzy_search")
def search():
    """
    Main endpoint for general searching.

    There are three ways of  searching:
    *  if you specify 'search_term' in the request json, all entries that have relevant fields matching the 'search_term' are returned.
         No wildcards are allowed, although fuzzy search is enabled (meaning, there are also matches when 1 or two characters differ)
    * if you specify 't1' and/or 't2' entries that are newer than timestamp 't1' and entries that are younger than timestamp 't2' are returned.
        the timestamp has to have the format YYYY-MM-DD
    * if 'field' and 'value' are specified, only entries where the 'field' matches the 'value' are returned.
        For example, if  the 'field' is 'creator.orcid' and the 'value' is '0000-0002-6778-0887',
        only entries created by the person with this specific orcid id are returned.
    If there are multiple parameters specified, they are combined via an AND-conjunction, so you can e.g. search for entries that match a certain keyword,
    were created in a certain time period, by a specific person.
    :return:
    """
    if request.content_type != "application/json":
        return {
            "status": 415,
            "message": "Unsupported Media Type",
            "suggested_content_types": ["application/json"],
        }, 415
    req_body = request.json
    logging.debug('search request body: %s', req_body)
    search_term = req_body.get("search_term")
    t1 = req_body.get("t1")
    t2 = req_body.get("t2")
    field = req_body.get("field")
    value = req_body.get("value")
    fieldValuePairs = req_body.get("fieldValuePairs")
    response = general_search(search_term, t1, t2, fieldValuePairs)
    return response, 200

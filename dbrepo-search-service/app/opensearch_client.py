"""
The opensearch_client.py is used by the different API endpoints in routes.py to handle requests  to the opensearch db
"""
import logging
import re
from flask import current_app
from collections.abc import MutableMapping


def flatten_dict(
        d: MutableMapping, parent_key: str = "", sep: str = "."
) -> MutableMapping:
    items = []
    for k, v in d.items():
        new_key = parent_key + sep + k if parent_key else k
        if isinstance(v, MutableMapping):
            items.extend(flatten_dict(v, new_key, sep=sep).items())
        else:
            items.append((new_key, v))
    return dict(items)


def create_friendly_name(attribute_name):
    """
    replaces the attribute names so they are more human readable for the front end

    :todo: extend special_attribute_names
    :param attribute_name:
    :return:
    """
    special_attribute_names = {
        "creator.properties.username": "Username (creator)",
        "owner.properties.username": "Username (owner)",
        "owner.properties.id": "Id (owner)",
        "creator.properties.id": "Id (creator)",
    }
    if attribute_name not in special_attribute_names:
        friendly_name = attribute_name.split(".")[-1]
        friendly_name = friendly_name.replace("_", " ").strip()
        friendly_name = friendly_name.capitalize()
        return friendly_name
    else:
        return special_attribute_names[attribute_name]


def get_keys(d, parent_key=""):
    # currently not in use, probably obsolete?
    keys = []
    for key, value in d.items():
        new_key = f"{parent_key}.{key}" if parent_key else key
        if isinstance(value, dict):
            if "type" in value.keys():
                keys.append(
                    {
                        "attribute_name": new_key,
                        "data_type": value["type"],
                    }
                )
            else:
                keys.extend(get_keys(value, new_key))
    return keys


def query_index_by_term_opensearch(index, term, mode):
    """
    old code, is effectively replaced by general_search() now

    sends an opensearch query
    :return list of dicts
    """
    query_str = ""
    if mode == "exact":
        query_str = f"{term}"
    elif mode == "contains":
        query_str = f"*{term}*"

    response = current_app.opensearch_client.search(
        index=index,
        body={
            "query": {
                "query_string": {
                    "query": query_str,
                    "allow_leading_wildcard": "true",  # default true
                }
            },
        },
    )
    results = [hit["_source"] for hit in response["hits"]["hits"]]
    return results


def get_fields_for_index(index):
    """
    returns a list of attributes of the data for a specific index.
    :param index: the index of interest
    :return: list of fields
    """
    logging.debug('request fields for index: %s', index)
    fields = current_app.opensearch_client.indices.get_mapping(index)
    fields = fields[index]["mappings"]["properties"]
    logging.debug('fields: %s', fields)
    fields_list = []
    fd = flatten_dict(fields)
    for key in fd.keys():
        entry = {}
        if key.split(".")[-1] == "type":
            entry["attribute_name"] = ".".join(key.split(".")[:-1])
            entry["type"] = fd[key]
            fields_list.append(entry)
    return fields_list


def general_search(search_term=None, t1=None, t2=None, fieldValuePairs=None):
    """
    Main method for seaching stuff in the opensearch db

    all parameters are optional

    :param search_term: the term you want to search for (no wildcards are allowed)
    :param t1: beginn time period
    :param t2:  end time period
    :param field: name of the field you want to look at
    :param value: the value the specified field should match
    :return:
    """
    searchable_indices = ["database", "user", "table", "column", "identifier", "view", "concept", "unit"]
    index = searchable_indices
    field_list = [
        "table.name",
        "identifier.titles.title",
        "identifier.descriptions.description",
        "identifier.publisher",
        "identifier.creators.*.firstname",
        "identifier.creators.*.lastname",
        "identifier.creators.*.creator_name",
        "column.column_type",
        "column.is_null_allowed",
        "column.is_primary_key",
        "unit.uri",
        "unit.name",
        "unit.description",
        "concept.uri",
        "concept.name",
        "concept.description",
        "funders",
        "title",
        "description",
        "creator.username",
        "author",
        "name",
        "uri",
        "database.*",
        "internal_name",
        "is_public",
    ]
    queries = []
    if search_term is not None:
        logging.debug('query has search_term present')
        fuzzy_body = {
            "query": {
                "multi_match": {
                    "query": search_term,
                    "fuzziness": "AUTO",
                    "fuzzy_transpositions": True,
                    "minimum_should_match": 3
                }
            }
        }
        logging.debug('search body: %s', fuzzy_body)
        response = current_app.opensearch_client.search(
            index=index,
            body=fuzzy_body
        )
        response["status"] = 200
        return response
    if t1 is not None:
        logging.debug(f"query has start value {t1} present")
        time_range_query = {
            "range": {
                "created": {
                    "gte": t1,
                    "lte": t2,
                }
            }
        }
        queries.append(time_range_query)
    if t1 is not None and t2 is not None:
        logging.debug(f"query has start value {t1} and end value {t2} present")
        time_range_query = {
            "range": {
                "created": {
                    "gte": t1,
                    "lte": t2,
                }
            }
        }
        queries.append(time_range_query)
    if fieldValuePairs is not None and len(fieldValuePairs) > 0:
        logging.debug('query has fieldValuePairs present')
        musts = []
        for key, value in fieldValuePairs.items():
            if key == "type" and value in searchable_indices:
                logging.debug("search for specific index: %s", value)
                index = value
                continue
            if key in field_list:
                if re.match(f"{key}\\.", key):
                    new_field = key[key.index(".") + 1:len(key)]
                    logging.debug(
                        f"field name {key} starts with index name {index}: flattened field name to {new_field}")
                    key = new_field
                musts.append({
                    "match": {
                        key: {"query": value, "minimum_should_match": "90%"}
                    }
                })
        specific_query = {"bool": {"must": musts}}
        queries.append(specific_query)
    body = {
        "query": {"bool": {"must": queries}},
        "_source": [
            "_class",
            "id",
            "table_id",
            "database_id",
            "name",
            "identifier.*",
            "column_type",
            "description",
            "title",
            "type",
            "uri",
            "username",
            "is_public",
            "created",
            "_score",
            "concept",
            "unit",
            "author",
            "docID",
            "creator.*",
            "owner.*",
            "details.*",
        ],
    }
    logging.debug('search index: %s', index)
    logging.debug('search body: %s', body)
    response = current_app.opensearch_client.search(
        index=index,
        body=body
    )
    response["status"] = 200
    # response = [hit["_source"] for hit in response["hits"]["hits"]]
    return response

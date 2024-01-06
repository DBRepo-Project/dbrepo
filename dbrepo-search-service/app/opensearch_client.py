"""
The opensearch_client.py is used by the different API endpoints in routes.py to handle requests  to the opensearch db
"""
import json
import logging
import re
from flask import current_app
from collections.abc import MutableMapping

from omlib.measure import om
from omlib.constants import SI, OM_IDS
from omlib.omconstants import OM
from omlib.unit import Unit


def key_to_attr_name(key: str) -> str:
    """
    Maps an attribute key to a machine-readable representation
    :param key: The attribute key
    :return: The machine-readable representation of the attribute key
    """
    parts = []
    previous = None
    for part in key.split(".")[1:-1]:  # remove the first and last sub-item database.xxx.yyy.zzz.type -> xxx.yyy.zzz
        if part == "mappings" or part == "mapping":  # remove the mapping sub-item(s)
            continue
        if part == previous:  # remove redundant sub-item(s)
            continue
        previous = part
        parts.append(part)
    return ".".join(parts)


def attr_name_to_attr_friendly_name(key: str) -> str:
    """
    Maps an attribute key to a human-readable representation
    :param key: The attribute key
    :return: The human-readable representation of the attribute key
    """
    with open('friendly_names_overrides.json') as json_data:
        d = json.load(json_data)
        for json_key in d.keys():
            if json_key == key:
                logging.debug(f"friendly name exists for key {json_key}")
                return d[json_key]
    return ''.join(key.replace('_', ' ').title().split('.')[-1:])


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


def query_index_by_term_opensearch(term, mode):
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
        index="database",
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


def get_fields_for_index(type: str):
    """
    returns a list of attributes of the data for a specific index.
    :param type: The search type
    :return: list of fields
    """
    fields = {
        "database": "*",
        "table": "tables.*",
        "column": "tables.columns.*",
        "concept": "tables.columns.concept.*",
        "unit": "tables.columns.unit.*",
        "identifier": "identifier.*",
        "view": "views.*",
        "user": "creator.*",
    }
    logging.debug(f'requesting field(s) {fields[type]} for filter: {type}')
    fields = current_app.opensearch_client.indices.get_field_mapping(fields[type])
    fields_list = []
    fd = flatten_dict(fields)
    for key in fd.keys():
        if not key.startswith('database'):
            continue
        entry = {}
        if key.split(".")[-1] == "type":
            entry["attr_name"] = key_to_attr_name(key)
            entry["attr_friendly_name"] = attr_name_to_attr_friendly_name(entry["attr_name"])
            entry["type"] = fd[key]
            fields_list.append(entry)
    return fields_list


def general_search(type=None, search_term=None, t1=None, t2=None, field_value_pairs=None):
    """
    Main method for seaching stuff in the opensearch db

    all parameters are optional

    :param type: The index to be searched. Optional.
    :param search_term: The search term. Optional.
    :param t1: The start range value. Optional.
    :param t2: The end range value. Optional.
    :param field_value_pairs: The key-value pair of properties that need to match. Optional.
    :return: The object of results and HTTP status code. e.g. { "hits": { "hits": [] } }, 200
    """
    queries = []
    if search_term is None:
        logging.info(f"Performing general search")
    else:
        logging.info(f"Performing fuzzy search")
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
        logging.debug(f'search body: {fuzzy_body}')
        response = current_app.opensearch_client.search(
            index="database",
            body=fuzzy_body
        )
        logging.info(f"Found {len(response['hits']['hits'])} result(s)")
        return response
    musts = []
    if field_value_pairs is not None and len(field_value_pairs) > 0:
        logging.debug('query has field_value_pairs present')
        is_range_open_end = False
        is_range_open_begin = False
        is_range_query = False
        if t1 is not None and t2 is None:
            is_range_open_begin = True
            logging.debug(f"query has only start value {t1} present")
        if t1 is None and t2 is not None:
            is_range_open_end = True
            logging.debug(f"query has only end value {t2} present")
        if t1 is not None and t2 is not None:
            is_range_query = True
            logging.debug(f"query has start value {t1} and end value {t2} present")
        for key, value in field_value_pairs.items():
            if is_range_open_end and re.match(f"unit\.", key):
                logging.debug(f"omit key={key} because query type=open end range and key is somewhat unit")
                logging.info(f"add match-query for range ),{t2}]")
                musts.append({
                    "range": {
                        "val_max": {
                            "lte": t2
                        }
                    }
                })
            elif is_range_open_begin and re.match(f"unit\.", key):
                logging.debug(f"omit key={key} because query type=open begin range and key is somewhat unit")
                logging.info(f"add match-query for range [{t1},(")
                musts.append({
                    "range": {
                        "val_min": {
                            "gte": t1
                        }
                    }
                })
            elif is_range_query and re.match(f"unit\.", key):
                logging.debug(
                    f"omit key={key} because query type=full range and key is somewhat unit")
                logging.info(f"add match-query for range [{t1},{t2}]")
                musts.append({
                    "range": {
                        "val_min": {
                            "gte": t1
                        }
                    }
                })
                musts.append({
                    "range": {
                        "val_max": {
                            "lte": t2
                        }
                    }
                })
            else:
                if '.' in key:
                    logging.debug(f'key {key} is nested: use nested query')
                    index = key.split('.')[0]
                    musts.append({
                        "nested": {
                            "path": index,
                            "query": {
                                "term": {
                                    key: value
                                }
                            }
                        }
                    })
                else:
                    logging.debug(f'key {key} is flat: use bool query')
                    musts.append({
                        "match": {
                            key: {"query": value, "minimum_should_match": "90%"}
                        }
                    })
    body = {
        "query": {"bool": {"must": musts}}
    }
    logging.debug(f'search in index database for type: {type}')
    logging.debug(f'search body: {body}')
    response = current_app.opensearch_client.search(
        index="database",
        body=json.dumps(body)
    )
    results = [hit["_source"] for hit in response["hits"]["hits"]]
    return results


def flatten(mylist):
    return [item for sublist in mylist for item in sublist]


def unit_uri_to_unit(uri):
    base_identifier = uri[len(OM_IDS.NAMESPACE):].replace("-", "")
    return getattr(OM, base_identifier)


def unit_independent_search(t1=None, t2=None, field_value_pairs=None):
    """
    Main method for searching stuff in the opensearch db

    all parameters are optional

    :param t1: start value
    :param t2: end value
    :param field_value_pairs: the key-value pairs
    :return:
    """
    logging.info(f"Performing unit-independent search")
    searches = []
    body = {
        "size": 0,
        "aggs": {
            "units": {
                "terms": {"field": "unit.uri", "size": 500}
            }
        }
    }
    response = current_app.opensearch_client.search(
        index="column",
        body=json.dumps(body)
    )
    unit_uris = [hit["key"] for hit in response["aggregations"]["units"]["buckets"]]
    logging.debug(f"found {len(unit_uris)} unit(s) in column index")
    base_unit = unit_uri_to_unit(field_value_pairs["unit.uri"])
    for unit_uri in unit_uris:
        gte = t1
        lte = t2
        if unit_uri != field_value_pairs["unit.uri"]:
            target_unit = unit_uri_to_unit(unit_uri)
            if not Unit.can_convert(base_unit, target_unit):
                logging.error(f"Cannot convert unit {field_value_pairs['unit.uri']} to target unit {unit_uri}")
                continue
            gte = om(t1, base_unit).convert(target_unit)
            lte = om(t2, base_unit).convert(target_unit)
            logging.debug(
                f"converted original range [{t1},{t2}] for base unit {base_unit} to mapped range [{gte},{lte}] for target unit={target_unit}")
        searches.append({'index': 'column'})
        searches.append({
            "query": {
                "bool": {
                    "must": [
                        {
                            "match": {
                                "concept.uri": {
                                    "query": field_value_pairs["concept.uri"]
                                }
                            }
                        },
                        {
                            "range": {
                                "val_min": {
                                    "gte": gte
                                }
                            }
                        },
                        {
                            "range": {
                                "val_max": {
                                    "lte": lte
                                }
                            }
                        },
                        {
                            "match": {
                                "unit.uri": {
                                    "query": unit_uri
                                }
                            }
                        }
                    ]
                }
            }
        })
    logging.debug('searches: %s', searches)
    body = ''
    for search in searches:
        body += '%s \n' % json.dumps(search)
    responses = current_app.opensearch_client.msearch(
        body=json.dumps(body)
    )
    response = {
        "hits": {
            "hits": flatten([hits["hits"]["hits"] for hits in responses["responses"]])
        },
        "took": responses["took"]
    }
    return response

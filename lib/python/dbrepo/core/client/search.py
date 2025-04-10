"""
The opensearch_client.py is used by the different API endpoints in routes.py to handle requests  to the opensearch db
"""
import logging
import os
from collections.abc import MutableMapping
from json import dumps, load

from dbrepo.api.dto import Database
from dbrepo.api.exceptions import ForbiddenError, NotExistsError
from dbrepo.core.omlib.constants import OM_IDS
from dbrepo.core.omlib.measure import om
from dbrepo.core.omlib.omconstants import OM
from dbrepo.core.omlib.unit import Unit
from opensearchpy import OpenSearch, NotFoundError
from requests import head


class SearchServiceClient:
    """
    The client to communicate with the OpenSearch database.
    """
    instance: OpenSearch = None

    def __init__(self, host: str = None, port: int = 9000, username: str = 'admin', password: str = 'admin'):
        if host is None:
            host = 'search-db'
        self.host = os.getenv('OPENSEARCH_HOST', host)
        self.metadata_endpoint = os.getenv('METADATA_SERVICE_ENDPOINT', 'http://metadata-service:8080')
        self.port = int(os.getenv('OPENSEARCH_PORT', port))
        self.system_username = os.getenv('SYSTEM_USERNAME', username)
        self.system_password = os.getenv('SYSTEM_PASSWORD', password)

    def _instance(self) -> OpenSearch:
        """
        Wrapper method to get the instance singleton.

        @returns: The opensearch instance singleton, if successful.
        """
        if self.instance is None:
            self.instance = OpenSearch(hosts=[{"host": self.host, "port": self.port}],
                                       http_compress=True,
                                       http_auth=(self.system_username, self.system_password))
        return self.instance

    def database_exists(self, database_id: str):
        try:
            SearchServiceClient()._instance().get(index="database", id=database_id)
            return True
        except NotFoundError:
            return False

    def index_update(self, mapping: dict) -> None:
        if SearchServiceClient()._instance().indices.exists(index="database"):
            logging.debug(f"index 'database' exists, removing...")
            SearchServiceClient()._instance().indices.delete(index="database")
        SearchServiceClient()._instance().indices.create(index="database", body=mapping)
        logging.info(f"Created index 'database'")

    def save_database(self, database_id: str, data: Database, fetch: bool = False) -> Database | None:
        """
        Creates the database data with given id. If a document with the id already exists, the document is updated.

        @param database_id: The database id.
        @param data: The database data.
        @param fetch: When enabled, fetches the saved database data. Default: `False`.

        :return The saved database data.

        @returns: The updated database, if successful.
        @throws: opensearchpy.exceptions.NotFoundError If the database was not found in the Search Database. Ignored when `force` is `True`.
        """
        self._instance().update(index="database", id=database_id,
                                body={'doc': data.model_dump(), 'doc_as_upsert': True})
        logging.info(f'Updated database with id: {database_id}')
        if fetch is False:
            return None
        response = self._instance().get(index="database", id=database_id)
        logging.debug(f'fetched database for return value with id: {database_id}')
        return Database.model_validate(response["_source"])

    def delete_database(self, database_id: str) -> None:
        """
        Deletes the database data with given id.

        @param database_id: The database id.
        @throws: opensearchpy.exceptions.NotFoundError If the database was not found in the Search Database.
        """
        self._instance().delete(index="database", id=database_id)
        logging.info(f"Deleted database with id {database_id} in index 'database'")

    def query_index_by_term_opensearch(self, term, mode):
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

        response = self._instance().search(
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

    def get_fields_for_index(self, field_type: str):
        """
        returns a list of attributes of the data for a specific index.
        :param field_type: The search type
        :return: list of fields
        """
        fields = {
            "database": "*",
            "table": "tables.*",
            "column": "tables.columns.*",
            "concept": "tables.columns.concept.*",
            "unit": "tables.columns.unit.*",
            "identifier": "identifiers.*",
            "view": "views.*",
            "user": "owner.*",
        }
        if field_type not in fields.keys():
            raise NotFoundError(f"Failed to find field type: {field_type}")
        logging.debug(f'requesting field(s) {fields[field_type]} for filter: {field_type}')
        fields = self._instance().indices.get_field_mapping(fields[field_type])
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

    def fuzzy_search(self, search_term: str, user_id: str | None = None, user_token: str | None = None) -> [Database]:
        response = self._instance().search(
            index="database",
            body={
                "query": {
                    "multi_match": {
                        "query": search_term,
                        "fuzziness": "AUTO",
                        "prefix_length": 2
                    }
                }
            }
        )
        results: [Database] = []
        if "hits" in response and "hits" in response["hits"]:
            results = [Database.model_validate(hit["_source"]) for hit in response["hits"]["hits"]]
        logging.debug(f'found {len(results)} results')
        return self.filter_results(results, user_id, user_token)

    def filter_results(self, results: [Database], user_id: str | None = None, user_token: str | None = None) -> [
        Database]:
        filtered: [Database] = []
        for database in results:
            if database.is_public or database.is_schema_public:
                logging.debug(f'database with id {database.id} is public or has public schema')
                filtered.append(database)
            elif user_id is not None and user_token is not None:
                try:
                    url = f'{self.metadata_endpoint}/api/database/{database.id}/access/{user_id}'
                    logging.debug(f'requesting access from url: {url}')
                    response = head(url=url, auth=(self.system_username, self.system_password))
                    if response.status_code == 200:
                        logging.debug(f'database with id {database.id} is draft and access was found')
                        filtered.append(database)
                    else:
                        logging.warning(
                            f'database with id {database.id} is not accessible: code {response.status_code}')
                except (ForbiddenError, NotExistsError) as e:
                    logging.warning(f'database with id {database.id} is draft but no access was found')
        logging.debug(f'filtered {len(filtered)} results')
        return filtered

    def general_search(self, field_type: str = None, field_value_pairs: dict = None, user_id: str | None = None,
                       user_token: str | None = None) -> [Database]:
        """
        Main method for searching stuff in the opensearch db

        all parameters are optional

        :param field_type: The index to be searched. Optional.
        :param field_value_pairs: The key-value pair of properties that need to match. Optional.
        :return: The object of results and HTTP status code. e.g. { "hits": { "hits": [] } }, 200
        """
        musts = []
        if field_value_pairs is not None and len(field_value_pairs) > 0:
            logging.debug(f'field_value_pairs present: {field_value_pairs}')
            for key, value in field_value_pairs.items():
                if field_value_pairs[key] == None:
                    logging.debug(f"skip empty key: {key}")
                    continue
                logging.debug(f"processing key: {key}")
                if '.' in key:
                    logging.debug(f'key {key} is nested: use nested query')
                    musts.append({
                        "match": {
                            key: value
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
        logging.debug(f'search in index database for type: {field_type}')
        response = self._instance().search(
            index="database",
            body=dumps(body)
        )
        results: [Database] = []
        if "hits" in response and "hits" in response["hits"]:
            results = [Database.model_validate(hit["_source"]) for hit in response["hits"]["hits"]]
        logging.debug(f'found {len(results)} results')
        return self.filter_results(results, user_id, user_token)

    def unit_independent_search(self, t1: float, t2: float, field_value_pairs: dict, userId: str | None = None) -> [
        Database]:
        """
        Main method for searching stuff in the opensearch db

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
        response = self._instance().search(
            index="database",
            body=dumps(body)
        )
        unit_uris = [hit["key"] for hit in response["aggregations"]["units"]["buckets"]]
        logging.debug(f"found {len(unit_uris)} unit(s) in column index")
        if len(unit_uris) == 0:
            raise NotFoundError("Failed to search: no unit assigned")
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
            body += '%s \n' % dumps(search)
        response = self._instance().msearch(
            body=dumps(body)
        )
        results = flatten([hits["hits"]["hits"] for hits in response["responses"]])
        return [database for database in results if
                database.is_public or database.is_schema_public or (userId is not None and database.owner.id == userId)]


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
    with open('./friendly_names_overrides.json') as json_data:
        d = load(json_data)
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


def flatten(mylist):
    return [item for sublist in mylist for item in sublist]


def unit_uri_to_unit(uri):
    base_identifier = uri[len(OM_IDS.NAMESPACE):].replace("-", "")
    return getattr(OM, base_identifier)

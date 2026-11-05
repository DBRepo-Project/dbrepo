"""
The opensearch_client.py is used by the different API endpoints in routes.py to handle requests  to the opensearch db
"""
import logging
import math
import os
from collections.abc import MutableMapping
from json import dumps, load
from decimal import Decimal

from opensearchpy import OpenSearch, NotFoundError
import requests

from dbrepo.api.dto import Database
from dbrepo.api.exceptions import ServiceConnectionError
from dbrepo.core.omlib.constants import OM_IDS
from dbrepo.core.omlib.measure import om
from dbrepo.core.omlib.exceptions.unitconversionexception import UnitConversionException
from dbrepo.core.omlib.omconstants import OM
from dbrepo.core.omlib.unit import Unit


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

    def fuzzy_search(self, search_term: str, username: str | None = None, user_token: str | None = None) -> list[
        Database]:
        response = self._instance().search(
            index="database",
            size=1000,
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
        results: list[Database] = []
        if "hits" in response and "hits" in response["hits"]:
            results = [Database.model_validate(hit["_source"]) for hit in response["hits"]["hits"]]
        logging.debug(f'found {len(results)} results')
        return self.filter_results(results, username, user_token)

    def filter_results(self, results: list[Database], username: str | None = None, user_token: str | None = None) -> \
            list[Database]:
        filtered: list[Database] = []
        # The index selects candidates; current Metadata responses decide what the caller may see.
        with requests.Session() as session:
            session.trust_env = False  # Do not inherit .netrc credentials or environment authentication.
            headers = {"Accept": "application/json"}
            if user_token is not None:
                headers["Authorization"] = user_token
            for database in results:
                try:
                    response = session.get(f'{self.metadata_endpoint}/api/v1/database/{database.id}',
                                           headers=headers, timeout=(3.05, 5), allow_redirects=False)
                    if response.status_code in (401, 403, 404):
                        continue
                    if response.status_code != 200:
                        raise ServiceConnectionError("Failed to retrieve current search metadata")
                    current = Database.model_validate(response.json())
                    if current.id != database.id:
                        raise ServiceConnectionError("Metadata response does not match the search candidate")
                    filtered.append(current)
                except (requests.RequestException, ValueError) as error:
                    raise ServiceConnectionError("Failed to retrieve current search metadata") from error
        logging.debug(f'filtered {len(filtered)} results')
        return filtered

    def general_search(self, field_value_pairs: dict = None, username: str | None = None,
                       user_token: str | None = None) -> list[Database]:
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
                if key in ('tables.columns.concept.uri', 'tables.columns.unit.uri',
                           'tables.columns.concept_uri', 'tables.columns.unit_uri'):
                    legacy = key.replace('_uri', '.uri')
                    current = legacy.replace('.uri', '_uri')
                    musts.append({'bool': {'should': [{'match': {legacy: value}}, {'match': {current: value}}],
                                           'minimum_should_match': 1}})
                elif '.' in key:
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
        logging.debug(f'search: {body}')
        response = self._instance().search(
            index="database",
            size=1000,
            body=dumps(body)
        )
        results: list[Database] = []
        if "hits" in response and "hits" in response["hits"]:
            results = [Database.model_validate(hit["_source"]) for hit in response["hits"]["hits"]]
        logging.debug(f'found {len(results)} results')
        return self.filter_results(results, username, user_token)

    def unit_independent_search(self, t1: float, t2: float, field_value_pairs: dict, username: str | None = None,
                                user_token: str | None = None) -> \
            list[Database]:
        """
        Main method for searching stuff in the opensearch db

        :param t1: start value
        :param t2: end value
        :param field_value_pairs: the key-value pairs, optional.
        :param username: the username, optional.
        :return:
        """
        concept_uri = field_value_pairs.get('tables.columns.concept.uri', field_value_pairs.get('concept.uri'))
        unit_uri = field_value_pairs.get('tables.columns.unit.uri', field_value_pairs.get('unit.uri'))
        fields = {key: value for key, value in field_value_pairs.items()
                  if key not in ('concept.uri', 'unit.uri', 'tables.columns.concept.uri', 'tables.columns.unit.uri')}
        # Database documents contain ordinary object arrays, so evaluate the range on one current column.
        databases = self.general_search(fields, username, user_token)
        results = []
        for database in databases:
            tables = []
            for table in database.tables or []:
                columns = [column for column in table.columns
                           if column_in_range(column, concept_uri, unit_uri, t1, t2)]
                if columns:
                    tables.append(table.model_copy(update={'columns': columns}))
            if tables:
                results.append(database.model_copy(update={'tables': tables}))
        return results


def semantic_value(column, field: str):
    """Prefer the current scalar URI; older indexed records can contain a full semantic object."""
    uri = getattr(column, f'{field}_uri')
    value = getattr(column, field)
    if uri is not None:
        return value if value is not None and value.uri == uri else {'uri': uri}
    return value


def column_in_range(column, concept_uri: str, unit_uri: str, lower: float, upper: float) -> bool:
    concept = semantic_value(column, 'concept')
    unit = semantic_value(column, 'unit')
    if concept is None or unit is None or column.val_min is None or column.val_max is None:
        return False
    actual_concept = concept['uri'] if isinstance(concept, dict) else concept.uri
    actual_unit = unit['uri'] if isinstance(unit, dict) else unit.uri
    if actual_concept != concept_uri or not all(math.isfinite(value) for value in
                                                (lower, upper, column.val_min, column.val_max)):
        return False
    lower, upper = Decimal(str(lower)), Decimal(str(upper))
    if actual_unit != unit_uri:
        try:
            # Absolute temperatures need offsets that OM's multiplicative Unit converter does not supply.
            temperatures = {OM_IDS.NAMESPACE + 'kelvin': (Decimal(1), Decimal('273.15')),
                            OM_IDS.NAMESPACE + 'degreeCelsius': (Decimal(1), Decimal(0)),
                            OM_IDS.NAMESPACE + 'degreeFahrenheit': (Decimal('1.8'), Decimal(32))}
            if unit_uri in temperatures and actual_unit in temperatures:
                source_factor, source_offset = temperatures[unit_uri]
                target_factor, target_offset = temperatures[actual_unit]
                lower = (lower - source_offset) / source_factor * target_factor + target_offset
                upper = (upper - source_offset) / source_factor * target_factor + target_offset
            else:
                source, target = unit_uri_to_unit(unit_uri), unit_uri_to_unit(actual_unit)
                if not Unit.can_convert(source, target) or source.dimensions.Theta != 0:
                    return False
                factor = om(1, source)
                factor.convert(target)
                lower *= Decimal(str(factor.numericalValue))
                upper *= Decimal(str(factor.numericalValue))
        except (AttributeError, TypeError, ValueError, UnitConversionException):
            return False
    return lower <= Decimal(str(column.val_min)) <= Decimal(str(column.val_max)) <= upper


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
    if not isinstance(uri, str) or not uri.startswith(OM_IDS.NAMESPACE):
        raise ValueError('Unit is outside the supported OM namespace')
    base_identifier = uri[len(OM_IDS.NAMESPACE):].replace("-", "")
    return getattr(OM, base_identifier)

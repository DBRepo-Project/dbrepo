"""
The opensearch_client.py is used by the different API endpoints in routes.py to handle requests  to the opensearch db
"""
import logging
import math
import os
import time
from copy import deepcopy
from datetime import datetime, timedelta, timezone
from urllib.parse import urlsplit
from uuid import UUID, uuid4
from collections.abc import MutableMapping
from json import dumps, load
from decimal import Decimal

from opensearchpy import OpenSearch, NotFoundError, RequestError, TransportError
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
    QUERY_INDEX = 'dbrepo-search-queries'
    QUERY_FIELD = 'search_query'
    EXPIRY_FIELD = 'search_expires_at'
    BATCH_SIZE = 100
    REQUEST_BUDGET = 30
    QUERY_LIFETIME = 300
    CLEANUP_LIMIT = 100

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
                                       http_auth=(self.system_username, self.system_password), max_retries=0)
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
        fields = self._instance().indices.get_field_mapping(fields=fields[field_type], index='database')
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

    @staticmethod
    def _remaining(deadline):
        remaining = deadline - time.monotonic()
        if remaining <= 0:
            raise ServiceConnectionError('Search request deadline exceeded')
        return remaining

    def _headers(self, user_token):
        endpoint = urlsplit(self.metadata_endpoint)
        if endpoint.username is not None or endpoint.password is not None:
            raise ServiceConnectionError('Metadata endpoint must not contain credentials')
        headers = {'Accept': 'application/json'}
        if user_token is not None:
            headers['Authorization'] = user_token
        return headers

    def _metadata_json(self, session, path, headers, deadline, params=None):
        remaining = self._remaining(deadline)
        response = session.get(self.metadata_endpoint.rstrip('/') + path, headers=headers, params=params,
                               timeout=(min(3.05, remaining), min(5, remaining)), allow_redirects=False)
        self._remaining(deadline)
        if response.status_code != 200:
            raise ServiceConnectionError('Failed to retrieve current search metadata')
        payload = response.json()
        if not isinstance(payload, list):
            raise ServiceConnectionError('Malformed current search metadata')
        return payload

    def _metadata_batch(self, session, ids, headers, deadline):
        payload = self._metadata_json(session, '/api/v1/database/batch', headers, deadline,
                                      params=[('id', identifier) for identifier in ids])
        results = []
        seen = set()
        for item in payload:
            # Validators normalize known aliases on a private copy; other authorized body fields remain searchable.
            body = deepcopy(item)
            database = Database.model_validate(body)
            identifier = str(UUID(database.id))
            if identifier not in ids or identifier in seen:
                raise ServiceConnectionError('Metadata batch returned an unexpected database')
            seen.add(identifier)
            results.append((database, body))
        self._remaining(deadline)
        return results

    def filter_results(self, results: list[Database], username: str | None = None,
                       user_token: str | None = None) -> list[Database]:
        deadline = time.monotonic() + self.REQUEST_BUDGET
        headers = self._headers(user_token)
        ids = sorted({str(UUID(database.id)) for database in results})
        current = []
        try:
            with requests.Session() as session:
                session.trust_env = False
                for offset in range(0, len(ids), self.BATCH_SIZE):
                    current.extend(database for database, _ in self._metadata_batch(
                        session, ids[offset:offset + self.BATCH_SIZE], headers, deadline))
        except (requests.RequestException, ValueError, KeyError, TypeError) as error:
            raise ServiceConnectionError('Failed to retrieve current search metadata') from error
        return current

    def _engine_call(self, operation, deadline, **kwargs):
        response = operation(request_timeout=min(5, self._remaining(deadline)), **kwargs)
        self._remaining(deadline)
        return response

    def _query_index(self, backend, deadline):
        try:
            source = self._engine_call(backend.indices.get_mapping, deadline, index='database')
            properties = deepcopy(source['database']['mappings'].get('properties', {}))
        except NotFoundError:
            properties = {}
        existing_properties = None
        try:
            existing = self._engine_call(backend.indices.get_mapping, deadline, index=self.QUERY_INDEX)
            existing_properties = existing[self.QUERY_INDEX]['mappings'].get('properties', {})
            established = deepcopy(existing[self.QUERY_INDEX]['mappings'].get('properties', {}))
            merge_properties(established, properties)
            properties = established
        except NotFoundError:
            pass
        merge_properties(properties, database_properties())
        properties[self.QUERY_FIELD] = {'type': 'percolator'}
        properties[self.EXPIRY_FIELD] = {'type': 'date'}
        if existing_properties is None:
            try:
                created = self._engine_call(backend.indices.create, deadline, index=self.QUERY_INDEX,
                                  body={'settings': {'number_of_shards': 1, 'number_of_replicas': 0},
                                        'mappings': {'properties': properties}})
            except RequestError as error:
                if error.error != 'resource_already_exists_exception':
                    raise
            else:
                if not isinstance(created, dict) or created.get('acknowledged') is not True:
                    raise ServiceConnectionError('Temporary search index creation was not acknowledged')
                existing_properties = properties
        if properties != existing_properties:
            updated = self._engine_call(backend.indices.put_mapping, deadline, index=self.QUERY_INDEX,
                              body={'properties': {name: mapping for name, mapping in properties.items()
                                                   if name not in (self.QUERY_FIELD, self.EXPIRY_FIELD)}})
            if not isinstance(updated, dict) or updated.get('acknowledged') is not True:
                raise ServiceConnectionError('Temporary search mapping update was not acknowledged')
        cleanup = self._engine_call(backend.delete_by_query, deadline, index=self.QUERY_INDEX,
                                   body={'query': {'range': {self.EXPIRY_FIELD: {'lt': 'now'}}}},
                                   max_docs=self.CLEANUP_LIMIT, refresh=True, conflicts='proceed')
        if not isinstance(cleanup, dict) or cleanup.get('timed_out') is not False or cleanup.get('failures'):
            raise ServiceConnectionError('Temporary search cleanup failed')
        return properties

    def _search_current(self, query, user_token=None):
        deadline = time.monotonic() + self.REQUEST_BUDGET
        headers = self._headers(user_token)
        backend = self._instance()
        query_id = str(uuid4())
        results = []
        attempted_create = False
        try:
            with requests.Session() as session:
                session.trust_env = False
                listed = self._metadata_json(session, '/api/v1/database', headers, deadline)
                if any(not isinstance(item, dict) or not isinstance(item.get('id'), str) for item in listed):
                    raise ServiceConnectionError('Malformed current database list')
                ids = sorted({str(UUID(item['id'])) for item in listed})
                if not ids:
                    return []
                for offset in range(0, len(ids), self.BATCH_SIZE):
                    batch = self._metadata_batch(session, ids[offset:offset + self.BATCH_SIZE], headers, deadline)
                    if not batch:
                        continue
                    if not attempted_create:
                        properties = self._query_index(backend, deadline)
                        native_query = query(properties) if callable(query) else query
                        expires = (datetime.now(timezone.utc) + timedelta(seconds=self.QUERY_LIFETIME)).isoformat()
                        attempted_create = True
                        self._engine_call(backend.index, deadline, index=self.QUERY_INDEX, id=query_id,
                                          body={self.QUERY_FIELD: native_query, self.EXPIRY_FIELD: expires}, refresh=True)
                    response = self._engine_call(backend.search, deadline, index=self.QUERY_INDEX,
                        allow_partial_search_results=False, body={'size': 1, '_source': False,
                            'query': {'bool': {
                                'filter': [{'ids': {'values': [query_id]}},
                                           {'range': {self.EXPIRY_FIELD: {'gt': 'now'}}}],
                                'should': [{'percolate': {'field': self.QUERY_FIELD,
                                                         'documents': [body for _, body in batch]}}],
                                'minimum_should_match': 0}}})
                    if (not isinstance(response, dict) or response.get('timed_out') is not False
                            or not isinstance(response.get('_shards'), dict) or response['_shards'].get('failed') != 0
                            or type(response['_shards'].get('total')) is not int
                            or response['_shards'].get('successful') != response['_shards']['total']):
                        raise ServiceConnectionError('Search evaluation was incomplete')
                    hits = response['hits']['hits']
                    if len(hits) != 1 or hits[0]['_id'] != query_id:
                        raise ServiceConnectionError('Temporary search query is missing or expired')
                    fields = hits[0].get('fields', {})
                    if not isinstance(fields, dict):
                        raise ServiceConnectionError('Malformed search document fields')
                    slots = fields.get('_percolator_document_slot', [])
                    if not isinstance(slots, list) or any(type(slot) is not int or slot < 0 or slot >= len(batch)
                                                          for slot in slots) or len(slots) != len(set(slots)):
                        raise ServiceConnectionError('Malformed search document slots')
                    results.extend(batch[slot][0] for slot in slots)
                self._remaining(deadline)
        except (requests.RequestException, TransportError, ValueError, KeyError, TypeError) as error:
            raise ServiceConnectionError('Failed to evaluate current search metadata') from error
        finally:
            if attempted_create:
                try:
                    backend.delete(index=self.QUERY_INDEX, id=query_id, refresh=True, ignore=404, request_timeout=5)
                except TransportError as error:
                    logging.warning('Temporary search query cleanup failed: %s', query_id)
                    raise ServiceConnectionError('Temporary search query cleanup failed') from error
        self._remaining(deadline)
        return results

    def fuzzy_search(self, search_term: str, username: str | None = None,
                     user_token: str | None = None) -> list[Database]:
        return self._search_current({'multi_match': {'query': search_term, 'fuzziness': 'AUTO', 'prefix_length': 2}},
                                    user_token)

    def general_search(self, field_value_pairs: dict = None, username: str | None = None,
                       user_token: str | None = None) -> list[Database]:
        def query(properties):
            musts = []
            for key, value in (field_value_pairs or {}).items():
                if value is None:
                    continue
                if key in ('tables.columns.concept.uri', 'tables.columns.unit.uri',
                           'tables.columns.concept_uri', 'tables.columns.unit_uri'):
                    legacy = key.replace('_uri', '.uri')
                    current = legacy.replace('.uri', '_uri')
                    musts.append({'bool': {'should': [{'match': {legacy: value}}, {'match': {current: value}}],
                                           'minimum_should_match': 1}})
                elif not mapped_field(properties, key):
                    # Ordinary index search does not match fields absent from its mapping.
                    musts.append({'match_none': {}})
                elif '.' in key:
                    musts.append({'match': {key: value}})
                else:
                    musts.append({'match': {key: {'query': value, 'minimum_should_match': '90%'}}})
            return {'bool': {'must': musts}}
        return self._search_current(query, user_token)

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


def database_properties():
    """Fill missing native field types from the fixed Database DTO shape."""
    schema = Database.model_json_schema()
    definitions = schema.get('$defs', {})

    def field_mapping(field):
        if '$ref' in field:
            return field_mapping(definitions[field['$ref'].rsplit('/', 1)[-1]])
        if 'anyOf' in field:
            return field_mapping(next(item for item in field['anyOf'] if item.get('type') != 'null'))
        kind = field.get('type')
        if kind == 'array':
            return field_mapping(field['items'])
        if kind == 'object':
            return {'properties': {name: field_mapping(value) for name, value in field['properties'].items()}}
        if kind == 'string':
            if field.get('format') in ('date-time', 'date'):
                return {'type': 'date'}
            return {'type': 'text', 'fields': {'keyword': {'type': 'keyword', 'ignore_above': 256}}}
        return {'type': {'boolean': 'boolean', 'integer': 'long', 'number': 'double'}[kind]}

    return field_mapping(schema)['properties']


def mapped_field(properties, path):
    mapping = {'properties': properties}
    for name in path.split('.'):
        mapping = mapping.get('properties', mapping.get('fields', {})).get(name)
        if mapping is None:
            return False
    return True


def merge_properties(existing, required):
    for name, mapping in required.items():
        if name not in existing:
            existing[name] = mapping
        elif 'properties' in mapping and 'properties' in existing[name]:
            merge_properties(existing[name]['properties'], mapping['properties'])


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

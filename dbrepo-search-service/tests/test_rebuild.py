import json
import runpy
from uuid import uuid4

import pytest
from opensearchpy import ConflictError, ConnectionTimeout, NotFoundError, RequestError, TransportError

from dbrepo.RestClient import RestClient
from dbrepo.api.exceptions import ServiceConnectionError
from dbrepo.core.client.search import SearchServiceClient
from tests.test_app import req
from tests.test_current_search import authority


@pytest.fixture
def catalog():
    client = SearchServiceClient()
    client.save_database(req.id, req)
    return client


def test_legacy_migration_and_next_rebuild(catalog, monkeypatch, requests_mock):
    monkeypatch.setenv('METADATA_SERVICE_ENDPOINT', 'http://metadata.invalid')
    with open('database.json') as file:
        mapping = json.load(file)
    mapping['aliases'] = {'catalog-export': {}}
    public = req.model_copy(update={'id': str(uuid4()), 'name': 'Public fixture'})
    private = req.model_copy(update={'id': str(uuid4()), 'name': 'Private fixture', 'is_public': False})
    os = catalog._instance()
    store = os.index

    def index_while_reading(**kwargs):
        assert os.get(index='database', id=req.id)['_source']['name'] == req.name
        assert not os.exists(index='database', id=public.id)
        return store(**kwargs)

    with monkeypatch.context() as active:
        active.setattr(os, 'index', index_while_reading)
        catalog.index_update(mapping, lambda: [public, private])
    first, = os.indices.get_alias(name='database')
    assert list(os.indices.get_alias(name='catalog-export')) == [first]
    assert first.startswith('database-')
    assert os.get(index='database', id=private.id)['_source']['name'] == private.name
    assert not os.exists(index='database', id=req.id)

    # The field and query mappings use a physical key. Search still uses the caller's metadata.
    assert 'name' in {field['attr_name'] for field in catalog.get_fields_for_index('database')}
    authority(requests_mock, [public.model_dump(mode='json')])
    assert [match.id for match in SearchServiceClient().general_search({})] == [public.id]

    catalog.index_update(mapping, lambda: [])
    second, = os.indices.get_alias(name='database')
    assert list(os.indices.get_alias(name='catalog-export')) == [second]
    assert second != first and not os.indices.exists(index=first)
    assert os.count(index='database')['count'] == 0
    catalog.save_database(req.id, req)
    assert catalog.database_exists(req.id)
    catalog.delete_database(req.id)
    assert not catalog.database_exists(req.id)


@pytest.mark.parametrize('phase', ['fetch', 'duplicate', 'count'])
def test_failed_rebuild_leaves_current_index_complete(catalog, monkeypatch, phase):
    os = catalog._instance()
    before = set(os.indices.get(index='database*'))
    if phase == 'fetch':
        def fetch():
            raise ConnectionError('metadata unavailable')
        error = ConnectionError
    elif phase == 'duplicate':
        def fetch():
            return [req, req]
        error = ConflictError
    else:
        def fetch():
            return [req]
        monkeypatch.setattr(os, 'count', lambda **kwargs: {'count': 0, '_shards': {'failed': 0}})
        error = ServiceConnectionError
    with pytest.raises(error):
        catalog.index_update({}, fetch)
    assert set(os.indices.get(index='database*')) == before
    assert catalog._catalog_indices()[1] == []
    assert os.get(index='database', id=req.id)['_source']['name'] == req.name


def test_write_between_validation_and_switch_invalidates_candidate(catalog, monkeypatch):
    os = catalog._instance()
    publish = os.indices.update_aliases
    newer = req.model_copy(update={'name': 'Updated during publication'})

    def update_aliases(*, body):
        if any('add' in action for action in body['actions']):
            catalog.save_database(req.id, newer)
        return publish(body=body)

    monkeypatch.setattr(os.indices, 'update_aliases', update_aliases)
    with pytest.raises((NotFoundError, RequestError)):
        catalog.index_update({}, lambda: [req])
    assert list(os.indices.get(index='database*')) == ['database']
    assert os.get(index='database', id=req.id)['_source']['name'] == newer.name


@pytest.mark.parametrize('delete', [False, True])
def test_write_that_started_on_retired_index_reaches_new_index(catalog, monkeypatch, delete):
    origin = catalog._catalog_indices
    newer = req.model_copy(update={'name': 'Latest'})
    started = False

    def switch():
        nonlocal started
        if not started:
            started = True
            catalog.index_update({}, lambda: [req])
        return origin()

    monkeypatch.setattr(catalog, '_catalog_indices', switch)
    if delete:
        catalog.delete_database(req.id)
        assert not catalog.database_exists(req.id)
    else:
        catalog.save_database(req.id, newer)
        assert catalog._instance().get(index='database', id=req.id)['_source']['name'] == newer.name


def test_catalog_write_only_deletes_marked_rebuild_guards(catalog):
    os = catalog._instance()
    other_index = catalog.REBUILD_GUARD_PREFIX + uuid4().hex
    os.indices.create(index=other_index)
    try:
        catalog.save_database(req.id, req)
        assert os.indices.exists(index=other_index)
    finally:
        os.indices.delete(index=other_index)


def test_lost_switch_response_keeps_published_index(catalog, monkeypatch):
    os = catalog._instance()
    update_aliases = os.indices.update_aliases

    def lost_response(**kwargs):
        response = update_aliases(**kwargs)
        if any('add' in action for action in kwargs['body']['actions']):
            raise ConnectionTimeout('timeout', 'lost response')
        return response

    monkeypatch.setattr(os.indices, 'update_aliases', lost_response)
    with pytest.raises(ConnectionTimeout):
        catalog.index_update({}, lambda: [req])
    assert os.get(index='database', id=req.id)['_source']['name'] == req.name
    assert len(os.indices.get_alias(name='database')) == 1


def test_switch_delayed_until_after_failure_cannot_republish(catalog, monkeypatch):
    os = catalog._instance()
    publish = os.indices.update_aliases
    queued = []

    def delay_switch(*, body):
        queued.append(body)
        raise ConnectionTimeout('timeout', 'publication not confirmed')

    monkeypatch.setattr(os.indices, 'update_aliases', delay_switch)
    with pytest.raises(ConnectionTimeout):
        catalog.index_update({}, lambda: [req])
    assert catalog._catalog_indices() == ('database', [])
    with pytest.raises(TransportError):
        publish(body=queued[0])
    assert os.get(index='database', id=req.id)['_source']['name'] == req.name


@pytest.mark.parametrize('starting_index', ['missing', 'concrete', 'alias', 'retained'])
def test_overlapping_rebuilds_publish_one_generation(catalog, monkeypatch, starting_index):
    os = catalog._instance()
    if starting_index == 'missing':
        os.indices.delete(index='database')
    elif starting_index in ('alias', 'retained'):
        catalog.index_update({}, lambda: [req])
        if starting_index == 'retained':
            previous, = os.indices.get_alias(name='database')
            os.indices.put_alias(index=previous, name='retained-generation')
    winner = req.model_copy(update={'name': 'Published by the other job'})
    update_aliases = os.indices.update_aliases

    def publish_other_first(**kwargs):
        if any('add' in action for action in kwargs['body']['actions']):
            with monkeypatch.context() as other:
                other.setattr(os.indices, 'update_aliases', update_aliases)
                catalog.index_update({}, lambda: [winner])
        return update_aliases(**kwargs)

    monkeypatch.setattr(os.indices, 'update_aliases', publish_other_first)
    with pytest.raises(TransportError):
        catalog.index_update({}, lambda: [req])
    active, = os.indices.get_alias(name='database')
    if starting_index == 'retained':
        assert set(os.indices.get(index='database*')) == {active, previous}
        assert list(os.indices.get_alias(name='retained-generation')) == [previous]
        os.indices.delete(index=previous)
    else:
        assert list(os.indices.get(index='database*')) == [active]
    assert catalog._catalog_indices() == (active, [])
    assert os.get(index='database', id=winner.id)['_source']['name'] == winner.name


def test_setup_updates_statistics_after_publication(catalog, monkeypatch):
    newer = req.model_copy(update={'name': 'Imported by setup'})
    monkeypatch.setattr(RestClient, 'get_databases', lambda self: [req])
    monkeypatch.setattr(RestClient, 'get_database', lambda self, database_id: newer)
    observed = []

    def update(self, database_id, table_id):
        record = catalog._instance().get(index='database', id=database_id)
        observed.append((record['_index'], record['_source']['name'], table_id))

    monkeypatch.setattr(RestClient, 'update_table_statistics', update)
    runpy.run_path('init.py', run_name='__main__')
    active, = catalog._instance().indices.get_alias(name='database')
    assert observed == [(active, newer.name, req.tables[0].id)]

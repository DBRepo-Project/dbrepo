#!/usr/bin/env python3
"""Exercise the installed REST client against an isolated live DBRepo database."""
import argparse
import ast
from collections import Counter
from datetime import datetime, timezone, timedelta
import importlib
import importlib.util
import hashlib
import json
import logging
import os
from pathlib import Path
import re
import sys
import subprocess
import struct
import zlib
import time
from unittest.mock import patch
from urllib.parse import parse_qs, quote, urlsplit
from uuid import uuid4
from xml.etree import ElementTree

import pandas as pd
import requests
import pydantic
import urllib3
from dbrepo.RestClient import RestClient
from dbrepo.api import dto

HERE = Path(__file__).resolve().parent


def canonical(method, url, params=None):
    parsed = urlsplit(url)
    path = re.sub(r'\{[^}]+\}|<[^>]+>|[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}', '{id}', parsed.path)
    if '/user/' in path or '/access/' in path or '/grant/' in path:
        path = re.sub(r'/(user|access|grant)/[^/]+$', r'/\1/{id}', path)
    path = re.sub(r'(^/api/v1/dashboard/)(?!user(?:/|$))[^/]+', r'\1{id}', path)
    if '/analyse/schema/' in path:
        path = path.split('/analyse/schema/')[0] + '/analyse/schema/{id}'
    if path.startswith('/api/v1/search/'):
        path = re.sub(r'^(/api/v1/search)/[^/]+(?=/fields$|$)', r'\1/{id}', path)
    query = dict(parse_qs(parsed.query))
    query.update(dict(params or {}))
    if path == '/api/v1/oai' and query.get('verb'):
        verb = query['verb']; path += '?verb=' + (verb[0] if isinstance(verb, list) else verb)
    return method.upper() + ' ' + path.rstrip('/')


def source_inventory(root):
    """Read Spring mappings and Flask decorators; no server imports or requests."""
    found = {}
    for service in ('metadata', 'data'):
        directory = root / f'dbrepo-{service}-service/rest-service/src/main/java/at/ac/tuwien/ifs/dbrepo/endpoints'
        for file in directory.glob('*.java'):
            text = file.read_text()
            mappings = list(re.finditer(r'^\s*@(Request|Get|Post|Put|Delete|Patch)Mapping\b(?:\((.*?)\))?', text, re.M | re.S))
            if not mappings or 'class ' not in text: continue
            first = mappings[0]
            prefix_match = re.search(r'"(/[^\"]*)"', first[2] or '')
            if first[1] != 'Request' or prefix_match is None: continue
            prefix = prefix_match[1]
            for mapping in mappings[1:]:
                args = mapping[2] or ''
                suffix = re.search(r'(?:^\s*|\b(?:value|path)\s*=\s*)"(/[^\"]*)"', args)
                path = prefix + (suffix[1] if suffix else '')
                methods = re.findall(r'RequestMethod\.(\w+)', args) if mapping[1] == 'Request' else [mapping[1].upper()]
                for method in methods:
                    key = canonical(method, path)
                    verb = re.search(r'params\s*=\s*"verb=([A-Za-z]+)"', args)
                    if verb: key += '?verb=' + verb[1]
                    found.setdefault(key, []).append(str(file.relative_to(root)))
    for service in ('search', 'dashboard'):
        file = root / f'dbrepo-{service}-service/app.py'
        for node in ast.walk(ast.parse(file.read_text())):
            if not isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)): continue
            for deco in node.decorator_list:
                if not isinstance(deco, ast.Call) or not isinstance(deco.func, ast.Attribute) or deco.func.attr != 'route': continue
                if not deco.args or not isinstance(deco.args[0], ast.Constant): continue
                methods = next((ast.literal_eval(k.value) for k in deco.keywords if k.arg == 'methods'), ['GET'])
                for method in methods:
                    key = canonical(method, deco.args[0].value)
                    if '/api/v1/' in key: found.setdefault(key, []).append(str(file.relative_to(root)))
    # Client endpoints without a server mapping are deliberately included: drift must fail visibly.
    tree = ast.parse((root / 'lib/python/dbrepo/RestClient.py').read_text())
    methods = {}
    for node in ast.walk(tree):
        if not isinstance(node, ast.FunctionDef) or node.name.startswith('_'): continue
        methods[node.name] = []
        for child in ast.walk(node):
            if isinstance(child, ast.Assign) and any(isinstance(t, ast.Name) and t.id == 'url' for t in child.targets):
                value = child.value
                if isinstance(value, ast.JoinedStr):
                    path = ''.join(v.value if isinstance(v, ast.Constant) else '{id}' for v in value.values)
                    methods[node.name].append(path.split('?')[0])
    found['GET /pid/{id}'] = ['dbrepo-gateway-service/dbrepo.conf']
    found['GET /health'] = ['dbrepo-gateway-service/dbrepo.conf']
    internal = {
        'PUT /api/v1/database/{id}/access/{id}/synchronize': 'Internal grant synchronization; requires the system identity and committed metadata.',
        'POST /api/v1/database/{id}/table/{id}/data/ingest': 'Internal Consumer write; requires the system identity and a broker-validated publisher.',
        'PUT /api/v1/database/{id}': 'Internal Data Service operation; gateway routes database metadata to Metadata.',
    }
    return {'endpoints': [{'operation': key, 'sources': sorted(set(sources)),
                            'excluded': internal.get(key)} for key, sources in sorted(found.items())],
            'client_methods': methods}


class Payload:
    def __init__(self, value): self.value = value
    def model_dump(self, **kwargs): return self.value


def make_client(endpoint, credentials):
    credentials = credentials or {}
    password = credentials.get('token') or credentials.get('password') or os.getenv(credentials.get('password_env', ''))
    client = RestClient()
    # RestClient's environment overrides must not turn a peer/anonymous request into an owner request.
    client.endpoint = endpoint
    client.username = None if credentials.get('token') else credentials.get('username')
    client.password = password
    client.secure = True
    return client


class Runner:
    def __init__(self, config, output):
        self.config, self.output = config, output
        self.profile = config.get('profile', 'full')
        if self.profile not in ('user', 'full'): raise ValueError('Unknown test profile')
        self.table_deletion = config.get('table_deletion', 'physical')
        if self.table_deletion not in ('physical', 'archive'): raise ValueError('Unknown table deletion contract')
        self.endpoint = config['endpoint'].rstrip('/')
        parsed = urlsplit(self.endpoint)
        if parsed.scheme not in ('https', 'http') or not parsed.hostname or parsed.username or parsed.password or parsed.query or parsed.fragment:
            raise ValueError('endpoint must be an HTTP(S) base URL without credentials, query or fragment')
        ca = config.get('ca_bundle', True)
        if ca is not True and (not isinstance(ca,str) or not ca.strip()): raise ValueError('TLS verification cannot be disabled')
        self.clients = {role: make_client(self.endpoint, config.get(role)) for role in ('owner', 'peer', 'admin', 'system')}
        self.clients['anonymous'] = make_client(self.endpoint, {})
        for client in self.clients.values(): client.secure = config.get('ca_bundle', True)
        if not self.clients['owner'].password: raise ValueError('owner credentials are required')
        self.run_id = 'client-live-' + datetime.now(timezone.utc).strftime('%Y%m%d%H%M%S') + '-' + uuid4().hex[:8]
        self.results, self.http_calls, self.methods, self.resources = [], [], set(), {}
        self.last_response = None
        self.instance = {}
        self.finished = False
        self.original_request = requests.request
        self.original_wrapper = RestClient._wrapper
        self.active = None
        self.catalog = json.loads((HERE / 'client-live-endpoints.json').read_text())

    def transport(self, **kwargs):
        if not kwargs['url'].startswith(self.endpoint + '/'):
            raise ValueError('Request escaped the configured endpoint')
        kwargs.setdefault('timeout', (5, self.config.get('read_timeout', 30)))
        kwargs.setdefault('allow_redirects', False)
        started = time.monotonic()
        entry = {'case': self.active, 'operation': canonical(kwargs['method'], kwargs['url'], kwargs.get('params'))}
        try:
            response = self.original_request(**kwargs)
            self.last_response = response
            entry['status'] = response.status_code
            if 'json' in response.headers.get('Content-Type', ''):
                try:
                    code = response.json().get('code') if isinstance(response.json(), dict) else None
                    if isinstance(code, str) and re.fullmatch(r'[a-zA-Z0-9_.-]+', code): entry['api_code'] = code
                except ValueError: pass
            return response
        except requests.RequestException as error:
            entry['error'] = type(error).__name__
            raise
        finally:
            entry['seconds'] = round(time.monotonic() - started, 3)
            self.http_calls.append(entry)

    def step(self, name, action, need=(), reason=None, resource=None, check=None, contract=None):
        self.active, self.last_response = name, None
        started = time.monotonic()
        item = {'case': name}
        if contract: item['contract'] = contract
        missing = [key for key in need if key not in self.resources]
        value = None
        full_only = (name == 'user.update.same_values' or name == 'oai.GetRecord' or
                     name.startswith(('dashboard.', 'image.create', 'image.get', 'image.update', 'image.delete',
                      'container.create', 'container.delete', 'message.', 'search.index.delete', 'search.index.restore',
                      'identifier.publish', 'identifier.retrieve.external')))
        optional_peer = (name.startswith(('peer.', 'access.', 'grant.', 'database.owner.transfer', 'database.owner.return')) and not self.clients['peer'].password)
        optional_amqp = name.startswith('amqp.') and not self.config.get('amqp')
        if self.profile == 'user' and (full_only or optional_peer or optional_amqp):
            item.update(status='not_in_profile', reason='Outside the selected user profile or optional connection not configured')
        elif reason or missing:
            item.update(status='skipped', reason=reason or 'Missing prerequisites: ' + ', '.join(missing))
        else:
            try:
                value = action()
                if check is not None: assert check(value), 'Response content did not match the expected test data'
                item['status'] = 'passed'
            except Exception as error:
                item.update(status='failed', error=type(error).__name__)
                if isinstance(error, AssertionError): item['detail'] = str(error)[:200]
                if hasattr(error, 'errors'):
                    item['validation'] = [{'location': list(e['loc']), 'type': e['type']} for e in error.errors(include_input=False, include_context=False)]
            if resource:
                # Keep the server-created ID even if the SDK rejected the response DTO. The case stays failed.
                identifier = getattr(value, 'id', None)
                if identifier is None and self.last_response is not None and self.last_response.status_code == 201:
                    try: identifier = self.last_response.json().get('id') or self.last_response.json().get('uid')
                    except (ValueError, AttributeError): pass
                if identifier: self.resources[resource] = str(identifier)
        item['seconds'] = round(time.monotonic() - started, 3)
        self.results.append(item)
        print(f"{item['status'].upper():7} {name}" + (f" ({item['reason']})" if item.get('reason') else ''), flush=True)
        self.save()
        return value

    def sdk(self, method, *args, role='owner', **kwargs):
        self.methods.add(method)
        return getattr(self.clients[role], method)(*args, **kwargs)

    def removed_catalog(self, method):
        calls = len(self.http_calls)
        try:
            self.sdk(method)
        except NotImplementedError as error:
            message = str(error)
            assert f'{method} is unavailable:' in message, 'Missing removed-method explanation'
            assert all(text in message for text in ('concept_uri/unit_uri', 'get_table()', 'update_table_column()')), 'Missing replacement guidance'
            assert len(self.http_calls) == calls, 'Removed catalog method performed an HTTP request'
            return
        raise AssertionError('Removed catalog method did not raise NotImplementedError')

    def http(self, method, path, payload=None, expected=(200,), role='owner', **kwargs):
        response = self.clients[role]._wrapper(method=method.lower(), url=path,
                    payload=Payload(payload) if payload is not None else None,
                    headers=kwargs.pop('headers', {'Accept': 'application/json'}), **kwargs)
        assert response.status_code in expected, f"Expected HTTP {expected}, received {response.status_code}"
        return response

    def save(self):
        counts = Counter(item['status'] for item in self.results)
        coverage = []
        for endpoint in self.catalog['endpoints']:
            calls = [c for c in self.http_calls if c['operation'] == endpoint['operation']]
            successful = [c for c in calls if 200 <= c.get('status', 0) < 300 and any(r['case'] == c['case'] and r['status'] == 'passed' for r in self.results)]
            operation = endpoint['operation']
            method, path = operation.split(' ', 1)
            outside = self.profile == 'user' and (
                path.startswith('/api/v1/dashboard') or
                (path == '/api/v1/container' and method == 'POST') or
                (path == '/api/v1/container/{id}' and method == 'DELETE') or
                (path.startswith('/api/v1/image') and method in ('POST','PUT','DELETE')) or
                (path.startswith('/api/v1/message') and (method in ('POST','PUT','DELETE') or '/message/message/' in path)) or
                (path.startswith('/api/v1/search/database/') and method in ('PUT','DELETE')) or
                (path == '/api/v1/user/{id}' and method == 'PUT') or
                path in ('/api/v1/identifier/retrieve','/api/v1/identifier/{id}/publish') or
                (('/access/' in path or '/grant/' in path) and not self.clients['peer'].password))
            coverage.append({**endpoint, 'attempts': len(calls), 'passed_cases': len(successful),
                             'state': 'excluded' if endpoint.get('excluded') else 'not_in_profile' if outside else 'passed' if successful else 'attempted' if calls else 'uncovered'})
        report = {'runtime_versions': {'python': sys.version.split()[0], 'requests': requests.__version__, 'urllib3': urllib3.__version__, 'pydantic': pydantic.__version__, 'pandas': pd.__version__}, 'instance': self.instance, 'profile': self.profile, 'setup': self.config.get('setup', []), 'completed': self.finished, 'run_id': self.run_id, 'endpoint': self.endpoint,
                  'client_module': str(Path(importlib.import_module('dbrepo.RestClient').__file__).resolve()),
                  'runner_sha256': hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
                  'client_sha256': hashlib.sha256(Path(importlib.import_module('dbrepo.RestClient').__file__).read_bytes()).hexdigest(),
                  'contracts': {'table_deletion': self.table_deletion},
                  'summary': dict(counts), 'resources': self.resources, 'cases': self.results,
                  'requests': self.http_calls, 'endpoint_coverage': coverage,
                  'client_methods': {m: 'attempted' if m in self.methods else 'not_in_profile' if self.profile == 'user' and m in ('update_user','create_container','delete_container','publish_identifier') else 'uncovered' for m in self.catalog['client_methods']},
                  'limitations': ['Database deletion is not exposed by the REST API; test databases remain.',
                    'HTTP coverage is not full semantic, security, concurrency or UI coverage.',
                    'Published identifiers are immutable and remain when --publish is used.']}
        self.output.parent.mkdir(parents=True, exist_ok=True)
        temporary = self.output.with_suffix('.tmp')
        with os.fdopen(os.open(temporary,os.O_WRONLY|os.O_CREAT|os.O_TRUNC,0o600),'w') as stream:
            os.fchmod(stream.fileno(),0o600)
            json.dump(report,stream,indent=2)
        temporary.replace(self.output)

    def run(self, keep=False, publish=False, external_doi=None):
        c = self.clients['owner']
        def call(method, *a, role='owner', **kw): return self.sdk(method, *a, role=role, **kw)
        def step(name, action, **kw): return self.step(name, action, **kw)
        def instance_version():
            response=self.http('GET','/',role='anonymous',headers={'Accept':'text/html','Cache-Control':'no-cache'},expected=(200,301,302,303,401,403,404))
            self.instance={'frontend_http_status':response.status_code,
                'advertised_commits':re.findall(r'/(?:commit|-/commit)/([a-f0-9]{7,40})',response.text),
                'advertised_versions':re.findall(r'https://www\.ifs\.tuwien\.ac\.at/infrastructures/dbrepo/([^/\"<> ]+)',response.text)}
        step('instance.version',instance_version)
        step('gateway.health',lambda:self.http('GET','/health',role='anonymous'),check=lambda r:r.json().get('status')=='UP')
        step('client.whoami', lambda: call('whoami'))
        for method in ('get_users', 'get_units', 'get_ontologies', 'get_concepts', 'get_licenses', 'get_images', 'get_messages', 'get_identifiers', 'get_databases', 'get_databases_count'):
            if method in ('get_units', 'get_ontologies', 'get_concepts'):
                step('catalog.' + method, lambda m=method: self.removed_catalog(m), contract='removed_sdk_method')
            else:
                step('catalog.' + method, lambda m=method: call(m))
        user = step('user.get', lambda: call('get_user', c.username), reason=None if c.username else 'Owner username required for account tests')
        step('user.head', lambda: self.http('HEAD', '/api/v1/user/' + quote(c.username, safe='')), reason=None if c.username else 'Owner username required')
        step('user.update.same_values', lambda: call('update_user', c.username, user.attributes.theme, user.attributes.language,
               firstname=user.given_name, lastname=user.family_name, affiliation=user.attributes.affiliation, orcid=user.attributes.orcid),
               reason=None if user else 'User response unavailable')
        containers = step('container.list', lambda: call('get_containers'), check=lambda x: len(x) > 0)
        selected = self.config.get('container_id') or (containers[0].id if containers else None)
        if selected: self.resources['container'] = selected
        container = step('container.get', lambda: call('get_container', selected), need=('container',))
        step('image.supported', lambda: call('get_image', container.image.id), reason=None if container else 'Container unavailable')
        step('database.create', lambda: call('create_database', self.run_id, selected, is_public=False, is_schema_public=False), need=('container',), resource='database')
        db = self.resources.get('database', 'missing')
        dp = '/api/v1/database/' + db
        for name, method, args in [('database.get', 'get_database', [db]), ('database.access.client', 'get_database_access', [db])]:
            step(name, lambda m=method,a=args: call(m,*a), need=('database',))
        step('database.batch', lambda: self.http('GET', '/api/v1/database/batch', params=[('id', db)]), need=('database',), check=lambda x: any(d['id']==db for d in x.json()))
        step('database.private.anonymous', lambda: self.http('GET', dp, role='anonymous', expected=(403,404)), need=('database',))
        df = pd.DataFrame({'id': [1, 2], 'value': [10, 20], 'label': ['alpha', 'beta']}).set_index('id')
        step('table.create', lambda: call('create_table', db, 'Measurements', False, False, df, with_data=False), need=('database',), resource='table')
        tid = self.resources.get('table', 'missing'); tp = dp + '/table/' + tid
        table = step('table.get', lambda: call('get_table', db, tid), need=('table',))
        step('table.list', lambda: call('get_tables', db), need=('database',), check=lambda x: any(t.id==tid for t in x))
        for name, method, args in [
            ('table.import', 'import_table_data', (db, tid, df.reset_index())),
            ('table.insert', 'create_table_data', (db, tid, {'id':3,'value':30,'label':'gamma'})),
            ('table.update.row', 'update_table_data', (db, tid, {'value':31}, {'id':3})),
            ('table.delete.row', 'delete_table_data', (db, tid, {'id':3})),
            ('table.statistics.client', 'analyse_table_statistics', (db, tid)),
            ('table.statistics.update', 'update_table_statistics', (db, tid)),
            ('table.history', 'get_table_history', (db, tid)),
        ]: step(name, lambda m=method,a=args: call(m,*a), need=('table',))
        step('table.data', lambda: call('get_table_data', db, tid, size=10), need=('table',), check=lambda x: set(x['id']) == {1,2} and set(x['value']) == {10,20})
        step('table.count', lambda: call('get_table_data_count', db, tid), need=('table',), check=lambda x: x==2)
        step('table.statistics.http', lambda: self.http('GET', tp+'/statistic'), need=('table',))
        step('table.update.metadata', lambda: self.http('PUT', tp, {'description':'Synthetic live client test','is_public':False,'is_schema_public':False}, expected=(202,)), need=('table',))
        if table: self.resources['column'] = table.columns[0].id
        concept_uri = f'https://example.invalid/{self.run_id}/concept'
        unit_uri = f'https://example.invalid/{self.run_id}/unit'
        step('table.column.semantics', lambda: call('update_table_column', db, tid, self.resources['column'], concept_uri=concept_uri, unit_uri=unit_uri),
             need=('table','column'), check=lambda column: column.concept_uri == concept_uri and column.unit_uri == unit_uri)
        step('table.column.semantics.readback', lambda: call('get_table', db, tid), need=('table','column'),
             check=lambda value: any(column.id == self.resources['column'] and column.concept_uri == concept_uri and column.unit_uri == unit_uri for column in value.columns))
        step('table.column.semantics.clear', lambda: call('update_table_column', db, tid, self.resources['column']),
             need=('table','column'), check=lambda column: column.concept_uri is None and column.unit_uri is None)
        step('table.column.semantics.clear.readback', lambda: call('get_table', db, tid), need=('table','column'),
             check=lambda value: any(column.id == self.resources['column'] and column.concept_uri is None and column.unit_uri is None for column in value.columns))
        uploaded = step('upload.csv', lambda: c._upload(df.reset_index()))
        for method in ('analyse_schema', 'analyse_datatypes', 'analyse_keys'):
            step('analysis.'+method, lambda m=method: call(m, df.reset_index()))
        step('analysis.database.schema', lambda: self.http('GET', dp+'/analyse/schema/'+quote(uploaded, safe='')), need=('database',), reason=None if uploaded else 'Upload unavailable')
        step('table.export.csv', lambda: self.http('GET', tp+'/data', headers={'Accept':'text/csv'}, params=[('page',0),('size',10)]), need=('table',), check=lambda r: 'value' in r.text and '10' in r.text)
        step('table.pagination', lambda: call('get_table_data', db, tid, page=0, size=1), need=('table',), check=lambda x: len(x)==1)
        internal = table.internal_name.split('.')[-1] if table else 'missing'
        query = dto.QueryDefinition(columns=[internal+'.id',internal+'.value'], datasources=[internal], orders=[dto.OrderDefinition(column=internal+'.id',direction=dto.OrderType.ASC)])
        step('subset.create', lambda: call('create_subset', db, query, size=10), need=('table',), check=lambda x: len(x)==2)
        queries = step('subset.list', lambda: call('get_queries', db), need=('database',))
        if queries: self.resources['subset'] = queries[-1].id
        qid = self.resources.get('subset','missing')
        for name, method, args, check in [
            ('subset.get','get_subset',(db,qid),None), ('subset.data','get_subset_data',(db,qid),lambda x:len(x)==2),
            ('subset.count','get_subset_data_count',(db,qid),lambda x:x==2), ('subset.persist','update_subset',(db,qid,True),lambda x:x.is_persisted),
        ]: step(name, lambda m=method,a=args:call(m,*a), need=('subset',), check=check)
        step('view.create', lambda: call('create_view', db, 'Measurement view', query, False, False), need=('table',), resource='view')
        vid = self.resources.get('view','missing'); vp=dp+'/view/'+vid
        for name, method, args, check in [
            ('view.list','get_views',(db,),None), ('view.get','get_view',(db,vid),None),
            ('view.visibility','update_view',(db,vid,False,False),None), ('view.data','get_view_data',(db,vid),lambda x:len(x)==2),
            ('view.count','get_view_data_count',(db,vid),lambda x:x==2),
        ]: step(name, lambda m=method,a=args:call(m,*a), need=('view',), check=check)
        step('view.statistics', lambda:self.http('GET',vp+'/statistic'), need=('view',))
        step('view.csv', lambda:self.http('GET',vp+'/data',headers={'Accept':'text/csv'},params=[('page',0),('size',10)]), need=('view',), check=lambda r:'value' in r.text)
        def preview_upload():
            def chunk(kind, data): return struct.pack('!I',len(data))+kind+data+struct.pack('!I',zlib.crc32(kind+data))
            png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('!IIBBBBB',1,1,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(b'\0\0\0\0'))+chunk(b'IEND',b'')
            response=self.http('POST','/api/v1/upload',expected=(201,204),files={'file':(self.run_id+'.png',png,'image/png')})
            return response.headers['X-S3-Key']
        preview=step('database.preview.upload',preview_upload)
        step('database.preview.set',lambda:self.http('PUT',dp+'/image',{'key':preview},expected=(202,)),need=('database',),reason=None if preview else 'Preview upload unavailable')
        step('database.preview.get',lambda:self.http('GET',dp+'/image',headers={'Accept':'image/webp'}),need=('database',),check=lambda r:bool(r.content))
        step('database.preview.private.denied',lambda:self.http('GET',dp+'/image',role='anonymous',expected=(403,404)),need=('database',))
        step('database.preview.clear',lambda:self.http('PUT',dp+'/image',{'key':None},expected=(202,)),need=('database',))
        step('database.refresh_schema', lambda:call('update_database_schema',db), need=('database',))
        for kind in ('table','view'):
            step('database.refresh.'+kind+'.http', lambda k=kind:self.http('PUT',dp+'/metadata/'+k), need=('database',))
        self.access_steps(step, call, db, tid)
        self.amqp_steps(step, call, db, tid, table)
        self.identifier_steps(step, call, db, qid, tid, vid, publish, external_doi, keep)
        self.search_steps(step, dp)
        step('database.visibility.public',lambda:call('update_database_visibility',db,True,True,False),need=('database',))
        step('database.public.anonymous',lambda:self.http('GET',dp,role='anonymous'),need=('database',))
        step('database.visibility.private',lambda:call('update_database_visibility',db,False,False,False),need=('database',))
        step('database.private.restored',lambda:self.http('GET',dp,role='anonymous',expected=(403,404)),need=('database',))
        self.admin_steps(step, call)
        if keep:
            step('cleanup',lambda:None,reason='--keep requested; synthetic tables/views/draft identifiers retained')
        else:
            step('view.delete',lambda:call('delete_view',db,vid),need=('view',))
            self.table_deletion_steps(step, call, db, tid, qid)
        self.save()

    def table_deletion_steps(self, step, call, db, tid, qid):
        tp = '/api/v1/database/' + db + '/table/' + tid
        historical = None
        if self.table_deletion == 'archive':
            historical = step('table.archive.history.before', lambda: call('get_subset_data', db, qid), need=('subset',))
        step('table.delete', lambda: call('delete_table', db, tid), need=('table',))
        def deleted():
            if self.table_deletion == 'physical':
                self.http('GET', tp, expected=(404,))
            else:
                body = self.http('GET', tp).json()
                assert body['id'] == tid and body.get('archived_at'), 'Expected the same archived table'
                datetime.fromisoformat(body['archived_at'].replace('Z', '+00:00'))
                self.resources['archive_timestamp'] = body['archived_at']
            active = self.http('GET', '/api/v1/database/' + db + '/table').json()
            assert all(t['id'] != tid for t in active), 'Removed table is still in active listings'
        step('table.deleted', deleted, need=('table',))
        if self.table_deletion == 'archive':
            step('table.archive.repeat', lambda: call('delete_table', db, tid), need=('table',))
            step('table.archive.timestamp', lambda: self.http('GET', tp), need=('archive_timestamp',),
                 check=lambda r: r.json()['id'] == tid and r.json()['archived_at'] == self.resources['archive_timestamp'])
            step('table.archive.write.denied', lambda: self.http('POST', tp + '/data',
                 {'data': {'id': 901, 'value': 901, 'label': 'archive-write-must-fail'}}, expected=(403,)), need=('table',))
            step('table.archive.history.after', lambda: call('get_subset_data', db, qid), need=('subset',),
                 reason=None if historical is not None else 'Historical query baseline unavailable',
                 check=lambda rows: rows.equals(historical))
            iid = self.resources.get('identifier', 'missing')
            step('identifier.archived.get', lambda: self.http('GET', '/api/v1/identifier/' + iid), need=('identifier',),
                 check=lambda r: r.json()['id'] == iid and r.json()['database_id'] == db and r.json()['query_id'] == qid)
            step('identifier.archived.data', lambda: call('get_identifier_data', iid, size=10), need=('identifier',),
                 reason=None if historical is not None else 'Historical query baseline unavailable',
                 check=lambda rows: rows.equals(historical))
            step('identifier.delete', lambda: self.http('DELETE', '/api/v1/identifier/' + iid, expected=(202,)), need=('identifier',))

    def access_steps(self, step, call, db, tid):
        dp='/api/v1/database/'+db; peer=self.clients['peer']; configured=bool(peer.password and peer.username)
        reason=None if configured else 'peer credentials required'
        username=quote(peer.username or 'missing',safe=''); ap=dp+'/access/'+username
        step('peer.private.denied',lambda:self.http('GET',dp,role='peer',expected=(403,404)),need=('database',),reason=reason)
        step('access.create',lambda:call('create_database_access',db,peer.username,dto.AccessType.READ),need=('database',),reason=reason)
        step('access.get',lambda:self.http('GET',ap,role='peer'),need=('database',),reason=reason,check=lambda r:r.json()['type']=='read')
        step('access.head',lambda:self.http('HEAD',ap,role='peer'),need=('database',),reason=reason)
        step('grant.get',lambda:self.http('GET',dp+'/grant/'+username,role='peer'),need=('database',),reason=reason)
        step('grant.head',lambda:self.http('HEAD',dp+'/grant/'+username,role='peer'),need=('database',),reason=reason)
        step('peer.private.allowed',lambda:call('get_database',db,role='peer'),need=('database',),reason=reason)
        step('peer.write.denied',lambda:self.http('POST',dp+'/table/'+tid+'/data',{'data':{'id':99,'value':99,'label':'denied'}},role='peer',expected=(403,)),need=('table',),reason=reason)
        step('access.update',lambda:call('update_database_access',db,peer.username,dto.AccessType.WRITE_ALL),need=('database',),reason=reason)
        step('peer.write.allowed',lambda:call('create_table_data',db,tid,{'id':99,'value':99,'label':'peer'},role='peer'),need=('table',),reason=reason)
        step('peer.row.delete',lambda:call('delete_table_data',db,tid,{'id':99},role='peer'),need=('table',),reason=reason)
        step('access.delete',lambda:call('delete_database_access',db,peer.username),need=('database',),reason=reason)
        step('peer.access.revoked',lambda:self.http('GET',dp,role='peer',expected=(403,404)),need=('database',),reason=reason)
        # Exercise the public SDK contract exactly; a mismatch is a failed case, not silently patched here.
        step('database.owner.client',lambda:call('update_database_owner',db,self.clients['owner'].username),need=('database',))
        step('database.owner.transfer',lambda:self.http('PUT',dp+'/owner',{'username':peer.username},expected=(202,)),need=('database',),reason=reason)
        step('database.owner.transferred',lambda:call('get_database',db,role='peer'),need=('database',),reason=reason,check=lambda d:d.owner.username==peer.username)
        step('database.owner.return',lambda:self.http('PUT',dp+'/owner',{'username':self.clients['owner'].username},role='peer',expected=(202,)),need=('database',),reason=reason)


    def amqp_steps(self, step, call, db, tid, table):
        config=self.config.get('amqp')
        reason=None if config and table else 'AMQP connection settings and a readable test table required'
        def publish():
            settings={**config,'username':self.clients['owner'].username,'password':self.clients['owner'].password}
            code="""import json,sys,pandas as pd
from dbrepo.AmqpClient import AmqpClient
v=json.load(sys.stdin);c=AmqpClient(**v['settings'])
c.publish(v['key'],{'id':4,'value':40,'label':'amqp-single'})
c.publish_dataframe(v['key'],pd.DataFrame([{'id':5,'value':50,'label':'amqp-frame'}]))
"""
            result=subprocess.run([sys.executable,'-c',code],input=json.dumps({'settings':settings,'key':table.routing_key}),
                   text=True,capture_output=True,timeout=45,env={k:v for k,v in os.environ.items() if not k.startswith('AMQP_API_')})
            if result.returncode:
                errors=re.findall(r'^(?:[A-Za-z_][\w.]*\.)?([A-Za-z_]\w*(?:Error|Exception))(?::|$)',result.stderr,re.M)
                raise AssertionError('AMQP client publish failed: '+(errors[-1] if errors else 'child process error'))
            deadline=time.monotonic()+20
            while time.monotonic()<deadline:
                data=call('get_table_data',db,tid,size=10)
                if {4,5}.issubset(set(data['id'])):
                    rows=data.set_index('id')
                    assert int(rows.loc[4,'value'])==40 and int(rows.loc[5,'value'])==50,'AMQP rows differ'
                    return True
                time.sleep(0.5)
            raise AssertionError('Published AMQP rows did not arrive through Consumer within 20 seconds')
        step('amqp.publish_and_consume',publish,need=('table',),reason=reason)
        for identifier in (4,5):
            step('amqp.cleanup.'+str(identifier),lambda i=identifier:call('delete_table_data',db,tid,{'id':i}),need=('table',),reason=reason)

    def identifier_steps(self, step, call, db, qid, tid, vid, publish, external_doi, keep):
        descriptions=[dto.CreateIdentifierDescription(description='Synthetic release regression data',type=dto.DescriptionType.ABSTRACT)]
        common=dict(database_id=db,publisher='DBRepo live test',publication_year=datetime.now(timezone.utc).year,
                    descriptions=descriptions,funders=[],licenses=[],related_identifiers=[])
        identifier=step('identifier.create',lambda:call('create_identifier',type=dto.IdentifierType.SUBSET,subset_id=qid,
                       titles=[dto.CreateIdentifierTitle(title=self.run_id)],creators=[dto.CreateIdentifierCreator(creator_name='Release Tester',name_type=dto.IdentifierNameType.PERSONAL)],**common),need=('subset',),resource='identifier')
        iid=self.resources.get('identifier','missing'); ip='/api/v1/identifier/'+iid
        step('identifier.list',lambda:call('get_identifiers',database_id=db),need=('database',))
        step('identifier.get',lambda:call('get_identifier',iid),need=('identifier',))
        step('identifier.pid',lambda:self.http('GET','/pid/'+iid),need=('identifier',))
        step('oai.GetRecord.draft.denied',lambda:self.http('GET','/api/v1/oai',role='anonymous',params=[('verb','GetRecord'),('metadataPrefix','oai_dc'),('identifier','oai:'+iid)],headers={'Accept':'text/xml'},expected=(404,)),need=('identifier',))
        step('identifier.data',lambda:call('get_identifier_data',iid,size=10),need=('identifier',),check=lambda x:len(x)==2)
        step('identifier.update',lambda:call('update_identifier',identifier_id=iid,type=dto.IdentifierType.SUBSET,subset_id=qid,
            titles=[dto.SaveIdentifierTitle(id=t.id,title=t.title) for t in identifier.titles],
            creators=[dto.SaveIdentifierCreator.model_validate(x.model_dump()) for x in identifier.creators],
            **{**common,'descriptions':[dto.SaveIdentifierDescription(id=d.id,description=d.description,type=d.type) for d in identifier.descriptions]}),
            need=('identifier',),reason=None if identifier else 'Client identifier DTO unavailable')
        for accept in ('application/ld+json','text/xml','text/bibliography; style=apa','text/bibliography; style=ieee','text/bibliography; style=bibtex'):
            step('identifier.format.'+accept,lambda a=accept:self.http('GET',ip,headers={'Accept':a}),need=('identifier',))
        step('identifier.retrieve.external',lambda:self.http('GET','/api/v1/identifier/retrieve',params=[('url','https://doi.org/'+external_doi)]),reason=None if external_doi else '--external-doi required; calls external metadata providers')
        for verb in ('Identify','ListMetadataFormats','ListSets','ListIdentifiers','ListRecords'):
            def oai(v=verb):
                response=self.http('GET','/api/v1/oai',role='anonymous',params=[('verb',v),('metadataPrefix','oai_dc')] if v in ('ListIdentifiers','ListRecords') else [('verb',v)],headers={'Accept':'text/xml'})
                root=ElementTree.fromstring(response.content)
                assert root.tag.endswith('OAI-PMH'), 'Missing OAI-PMH document'
                errors=root.findall('{http://www.openarchives.org/OAI/2.0/}error')
                assert all(e.attrib.get('code') in ('noRecordsMatch','noSetHierarchy') for e in errors), 'Unexpected OAI error'
                if v in ('ListIdentifiers','ListRecords') and 'identifier' in self.resources:
                    assert self.resources['identifier'] not in response.text, 'Draft identifier leaked into anonymous OAI response'
                return response
            step('oai.'+verb,oai)
        step('oai.default',lambda:self.http('GET','/api/v1/oai',role='anonymous',headers={'Accept':'text/xml'}))
        publish_reason=None if publish else '--publish required; published records are immutable and retained'
        step('identifier.publishable.create',lambda:call('create_identifier',type=dto.IdentifierType.DATABASE,
             titles=[dto.CreateIdentifierTitle(title=self.run_id+' published record')],
             creators=[dto.CreateIdentifierCreator(creator_name='Release Tester',name_type=dto.IdentifierNameType.PERSONAL)],**common),
             need=('database',),reason=publish_reason,resource='published_identifier')
        published_id=self.resources.get('published_identifier','missing')
        step('identifier.publishable.public',lambda:self.http('PUT','/api/v1/database/'+db+'/visibility',
             {'is_public':True,'is_schema_public':True,'is_dashboard_enabled':False},expected=(202,)),need=('published_identifier',),reason=publish_reason)
        step('identifier.publish',lambda:call('publish_identifier',published_id),need=('published_identifier',),reason=publish_reason)
        step('oai.GetRecord',lambda:self.http('GET','/api/v1/oai',role='anonymous',params=[('verb','GetRecord'),('metadataPrefix','oai_dc'),('identifier','oai:'+published_id)],headers={'Accept':'text/xml'}),
             need=('published_identifier',),reason=publish_reason,check=lambda r:ElementTree.fromstring(r.content).find('.//{http://www.openarchives.org/OAI/2.0/}metadata') is not None)
        step('identifier.publishable.private',lambda:self.http('PUT','/api/v1/database/'+db+'/visibility',
             {'is_public':False,'is_schema_public':False,'is_dashboard_enabled':False},expected=(202,)),need=('published_identifier',),reason=publish_reason)
        if not keep and self.table_deletion == 'physical':
            step('identifier.delete',lambda:self.http('DELETE',ip,expected=(202,)),need=('identifier',))

    def search_steps(self, step, dp):
        credentials=self.config.get('oidc')
        bearer=self.clients['owner']
        if credentials:
            def token():
                owner=self.clients['owner']; response=requests.request(method='post',url=self.endpoint+'/realms/dbrepo/protocol/openid-connect/token',
                     data={'grant_type':'password','client_id':credentials.get('client_id','dbrepo-client'),'client_secret':credentials['client_secret'],
                           'username':owner.username,'password':owner.password})
                assert response.status_code==200,'Could not obtain an owner token'
                secret=response.json()['access_token']
                self.clients['search']=make_client(self.endpoint,{'token':secret})
                return True
            step('search.owner.token',token)
            bearer=self.clients.get('search')
        public_search=not (bearer and bearer.username is None)
        if self.config.get('search'):
            bearer=make_client(self.endpoint,self.config['search'])
            bearer.secure=self.config.get('ca_bundle',True)
            public_search=False
        if public_search:
            self.clients['search']=self.clients['anonymous']
            step('search.public.enable',lambda:self.http('PUT',dp+'/visibility',
                 {'is_public':True,'is_schema_public':True,'is_dashboard_enabled':False},expected=(202,)),need=('database',))
        else: self.clients['search']=bearer
        for name,method,path,kwargs in [
            ('search.fuzzy','GET','/api/v1/search',{'params':[('q',self.run_id)]}),
            ('search.index','GET','/api/v1/search/database',{}),
            ('search.fields','GET','/api/v1/search/database/fields',{}),
            ('search.general','POST','/api/v1/search/database',{'payload':{'name':self.run_id}}),
        ]: step(name,lambda m=method,p=path,k=kwargs:self.http(m,p,role='search',**k),need=('database',))
        if public_search:
            step('search.public.restore_private',lambda:self.http('PUT',dp+'/visibility',
                 {'is_public':False,'is_schema_public':False,'is_dashboard_enabled':False},expected=(202,)),need=('database',))
        system_reason=None if self.clients['system'].password else 'system credentials required'
        step('search.index.delete.own',lambda:self.http('DELETE','/api/v1/search/database/'+self.resources['database'],role='system',expected=(202,)),need=('database',),reason=system_reason)
        step('search.index.restore.own',lambda:self.http('PUT','/api/v1/search/database/'+self.resources['database'],self.http('GET',dp,role='system').json(),role='system',expected=(202,)),need=('database',),reason=system_reason)
        def dashboard_update(content=False):
            database=self.http('GET',dp,role='system').json()
            self.resources['dashboard']=database['dashboard_uid']
            return self.http('PUT','/api/v1/dashboard/'+database['dashboard_uid']+('/content' if content else ''),database,role='system',expected=(202,))
        step('dashboard.update',lambda:dashboard_update(),need=('database',),reason=system_reason)
        step('dashboard.content',lambda:dashboard_update(True),need=('database',),reason=system_reason)
        step('dashboard.user',lambda:self.http('HEAD','/api/v1/dashboard/user/'+quote(self.clients['owner'].username or 'missing',safe=''),role='system',expected=(204,)),reason=system_reason)
        step('dashboard.create',lambda:self.http('POST','/api/v1/dashboard',{'database_name':self.run_id,'owner_username':self.clients['owner'].username,'is_public':False,'is_schema_public':False},role='system',expected=(201,)),reason=system_reason,resource='extra_dashboard')
        step('dashboard.access',lambda:self.http('PUT','/api/v1/dashboard/'+self.resources['dashboard']+'/access/'+quote(self.clients['owner'].username,safe=''),{'permission':'Admin'},role='system',expected=(202,)),need=('dashboard',),reason=system_reason)

    def admin_steps(self, step, call):
        reason=None if self.clients['admin'].password else 'admin credentials with image/message/container roles required'
        spec={'registry':'docker.io/library','name':'mariadb','version':self.run_id,'default_port':3306,
              'driver_class':'org.mariadb.jdbc.Driver','dialect':'org.hibernate.dialect.MariaDBDialect','jdbc_method':'mariadb'}
        step('image.create',lambda:self.http('POST','/api/v1/image',spec,role='admin',expected=(201,)),reason=reason,resource='image')
        image=self.resources.get('image','missing')
        step('image.get',lambda:call('get_image',image),need=('image',))
        step('image.update',lambda:self.http('PUT','/api/v1/image/'+image,{k:v for k,v in spec.items() if k not in ('name','version')},role='admin',expected=(202,)),need=('image',),reason=reason)
        container=self.config.get('test_container')
        step('container.create',lambda:call('create_container',name=self.run_id,role='admin',**container),reason=reason or (None if container else 'test_container connection settings required'),resource='extra_container')
        if 'extra_container' not in self.resources:
            step('container.create.http',lambda:self.http('POST','/api/v1/container',{'name':self.run_id+'-http','quota':50,**container},role='admin',expected=(201,)),reason=reason or (None if container else 'test_container connection settings required'),resource='extra_container')
        step('container.delete.own',lambda:call('delete_container',self.resources['extra_container'],role='admin'),need=('extra_container',),reason=reason)
        step('image.delete.own',lambda:self.http('DELETE','/api/v1/image/'+image,role='admin',expected=(202,)),need=('image',),reason=reason)
        tomorrow=datetime.now(timezone.utc)+timedelta(days=1)
        message={'type':'info','message':self.run_id,'display_start':tomorrow.isoformat(timespec='milliseconds'),'display_end':(tomorrow+timedelta(days=1)).isoformat(timespec='milliseconds')}
        step('message.create',lambda:self.http('POST','/api/v1/message',message,role='admin',expected=(201,)),reason=reason,resource='message')
        mid=self.resources.get('message','missing')
        step('message.get',lambda:self.http('GET','/api/v1/message/message/'+mid),need=('message',))
        step('message.update',lambda:self.http('PUT','/api/v1/message/'+mid,message,role='admin',expected=(202,)),need=('message',),reason=reason)
        step('message.delete.own',lambda:self.http('DELETE','/api/v1/message/'+mid,role='admin',expected=(202,)),need=('message',),reason=reason)


def main(argv=None):
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config',type=Path,help='JSON file: endpoint, owner, optional peer/admin/system/oidc/test_container')
    parser.add_argument('--endpoint',help='Any DBRepo instance base URL')
    parser.add_argument('--table-deletion', choices=('physical', 'archive'), help='Expected server table-removal contract; default physical')
    parser.add_argument('--container-id',help='Optional existing container ID for the new test database')
    parser.add_argument('--profile',choices=('user','full'),help='user: ordinary account; full: additionally admin/system checks')
    parser.add_argument('--username',help='Owner username; password is prompted without echo')
    parser.add_argument('--password-env',help='Read the owner password from this environment variable')
    parser.add_argument('--peer-username',help='Optional second account for grants and ownership tests')
    parser.add_argument('--peer-password-env',help='Read the peer password from this environment variable')
    parser.add_argument('--register-users',action='store_true',help='Explicitly create two fresh accounts through ordinary public registration')
    parser.add_argument('--credentials-output',type=Path,help='Private file for generated accounts; never printed')
    parser.add_argument('--email-domain',default='example.invalid',help='Email domain for generated accounts; verification may require a real mailbox')
    parser.add_argument('--prepare-grafana',action='store_true',help='Perform ordinary Grafana OAuth login for the configured test accounts')
    parser.add_argument('--ca-bundle',type=Path,help='Trusted private CA bundle for a customer instance')
    parser.add_argument('--output',type=Path,help='Report file; default includes host, profile and UTC timestamp')
    parser.add_argument('--keep',action='store_true',help='Keep the test table and view for inspection')
    parser.add_argument('--publish',action='store_true',help='Publish a synthetic immutable local identifier; record remains')
    parser.add_argument('--external-doi',help='Exercise external metadata retrieval for this DOI')
    parser.add_argument('--check-source',type=Path,help='Compare the endpoint catalogue to this repository checkout, without network calls')
    args=parser.parse_args(argv)
    if args.check_source:
        expected=json.loads((HERE/'client-live-endpoints.json').read_text())
        actual=source_inventory(args.check_source)
        if actual!=expected:
            print('Endpoint/client inventory changed; review and regenerate the catalogue',file=sys.stderr);return 1
        print(f"Inventory matches: {len(actual['endpoints'])} operations; {len(actual['client_methods'])} public client methods")
        return 0
    if not args.config and not args.endpoint: parser.error('--endpoint or --config is required')
    if args.credentials_output is None:
        args.credentials_output=Path('.client-live-credentials')/('accounts-'+uuid4().hex[:12]+'.json')
    if args.config and args.output and args.output.resolve()==args.config.resolve(): parser.error('--output must not overwrite --config')
    if args.output and args.output.resolve()==args.credentials_output.resolve(): parser.error('--output must not overwrite generated credentials')
    if args.register_users and args.publish and args.profile!='full': parser.error('--register-users with --publish requires explicit --profile full')
    helper_path=HERE/'client-live-login.py'
    spec=importlib.util.spec_from_file_location('client_live_login',helper_path)
    login=importlib.util.module_from_spec(spec);spec.loader.exec_module(login)
    logging.getLogger().setLevel(logging.WARNING)
    logging.getLogger('urllib3').setLevel(logging.ERROR)
    try: config=login.configure(args)
    except (ValueError,RuntimeError,requests.RequestException,OSError) as error:
        detail=str(error) if isinstance(error,(ValueError,RuntimeError)) else type(error).__name__
        print('Account setup was not completed: '+detail,file=sys.stderr)
        return 2
    if args.table_deletion: config['table_deletion'] = args.table_deletion
    if (args.publish or args.external_doi) and config['profile']!='full': parser.error('--publish and --external-doi require --profile full')
    if args.output is None:
        host=re.sub(r'[^a-zA-Z0-9.-]','_',urlsplit(config['endpoint']).netloc)
        args.output=Path('client-live-report-'+host+'-'+config['profile']+'-'+datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')+'.json')
    logging.getLogger().setLevel(logging.WARNING)
    runner=Runner(config,args.output)
    try:
        with patch.object(requests,'request',runner.transport):
            runner.run(args.keep,args.publish,args.external_doi)
            runner.finished=True
    except (KeyboardInterrupt, Exception) as error:
        runner.results.append({'case':'run','status':'failed','error':type(error).__name__})
    finally:
        runner.save()
    summary=Counter(r['status'] for r in runner.results)
    report=json.loads(args.output.read_text())
    uncovered=[r for r in report['endpoint_coverage'] if r['state']=='uncovered']
    print(f"Report: {args.output.resolve()} | {dict(summary)} | {len(uncovered)} uncovered operations")
    return 1 if summary['failed'] else 2 if summary['skipped'] or uncovered else 0


if __name__=='__main__':
    sys.exit(main())

#!/usr/bin/env python3
"""Offline checks for live-run isolation, reporting and inventory drift."""
import importlib.util
from contextlib import redirect_stdout
import io
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import requests

spec = importlib.util.spec_from_file_location('client_live', Path(__file__).with_name('client-live-test.py'))
live = importlib.util.module_from_spec(spec)
spec.loader.exec_module(live)


class LiveRunnerTest(unittest.TestCase):
    def setUp(self):
        self.enterContext(redirect_stdout(io.StringIO()))
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.runner = live.Runner({'endpoint':'https://example.invalid','owner':{'username':'owner','password':'owner-secret'},
                                   'peer':{'username':'peer','password':'peer-secret'}},Path(self.directory.name)/'report.json')

    def response(self, status=200, body=None):
        response = requests.Response()
        response.status_code = status
        response._content = json.dumps(body if body is not None else []).encode()
        response.headers['Content-Type'] = 'application/json'
        return response

    def test_image_update_sends_only_change_fields(self):
        self.runner.resources['image'] = 'synthetic-image'
        def step(name, action, **kwargs):
            if name == 'image.update': action()
        with patch.object(self.runner, 'http') as http:
            self.runner.admin_steps(step, lambda *args, **kwargs: None)
        method, path, payload = http.call_args.args
        self.assertEqual('PUT', method)
        self.assertEqual('/api/v1/image/synthetic-image', path)
        self.assertEqual({'registry','default_port','driver_class','dialect','jdbc_method'}, set(payload))

    def test_roles_ignore_owner_environment_and_requests_have_timeout(self):
        with patch.dict(os.environ, {'REST_API_USERNAME':'owner','REST_API_PASSWORD':'environment-secret','REST_API_ENDPOINT':'https://wrong.invalid','REST_API_SECURE':'False'}):
            peer = live.make_client(self.runner.endpoint, {'username':'peer','password':'peer-secret'})
            anonymous = live.make_client(self.runner.endpoint, {})
        self.assertEqual('peer', peer.username)
        self.assertEqual('peer-secret', peer.password)
        self.assertIsNone(anonymous.username)
        self.assertIsNone(anonymous.password)
        self.assertTrue(anonymous.secure)
        seen = []
        self.runner.original_request = lambda **kwargs: (seen.append(kwargs) or self.response())
        with patch.object(requests, 'request', self.runner.transport):
            peer.get_databases()
            anonymous.get_databases()
        self.assertEqual(('peer','peer-secret'),seen[0]['auth'])
        self.assertIsNone(seen[1]['auth'])
        self.assertEqual((5,30),seen[0]['timeout'])
        self.assertFalse(seen[0]['allow_redirects'])

    def test_failed_sdk_creation_keeps_owned_id_but_does_not_pass_coverage(self):
        self.runner.original_request = lambda **kwargs: self.response(201, {'id':'0b22324e-d06c-4876-b182-005c99e2ec41'})
        with patch.object(requests, 'request', self.runner.transport):
            self.runner.step('create',lambda:self.runner.sdk('create_database','Synthetic','container'),resource='database')
        report=json.loads(self.runner.output.read_text())
        self.assertEqual('failed',report['cases'][0]['status'])
        self.assertEqual('0b22324e-d06c-4876-b182-005c99e2ec41',report['resources']['database'])
        endpoint=next(e for e in report['endpoint_coverage'] if e['operation']=='POST /api/v1/database')
        self.assertEqual('attempted',endpoint['state'])
        self.assertNotIn('owner-secret',self.runner.output.read_text())
        self.assertEqual(0o600,self.runner.output.stat().st_mode&0o777)

    def test_skip_is_explicit_and_negative_access_does_not_pass_positive_coverage(self):
        self.runner.step('dependent',lambda:self.fail('must not execute'),need=('database',))
        self.runner.original_request=lambda **kwargs:self.response(403,{'code':'error.private'})
        with patch.object(requests,'request',self.runner.transport):
            self.runner.step('denied',lambda:self.runner.http('GET','/api/v1/database/0b22324e-d06c-4876-b182-005c99e2ec41',expected=(403,),role='anonymous'))
        report=json.loads(self.runner.output.read_text())
        self.assertEqual(['skipped','passed'],[r['status'] for r in report['cases']])
        endpoint=next(e for e in report['endpoint_coverage'] if e['operation']=='GET /api/v1/database/{id}')
        self.assertEqual('attempted',endpoint['state'])

    def test_refuses_cross_origin_requests_and_credential_urls(self):
        with self.assertRaises(ValueError):
            self.runner.transport(method='get',url='https://attacker.invalid/api/v1/database')
        with self.assertRaises(ValueError):
            live.Runner({'endpoint':'https://example.invalid','owner':{'token':'token'},'ca_bundle':False},self.runner.output)
        with self.assertRaises(ValueError):
            live.Runner({'endpoint':'https://example.invalid','owner':{'token':'token'},'ca_bundle':''},self.runner.output)
        with self.assertRaises(ValueError):
            live.Runner({'endpoint':'https://user:secret@example.invalid','owner':{'token':'token'}},self.runner.output)

    def test_templates_and_actual_ids_match_including_oai_and_dashboard_uids(self):
        self.assertEqual('GET /api/v1/database/{id}',live.canonical('get','/api/v1/database/0b22324e-d06c-4876-b182-005c99e2ec41'))
        self.assertEqual('PUT /api/v1/dashboard/{id}/content',live.canonical('put','/api/v1/dashboard/abc123/content'))
        self.assertEqual('GET /api/v1/oai?verb=Identify',live.canonical('get','/api/v1/oai',[('verb','Identify')]))
        self.assertEqual('GET /api/v1/search/{id}/fields',live.canonical('get','/api/v1/search/database/fields'))

    def test_publication_and_external_lookup_require_explicit_flags(self):
        planned={}
        def record(name, action, **kwargs):
            planned[name]=kwargs
        self.runner.identifier_steps(record,lambda *a,**kw:None,'database','query','table','view',False,None,False)
        for name in ('identifier.publishable.create','identifier.publish','oai.GetRecord','identifier.retrieve.external'):
            self.assertTrue(planned[name].get('reason'),name)

    def test_user_profile_does_not_execute_privileged_cases(self):
        self.runner.profile='user'
        self.runner.clients['admin'].password='admin-secret'
        self.runner.step('image.create',lambda:self.fail('admin operation must not run'))
        report=json.loads(self.runner.output.read_text())
        self.assertEqual('not_in_profile',report['cases'][-1]['status'])
        self.assertEqual('not_in_profile',next(e for e in report['endpoint_coverage'] if e['operation']=='POST /api/v1/image')['state'])
        self.assertNotIn('admin-secret',self.runner.output.read_text())
        self.runner.clients['peer'].password=None
        self.runner.step('grant.get',lambda:self.fail('second-account grant probe must not run'))
        self.assertEqual('not_in_profile',self.runner.results[-1]['status'])

    def test_login_parser_keeps_csrf_and_select_defaults_and_rejects_cross_origin(self):
        path=live.HERE/'client-live-login.py'
        spec=importlib.util.spec_from_file_location('live_login',path)
        login=importlib.util.module_from_spec(spec);spec.loader.exec_module(login)
        p=login.Forms();p.feed('<form id="kc-register-form" action="/register"><input type="hidden" name="csrf" value="nonce"><input name="username"><select name="language"><option value="de">German</option><option selected value="en">English</option></select></form>')
        self.assertEqual('nonce',p.forms[0]['fields']['csrf'])
        self.assertEqual('en',p.forms[0]['fields']['language'])
        self.assertFalse(login.same_origin('https://instance.invalid','https://instance.invalid.attacker.invalid/register'))
        self.assertFalse(login.same_origin('https://instance.invalid','http://instance.invalid/register'))
        from types import SimpleNamespace
        config_path=Path(self.directory.name)/'target.json'
        config_path.write_text(json.dumps({'endpoint':'https://first.invalid','owner':{'password':'private-secret'}}))
        with self.assertRaises(ValueError):login.configure(SimpleNamespace(config=config_path,endpoint='https://second.invalid'))
        path=Path(self.directory.name)/'accounts.json'
        login.private_config(path,{'owner':{'password':'private-secret'}},create=True)
        self.assertEqual(0o600,path.stat().st_mode&0o777)
        with self.assertRaises(FileExistsError):login.private_config(path,{},create=True)

    def test_comparison_rejects_different_clients_and_does_not_call_skips_passes(self):
        path=live.HERE/'client-live-compare.py'
        spec=importlib.util.spec_from_file_location('live_compare',path)
        module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
        a={'completed':True,'client_sha256':'same','profile':'user','cases':[{'case':'operation','status':'passed'},{'case':'optional','status':'skipped'}]}
        b={'completed':True,'client_sha256':'same','profile':'user','cases':[{'case':'operation','status':'failed','error':'AssertionError'},{'case':'optional','status':'passed'}]}
        result=module.compare(a,b)
        self.assertEqual('candidate_only_failure',result['cases'][0 if result['cases'][0]['case']=='operation' else 1]['state'])
        self.assertEqual(1,result['summary']['uncompared'])
        b['client_sha256']='different'
        self.assertFalse(module.compare(a,b)['comparable_client_and_profile'])
        b['client_sha256']='same';b['completed']=False
        self.assertFalse(module.compare(a,b)['runs_completed'])

    def removal_run(self, contract, *, archived=True, writable=False, listed=False):
        import pandas as pd
        self.runner.table_deletion = contract
        self.runner.resources = {'database':'db', 'table':'table', 'subset':'query', 'identifier':'pid'}
        rows = pd.DataFrame({'id':[1,2], 'value':[10,20]})
        def request(**kw):
            path = kw['url'].split(self.runner.endpoint)[1]
            if kw['method'].lower() == 'delete': return self.response(202)
            if kw['method'].lower() == 'post': return self.response(201 if writable else 403)
            if path == '/api/v1/database/db/table': return self.response(body=[{'id':'table'}] if listed else [])
            if path.startswith('/api/v1/identifier'):
                return self.response(body={'id':'pid','database_id':'db','query_id':'query'})
            if contract == 'physical': return self.response(404)
            return self.response(body={'id':'table','archived_at':'2026-10-03T10:00:00Z' if archived else None})
        self.runner.original_request = request
        def call(method, *a, **kw):
            return rows.copy() if method in ('get_subset_data','get_identifier_data') else None
        with patch.object(requests, 'request', self.runner.transport):
            self.runner.table_deletion_steps(self.runner.step, call, 'db', 'table', 'query')
        return {r['case']:r['status'] for r in self.runner.results}

    def test_physical_removal_requires_absence_from_active_list(self):
        self.assertEqual('passed', self.removal_run('physical')['table.deleted'])
        self.runner.results = []
        self.assertEqual('failed', self.removal_run('physical', listed=True)['table.deleted'])

    def test_archive_checks_identity_timestamp_read_only_and_history(self):
        outcomes = self.removal_run('archive')
        self.assertTrue(all(status == 'passed' for status in outcomes.values()), outcomes)
        self.assertIn('identifier.archived.data', outcomes)
        self.runner.results = []
        outcomes = self.removal_run('archive', archived=False, writable=True)
        self.assertEqual('failed', outcomes['table.deleted'])
        self.assertEqual('failed', outcomes['table.archive.write.denied'])
        self.assertEqual('skipped', outcomes['table.archive.timestamp'])

    def test_comparison_reports_explicit_deletion_contract_difference(self):
        spec = importlib.util.spec_from_file_location('compare_contract', live.HERE/'client-live-compare.py')
        module = importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
        a = {'completed':True,'client_sha256':'same','profile':'user',
             'contracts':{'table_deletion':'physical'},'cases':[{'case':'table.deleted','status':'passed'}]}
        b = {**a, 'contracts':{'table_deletion':'archive'}}
        result = module.compare(a,b)
        self.assertFalse(result['contracts_match'])
        self.assertEqual('contract_mismatch', result['cases'][0]['state'])

    def test_inventory_matches_current_source(self):
        root=Path(__file__).resolve().parents[1]
        self.assertEqual(json.loads((live.HERE/'client-live-endpoints.json').read_text()),live.source_inventory(root))


if __name__=='__main__':
    unittest.main()

#!/usr/bin/env python3
"""Check fresh-install error topology without using deployment credentials."""
import json
from pathlib import Path
import subprocess
import yaml

ROOT = Path(__file__).resolve().parents[1]


def check(definitions):
    source = next(v for v in definitions['queues'] if v['name']=='dbrepo')
    assert source['arguments']=={'x-queue-type':'quorum'}, 'Source queue arguments must remain compatible'
    error = next(v for v in definitions['queues'] if v['name']=='dbrepo.consumer.errors')
    assert error['durable'] and error['arguments']['x-queue-type']=='quorum'
    policy = next(v for v in definitions['policies'] if v['name']=='dbrepo-consumer-errors')
    assert policy['pattern']=='^dbrepo$' and policy['apply-to']=='queues'
    assert policy['definition']=={'dead-letter-exchange':'dbrepo.consumer.errors',
        'dead-letter-routing-key':'failed','dead-letter-strategy':'at-least-once',
        'overflow':'reject-publish','delivery-limit':10}
    assert any(v['source']=='dbrepo.consumer.errors' and v['destination']=='dbrepo.consumer.errors'
               and v['routing_key']=='failed' for v in definitions['bindings'])


def main():
    for file in ['dbrepo-broker-service/definitions.json', '.live/config/includes.chroot_after_packages/etc/skel/config/definitions.json']:
        check(json.loads((ROOT/file).read_text()))
    # UI values are explicit disposable fixtures if the independent session-secret patch is present.
    result = subprocess.run(['helm','template','error-check',str(ROOT/'helm/dbrepo'),
        '--set-string','ui.oidc.tokenKey=data:;base64\\,'+'A'*43+'=',
        '--set-string','ui.oidc.authSessionSecret='+'synthetic-auth-'+'a'*48,
        '--set-string','ui.oidc.sessionSecret='+'synthetic-session-'+'b'*48],capture_output=True,text=True)
    assert result.returncode==0, 'Helm rendering failed'
    secret = next(v['stringData'] for v in yaml.safe_load_all(result.stdout)
                  if v and v.get('kind')=='Secret' and v['metadata']['name']=='broker-service-secret')
    check(json.loads(secret['load_definition.json']))
    for value in [secret['advanced.config'],(ROOT/'dbrepo-broker-service/advanced.config').read_text(),
                  (ROOT/'.live/config/includes.chroot_after_packages/etc/skel/config/advanced.config').read_text()]:
        assert "{'not', {match" in value and '^dbrepo[.]consumer[.]errors($|[.])' in value
    print('PASS: Compose, live and Helm retain source arguments and protect the durable error topology')


if __name__=='__main__':main()

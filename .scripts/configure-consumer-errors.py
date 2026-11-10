#!/usr/bin/env python3
"""Add the Consumer error topology to an existing quorum queue without replacing it."""
import argparse
import base64
import json
from pathlib import Path
import re
import time
from urllib.error import HTTPError
from urllib.parse import quote, urlsplit
from urllib.request import Request, urlopen

ERROR = 'dbrepo.consumer.errors'
POLICY = 'dbrepo-consumer-errors'


def configure(config):
    endpoint = config['endpoint'].rstrip('/')
    parsed = urlsplit(endpoint)
    if parsed.scheme not in ('http', 'https') or not parsed.hostname or parsed.username or parsed.password or parsed.query or parsed.fragment:
        raise ValueError('Use a broker management HTTP(S) base URL without embedded credentials')
    authorization = 'Basic ' + base64.b64encode((config['username']+':'+config['password']).encode()).decode()
    vhost = quote(config.get('vhost', 'dbrepo'), safe='')
    queue = config.get('queue', 'dbrepo')

    def api(method, path, body=None, missing_ok=False):
        request = Request(endpoint+'/api/'+path, method=method,
            data=json.dumps(body).encode() if body is not None else None,
            headers={'Authorization': authorization, 'Content-Type':'application/json'})
        try:
            with urlopen(request, timeout=30) as response:
                data = response.read()
                return json.loads(data) if data else None
        except HTTPError as error:
            if missing_ok and error.code == 404:
                return None
            # Broker response bodies can include details that do not belong in operator logs.
            raise RuntimeError('Broker management request failed (HTTP '+str(error.code)+')') from None

    source = api('GET', 'queues/'+vhost+'/'+quote(queue, safe=''))
    if source.get('type') != 'quorum':
        raise ValueError('Consumer source queue must already be a quorum queue; do not recreate it')
    if source.get('policy') not in (None, '', POLICY):
        raise ValueError('Merge the error settings into the existing queue policy manually; it was not replaced')
    if source.get('operator_policy'):
        raise ValueError('Resolve the existing operator policy manually before enabling error retention')
    definition = {'dead-letter-exchange':ERROR, 'dead-letter-routing-key':'failed',
                  'dead-letter-strategy':'at-least-once', 'overflow':'reject-publish', 'delivery-limit':10}
    if any(source.get('arguments', {}).get('x-'+key, value) != value for key,value in definition.items()):
        raise ValueError('Source queue arguments override the required error policy; the queue was not changed')
    extra = {}
    pattern = '^'+re.escape(queue)+'$'
    existing = api('GET', 'policies/'+vhost+'/'+POLICY, missing_ok=True)
    if existing is not None:
        if existing['pattern'] != pattern or existing['apply-to'] != 'queues':
            raise ValueError('The existing named policy has a different scope; merge it manually')
        extra = existing['definition']
    flags = api('GET', 'feature-flags')
    if not any(flag['name']=='stream_queue' and flag['state']=='enabled' for flag in flags):
        raise ValueError('At-least-once dead lettering requires the existing stream_queue feature flag')
    api('PUT', 'exchanges/'+vhost+'/'+ERROR,
        {'type':'direct', 'durable':True, 'auto_delete':False, 'internal':False, 'arguments':{}})
    api('PUT', 'queues/'+vhost+'/'+ERROR,
        {'durable':True, 'auto_delete':False, 'arguments':{'x-queue-type':'quorum'}})
    api('POST', 'bindings/'+vhost+'/e/'+ERROR+'/q/'+ERROR, {'routing_key':'failed', 'arguments':{}})
    definition = {**extra, **definition}
    api('PUT', 'policies/'+vhost+'/'+POLICY,
        {'pattern':pattern, 'apply-to':'queues', 'priority':100, 'definition':definition})
    # RabbitMQ 3.13 omits effective_policy_definition from this response. Verify
    # the active policy and its definition after rejecting argument/operator overrides.
    for attempt in range(20):
        active = api('GET', 'queues/'+vhost+'/'+quote(queue, safe=''))
        if active.get('policy') == POLICY:
            break
        time.sleep(.5)
    else:
        raise RuntimeError('The broker did not activate the error policy')
    applied = api('GET', 'policies/'+vhost+'/'+POLICY)['definition']
    if any(applied.get(key) != value for key,value in definition.items()):
        raise RuntimeError('The applied broker policy differs; resolve it before starting Consumer')
    print('Consumer error exchange, quorum queue and at-least-once policy configured; source queue retained.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', required=True, type=Path,
                        help='Protected JSON containing endpoint, username, password and optional vhost/queue')
    args = parser.parse_args()
    configure(json.loads(args.config.read_text()))

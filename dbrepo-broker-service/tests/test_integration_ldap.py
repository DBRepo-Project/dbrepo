#!/usr/bin/env python3
"""Check the shipped broker configuration with disposable LDAP and AMQP services."""
import hashlib
import json
import os
from pathlib import Path
import secrets
import subprocess
import tempfile
import time
import base64

import pandas as pd
import pika
from dbrepo.AmqpClient import AmqpClient

ROOT = Path(__file__).resolve().parents[2]


def docker(*args, env=None, timeout=120):
    # Keep command output private: Docker and LDAP errors can include credentials.
    result = subprocess.run(['docker', *args], capture_output=True, text=True,
                            env={**os.environ, **(env or {})}, timeout=timeout)
    if result.returncode:
        raise RuntimeError('Docker operation failed: ' + args[0])
    return result.stdout.strip()


def ready(name, command):
    deadline = time.monotonic() + 120
    while time.monotonic() < deadline:
        try:
            docker('exec', name, *command, timeout=15)
            return
        except (RuntimeError, subprocess.TimeoutExpired):
            time.sleep(1)
    raise RuntimeError('Disposable service did not become ready')


def main():
    suffix = secrets.token_hex(5)
    network, ldap, broker = ['dbrepo-ldap-test-' + suffix + s for s in ('', '-ldap', '-broker')]
    password, bind_password, internal_password = [secrets.token_urlsafe(24) for _ in range(3)]
    salt = secrets.token_bytes(4)
    password_hash = base64.b64encode(salt + hashlib.sha256(salt + internal_password.encode()).digest()).decode()
    with tempfile.TemporaryDirectory(prefix='dbrepo-ldap-test-') as directory:
        definitions = Path(directory) / 'definitions.json'
        definitions.write_text(json.dumps({
            'users': [{'name':'fixture-reader', 'password_hash':password_hash,
                       'hashing_algorithm':'rabbit_password_hashing_sha256', 'tags':[]}],
            'vhosts':[{'name':'dbrepo'}],
            'permissions':[{'user':'fixture-reader','vhost':'dbrepo','configure':'.*','write':'.*','read':'.*'}],
            'exchanges':[{'name':'dbrepo','vhost':'dbrepo','type':'topic','durable':True,'auto_delete':False,'arguments':{}}],
            'queues':[{'name':'probe','vhost':'dbrepo','durable':False,'auto_delete':False,'arguments':{}}],
            'bindings':[{'source':'dbrepo','vhost':'dbrepo','destination':'probe','destination_type':'queue',
                         'routing_key':'dbrepo.#','arguments':{}}]}))
        definitions.chmod(0o644)
        mounts = []
        for name in ('rabbitmq.conf','advanced.config','enabled_plugins'):
            copy = Path(directory) / name
            copy.write_bytes((ROOT/'dbrepo-broker-service'/name).read_bytes())
            copy.chmod(0o644)
            mounts.append((copy, '/etc/rabbitmq/'+name))
        mounts.append((definitions, '/app/definitions.json'))
        try:
            docker('network', 'create', network)
            ldap_env = {'LDAP_ROOT':'dc=dbrepo,dc=at', 'LDAP_ADMIN_PASSWORD':bind_password,
                        'LDAP_ADMIN_DN':'cn=admin,dc=dbrepo,dc=at', 'LDAP_USERS':'publisher', 'LDAP_PASSWORDS':password}
            docker('run', '-d', '--name', ldap, '--network', network, '--network-alias', 'identity-service',
                   *[arg for key in ldap_env for arg in ('--env', key)],
                   'docker.io/bitnamilegacy/openldap:2.6.8', env=ldap_env)
            ready(ldap, ['sh', '-c', 'ldapwhoami -x -H ldap://localhost:1389 -D "$LDAP_ADMIN_DN" -w "$LDAP_ADMIN_PASSWORD"'])
            broker_env = {'RABBITMQ_LDAP_ADMIN_DN':ldap_env['LDAP_ADMIN_DN'],
                          'RABBITMQ_LDAP_ADMIN_PASSWORD':bind_password,
                          'RABBITMQ_USERNAME':'fixture-reader', 'RABBITMQ_PASSWORD':internal_password}
            docker('run', '-d', '--name', broker, '--network', network, '-p', '127.0.0.1::5672',
                   *[arg for source,target in mounts for arg in ('-v', str(source)+':'+target+':ro')],
                   *[arg for key in broker_env for arg in ('--env', key)],
                   'docker.io/bitnamilegacy/rabbitmq:3.13.7', env=broker_env)
            ready(broker, ['rabbitmq-diagnostics','-q','check_running'])
            port = int(docker('port', broker, '5672/tcp').rsplit(':', 1)[1])
            # Environment overrides must not substitute another publishing account.
            for key in list(os.environ):
                if key.startswith('AMQP_API_'): del os.environ[key]
            publisher = AmqpClient(broker_host='127.0.0.1', broker_port=port, username='publisher', password=password)
            publisher.publish('dbrepo.fixture.table', {'id':1, 'value':10})
            publisher.publish_dataframe('dbrepo.fixture.table', pd.DataFrame([{'id':2,'value':20}]))
            connection = pika.BlockingConnection(pika.ConnectionParameters(host='127.0.0.1', port=port,
                virtual_host='dbrepo', credentials=pika.PlainCredentials('fixture-reader', internal_password)))
            try:
                channel = connection.channel()
                rows = []
                for _ in range(2):
                    method, _, body = channel.basic_get('probe', auto_ack=True)
                    assert method is not None, 'A published row did not arrive'
                    rows.append(json.loads(body))
                assert rows == [{'id':1,'value':10}, {'id':2,'value':20}], 'Published rows differ'
            finally:
                connection.close()
            rejected = False
            try:
                AmqpClient(broker_host='127.0.0.1', broker_port=port, username='publisher',
                           password=secrets.token_urlsafe(24)).publish('dbrepo.fixture.table', {'id':3})
            except pika.exceptions.ProbableAuthenticationError:
                rejected = True
            assert rejected, 'Invalid credentials were accepted'
            print('LDAP publisher: single row and dataframe received; invalid credentials rejected')
        finally:
            for name in (broker, ldap):
                subprocess.run(['docker','rm','-f','-v',name], capture_output=True, timeout=30)
            subprocess.run(['docker','network','rm',network], capture_output=True, timeout=30)


if __name__ == '__main__':
    import logging
    logging.disable(logging.CRITICAL)
    main()
